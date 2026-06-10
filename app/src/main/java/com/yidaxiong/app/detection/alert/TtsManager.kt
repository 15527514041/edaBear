package com.yidaxiong.app.detection.alert

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TTS 语音播报管理器
 *
 * 使用 Android 系统内置 TextToSpeech API，纯离线。
 */
@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "TtsManager"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val initListener = OnInitListener { status ->
        isInitialized = (status == TextToSpeech.SUCCESS)
        if (isInitialized) {
            tts?.language = Locale.CHINESE
            tts?.setSpeechRate(0.9f) // 稍慢语速，适合儿童
            Log.i(TAG, "TTS 初始化成功")
        } else {
            Log.e(TAG, "TTS 初始化失败，status=$status")
        }
    }

    /**
     * 初始化 TTS（幂等，可重复调用）
     */
    @Synchronized
    fun init() {
        if (tts == null) {
            tts = TextToSpeech(context, initListener)
        }
    }

    /**
     * 播报语音文本
     */
    @Synchronized
    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            Log.d(TAG, "TTS 播报: $text")
        } else {
            Log.w(TAG, "TTS 未就绪，无法播报: $text")
        }
    }

    /**
     * 停止当前播报
     */
    @Synchronized
    fun stop() {
        tts?.stop()
    }

    /**
     * 释放 TTS 资源
     */
    @Synchronized
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        Log.i(TAG, "TTS 已释放")
    }
}
