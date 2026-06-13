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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

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
        /** 期望推理帧率，用于帧数→秒数换算 */
        private const val FPS = 10
    }

    // ── FaceMesh 468 关键点索引 ──
    private object FaceLM {
        const val NOSE_TIP = 1
        const val NOSE_BRIDGE_TOP = 6       // 鼻梁上段（眉心之间偏下）
        const val FOREHEAD = 10             // 眉心上方前额
        const val NOSE_BRIDGE_BOT = 27      // 鼻梁下段（两眼之间）
        const val LEFT_EYE_OUTER = 33
        const val MOUTH_TOP = 13            // 上唇顶
        const val LEFT_EYE_TOP = 159
        const val LEFT_EYE_BOT = 145
        const val LEFT_EYE_INNER = 133
        const val CHIN = 152
        const val LEFT_EAR = 234
        const val RIGHT_EYE_OUTER = 263
        const val RIGHT_EYE_TOP = 386
        const val RIGHT_EYE_BOT = 374
        const val RIGHT_EYE_INNER = 362
        const val RIGHT_EAR = 454
    }

    private object PoseLM {
        const val LEFT_SHOULDER = 11
        const val RIGHT_SHOULDER = 12
    }

    private object HandLM {
        const val WRIST = 0
        const val THUMB_TIP = 4
        const val INDEX_TIP = 8
        const val INDEX_PIP = 6   // 食指中段，辅助判断
    }

    // ── 状态流 ──
    private val _detectionState = MutableStateFlow(DetectionState())
    val detectionState: StateFlow<DetectionState> = _detectionState.asStateFlow()
    val alertEvents: SharedFlow<AlertEvent> = alertManager.alertEvents

    // ── 帧处理 ──
    private var frameChannel: Channel<Bitmap>? = null
    private val engineScope = CoroutineScope(
        Dispatchers.Default + SupervisorJob() + CoroutineName("DetectionEngine")
    )
    private var frameProcessorJob: Job? = null

    // ── 违规计数器 ──
    private var postureViolationFrames = 0
    private var distractionViolationFrames = 0
    private var dazingFrames = 0
    private val postureThreshold = RuleConfig.postureViolationDuration * FPS
    private val distractionThreshold = RuleConfig.distractionViolationDuration * FPS
    private val dazingThreshold = RuleConfig.dazingDuration * FPS

    // ── 帧间状态 ──
    private var lastHeadPitch = 0f
    private var lastNoseBridgeAngle = 0f
    private var lastGazeX = 0f
    private var lastGazeY = 0f
    private var lastGazeNormX = 0.5f
    private var lastGazeNormY = 0.5f
    private var lastHandWristX = 0f
    private var lastHandWristY = 0f
    private var hasLastFrame = false
    private var startTimeMs = 0L

    // ── 手部速度环形缓冲区 ──
    private val handVelocityBuffer = FloatArray(RuleConfig.HAND_VELOCITY_BUFFER_SIZE)
    private var handVelBufIdx = 0
    private var handFrameCount = 0

    // ── 活跃度环形缓冲区（越大=越活跃） ──
    private val activityBuffer = FloatArray(RuleConfig.ACTIVITY_BUFFER_SIZE)
    private var activityBufIdx = 0
    private var activityCount = 0

    // ── 帧计数器 ──
    private var frameIndex = 0L

    fun startDetection(): String? {
        Log.i(TAG, "开始检测，加载模型...")
        alertManager.reset()
        val faceOk = faceMesh.initialize()
        val poseOk = poseLite.initialize()
        val handsOk = hands.initialize()
        if (!faceOk && !poseOk && !handsOk) {
            val msg = "模型加载失败（face/pose/hands 全部失败）"
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
        resetAll()
        startTimeMs = System.currentTimeMillis()
        frameChannel = Channel(capacity = Channel.CONFLATED)
        val ch = frameChannel!!
        _detectionState.value = DetectionState(isDetecting = true, isModelReady = true)
        frameProcessorJob = engineScope.launch { for (bm in ch) processFrame(bm) }
        return null
    }

    fun stopDetection() {
        Log.i(TAG, "停止检测")
        frameChannel?.close()
        runBlocking { frameProcessorJob?.join() }
        frameProcessorJob = null
        frameChannel = null
        _detectionState.value = DetectionState(isDetecting = false, isModelReady = false)
        faceMesh.close(); poseLite.close(); hands.close()
        alertManager.reset()
        resetAll()
    }

    fun onFrame(bitmap: Bitmap) { frameChannel?.trySend(bitmap) }

    fun destroy() { stopDetection(); engineScope.cancel() }

    // ═══════════════════════════════════════════════
    //  帧处理
    // ═══════════════════════════════════════════════

    private suspend fun processFrame(bitmap: Bitmap) {
        try {
            val faceResult = faceMesh.detect(bitmap)
            if (faceResult == null || faceResult.faceLandmarks().isEmpty()) {
                // 无人脸 → 降低活跃度（可视为"不在学习状态"）
                pushActivityScore(0f)
                return
            }

            val poseResult = poseLite.detect(bitmap)
            val handResult = hands.detect(bitmap)
            val faceLM = faceResult.faceLandmarks().get(0)

            val dr = extractResult(faceLM, poseResult, handResult)
            val ps = postureRule.evaluate(dr)
            val fs = distractionRule.evaluate(dr)
            val final = dr.copy(postureStatus = ps, focusStatus = fs)

            val elapsed = System.currentTimeMillis() - startTimeMs
            _detectionState.value = DetectionState(
                isDetecting = true, result = final,
                elapsedMs = elapsed, isModelReady = true
            )

            checkPosture(ps)
            checkDistraction(fs, final.activityScore)

            // 更新帧间状态
            lastHeadPitch = dr.headPitch
            lastNoseBridgeAngle = dr.noseBridgeAngle
            lastGazeX = dr.gazeVector.first
            lastGazeY = dr.gazeVector.second
            lastGazeNormX = dr.leftPupilOffset
            lastGazeNormY = dr.rightPupilOffset
            hasLastFrame = true
            frameIndex++

        } catch (e: Throwable) {
            Log.e(TAG, "帧处理异常: ${e.message}", e)
        } finally {
            bitmap.recycle()
        }
    }

    // ═══════════════════════════════════════════════
    //  特征提取（核心算法）
    // ═══════════════════════════════════════════════

    private fun extractResult(
        face: List<NormalizedLandmark>,
        pose: PoseLandmarkerResult?,
        hand: HandLandmarkerResult?
    ): DetectionResult {
        val dr = DetectionResult()

        // 1. 人脸包围盒 & 面积
        val (bboxW, bboxH, faceArea, cx) = computeFaceBBox(face)

        // 2. 头部俯仰角（低头检测）
        //   nose_chin_pitch: 鼻尖-下巴连线与垂直方向的夹角
        val nc = face[FaceLM.NOSE_TIP]; val chin = face[FaceLM.CHIN]
        val headPitch = Math.toDegrees(
            atan2(
                (nc.z() - chin.z()).toDouble(),
                (nc.y() - chin.y()).toDouble()
            )
        ).toFloat()

        // 3. 鼻梁俯仰角（辅助低头检测，对驼背更敏感）
        val nbTop = face[FaceLM.NOSE_BRIDGE_TOP]
        val nbBot = face[FaceLM.NOSE_BRIDGE_BOT]
        val noseBridgeAngle = Math.toDegrees(
            atan2(
                (nbTop.z() - nbBot.z()).toDouble(),
                (nbTop.y() - nbBot.y()).toDouble()
            )
        ).toFloat()

        // 4. 歪头：归一化双眼高度差
        //    使用内外眼角 Y 差 / 脸高，消除距离影响
        val leftEyeOuter = face[FaceLM.LEFT_EYE_OUTER]
        val leftEyeInner = face[FaceLM.LEFT_EYE_INNER]
        val rightEyeOuter = face[FaceLM.RIGHT_EYE_OUTER]
        val rightEyeInner = face[FaceLM.RIGHT_EYE_INNER]

        val leftEyeY = (leftEyeOuter.y() + leftEyeInner.y()) / 2f
        val rightEyeY = (rightEyeOuter.y() + rightEyeInner.y()) / 2f
        val eyeYDiff = abs(leftEyeY - rightEyeY)
        val normalizedEyeYDiff = if (bboxH > 0f) eyeYDiff / bboxH else 0f

        // 5. 耳朵 Y 差（保留，兼容旧逻辑）
        val leftEar = face.getOrNull(FaceLM.LEFT_EAR)
        val rightEar = face.getOrNull(FaceLM.RIGHT_EAR)
        val earYDiff = if (leftEar != null && rightEar != null) {
            abs(leftEar.y() - rightEar.y())
        } else 0f

        // 6. 视线方向：瞳孔在眼框中的位置
        val leftEyeTop = face[FaceLM.LEFT_EYE_TOP]
        val leftEyeBot = face[FaceLM.LEFT_EYE_BOT]
        val rightEyeTop = face[FaceLM.RIGHT_EYE_TOP]
        val rightEyeBot = face[FaceLM.RIGHT_EYE_BOT]

        // 瞳孔近似位置 = 眼裂中点
        val leftPupilX = (leftEyeOuter.x() + leftEyeInner.x()) / 2f
        val leftPupilY = (leftEyeTop.y() + leftEyeBot.y()) / 2f
        val rightPupilX = (rightEyeOuter.x() + rightEyeInner.x()) / 2f
        val rightPupilY = (rightEyeTop.y() + rightEyeBot.y()) / 2f

        // 瞳孔在眼框中的归一化偏移（0~1，0.5=正中）
        val eyeWidthL = abs(leftEyeOuter.x() - leftEyeInner.x())
        val eyeWidthR = abs(rightEyeOuter.x() - rightEyeInner.x())
        val leftPupilOffset = if (eyeWidthL > 0f) {
            (leftPupilX - minOf(leftEyeOuter.x(), leftEyeInner.x())) / eyeWidthL
        } else 0.5f
        val rightPupilOffset = if (eyeWidthR > 0f) {
            (rightPupilX - minOf(rightEyeOuter.x(), rightEyeInner.x())) / eyeWidthR
        } else 0.5f

        // 7. 视线向量（鼻尖→眼中心，综合头部转动+眼球方向）
        val eyeCenterX = (leftPupilX + rightPupilX) / 2f
        val eyeCenterY = (leftPupilY + rightPupilY) / 2f
        val nose = face[FaceLM.NOSE_TIP]
        val gazeVector = Pair(eyeCenterX - nose.x(), eyeCenterY - nose.y())

        // 8. 眼睛纵横比（EAR: Eye Aspect Ratio，用于检测眨眼）
        //    EAR = (|p2-p6| + |p3-p5|) / (2 * |p1-p4|)
        val leftEAR = computeEAR(
            leftEyeOuter, leftEyeInner, leftEyeTop, leftEyeBot
        )
        val rightEAR = computeEAR(
            rightEyeOuter, rightEyeInner, rightEyeTop, rightEyeBot
        )

        // 9. 肩膀倾斜
        val shoulderTilt = if (pose != null && pose.landmarks().isNotEmpty()) {
            val p = pose.landmarks()[0]
            abs(p[PoseLM.LEFT_SHOULDER].y() - p[PoseLM.RIGHT_SHOULDER].y())
        } else 0f

        // 10. 侧身综合评分 = 肩膀倾斜 + 头部偏移加权
        val headOffset = abs(cx - 0.5f)  // 头部中心偏离画面中线
        val combinedLeanScore = (shoulderTilt * 1.5f + headOffset * 0.5f)

        // 11. 手部特征
        var handMotionDelta = 0f
        var handPinchDistance = 0f
        var handFaceDistance = 1f
        var handVelocity = 0f
        var handCenterX = 0f
        var handCenterY = 1f

        if (hand != null && hand.landmarks().isNotEmpty()) {
            val h = hand.landmarks()[0]
            val wx = h[HandLM.WRIST].x()
            val wy = h[HandLM.WRIST].y()

            handCenterX = wx
            handCenterY = wy

            if (hasLastFrame) {
                handMotionDelta = abs(wx - lastHandWristX) + abs(wy - lastHandWristY)
            }
            lastHandWristX = wx
            lastHandWristY = wy

            // 拇指-食指捏合距离
            val thumbTip = h[HandLM.THUMB_TIP]
            val indexTip = h[HandLM.INDEX_TIP]
            val dx = thumbTip.x() - indexTip.x()
            val dy = thumbTip.y() - indexTip.y()
            handPinchDistance = sqrt(dx * dx + dy * dy)

            // 手到脸距离
            val faceDx = wx - cx
            val faceDy = wy - (eyeballCenterY(face) ?: 0.5f)
            handFaceDistance = sqrt(faceDx * faceDx + faceDy * faceDy)

            // 手部速度（归一化到脸宽消除距离影响）
            if (hasLastFrame && bboxW > 0f) {
                handVelocity = handMotionDelta / bboxW
            }

            // 写入环形缓冲区
            pushHandVelocity(handVelocity)
            handVelocity = averageHandVelocity()
        } else {
            // 手不可见 → push 0 速度
            pushHandVelocity(0f)
        }

        // 12. 帧间变化
        val headPitchDelta = if (hasLastFrame) abs(headPitch - lastHeadPitch) else 0f
        val gazeDelta = if (hasLastFrame) {
            abs(gazeVector.first - lastGazeX) + abs(gazeVector.second - lastGazeY)
        } else 0f

        // 13. 活跃度评分（用于发呆判定）
        val activityScore = computeActivityScore(
            headPitchDelta, gazeDelta, handVelocity, face, cx
        )
        pushActivityScore(activityScore)
        val smoothedActivity = averageActivityScore()

        return DetectionResult(
            headPitch = headPitch,
            noseBridgeAngle = noseBridgeAngle,
            headCenterX = cx,
            faceAreaRatio = faceArea,
            faceBboxWidth = bboxW,
            faceBboxHeight = bboxH,
            earYDiff = earYDiff,
            normalizedEyeYDiff = normalizedEyeYDiff,
            gazeVector = gazeVector,
            leftPupilOffset = leftPupilOffset,
            rightPupilOffset = rightPupilOffset,
            leftEyeAspectRatio = leftEAR,
            rightEyeAspectRatio = rightEAR,
            shoulderTilt = shoulderTilt,
            handMotionDelta = handMotionDelta,
            handVelocity = handVelocity,
            handPinchDistance = handPinchDistance,
            handFaceDistance = handFaceDistance,
            headPitchDelta = headPitchDelta,
            gazeDelta = gazeDelta,
            combinedLeanScore = combinedLeanScore,
            activityScore = smoothedActivity,
            handCenterX = handCenterX,
            handCenterY = handCenterY
        )
    }

    /**
     * 计算人脸包围盒
     * @return (bboxWidth, bboxHeight, area, centerX)
     */
    private fun computeFaceBBox(
        landmarks: List<NormalizedLandmark>
    ): Quadruple<Float, Float, Float, Float> {
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE

        // 遍历所有 468 个点找到完整人脸轮廓
        for (lm in landmarks) {
            if (lm.x() < minX) minX = lm.x()
            if (lm.y() < minY) minY = lm.y()
            if (lm.x() > maxX) maxX = lm.x()
            if (lm.y() > maxY) maxY = lm.y()
        }

        val w = maxX - minX
        val h = maxY - minY
        val area = w * h
        val cx = (minX + maxX) / 2f
        return Quadruple(w, h, area, cx)
    }

    /**
     * 计算眼睛纵横比 EAR（Eye Aspect Ratio）
     *
     * EAR = |eyeTop.y - eyeBot.y| / |eyeOuter.x - eyeInner.x|
     * 正常睁眼 ~0.3，闭眼 ~0.05
     */
    private fun computeEAR(
        outer: NormalizedLandmark,
        inner: NormalizedLandmark,
        top: NormalizedLandmark,
        bot: NormalizedLandmark
    ): Float {
        val eyeWidth = abs(outer.x() - inner.x())
        if (eyeWidth < 0.001f) return 0.3f
        val eyeHeight = abs(top.y() - bot.y())
        return eyeHeight / eyeWidth
    }

    /**
     * 双眼中心 Y 坐标
     */
    private fun eyeballCenterY(face: List<NormalizedLandmark>): Float? {
        val le = face.getOrNull(FaceLM.LEFT_EYE_INNER) ?: return null
        val re = face.getOrNull(FaceLM.RIGHT_EYE_INNER) ?: return null
        return (le.y() + re.y()) / 2f
    }

    // ═══════════════════════════════════════════════
    //  手部速度缓冲区（环形，10帧）
    // ═══════════════════════════════════════════════

    private fun pushHandVelocity(v: Float) {
        handVelocityBuffer[handVelBufIdx] = v
        handVelBufIdx = (handVelBufIdx + 1) % RuleConfig.HAND_VELOCITY_BUFFER_SIZE
        if (handFrameCount < RuleConfig.HAND_VELOCITY_BUFFER_SIZE) handFrameCount++
    }

    private fun averageHandVelocity(): Float {
        if (handFrameCount == 0) return 0f
        var sum = 0f
        for (i in 0 until handFrameCount) sum += handVelocityBuffer[i]
        return sum / handFrameCount
    }

    // ═══════════════════════════════════════════════
    //  活跃度缓冲区（环形，30帧 ≈ 3秒）
    // ═══════════════════════════════════════════════

    /**
     * 计算单帧活跃度评分
     *
     * 综合头部运动、视线变化、手部运动，归一化到 0~1
     */
    private fun computeActivityScore(
        headPitchDelta: Float,
        gazeDelta: Float,
        handVel: Float,
        face: List<NormalizedLandmark>?,
        headCx: Float
    ): Float {
        // 头部运动分：俯仰角变化 > 2° → 活跃
        val headScore = minOf(headPitchDelta / 5f, 1f)

        // 视线变化分
        val gazeScore = minOf(gazeDelta / 0.05f, 1f)

        // 手部运动分
        val handScore = minOf(handVel / 0.05f, 1f)

        // 头部偏移（转头）分
        val headTurnScore = minOf(abs(headCx - 0.5f) * 4f, 1f)

        // 加权平均：头部运动权重最高
        return (headScore * 0.35f + gazeScore * 0.25f + handScore * 0.25f + headTurnScore * 0.15f)
    }

    private fun pushActivityScore(score: Float) {
        activityBuffer[activityBufIdx] = score
        activityBufIdx = (activityBufIdx + 1) % RuleConfig.ACTIVITY_BUFFER_SIZE
        if (activityCount < RuleConfig.ACTIVITY_BUFFER_SIZE) activityCount++
    }

    private fun averageActivityScore(): Float {
        if (activityCount == 0) return 0.5f  // 默认中等活跃
        var sum = 0f
        for (i in 0 until activityCount) sum += activityBuffer[i]
        return sum / activityCount
    }

    // ═══════════════════════════════════════════════
    //  违规判定
    // ═══════════════════════════════════════════════

    private fun checkPosture(s: PostureStatus) {
        if (s != PostureStatus.GOOD && s != PostureStatus.UNKNOWN) {
            postureViolationFrames++
            if (postureViolationFrames >= postureThreshold) {
                alertManager.triggerAlert(AlertType.POSTURE_VIOLATION, msgPosture(s))
                postureViolationFrames = 0
            }
        } else {
            postureViolationFrames = 0
        }
    }

    private fun checkDistraction(s: FocusStatus, activityScore: Float) {
        if (s == FocusStatus.DAZING) {
            // 发呆使用独立且更长的判定窗口
            dazingFrames++
            if (dazingFrames >= dazingThreshold) {
                alertManager.triggerAlert(AlertType.DISTRACTION_VIOLATION, msgDistraction(s))
                dazingFrames = 0
            }
        } else if (s != FocusStatus.FOCUSED && s != FocusStatus.UNKNOWN) {
            dazingFrames = 0
            distractionViolationFrames++
            if (distractionViolationFrames >= distractionThreshold) {
                alertManager.triggerAlert(AlertType.DISTRACTION_VIOLATION, msgDistraction(s))
                distractionViolationFrames = 0
            }
        } else {
            dazingFrames = 0
            distractionViolationFrames = 0
        }
    }

    private fun resetAll() {
        postureViolationFrames = 0
        distractionViolationFrames = 0
        dazingFrames = 0
        hasLastFrame = false
        frameIndex = 0L
        handFrameCount = 0
        handVelBufIdx = 0
        activityCount = 0
        activityBufIdx = 0
        handVelocityBuffer.fill(0f)
        activityBuffer.fill(0f)
    }

    private fun msgPosture(s: PostureStatus) = when (s) {
        PostureStatus.HEAD_DOWN -> "小朋友，抬起头来，坐直一点哦！"
        PostureStatus.HEAD_TILTED -> "小朋友，头不要歪哦，摆正坐好！"
        PostureStatus.TOO_CLOSE -> "离屏幕太近了，往后靠一点！"
        PostureStatus.LEANING -> "身体不要歪，坐正哦！"
        else -> "注意坐姿哦！"
    }

    private fun msgDistraction(s: FocusStatus) = when (s) {
        FocusStatus.GAZE_AWAY -> "小朋友，专心看屏幕，不要东张西望哦！"
        FocusStatus.FIDGETING -> "小手不要乱动，专心学习哦！"
        FocusStatus.PLAYING_OBJECT -> "不要玩文具，认真学习哦！"
        FocusStatus.DAZING -> "小朋友，不要发呆，快点学习吧！"
        else -> "专心学习哦！"
    }
}

/**
 * 四元组辅助类（替代 Pair/Triple 的不足）
 */
private data class Quadruple<A, B, C, D>(
    val first: A, val second: B, val third: C, val fourth: D
)
