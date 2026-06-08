package com.yidaxiong.app.detection.alert

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
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

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val initListener = OnInitListener { status ->
        isInitialized = (status == TextToSpeech.SUCCESS)
        tts?.language = Locale.CHINESE
    }

    fun init() {
        if (tts == null) {
            tts = TextToSpeech(context, initListener)
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
