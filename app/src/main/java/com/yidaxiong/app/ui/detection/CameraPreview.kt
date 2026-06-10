package com.yidaxiong.app.ui.detection

import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yidaxiong.app.util.Constants
import java.util.concurrent.Executors

private const val TAG = "CameraPreview"

/**
 * CameraX 相机预览 Composable
 *
 * 显示前置摄像头实时画面，并通过 [onFrame] 回调输出 ImageAnalysis 帧。
 *
 * @param onFrame 每帧回调，接收 ImageProxy。调用方需负责 imageProxy.close()
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onFrame: (ImageProxy) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                // 设置缩放类型
                scaleType = PreviewView.ScaleType.FILL_CENTER
                // 关键：必须设置 implementationMode 以兼容 Compose
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // 前置摄像头选择器
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()

                // Preview use case
                val preview = Preview.Builder()
                    .setTargetResolution(
                        Size(Constants.CAMERA_RESOLUTION_WIDTH, Constants.CAMERA_RESOLUTION_HEIGHT)
                    )
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                // ImageAnalysis use case — 按帧回调
                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(
                        Size(Constants.CAMERA_RESOLUTION_WIDTH, Constants.CAMERA_RESOLUTION_HEIGHT)
                    )
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            onFrame(imageProxy)
                        }
                    }

                try {
                    // 解绑之前的所有用例再重新绑定
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    Log.i(TAG, "CameraX 绑定成功（前置摄像头）")
                } catch (e: Exception) {
                    Log.e(TAG, "CameraX 绑定失败: ${e.message}", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}
