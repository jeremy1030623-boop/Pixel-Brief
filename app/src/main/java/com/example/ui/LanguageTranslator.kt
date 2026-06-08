package com.example.ui

import java.util.Locale

object LanguageTranslator {

    // 35 Supported Languages (System Default + 34 manual languages)
    val supportedLanguages = listOf(
        "system_default" to Pair("系統預設 (System Default)", "自動"),
        "zh_TW" to Pair("繁體中文 (台灣)", "繁中"),
        "zh_HK" to Pair("廣東話 (香港)", "粵語"),
        "zh_CN" to Pair("简体中文 (中国)", "简中"),
        "en" to Pair("English (US)", "EN"),
        "ja" to Pair("日本語", "日本語"),
        "ko" to Pair("한국어", "한국어"),
        "fr" to Pair("Français", "FR"),
        "de" to Pair("Deutsch", "DE"),
        "es" to Pair("Español", "ES"),
        "it" to Pair("Italiano", "IT"),
        "ru" to Pair("Русский", "RU"),
        "pt" to Pair("Português", "PT"),
        "ar" to Pair("العربية", "AR"),
        "th" to Pair("ไทย", "TH"),
        "vi" to Pair("Tiếng Việt", "VI"),
        "tr" to Pair("Türkçe", "TR"),
        "hi" to Pair("हिन्दी", "HI"),
        "id" to Pair("Bahasa Indonesia", "ID"),
        "ms" to Pair("Bahasa Melayu", "MS"),
        "nl" to Pair("Nederlands", "NL"),
        "sv" to Pair("Svenska", "SV"),
        "da" to Pair("Dansk", "DA"),
        "fi" to Pair("Suomi", "FI"),
        "nb" to Pair("Norsk Bokmål", "NB"),
        "pl" to Pair("Polski", "PL"),
        "uk" to Pair("Українська", "UK"),
        "cs" to Pair("Čeština", "CS"),
        "el" to Pair("Ελληνικά", "EL"),
        "bg" to Pair("Български", "BG"),
        "ca" to Pair("Català", "CA"),
        "hr" to Pair("Hrvatski", "HR"),
        "hu" to Pair("Magyar", "HU"),
        "ro" to Pair("Română", "RO"),
        "sk" to Pair("Slovenčina", "SK")
    )

