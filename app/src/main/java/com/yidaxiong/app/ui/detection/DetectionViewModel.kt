package com.yidaxiong.app.ui.detection

import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yidaxiong.app.detection.DetectionEngine
import com.yidaxiong.app.detection.frame.FramePreprocessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetectionUiState(
    val isDetecting: Boolean = false,
    val isModelReady: Boolean = false,
    val postureStatus: String = "--",
    val focusStatus: String = "--",
    val elapsedTime: String = "00:00:00",
    val alertMessage: String? = null
)

@HiltViewModel
class DetectionViewModel @Inject constructor(
    private val detectionEngine: DetectionEngine,
    private val preprocessor: FramePreprocessor
) : ViewModel() {
    companion object { private const val TAG = "DetectionVM" }

    private val _uiState = MutableStateFlow(DetectionUiState())
    val uiState: StateFlow<DetectionUiState> = _uiState.asStateFlow()
    private var timerJob: Job? = null
    private var startTimeMs = 0L

    init {
        // 收集检测引擎状态
        viewModelScope.launch {
            detectionEngine.detectionState.collect { state ->
                _uiState.update { it.copy(
                    isDetecting = state.isDetecting,
                    isModelReady = state.isModelReady,
                    postureStatus = state.result.postureStatus.toDisplayString(),
                    focusStatus = state.result.focusStatus.toDisplayString(),
                    elapsedTime = formatElapsed(state.elapsedMs)
                )}
            }
        }
        // 收集违规提醒事件
        viewModelScope.launch {
            detectionEngine.alertEvents.collect { event ->
                _uiState.update { it.copy(alertMessage = event.message) }
            }
        }
    }

    /**
     * 初始化并启动检测引擎（加载模型 + 启动推理循环）
     */
    fun startDetection() {
        viewModelScope.launch(Dispatchers.IO) {
            val errMsg = detectionEngine.startDetection()
            if (errMsg == null) {
                startTimeMs = System.currentTimeMillis()
                startTimer()
                Log.i(TAG, "检测启动成功")
            } else {
                _uiState.update { it.copy(alertMessage = errMsg) }
                Log.e(TAG, "检测启动失败: $errMsg")
            }
        }
    }

    /**
     * 停止检测引擎
     */
    fun stopDetection() {
        viewModelScope.launch(Dispatchers.IO) {
            detectionEngine.stopDetection()
            timerJob?.cancel()
            preprocessor.reset()
            _uiState.update { DetectionUiState(isModelReady = it.isModelReady) }
            Log.i(TAG, "检测已停止")
        }
    }

    /**
     * 切换检测开关（暂停/继续）
     */
    fun toggleDetection() {
        if (_uiState.value.isDetecting) {
            stopDetection()
        } else {
            startDetection()
        }
    }

    /**
     * CameraX ImageAnalysis 帧回调入口
     */
    fun onCameraFrame(imageProxy: ImageProxy) {
        // 检测未运行时直接关闭帧
        if (!_uiState.value.isDetecting) {
            imageProxy.close()
            return
        }
        // 按设定间隔处理帧（跳过部分帧以控制推理频率）
        if (preprocessor.shouldProcessFrame()) {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val bitmap = preprocessor.preprocess(imageProxy, rotation, isFrontCamera = true)
            if (bitmap != null) {
                detectionEngine.onFrame(bitmap)
            }
        } else {
            imageProxy.close()
        }
    }

    /**
     * 关闭提醒横幅
     */
    fun onAlertDismissed() {
        _uiState.update { it.copy(alertMessage = null) }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update {
                    it.copy(elapsedTime = formatElapsed(System.currentTimeMillis() - startTimeMs))
                }
            }
        }
    }

    private fun formatElapsed(ms: Long): String {
        val s = (ms / 1000).toInt()
        return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }

    override fun onCleared() {
        super.onCleared()
        detectionEngine.destroy()
        timerJob?.cancel()
    }
}
