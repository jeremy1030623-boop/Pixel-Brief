package com.example.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class MorningTtsManager(
    context: Context,
    private val onSpeakingDone: () -> Unit = {}
) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    init {
        // 初始化 Android 原生 TTS
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // 設定語言為繁體中文（或跟隨系統設定）
            val result = tts?.setLanguage(Locale.TRADITIONAL_CHINESE)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isReady = true
                // 可調語速（0.5 ~ 2.0），晨間建議 1.0 ~ 1.1 較為溫柔舒適
                tts?.setSpeechRate(1.0f) 

                // 設定播放進度監聽器
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    
                    override fun onDone(utteranceId: String?) {
                        mainHandler.post {
                            onSpeakingDone()
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        mainHandler.post {
                            onSpeakingDone()
                        }
                    }
                })
            }
        }
    }

    fun speak(text: String) {
        if (isReady) {
            // QUEUE_FLUSH 代表如果有播到一半的語音，直接覆蓋並播放最新的
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MorningBriefID")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}
