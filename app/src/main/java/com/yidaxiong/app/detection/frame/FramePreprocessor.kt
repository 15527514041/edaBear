package com.yidaxiong.app.detection.frame

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import javax.inject.Inject

class FramePreprocessor @Inject constructor() {

    companion object {
        private const val TAG = "FramePreprocessor"
        const val TARGET_WIDTH = 640
        const val TARGET_HEIGHT = 480
        const val INFERENCE_INTERVAL = 3
    }

    private var frameCount = 0

    fun shouldProcessFrame(): Boolean {
        frameCount++
        return frameCount % INFERENCE_INTERVAL == 0
    }

    fun preprocess(
        imageProxy: ImageProxy,
        rotationDegrees: Int = 0,
        isFrontCamera: Boolean = true
    ): Bitmap? {
        return try {
            val width = imageProxy.width
            val height = imageProxy.height
            if (width <= 0 || height <= 0) {
                imageProxy.close()
                return null
            }
            val buffer: ByteBuffer = imageProxy.planes[0].buffer
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)

            val rotatedBitmap = if (rotationDegrees != 0 || isFrontCamera) {
                rotateAndFlipBitmap(bitmap, rotationDegrees, isFrontCamera)
            } else bitmap

            if (rotatedBitmap !== bitmap) bitmap.recycle()
            rotatedBitmap
        } catch (e: Exception) {
            Log.e(TAG, "预处理失败: ${e.message}", e)
            null
        } finally {
            imageProxy.close()
        }
    }

    private fun rotateAndFlipBitmap(
        source: Bitmap, rotationDegrees: Int, flipHorizontal: Boolean
    ): Bitmap {
        val matrix = Matrix()
        if (flipHorizontal) {
            matrix.postScale(-1f, 1f, source.width / 2f, source.height / 2f)
        }
        if (rotationDegrees != 0) {
            matrix.postRotate(rotationDegrees.toFloat())
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun reset() {
        frameCount = 0
    }
}
