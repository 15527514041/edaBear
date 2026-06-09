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
        viewModelScope.launch {
            detectionEngine.detectionState.collect { state ->
                _uiState.update { it.copy(isDetecting = state.isDetecting, isModelReady = state.isModelReady,
                    postureStatus = state.result.postureStatus.toDisplayString(),
                    focusStatus = state.result.focusStatus.toDisplayString(),
                    elapsedTime = formatElapsed(state.elapsedMs)) }
            }
        }
        viewModelScope.launch {
            detectionEngine.alertEvents.collect { event ->
                _uiState.update { it.copy(alertMessage = event.message) }
            }
        }
    }

    fun onCameraFrame(imageProxy: ImageProxy) {
        if (!_uiState.value.isDetecting) { imageProxy.close(); return }
        if (preprocessor.shouldProcessFrame()) {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val bitmap = preprocessor.preprocess(imageProxy, rotation, isFrontCamera = true)
            if (bitmap != null) detectionEngine.onFrame(bitmap)
        } else { imageProxy.close() }
    }

    fun toggleDetection() {
        if (_uiState.value.isDetecting) stopDetection() else startDetection()
    }

    private fun startDetection() {
        viewModelScope.launch(Dispatchers.IO) {
            val errMsg = detectionEngine.startDetection()
            if (errMsg == null) { startTimeMs = System.currentTimeMillis(); startTimer(); Log.i(TAG, "检测启动") }
            else { _uiState.update { it.copy(alertMessage = errMsg) }; Log.e(TAG, "启动失败: $errMsg") }
        }
    }

    private fun stopDetection() {
        viewModelScope.launch(Dispatchers.IO) {
            detectionEngine.stopDetection(); timerJob?.cancel(); preprocessor.reset()
            _uiState.update { DetectionUiState(isModelReady = it.isModelReady) }; Log.i(TAG, "检测停止") }
    }

    fun onAlertDismissed() { _uiState.update { it.copy(alertMessage = null) } }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) { delay(1000); _uiState.update { it.copy(elapsedTime = formatElapsed(System.currentTimeMillis() - startTimeMs)) } }
        }
    }

    private fun formatElapsed(ms: Long): String {
        val s = (ms / 1000).toInt()
        return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }

    override fun onCleared() { super.onCleared(); detectionEngine.stopDetection(); timerJob?.cancel() }
}