    // Dynamic UI Translation Map
    private val translations = mapOf(
        "en" to mapOf(
            "app_title" to "pixel brief",
            "settings_title" to "Exclusive Settings",
            "save_button" to "Save",
            "back_button" to "Back",
            "section_1" to "I. Account & Login",
            "section_2" to "II. Weather & Location",
            "section_3" to "III. Sleep & Health",
            "section_4" to "IV. Activity & Calendar",
            "section_5" to "V. Daily News & On-Device/Cloud Gemini Settings",
            "section_6" to "VI. System & Permissions Integration",
            "section_7" to "VII. Privacy & Biometrics Protection",
            "today_weather" to "Today's Weather",
            "morning_calendar" to "Morning Calendar Schedule",
            "ai_briefing" to "On-Device/Cloud AI Morning Brief",
            "health_status" to "Health Status Monitoring",
            "breathing_monitor" to "Breathing Sound & Cough Sniffing",
            "daily_news" to "Daily News Feed",
            "daily_goal" to "Daily Recommended Goal",
            "toast_saved" to "Settings saved successfully!",
            "news_mode_local" to "Local News",
            "news_mode_international" to "Global News",
            "tts_label" to "Morning Brief Voice (TTS)",
            "tts_desc" to "Custom morning assistant voice translation for your localized voice reading experience.",
            "biometric_label" to "Enable Security Lock (Fingerprint/Face/PIN)",
            "biometric_desc" to "When enabled, unlocks biometric verification on launch and background resume."
        ),
        "ja" to mapOf(
            "app_title" to "pixel brief",
            "settings_title" to "専有設定",
            "save_button" to "保存",
            "back_button" to "戻る",
            "section_1" to "一、アカウントとログイン",
            "section_2" to "二、天気と位置",
            "section_3" to "三、睡眠と健康",
            "section_4" to "四、アクティビティとカレンダー",
            "section_5" to "五、ニュースとオンデバイス/クラウド Gemini 設定",
            "section_6" to "六、システムと権限統合",
            "section_7" to "七、プライバシーと生体認証保護",
            "today_weather" to "今日の天気",
            "morning_calendar" to "午前のカレンダースケジュール",
            "ai_briefing" to "オンデバイス/クラウド AI 午前ブリーフィング",
            "health_status" to "健康状態モニタリング",
            "breathing_monitor" to "呼吸音・咳簡易検出",
            "daily_news" to "今日のニュース",
            "daily_goal" to "本日のおすすめ目標",
            "toast_saved" to "設定を保存しました！",
            "news_mode_local" to "ローカルニュース",
            "news_mode_international" to "国際ニュース",
            "tts_label" to "読み上げ用音声 (TTS)",
            "tts_desc" to "パーソナライズされたアシスタントによるローカライズ音声読み上げ体験。"
        ),
        "ko" to mapOf(
            "app_title" to "pixel brief",
            "settings_title" to "전용 설정",
            "save_button" to "저장",
            "back_button" to "뒤로",
            "section_1" to "1. 계정 및 로그인",
            "section_2" to "2. 날씨 및 위치",
            "section_3" to "3. 수면 및 건강",
            "section_4" to "4. 활동 및 캘린더",
            "section_5" to "5. 뉴스 및 온디바이스/클라우드 Gemini 설정",
            "section_6" to "6. 시스템 및 권한 통합",
            "section_7" to "7. 개인정보 및 생체인식 보안",
            "today_weather" to "오늘의 날씨",
            "morning_calendar" to "오전 캘린더 일정",
            "ai_briefing" to "온디바이스/클라우드 AI 오전 브리핑",
            "health_status" to "건강 상태 모니터링",
            "breathing_monitor" to "호흡음 및 기침 간이 탐지",
            "daily_news" to "일일 뉴스 피드",
            "daily_goal" to "오늘의 추천 목표",
            "toast_saved" to "설정이 성공적으로 저장되었습니다!"
        ),
        "fr" to mapOf(
            "app_title" to "pixel brief",
            "settings_title" to "Paramètres Exclusifs",
            "save_button" to "Enregistrer",
            "back_button" to "Retour",
            "section_1" to "I. Compte & Connexion",
            "section_2" to "II. Météo & Localisation",
            "section_3" to "III. Sommeil & Santé",
            "section_4" to "IV. Activité & Calendrier",
            "section_5" to "V. Actualités & Configuration Gemini",
            "section_6" to "VI. Système & Intégration des Autorisations",
            "section_7" to "VII. Confidentialité & Protection Biométrique",
            "today_weather" to "Météo d'aujourd'hui",
            "morning_calendar" to "Calendrier du Matin",
            "ai_briefing" to "Briefing Matinal AI",
            "health_status" to "Moniteur de Santé",
            "breathing_monitor" to "Surveillance Respiratoire & Toux",
            "daily_news" to "Fil d'actualités",
            "daily_goal" to "Objectif Recommandé du Jour",
            "toast_saved" to "Paramètres enregistrés avec succès !"
        ),
        "de" to mapOf(
            "app_title" to "pixel brief",
            "settings_title" to "Exklusive Einstellungen",
            "save_button" to "Speichern",
            "back_button" to "Zurück",
            "section_1" to "I. Konto & Anmeldung",
            "section_2" to "II. Wetter & Standort",
            "section_3" to "III. Schlaf & Gesundheit",
            "section_4" to "IV. Aktivität & Kalender",
            "section_5" to "V. Nachrichten & Gemini-Einstellungen",
            "section_6" to "VI. System & Berechtigungsintegration",
            "section_7" to "VII. Datenschutz & biometrischer Schutz",
            "today_weather" to "Heutiges Wetter",
            "morning_calendar" to "Morgenkalender-Zeitplan",
            "ai_briefing" to "Morgendliche AI-Briefing-Zusammenfassung",
            "health_status" to "Gesundheitsstatus-Überwachung",
            "breathing_monitor" to "Atemton- & Hustenerkennung",
            "daily_news" to "Tägliche Nachrichten",
            "daily_goal" to "Täglich empfohlenes Ziel",
            "toast_saved" to "Einstellungen erfolgreich gespeichert!"
        ),
        "es" to mapOf(
            "app_title" to "pixel brief",
            "settings_title" to "Configuración Exclusiva",
            "save_button" to "Guardar",
            "back_button" to "Atrás",
            "section_1" to "I. Cuenta e Inicio de sesión",
            "section_2" to "II. Clima y Ubicación",
            "section_3" to "III. Sueño y Salud",
            "section_4" to "IV. Actividad y Calendario",
            "section_5" to "V. Noticias y Configuración de Gemini",
            "section_6" to "VI. Sistema e Integración de Permisos",
            "section_7" to "VII. Privacidad y Protección Biométrica",
            "today_weather" to "Clima de hoy",
            "morning_calendar" to "Horario del Calendario Matutino",
            "ai_briefing" to "Resumen Matutino de AI",
            "health_status" to "Seguimiento de Salud",
            "breathing_monitor" to "Monitor de Sonido de Respiración",
            "daily_news" to "Noticias Diarias",
            "daily_goal" to "Objetivo Recomendado de Hoy",
            "toast_saved" to "¡Configuración guardada correctamente!"
        )
    )

