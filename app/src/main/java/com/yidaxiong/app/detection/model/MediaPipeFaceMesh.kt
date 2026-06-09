package com.yidaxiong.app.detection.model

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaPipeFaceMesh @Inject constructor(@ApplicationContext private val context: Context) {
    companion object { private const val TAG = "FaceMesh"; private const val MODEL = "face_landmarker.task" }
    private var landmarker: FaceLandmarker? = null
    val isInitialized: Boolean get() = landmarker != null

    fun initialize(): Boolean {
        if (landmarker != null) return true
        return try {
            val opts = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL).setDelegate(Delegate.CPU).build())
                .setRunningMode(RunningMode.IMAGE).setNumFaces(1)
                .setMinFaceDetectionConfidence(0.5f).setMinFacePresenceConfidence(0.5f).build()
            landmarker = FaceLandmarker.createFromOptions(context, opts)
            Log.i(TAG, "加载成功"); true
        } catch (e: Throwable) { Log.e(TAG, "加载失败: ${e.message}", e); landmarker = null; false }
    }

    fun detect(bitmap: android.graphics.Bitmap): FaceLandmarkerResult? {
        val lm = landmarker ?: return null
        return try { lm.detect(BitmapImageBuilder(bitmap).build()) } catch (e: Exception) { Log.e(TAG, "推理失败: ${e.message}", e); null }
    }

    fun close() { landmarker?.close(); landmarker = null; Log.i(TAG, "已释放") }
}
