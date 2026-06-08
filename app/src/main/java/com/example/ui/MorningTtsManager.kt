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
    private var currentLangCode = "zh_TW"

    init {
        // 初始化 Android 原生 TTS
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true
            setLanguage(currentLangCode)
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

    fun setLanguage(langCode: String) {
        currentLangCode = langCode
        if (isReady) {
            val actualCode = if (langCode == "system_default") Locale.getDefault().toString() else langCode
            val locale = when (actualCode) {
                "zh_TW" -> Locale.TRADITIONAL_CHINESE
                "zh_CN" -> Locale.SIMPLIFIED_CHINESE
                "en" -> Locale.US
                "ja" -> Locale.JAPANESE
                "zh_HK" -> Locale("zh", "HK")
                "ko" -> Locale.KOREAN
                "fr" -> Locale.FRANCE
                "de" -> Locale.GERMANY
                "es" -> Locale("es", "ES")
                "it" -> Locale.ITALY
                "ru" -> Locale("ru", "RU")
                "pt" -> Locale("pt", "PT")
                "ar" -> Locale("ar")
                "th" -> Locale("th", "TH")
                "vi" -> Locale("vi", "VN")
                "tr" -> Locale("tr", "TR")
                "hi" -> Locale("hi", "IN")
                "id" -> Locale("id", "ID")
                "ms" -> Locale("ms", "MY")
                "nl" -> Locale("nl", "NL")
                "sv" -> Locale("sv", "SE")
                "da" -> Locale("da", "DK")
                "fi" -> Locale("fi", "FI")
                "nb" -> Locale("nb", "NO")
                "pl" -> Locale("pl", "PL")
                "uk" -> Locale("uk", "UA")
                "cs" -> Locale("cs", "CZ")
                "el" -> Locale("el", "GR")
                "bg" -> Locale("bg", "BG")
                "ca" -> Locale("ca", "ES")
                "hr" -> Locale("hr", "HR")
                "hu" -> Locale("hu", "HU")
                "ro" -> Locale("ro", "RO")
                "sk" -> Locale("sk", "SK")
                else -> {
                    try {
                        val parts = actualCode.split("_")
                        if (parts.size == 2) {
                            Locale(parts[0], parts[1])
                        } else {
                            Locale(actualCode)
                        }
                    } catch (e: Exception) {
                        Locale.getDefault()
                    }
                }
            }
            tts?.setLanguage(locale)
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