    /**
     * Get translated text string by key and fallback
     */
    fun get(key: String, lang: String): String {
        val actualLang = if (lang == "system_default") Locale.getDefault().toString() else lang
        
        // Simple direct translation fallbacks
        val langKey = if (actualLang.startsWith("zh")) {
            if (actualLang.contains("CN")) "zh_CN" else if (actualLang.contains("HK")) "zh_HK" else "zh_TW"
        } else {
            actualLang.substringBefore("_")
        }
        
        val localizedMap = translations[langKey]
        if (localizedMap != null) {
            val res = localizedMap[key]
            if (res != null) return res
        }

        // Default Traditional Chinese Translation fallback (original strings)
        return when (key) {
            "app_title" -> "pixel brief"
            "settings_title" -> "專屬設定"
            "save_button" -> "儲存"
            "back_button" -> "返回"
            "section_1" -> "一、帳號與登入"
            "section_2" -> "二、天氣與位置"
            "section_3" -> "三、睡眠與健康"
            "section_4" -> "四、活動與日曆"
            "section_5" -> "五、每日新聞與端側/雲端 Gemini 設定"
            "section_6" -> "六、系統與專屬權限整合"
            "section_7" -> "七、隱私與生物辨識防護"
            "today_weather" -> "今日氣象"
            "morning_calendar" -> "晨間行程行事曆"
            "ai_briefing" -> "端側/雲端 AI 晨間簡報摘要"
            "health_status" -> "健康狀態監測"
            "breathing_monitor" -> "睡眠呼吸紀錄讀取權限"
            "daily_news" -> "每日新聞資訊"
            "daily_goal" -> "每日推薦目標"
            "toast_saved" -> "設定已成功儲存！"
            "news_mode_local" -> "在地最新新聞"
            "news_mode_international" -> "國際焦點新聞"
            "tts_label" -> "語音簡報導讀語言 (TTS)"
            "tts_desc" -> "為您專屬訂製的晨間小助手語音朗讀，提供多國常用語言與地方特色語音導讀體驗。"
            "biometric_label" -> "啟用安全鎖 (指紋、面孔或裝置密碼)"
            "biometric_desc" -> "開啟後，每次開啟 App 或自背景返回時都會進行系統安全解鎖 verify，防護個人隱私與昨夜睡眠等敏感醫療與日程行程資訊安全"
            else -> key
        }
    }

