package com.yidaxiong.app.detection.model

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaPipePoseLite @Inject constructor(@ApplicationContext private val context: Context) {
    companion object { private const val TAG = "PoseLite"; private const val MODEL = "pose_landmarker_lite.task" }
    private var landmarker: PoseLandmarker? = null
    val isInitialized: Boolean get() = landmarker != null

    fun initialize(): Boolean {
        if (landmarker != null) return true
        return try {
            val opts = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL).setDelegate(Delegate.CPU).build())
                .setRunningMode(RunningMode.IMAGE).setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f).setMinPosePresenceConfidence(0.5f).build()
            landmarker = PoseLandmarker.createFromOptions(context, opts)
            Log.i(TAG, "加载成功"); true
        } catch (e: Exception) { Log.e(TAG, "加载失败: ${e.message}", e); landmarker = null; false }
    }

    fun detect(bitmap: android.graphics.Bitmap): PoseLandmarkerResult? {
        val lm = landmarker ?: return null
        return try { lm.detect(BitmapImageBuilder(bitmap).build()) } catch (e: Exception) { Log.e(TAG, "推理失败: ${e.message}", e); null }
    }

    fun close() { landmarker?.close(); landmarker = null; Log.i(TAG, "已释放") }
}
