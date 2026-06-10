package com.yidaxiong.app.detection.alert

import android.util.Log
import com.yidaxiong.app.detection.AlertEvent
import com.yidaxiong.app.detection.AlertType
import com.yidaxiong.app.detection.rule.RuleConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 提醒管理器
 *
 * 协调语音播报和 UI 弹窗提醒：
 * - 违规触发时调用 TtsManager.speak()
 * - 发送 AlertEvent 到 UI State
 * - 实现提醒冷却（AlertCooldown）
 */
@Singleton
class AlertManager @Inject constructor(
    private val ttsManager: TtsManager
) {

    companion object {
        private const val TAG = "AlertManager"
    }

    private val _alertEvents = MutableSharedFlow<AlertEvent>(replay = 0, extraBufferCapacity = 8)
    val alertEvents: SharedFlow<AlertEvent> = _alertEvents.asSharedFlow()

    private var lastAlertTime = 0L

    /**
     * 触发违规提醒
     */
    fun triggerAlert(type: AlertType, message: String) {
        val now = System.currentTimeMillis()
        val cooldownMs = RuleConfig.alertCooldownSeconds * 1000L

        // 冷却期内不重复提醒
        if (now - lastAlertTime < cooldownMs) {
            Log.d(TAG, "冷却期内，跳过提醒: $message")
            return
        }
        lastAlertTime = now

        val event = AlertEvent(type = type, message = message)
        Log.i(TAG, "触发提醒: type=$type, message=$message")

        // TTS 语音播报
        ttsManager.init()
        ttsManager.speak(message)

        // 发送事件到 UI
        _alertEvents.tryEmit(event)
    }

    /**
     * 重置提醒冷却（如停止检测时）
     */
    fun reset() {
        lastAlertTime = 0L
    }
}