    /**
     * Generate spoken speech briefing dynamically for any of the 34 supported languages
     */
    fun generateSpeechBrief(
        lang: String,
        username: String,
        greeting: String,
        time: String,
        condition: String,
        currentTemp: String,
        eventCount: Int
    ): String {
        val actualLang = if (lang == "system_default") Locale.getDefault().toString() else lang
        val cleanGreeting = greeting.replace(Regex("[🌅☀️🚀🍱☕🌌💤🦉]"), "").trim()
        
        return when (actualLang) {
            "en" -> {
                val engGreeting = when {
                    greeting.contains("早") -> "Good morning"
                    greeting.contains("午") -> "Good afternoon"
                    greeting.contains("晚") -> "Good evening"
                    else -> "Hello"
                }
                val weatherSection = if (condition.isNotEmpty()) "The weather is $condition with $currentTemp degrees." else ""
                val eventSection = if (eventCount > 0) "You have $eventCount events scheduled today." else "No events scheduled today."
                "$engGreeting, $username! $weatherSection $eventSection"
            }
            "zh_TW" -> {
                val weatherSection = if (condition.isNotEmpty()) "，今天天氣$condition，氣溫約 $currentTemp 度" else ""
                val eventSection = if (eventCount > 0) "，今天有 $eventCount 項行程" else "，今天沒有行程"
                "${cleanGreeting}，${username}！現在時間 ${time}${weatherSection}${eventSection}。"
            }
            "zh_HK" -> {
                val weatherSection = if (condition.isNotEmpty()) "，今日天氣$condition，氣溫約 $currentTemp 度" else ""
                val eventSection = if (eventCount > 0) "，今日有 $eventCount 項活動" else "，今日無日程"
                "${cleanGreeting}，${username}！現在時間 ${time}${weatherSection}${eventSection}。"
            }
            "zh_CN" -> {
                val weatherSection = if (condition.isNotEmpty()) "，今天天气$condition，气温约 $currentTemp 度" else ""
                val eventSection = if (eventCount > 0) "，今天有 $eventCount 项行程" else "，今天没有行程"
                "${cleanGreeting}，${username}！现在时间 ${time}${weatherSection}${eventSection}。"
            }
            "ja" -> {
                val jaGreeting = when {
                    greeting.contains("早") -> "おはようございます"
                    greeting.contains("午") -> "こんにちは"
                    greeting.contains("晚") -> "こんばんは"
                    else -> "こんにちは"
                }
                val weatherSection = if (condition.isNotEmpty()) "、今日の天気は $condition、気温は約 $currentTemp 度です" else ""
                val eventSection = if (eventCount > 0) "、本日は $eventCount 件の予定があります" else "、本日の予定はありません"
                "${jaGreeting}、${username}さん！${weatherSection}${eventSection}。"
            }
            "ko" -> {
                val koGreeting = when {
                    greeting.contains("早") -> "좋은 아침입니다"
                    greeting.contains("午") -> "안녕하세요"
                    greeting.contains("晚") -> "좋은 저녁입니다"
                    else -> "안녕하세요"
                }
                val weatherSection = if (condition.isNotEmpty()) "오늘의 날씨는 $condition, 기온은 약 $currentTemp 도입니다." else ""
                val eventSection = if (eventCount > 0) "오늘은 $eventCount 개의 일정이 있습니다." else "오늘의 일정은 없습니다."
                "${koGreeting}, ${username}님! $weatherSection $eventSection"
            }
            "fr" -> {
                val frGreeting = when {
                    greeting.contains("早") -> "Bonjour"
                    greeting.contains("午") -> "Bon après-midi"
                    greeting.contains("晚") -> "Bonsoir"
                    else -> "Bonjour"
                }
                val weatherSection = if (condition.isNotEmpty()) "Le temps est $condition avec $currentTemp degrés." else ""
                val eventSection = if (eventCount > 0) "Vous avez $eventCount événements aujourd'hui." else "Aucun événement prévu aujourd'hui."
                "$frGreeting, $username! $weatherSection $eventSection"
            }
            "de" -> {
                val deGreeting = when {
                    greeting.contains("早") -> "Guten Morgen"
                    greeting.contains("午") -> "Guten Tag"
                    greeting.contains("晚") -> "Guten Abend"
                    else -> "Hallo"
                }
                val weatherSection = if (condition.isNotEmpty()) "Das Wetter ist $condition bei $currentTemp Grad." else ""
                val eventSection = if (eventCount > 0) "Sie haben heute $eventCount Termine." else "Heute stehen keine Termine an."
                "$deGreeting, $username! $weatherSection $eventSection"
            }
            "es" -> {
                val esGreeting = when {
                    greeting.contains("早") -> "Buenos días"
                    greeting.contains("午") -> "Buenas tardes"
                    greeting.contains("晚") -> "Buenas noches"
                    else -> "Hola"
                }
                val weatherSection = if (condition.isNotEmpty()) "El clima es $condition con $currentTemp grados." else ""
                val eventSection = if (eventCount > 0) "Tienes $eventCount eventos programados para hoy." else "No hay eventos programados para hoy."
                "$esGreeting, $username! $weatherSection $eventSection"
            }
            "it" -> {
                val itGreeting = when {
                    greeting.contains("早") -> "Buongiorno"
                    greeting.contains("午") -> "Buon pomeriggio"
                    greeting.contains("晚") -> "Buonasera"
                    else -> "Ciao"
                }
                val weatherSection = if (condition.isNotEmpty()) "Il tempo è $condition con $currentTemp gradi." else ""
                val eventSection = if (eventCount > 0) "Hai $eventCount eventi in programma oggi." else "Nessun evento in programma oggi."
                "$itGreeting, $username! $weatherSection $eventSection"
            }
            "ru" -> {
                val ruGreeting = when {
                    greeting.contains("早") -> "Доброе утро"
                    greeting.contains("午") -> "Добрый день"
                    greeting.contains("晚") -> "Добрый вечер"
                    else -> "Привет"
                }
                val weatherSection = if (condition.isNotEmpty()) "Погода $condition, температура около $currentTemp градусов." else ""
                val eventSection = if (eventCount > 0) "У вас запланировано $eventCount событий на сегодня." else "На сегодня событий нет."
                "$ruGreeting, $username! $weatherSection $eventSection"
            }
            "pt" -> {
                val ptGreeting = when {
                    greeting.contains("早") -> "Bom dia"
                    greeting.contains("午") -> "Boa tarde"
                    greeting.contains("晚") -> "Boa noite"
                    else -> "Olá"
                }
                val weatherSection = if (condition.isNotEmpty()) "O clima está $condition con $currentTemp graus." else ""
                val eventSection = if (eventCount > 0) "Você tem $eventCount eventos hoje." else "Sem eventos para hoje."
                "$ptGreeting, $username! $weatherSection $eventSection"
            }
            "ar" -> {
                val arGreeting = when {
                    greeting.contains("早") -> "صباح الخير"
                    greeting.contains("午") -> "مساء الخير"
                    greeting.contains("晚") -> "مساء الخير"
                    else -> "مرحباً"
                }
                val weatherSection = if (condition.isNotEmpty()) "الطقس هو $condition مع $currentTemp درجة." else ""
                val eventSection = if (eventCount > 0) "لديك $eventCount أحداث اليوم." else "لا توجد أحداث مجдولة اليوم."
                "$arGreeting، $username! $weatherSection $eventSection"
            }
            "th" -> {
                val thGreeting = when {
                    greeting.contains("早") -> "อรุณสวัสดิ์"
                    greeting.contains("午") -> "สวัสดีตอนบ่าย"
                    greeting.contains("晚") -> "สวัสดีตอนเย็น"
                    else -> "สวัสดี"
                }
                val weatherSection = if (condition.isNotEmpty()) "สภาพอากาศคือ $condition อุณหภูมิ $currentTemp องศา" else ""
                val eventSection = if (eventCount > 0) "วันนี้คุณมีกิจกรรม scheduled ทั้งหมด $eventCount รายการ" else "ไม่มีกิจกรรม scheduled สำหรับวันนี้"
                "$thGreeting คุณ $username! $weatherSection $eventSection"
            }
            "vi" -> {
                val viGreeting = when {
                    greeting.contains("早") -> "Chào buổi sáng"
                    greeting.contains("午") -> "Chào buổi chiều"
                    greeting.contains("晚") -> "Chào buổi tối"
                    else -> "Xin chào"
                }
                val weatherSection = if (condition.isNotEmpty()) "Thời tiết hôm nay là $condition với $currentTemp độ." else ""
                val eventSection = if (eventCount > 0) "Bạn có $eventCount sự kiện hôm nay." else "Không có sự kiện nào cho hôm nay."
                "$viGreeting, $username! $weatherSection $eventSection"
            }
            else -> {
                // Determine if we should use Eastern or Western generic fallback
                val lk = if (actualLang.startsWith("zh")) actualLang else actualLang.substringBefore("_")
                val isEastern = lk == "zh_TW" || lk == "zh_HK" || lk == "zh_CN" || lk == "ja" || lk == "ko" || lk == "th" || lk == "vi"
                if (isEastern) {
                    val weatherSection = if (condition.isNotEmpty()) "，天氣$condition，氣溫 $currentTemp" else ""
                    val eventSection = if (eventCount > 0) "，今天有 $eventCount 項行程" else "，今天沒有行程"
                    "${cleanGreeting}，${username}！現在時間 ${time}${weatherSection}${eventSection}。"
                } else {
                    val weatherSection = if (condition.isNotEmpty()) "The weather is $condition, $currentTemp degrees." else ""
                    val eventSection = if (eventCount > 0) "You have $eventCount events today." else "No events scheduled today."
                    "Hello, $username! $cleanGreeting. Present time is $time. $weatherSection $eventSection"
                }
            }
        }
    }
}
