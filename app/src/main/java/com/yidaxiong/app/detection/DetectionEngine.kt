package com.yidaxiong.app.detection

import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.yidaxiong.app.detection.alert.AlertManager
import com.yidaxiong.app.detection.model.MediaPipeFaceMesh
import com.yidaxiong.app.detection.model.MediaPipeHands
import com.yidaxiong.app.detection.model.MediaPipePoseLite
import com.yidaxiong.app.detection.rule.DistractionRuleEngine
import com.yidaxiong.app.detection.rule.PostureRuleEngine
import com.yidaxiong.app.detection.rule.RuleConfig
import com.yidaxiong.app.domain.model.DetectionResult
import com.yidaxiong.app.domain.model.FocusStatus
import com.yidaxiong.app.domain.model.PostureStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.atan2

data class DetectionState(
    val isDetecting: Boolean = false,
    val result: DetectionResult = DetectionResult(),
    val elapsedMs: Long = 0L,
    val isModelReady: Boolean = false
)

data class AlertEvent(
    val type: AlertType,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AlertType {
    POSTURE_VIOLATION,
    DISTRACTION_VIOLATION,
    RECOVERY
}

@Singleton
class DetectionEngine @Inject constructor(
    private val faceMesh: MediaPipeFaceMesh,
    private val poseLite: MediaPipePoseLite,
    private val hands: MediaPipeHands,
    private val postureRule: PostureRuleEngine,
    private val distractionRule: DistractionRuleEngine,
    private val alertManager: AlertManager
) {
    companion object {
        private const val TAG = "DetectionEngine"
        private const val FPS = 10
    }

    private object FaceLM {
        const val NOSE_TIP = 1; const val CHIN = 152
        const val LEFT_EAR = 234; const val RIGHT_EAR = 454
        const val LEFT_EYE_INNER = 133; const val RIGHT_EYE_INNER = 362
    }

    private object PoseLM {
        const val LEFT_SHOULDER = 11; const val RIGHT_SHOULDER = 12
    }

    private object HandLM {
        const val WRIST = 0; const val THUMB_TIP = 4; const val INDEX_TIP = 8
    }

    private val _detectionState = MutableStateFlow(DetectionState())
    val detectionState: StateFlow<DetectionState> = _detectionState.asStateFlow()

    private val _alertEvents = MutableSharedFlow<AlertEvent>(replay = 0)
    val alertEvents: SharedFlow<AlertEvent> = _alertEvents.asSharedFlow()

    private var frameChannel: Channel<Bitmap>? = null
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName("DetectionEngine"))
    private var frameProcessorJob: Job? = null

    private var postureViolationFrames = 0
    private var distractionViolationFrames = 0
    private val postureThreshold = RuleConfig.postureViolationDuration * FPS
    private val distractionThreshold = RuleConfig.distractionViolationDuration * FPS

    private var lastHeadPitch = 0f; private var lastGazeX = 0f; private var lastGazeY = 0f
    private var lastHandWristX = 0f; private var lastHandWristY = 0f
    private var hasLastFrame = false
    private var startTimeMs = 0L

    /**
     * @return null on success, error message string on failure
     */
    @Synchronized
    fun startDetection(): String? {
        Log.i(TAG, "开始检测，加载模型...")
        alertManager.reset()
        val faceOk = faceMesh.initialize()
        val poseOk = poseLite.initialize()
        val handsOk = hands.initialize()
        if (!faceOk && !poseOk && !handsOk) {
            val msg = "模型加载失败（face/pose/hands 全部失败），请确认：\n1. 模型文件在 assets/ 目录\n2. 设备架构支持 MediaPipe 原生库（需 arm64-v8a）"
            Log.e(TAG, msg)
            _detectionState.value = _detectionState.value.copy(isModelReady = false)
            return msg
        }
        val failed = mutableListOf<String>()
        if (!faceOk) failed.add("人脸")
        if (!poseOk) failed.add("姿态")
        if (!handsOk) failed.add("手势")
        if (failed.isNotEmpty()) {
            Log.w(TAG, "部分模型加载失败: ${failed.joinToString()}")
        }
        Log.i(TAG, "模型加载: face=$faceOk, pose=$poseOk, hands=$handsOk")
        resetCounters(); hasLastFrame = false; startTimeMs = System.currentTimeMillis()
        // 每次启动检测时重建 channel，避免复用导致的状态污染
        frameChannel = Channel(capacity = Channel.CONFLATED)
        val ch = frameChannel!!
        _detectionState.value = DetectionState(isDetecting = true, isModelReady = true)
        frameProcessorJob = engineScope.launch { for (bm in ch) processFrame(bm) }
        return null
    }

    @Synchronized
    fun stopDetection() {
        Log.i(TAG, "停止检测")
        // 先关闭 channel 让消费者协程结束，然后等它完成再关闭模型，避免竞态崩溃
        frameChannel?.close()
        runBlocking { frameProcessorJob?.join() }
        frameProcessorJob = null
        frameChannel = null
        _detectionState.value = DetectionState(isDetecting = false, isModelReady = false)
        faceMesh.close(); poseLite.close(); hands.close()
        alertManager.reset(); resetCounters()
    }

    fun onFrame(bitmap: Bitmap) { frameChannel?.trySend(bitmap) }

    fun destroy() { stopDetection(); engineScope.cancel() }

    private suspend fun processFrame(bitmap: Bitmap) {
        try {
            val faceResult = faceMesh.detect(bitmap)
            if (faceResult == null || faceResult.faceLandmarks().isEmpty()) { bitmap.recycle(); return }
            val poseResult = poseLite.detect(bitmap)
            val handResult = hands.detect(bitmap)
            val faceLM = faceResult.faceLandmarks().get(0)
            val dr = extractResult(faceLM, poseResult, handResult)
            val ps = postureRule.evaluate(dr); val fs = distractionRule.evaluate(dr)
            val final = dr.copy(postureStatus = ps, focusStatus = fs)
            val elapsed = System.currentTimeMillis() - startTimeMs
            _detectionState.value = DetectionState(isDetecting = true, result = final, elapsedMs = elapsed, isModelReady = true)
            checkPosture(ps); checkDistraction(fs)
            lastHeadPitch = dr.headPitch; lastGazeX = dr.gazeVector.first; lastGazeY = dr.gazeVector.second
            hasLastFrame = true
        } catch (e: Throwable) { Log.e(TAG, "帧处理异常: ${e.message}", e) }
        finally { bitmap.recycle() }
    }

    private fun extractResult(face: List<NormalizedLandmark>, pose: PoseLandmarkerResult?, hand: HandLandmarkerResult?): DetectionResult {
        val hp = Math.toDegrees(atan2((face[FaceLM.NOSE_TIP].z() - face[FaceLM.CHIN].z()).toDouble(), (face[FaceLM.NOSE_TIP].y() - face[FaceLM.CHIN].y()).toDouble())).toFloat()
        var minX=Float.MAX_VALUE;var minY=Float.MAX_VALUE;var maxX=Float.MIN_VALUE;var maxY=Float.MIN_VALUE
        for (lm in face) { if (lm.x()<minX) minX=lm.x(); if (lm.y()<minY) minY=lm.y(); if (lm.x()>maxX) maxX=lm.x(); if (lm.y()>maxY) maxY=lm.y() }
        val far = (maxX-minX)*(maxY-minY)
        val earDiff = abs(face[FaceLM.LEFT_EAR].y()-face[FaceLM.RIGHT_EAR].y())
        val le = face[FaceLM.LEFT_EYE_INNER]; val re = face[FaceLM.RIGHT_EYE_INNER]; val no = face[FaceLM.NOSE_TIP]
        val ecx = (le.x()+re.x())/2f; val ecy = (le.y()+re.y())/2f
        val gv = Pair(ecx-no.x(), ecy-no.y())
        val st = if (pose!=null&&pose.landmarks().isNotEmpty()) { val p=pose.landmarks()[0]; abs(p[PoseLM.LEFT_SHOULDER].y()-p[PoseLM.RIGHT_SHOULDER].y()) } else 0f
        var hmd=0f; var hpd=0f
        if (hand!=null&&hand.landmarks().isNotEmpty()) { val h=hand.landmarks()[0]; val wx=h[HandLM.WRIST].x();val wy=h[HandLM.WRIST].y()
            if (hasLastFrame) hmd=abs(wx-lastHandWristX)+abs(wy-lastHandWristY); lastHandWristX=wx;lastHandWristY=wy
            val tt=h[HandLM.THUMB_TIP];val it=h[HandLM.INDEX_TIP]; val dx=tt.x()-it.x();val dy=tt.y()-it.y(); hpd= kotlin.math.sqrt(dx*dx+dy*dy) }
        val hpd2=if(hasLastFrame) abs(hp-lastHeadPitch) else 0f
        val gd=if(hasLastFrame) abs(gv.first-lastGazeX)+abs(gv.second-lastGazeY) else 0f
        return DetectionResult(headPitch=hp,faceAreaRatio=far,shoulderTilt=st,earYDiff=earDiff,gazeVector=gv,handMotionDelta=hmd,handPinchDistance=hpd,headPitchDelta=hpd2,gazeDelta=gd)
    }

    private fun checkPosture(s: PostureStatus) {
        if (s!=PostureStatus.GOOD&&s!=PostureStatus.UNKNOWN) { postureViolationFrames++; if(postureViolationFrames>=postureThreshold){alertManager.triggerAlert(AlertType.POSTURE_VIOLATION,msgPosture(s));postureViolationFrames=0} }
        else postureViolationFrames=0
    }

    private fun checkDistraction(s: FocusStatus) {
        if (s!=FocusStatus.FOCUSED&&s!=FocusStatus.UNKNOWN) { distractionViolationFrames++; if(distractionViolationFrames>=distractionThreshold){alertManager.triggerAlert(AlertType.DISTRACTION_VIOLATION,msgDistraction(s));distractionViolationFrames=0} }
        else distractionViolationFrames=0
    }

    private fun resetCounters() { postureViolationFrames=0;distractionViolationFrames=0 }

    private fun msgPosture(s:PostureStatus)=when(s){PostureStatus.HEAD_DOWN->"小朋友，抬起头来，坐直一点哦！";PostureStatus.HEAD_TILTED->"小朋友，头不要歪哦，摆正坐好！";PostureStatus.TOO_CLOSE->"离屏幕太近了，往后靠一点！";PostureStatus.LEANING->"身体不要歪，坐正哦！";else->"注意坐姿哦！"}
    private fun msgDistraction(s:FocusStatus)=when(s){FocusStatus.GAZE_AWAY->"小朋友，专心看屏幕，不要东张西望哦！";FocusStatus.FIDGETING->"小手不要乱动，专心学习哦！";FocusStatus.PLAYING_OBJECT->"不要玩文具，认真学习哦！";FocusStatus.DAZING->"小朋友，不要发呆，快点学习吧！";else->"专心学习哦！"}
}
