package com.yidaxiong.app.detection.frame

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * 帧预处理器
 *
 * 负责 CameraX 帧 → Bitmap 的转换、旋转、镜像。
 * 提供帧率控制（跳帧机制），控制推理频率。
 */
class FramePreprocessor @Inject constructor() {

    companion object {
        private const val TAG = "FramePreprocessor"
        const val TARGET_WIDTH = 640
        const val TARGET_HEIGHT = 480
        /** 每隔 N 帧推理一次，控制 FPS */
        const val INFERENCE_INTERVAL = 3
    }

    private var frameCount = 0

    /**
     * 是否应处理当前帧（控制推理频率）
     */
    fun shouldProcessFrame(): Boolean {
        frameCount++
        return frameCount % INFERENCE_INTERVAL == 1
    }

    /**
     * 预处理 CameraX 帧 → 旋转/镜像后的 Bitmap
     */
    fun preprocess(
        imageProxy: ImageProxy,
        rotationDegrees: Int = 0,
        isFrontCamera: Boolean = true
    ): Bitmap? {
        return try {
            val bitmap = imageProxyToBitmap(imageProxy) ?: return null

            val rotatedBitmap = if (rotationDegrees != 0 || isFrontCamera) {
                rotateAndFlipBitmap(bitmap, rotationDegrees, isFrontCamera)
            } else {
                bitmap
            }

            // 如果旋转产生了新 Bitmap，回收原图
            if (rotatedBitmap !== bitmap) bitmap.recycle()

            rotatedBitmap
        } catch (e: Throwable) {
            Log.e(TAG, "预处理失败: ${e.message}", e)
            null
        } finally {
            imageProxy.close()
        }
    }

    /**
     * 将 CameraX ImageProxy (YUV_420_888) 转换为 Bitmap
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null
        val format = image.format

        return when (format) {
            ImageFormat.YUV_420_888 -> yuv420888ToBitmap(image)
            ImageFormat.NV21 -> nv21ToBitmap(image, imageProxy.width, imageProxy.height)
            else -> {
                Log.w(TAG, "不支持的图像格式: $format")
                null
            }
        }
    }

    /**
     * YUV_420_888 → NV21 → JPEG → Bitmap
     *
     * 两步转换确保正确的颜色还原。
     * YUV_420_888 是 CameraX 默认输出格式。
     */
    private fun yuv420888ToBitmap(image: android.media.Image): Bitmap? {
        val planes = image.planes
        val width = image.width
        val height = image.height

        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride

        val yPixelStride = yPlane.pixelStride
        val uPixelStride = uPlane.pixelStride

        // NV21 缓冲区大小：Y + VU 交错
        val ySize = yRowStride * height
        val uvSize = width * height / 2
        val nv21 = ByteArray(ySize + uvSize)

        // 复制 Y 平面（处理 rowStride > width 的 padding）
        for (row in 0 until height) {
            yBuffer.position(row * yRowStride)
            yBuffer.get(nv21, row * width, width)
        }

        // 复制 UV 平面（YUV_420_888 中 V 在前，U 在后，转为 NV21 的 VU 交错）
        val uvHeight = height / 2
        for (row in 0 until uvHeight) {
            val uvRowOffset = ySize + row * width
            for (col in 0 until width / 2) {
                val uPos = row * uRowStride + col * uPixelStride
                val vPos = row * vRowStride + col * uPixelStride // vPixelStride same as u
                val v = vBuffer.get(vPos).toInt() and 0xFF
                val u = uBuffer.get(uPos).toInt() and 0xFF
                nv21[uvRowOffset + col * 2] = v.toByte()      // V first
                nv21[uvRowOffset + col * 2 + 1] = u.toByte()  // U second
            }
        }

        return nv21ToBitmap(nv21, width, height)
    }

    /**
     * NV21 → JPEG 压缩 → Bitmap
     *
     * 使用 Android 内置 YuvImage 进行正确色彩转换。
     */
    private fun nv21ToBitmap(data: ByteArray, width: Int, height: Int): Bitmap? {
        return try {
            val yuvImage = YuvImage(data, ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
            val jpegBytes = out.toByteArray()
            android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "NV21→Bitmap 转换失败: ${e.message}", e)
            null
        }
    }

    private fun nv21ToBitmap(image: android.media.Image, width: Int, height: Int): Bitmap? {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val vuBuffer = planes[2].buffer // NV21 中 VU 在 plane[2]

        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()
        val nv21 = ByteArray(ySize + vuSize)

        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)

        return nv21ToBitmap(nv21, width, height)
    }

    /**
     * 旋转和镜像 Bitmap
     */
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
