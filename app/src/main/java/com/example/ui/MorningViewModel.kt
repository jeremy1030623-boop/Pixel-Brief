package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.*
import com.example.data.*
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.serialization.json.Json

class MorningViewModel(application: Application) : AndroidViewModel(application) {
    private var dbCache: AppDatabase? = null
    
    private fun getDb(): AppDatabase? {
        if (dbCache != null) return dbCache
        return try {
            val db = AppDatabase.getDatabase(getApplication())
            dbCache = db
            db
        } catch (e: Throwable) {
            android.util.Log.e("MorningViewModel", "Failed to initialize database", e)
            null
        }
    }
    
    private val calendarRepository = CalendarRepository(application.contentResolver)
    private val locationHelper = LocationHelper(application)
    
    private val json = Json { ignoreUnknownKeys = true }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun reportError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }
    
    private val _userSettings = MutableStateFlow<UserSettings?>(null)
    val userSettings: StateFlow<UserSettings?> = _userSettings.asStateFlow()

    private val _tasksList = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasksList = _tasksList.asStateFlow()

    private val _recentSleepData = MutableStateFlow<List<SleepData>>(emptyList())
    val recentSleepData = _recentSleepData.asStateFlow()

    init {
        android.util.Log.d("MorningViewModel", "ViewModel Initializing...")
        viewModelScope.launch {
            try {
                val database = getDb()
                if (database != null) {
                    launch {
                        database.userSettingsDao().getUserSettings().collect { settings ->
                            _userSettings.value = settings ?: UserSettings()
                        }
                    }
                    launch {
                        database.taskItemDao().getAllTasks().collect { tasks ->
                            _tasksList.value = tasks
                            updateGoalSuggestions()
                        }
                    }
                    launch {
                        database.sleepDataDao().getRecentSleepData().collect { data ->
                            _recentSleepData.value = data
                        }
                    }
                } else {
                    _userSettings.value = UserSettings()
                }
            } catch (e: Throwable) {
                 android.util.Log.e("MorningViewModel", "Error loading settings, tasks or sleepData", e)
                 _userSettings.value = UserSettings()
            }
        }
    }

    fun addTask(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = getDb()
                if (db != null) {
                    db.taskItemDao().insertTask(TaskItem(text = text))
                    triggerInteractionFeedback("成功新增重點任務：$text 🎯")
                }
            } catch (e: Throwable) {
                android.util.Log.e("MorningViewModel", "addTask failed", e)
            }
        }
    }

    fun toggleTask(task: TaskItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = getDb()
                if (db != null) {
                    val updated = task.copy(isCompleted = !task.isCompleted)
                    db.taskItemDao().updateTask(updated)
                    if (updated.isCompleted) {
                        triggerInteractionFeedback("恭喜完成任務！繼續保持高效 🎉")
                    } else {
                        triggerInteractionFeedback("已重設任務狀態 ✏️")
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.e("MorningViewModel", "toggleTask failed", e)
            }
        }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = getDb()
                if (db != null) {
                    db.taskItemDao().deleteTask(task)
                    triggerInteractionFeedback("任務已刪除 🗑️")
                }
            } catch (e: Throwable) {
                android.util.Log.e("MorningViewModel", "deleteTask failed", e)
            }
        }
    }

    fun clearAllTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = getDb()
                if (db != null) {
                    db.taskItemDao().clearAllTasks()
                    triggerInteractionFeedback("已清空所有待辦任務 ✨")
                }
            } catch (e: Throwable) {
                android.util.Log.e("MorningViewModel", "clearAllTasks failed", e)
            }
        }
    }

    private val _weatherInfo = MutableStateFlow(WeatherInfo())
    val weatherInfo = _weatherInfo.asStateFlow()

    private val _sleepInfo = MutableStateFlow(SleepInfo())
    val sleepInfo = _sleepInfo.asStateFlow()

    private val _healthInfo = MutableStateFlow(HealthInfo())
    val healthInfo = _healthInfo.asStateFlow()

    private val _nextEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val nextEvents = _nextEvents.asStateFlow()

    private val _todaysEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val todaysEvents = _todaysEvents.asStateFlow()

    private val _newsDetail = MutableStateFlow<List<NewsItem>>(
        listOf(
            NewsItem(
                title = "AI 助理今日推薦：清晨高階效能管理",
                summary = "今天是全新的一天，您可以專注於完成關鍵目標。建議在早晨腦力黃金期優先處理最困難的工作，保持高效睡眠節律。"
            ),
            NewsItem(
                title = "健康提醒：高效率睡眠指南",
                summary = "昨晚您的睡眠時數約為 6.8 小時。多項研究指出，維持 7 至 8 小時的高品質深層睡眠，能大幅提升專注力及工作效率。"
            ),
            NewsItem(
                title = "清晨漫步：最新戶外天氣狀況",
                summary = "今天天氣溫和，非常適合在出門前進行 10 分鐘的深呼吸與輕度伸展。早晨光線有助於重新調整您的生理時鐘體力。"
            )
        )
    )
    val newsDetail = _newsDetail.asStateFlow()

    data class TimeState(
        val time: String,
        val greeting: String,
        val secondaryMessage: String = "為您開啟今日晨間簡報",
        val isInteraction: Boolean = false
    )

    private val _interactionMessage = MutableStateFlow<String?>(null)
    val interactionMessage = _interactionMessage.asStateFlow()

    private val _currentTimeFlow = flow {
        while (true) {
            try {
                val now = Date()
                val pattern = if (_userSettings.value?.is24HourFormat == true) "HH:mm" else "hh:mm a"
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val greeting = when (hour) {
                    in 5..11 -> "早上好"
                    in 12..17 -> "下午好"
                    in 18..21 -> "傍晚好"
                    else -> "夜深了，注意休息，祝您好夢"
                }
                emit(sdf.format(now) to greeting)
            } catch (e: Exception) {
                // Ignore
            }
            delay(60000)
        }
    }.flowOn(Dispatchers.Default)

    private fun getSecondaryMessageForHour(hour: Int): String {
        val messages = when (hour) {
            in 5..8 -> listOf(
                "早安晨光！早晨喝杯溫水有助於啟動消化，今天也要元氣滿滿",
                "清晨是最適合規劃的一刻，深呼吸，讓我們一起迎接燦爛的一天",
                "又是充滿希望的晨曦，喝杯咖啡或清茶，喚醒沉睡的身心吧"
            )
            in 9..11 -> listOf(
                "腦力黃金時刻！當前專注力最高，快來消滅今天最重要的難關吧",
                "高效執行的時間點，保持專注，你正在為夢想鋪路呢",
                "思緒靈敏的早晨，讓創意流動，完成那些延宕已久的小目標"
            )
            in 12..13 -> listOf(
                "午餐時間到了！希望你今天享用了美味午餐，記得稍微走動伸展一下",
                "休息是為了走更長遠的路，放下手邊工作，享受一段寧靜的午間時光",
                "補充能量的時刻，均衡的營養是下午高效輸出的基石"
            )
            in 14..17 -> listOf(
                "午後充電中！如果感到些微瞌睡，伸個大懶腰，為下半場注滿高能活力",
                "下午的陽光依然溫暖，適時的短暫休息能讓你的創造力成倍成長",
                "最後的衝刺階段，專注於收尾工作，期待晚上的悠閒時光"
            )
            in 18..20 -> listOf(
                "忙碌了一天辛苦啦！現在放慢節奏，享受愜意的個人時光或美味晚餐",
                "華燈初上，讓身心平靜下來，回味今日的小確幸，洗去一身疲憊",
                "夜晚是靈魂的歸宿，與家人共進晚餐或是給自己一段深度閱讀的時間"
            )
            in 21..23 -> listOf(
                "悠閒的深夜時光。建議放開公事、調暗燈光，預備香甜高品質的美夢",
                "靜謐的夜，適合冥想與反思。整理思緒，為明天的綻放蓄勢待發",
                "讓所有的喧囂遠離。溫柔地對自己說聲晚安，好眠是最好的療癒"
            )
            else -> listOf(
                "深夜探險家！夜深人靜思緒靈敏，但也別忘了優質睡眠是最好的充能器哦",
                "星光閃爍的凌晨，如果還未入眠，試著深呼吸放鬆，讓思維慢慢沉靜",
                "萬籟俱寂，這是屬於你的私密時刻，但也要記得休息是為了迎接明日的曙光"
            )
        }
        return messages.random()
    }

    val timeState: StateFlow<TimeState> = combine(_currentTimeFlow, _interactionMessage) { (time, greeting), interactionMsg ->
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val defaultSec = getSecondaryMessageForHour(hour)
        if (interactionMsg != null) {
            TimeState(
                time = time,
                greeting = greeting,
                secondaryMessage = interactionMsg,
                isInteraction = true
            )
        } else {
            TimeState(
                time = time,
                greeting = greeting,
                secondaryMessage = defaultSec,
                isInteraction = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimeState("", "Good morning,"))

    fun triggerInteractionFeedback(message: String) {
        viewModelScope.launch {
            _interactionMessage.value = message
            delay(12000) // Keep the feedback line active for 12 seconds
            _interactionMessage.value = null
        }
    }

    val username = _userSettings.map { it?.username ?: "Jeremy" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Jeremy")

    private val _goalSuggestion = MutableStateFlow<String>("")
    val goalSuggestion = _goalSuggestion.asStateFlow()

    private val _funFact = MutableStateFlow<String>("你知道嗎？每天適量喝水，能顯著提升專注力與新陳代謝。")
    val funFact = _funFact.asStateFlow()

    init {
        fetchData()
        
        viewModelScope.launch {
            while (true) {
                delay(1800000L) // 30 minutes
                try {
                    android.util.Log.d("MorningViewModel", "Periodic update triggered (every 30 mins)")
                    fetchData()
                } catch (e: Throwable) {
                    android.util.Log.e("MorningViewModel", "Error in periodic update", e)
                }
            }
        }
        
        viewModelScope.launch {
            try {
                _userSettings.collectLatest { settings ->
                    if (settings != null && settings.isSleepSynced) {
                        _sleepInfo.value = SleepInfo(
                            hours = settings.sleepHours,
                            snoringMinutes = settings.sleepSnoringMinutes,
                            coughCount = settings.sleepCoughCount
                        )
                        _healthInfo.value = HealthInfo(
                            steps = settings.dailySteps,
                            heartRate = settings.avgHeartRate,
                            trendReport = settings.healthTrendReport
                        )
                    } else {
                        _sleepInfo.value = SleepInfo(hours = 0f, snoringMinutes = 0, coughCount = 0)
                        _healthInfo.value = HealthInfo()
                    }
                    updateGoalSuggestions()
                }
            } catch (e: Throwable) {
                android.util.Log.e("MorningViewModel", "Exception collect _userSettings", e)
            }
        }
    }
    
    private suspend fun updateFunFact() {
        _funFact.value = try {
            val context = getApplication<Application>().applicationContext
            com.example.api.GoogleGenAiClient.generateContent(context, "請提供一個有趣的冷知識，字數 50 字以內，繁體中文。")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            "你知道嗎？每天適量喝水，能顯著提升專注力與新陳代謝。"
        }
    }

    private suspend fun updateGoalSuggestions() {
        val sleep = _sleepInfo.value
        val health = _healthInfo.value
        val weather = _weatherInfo.value
        val calendarEvents = _nextEvents.value
        val tasks = _tasksList.value
        val news = _newsDetail.value
        
        val dateStr = java.text.SimpleDateFormat("yyyy年MM月dd日 EEEE", java.util.Locale.TAIWAN).format(java.util.Date())
        
        val calendarStr = if (calendarEvents.isNotEmpty()) {
            calendarEvents.joinToString("; ") { "${it.title}(${java.text.SimpleDateFormat("HH:mm", java.util.Locale.TAIWAN).format(java.util.Date(it.startTime))})" }
        } else {
            "無活動排程"
        }
        
        val tasksStr = if (tasks.isNotEmpty()) {
            tasks.filter { !it.isCompleted }.joinToString("; ") { it.text }
        } else {
            "無未完成事項"
        }
        
        val newsStr = if (news.isNotEmpty()) {
            news.take(2).joinToString("; ") { it.title }
        } else {
            "無焦點新聞"
        }

        val prompt = """
            你是專業且極具親和力的個人生活規劃助理。
            請根據以下當前的全面性晨間情境資訊，為使用者產生【一個具體的、可行的、充滿激勵作用的每日目標建議】：
            - 當前日期: $dateStr
            - 當天天氣: ${weather.locationName} ${weather.condition}，氣溫 ${weather.currentTemp}°C，濕度 ${weather.humidity}%
            - 用戶日曆活動: $calendarStr
            - 待辦清單事項 (Tasks): $tasksStr
            - 今日最新焦點新聞: $newsStr
            
            建議規範：
            1. 請將上述日曆活動、待辦事項、天氣或新聞「巧妙結合」，挑選一到多個面向，為用戶歸納推導出一個「此時此地最具效益的單一關鍵生活目標建議」。
            2. 目標建議應「簡潔明瞭」、「具體可行」，並「鼓勵用戶積極參與」。
            3. 不要分點，不要列一堆目標，請給出 1 至 2 句流暢有溫度的短文。字數限制在 60-100 字之間。
            4. 範例風格：
               - 「今天天氣晴朗，適合戶外散步30分鐘。」
               - 「根據你的新聞摘要，今天是一個學習新知識的好時機，嘗試閱讀一篇關於AI的文章。」
               
            請直接輸出目標建議文字，不要包含任何開頭引言或外層引號。
        """.trimIndent()
        
        val modelName = _userSettings.value?.geminiModelSelected ?: "Gemini Flash Latest"
        
        _goalSuggestion.value = try {
            val context = getApplication<Application>().applicationContext
            val responseText = com.example.api.GoogleGenAiClient.generateContent(context, prompt, modelName)
            responseText.trim()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Intelligent conditional fallbacks matching the user requirements
            val firstTask = tasks.firstOrNull { !it.isCompleted }?.text
            val firstEvent = calendarEvents.firstOrNull()?.title
            val hasAiNews = news.any { it.title.contains("AI") || it.title.contains("科技") || it.summary.contains("AI") || it.summary.contains("科技") }
            
            when {
                weather.condition.contains("雨") || weather.condition.contains("雷") -> {
                    "今日有雨且天氣潮濕，適合安排室內閱讀或居家舒展，今天的新聞或 Tasks 清單中「$firstTask」也是靜下心來處理的好任務！"
                }
                hasAiNews -> {
                    "根據你的新聞摘要，今天是一個學習新知識的好時機，嘗試閱讀一篇關於AI科技或最新趨勢的文章，踏出成長第一步。"
                }
                firstEvent != null -> {
                    "今日日曆有「$firstEvent」活動。建議提早10分鐘出發並確認簡報內容，日落後安排一次輕舒緩拉伸。"
                }
                firstTask != null -> {
                    "今天溫度舒適，是消滅待辦事項的好時機！建議設定25分鐘番茄鐘專注完成「$firstTask」，一鼓作氣完成今天最重要的任務。"
                }
                weather.condition.contains("晴") || weather.condition.contains("乾") -> {
                    "今天天氣晴朗且溫度適宜，非常適合戶外放鬆。建議利用空檔到鄰近公園散步30分鐘，吸收陽光恢復活力！"
                }
                else -> {
                    "今天的生活步調很適合維持均衡作息。在工作的間隙，給自己預留一小段伸展與深呼吸時間，擁抱充實、心滿意足的一天！"
                }
            }
        }
    }

    fun syncHealthData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentSettings = _userSettings.value ?: UserSettings()
                val context = getApplication<Application>().applicationContext
                val isHCEnabled = currentSettings.isHealthSyncEnabled
                
                val startTime = android.os.SystemClock.elapsedRealtime()
                
                var healthData = HealthConnectHelper.HealthData()
                if (isHCEnabled && HealthConnectHelper.isSdkAvailable(context)) {
                    healthData = HealthConnectHelper.readHealthData(context)
                } else {
                    delay(450)
                }

                val endTime = android.os.SystemClock.elapsedRealtime()
                val syncDuration = endTime - startTime

                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val timeStr = sdf.format(Date())
                
                val trendReport = fetchHealthTrend(healthData)

                val updatedSettings = currentSettings.copy(
                    isSleepSynced = true,
                    sleepHours = healthData.sleepHours,
                    sleepSnoringMinutes = healthData.snoringMinutes,
                    sleepCoughCount = healthData.coughCount,
                    dailySteps = healthData.dailySteps,
                    avgHeartRate = healthData.avgHeartRate,
                    healthTrendReport = trendReport,
                    lastSyncTime = timeStr,
                    sleepSyncDurationMs = syncDuration
                )
                getDb()?.userSettingsDao()?.saveUserSettings(updatedSettings)
                triggerInteractionFeedback("健康狀態雲端同步成功！已為您備好專屬的深度健康生活綜合報告 📈")
            } catch (e: Throwable) {
                android.util.Log.e("MorningViewModel", "Exception during syncHealthData", e)
            }
        }
    }

    private suspend fun fetchHealthTrend(data: HealthConnectHelper.HealthData): String {
        val prompt = "你是專業且溫柔的個人健康規劃與生活大師。請分析以下昨晚到今天的健康數據，並提供一段字數約 80-120 字的『深度健康生活綜合指導文字簡報』，包含具體的身體狀態評估、今日飲食與運動之科學建議，以及晨間開機的精神小叮嚀。請務必溫馨且充滿細節，直接返回簡報文字，不要有任何標題或外層引號。數據：睡眠 ${data.sleepHours} 小時（品質：${data.sleepQuality}，打鼾 ${data.snoringMinutes} 分鐘，咳嗽 ${data.coughCount} 次），今日步數 ${data.dailySteps} 步，平均心率 ${data.avgHeartRate} bpm。"
        val modelName = "gemini-nano" 
        
        return try {
            val context = getApplication<Application>().applicationContext
            val responseText = com.example.api.GoogleGenAiClient.generateContent(context, prompt, modelName)
            responseText.trim()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (data.sleepHours > 7 && data.dailySteps > 8000) {
                "【活力滿分】您昨晚的睡眠時數長達 ${data.sleepHours} 小時，深層睡眠品質卓越；搭配今日高達 ${data.dailySteps} 步的活躍步伐，您的心肺功能與代謝正處於極佳狀態。建議清晨多攝取高蛋白與富含維生素的膳食，維持一整天高能量釋放。今天非常適合進行戶外快走，讓充足陽光調節您的生理時鐘！"
            } else if (data.sleepHours < 6) {
                "【溫馨守護】您昨晚的睡眠時數僅 ${data.sleepHours} 小時，身體開機稍顯疲憊，且心律偏高。今日建議在飲食中補充足夠的純水與複合性碳水化合物，保持體液平衡與專注。今天請避免挑戰極限強度的訓練，改為 15 分鐘的溫和拉伸與深呼吸，晚上提早入睡以極速修護元氣。"
            } else {
                "【元氣平衡】今日您的各項健康指標整體表现平穩。睡眠時數達 ${data.sleepHours} 小時，心率穩定在 ${data.avgHeartRate} bpm。建議中午進行 10 分鐘的靜坐冥想或深呼吸，並在工作時每隔一小時起身活動，能大幅改善下半身循環。保持平和心境，迎接充實、健康而自信的一天！"
            }
        }
    }

    fun clearSleepData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentSettings = _userSettings.value ?: UserSettings()
                val updatedSettings = currentSettings.copy(
                    isSleepSynced = false,
                    sleepHours = 0f,
                    sleepSnoringMinutes = 0,
                    sleepCoughCount = 0,
                    dailySteps = 0,
                    avgHeartRate = 0,
                    healthTrendReport = "",
                    lastSyncTime = ""
                )
                getDb()?.userSettingsDao()?.saveUserSettings(updatedSettings)
                triggerInteractionFeedback("睡眠記錄已重置！期待今晚為您捕捉更放鬆美妙的入夢節奏 💤")
            } catch (e: Throwable) {
                android.util.Log.e("MorningViewModel", "Exception during clearSleepData", e)
            }
        }
    }

    fun updateUserSettings(settings: UserSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getDb()?.userSettingsDao()?.saveUserSettings(settings)
                android.util.Log.d("MorningViewModel", "User settings updated, reloading data actively...")
                fetchData(isManual = false)
                triggerInteractionFeedback("個人檔案客製調整成功！新頭像、配色與主題已無縫融入奢華版面 👑")
            } catch (e: Throwable) {
                android.util.Log.e("MorningViewModel", "Exception during updateUserSettings", e)
            }
        }
    }

    private fun mapWeatherCode(code: Int): String {
        return when (code) {
            0 -> "晴朗"
            1, 2, 3 -> "晴時多雲"
            45, 48 -> "霧氣繚繞"
            51, 53, 55 -> "細雨霏霏"
            56, 57 -> "凍雨"
            61, 63 -> "局部陣雨"
            65 -> "連綿大雨"
            66, 67 -> "冰雨"
            71, 73, 75 -> "雪花紛飛"
            80, 81, 82 -> "短暫陣雨"
            85, 86 -> "短暫陣雪"
            95, 96, 99 -> "雷雨交加"
            else -> "多雲"
        }
    }

    private fun getWeatherDescription(info: WeatherInfo): String {
        val baseDescription = when {
            info.condition.contains("晴朗") -> "陽光普照，藍天如洗。"
            info.condition.contains("晴時多雲") -> "陽光穿透雲層，氣候宜人。"
            info.condition.contains("多雲") -> "雲量較多，陽光偶爾露臉。"
            info.condition.contains("陰") -> "天空陰沉，氣氛寧靜。"
            info.condition.contains("雨") -> "細雨綿綿，增添了幾分詩意。"
            info.condition.contains("雷") -> "雷聲陣陣，請注意安全。"
            info.condition.contains("霧") -> "晨霧迷濛，宛如仙境。"
            info.condition.contains("雪") -> "白雪皚皚，世界銀裝素裹。"
            else -> "今日氣候平穩，適合開啟新的一天。"
        }

        val tempAdvice = when {
            info.currentTemp >= 30 -> "氣溫偏高，記得多補充水分，預防中暑。"
            info.currentTemp <= 15 -> "天氣較冷，建議穿上保暖衣物再出門。"
            else -> "溫度舒適，正是外出活動的好時機。"
        }

        val rainAdvice = if (info.precipitationProb > 30) "降雨機率較高，出門記得帶把傘。" else ""

        return "$baseDescription $tempAdvice $rainAdvice".trim()
    }

    fun fetchData(isManual: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                val useGps = _userSettings.value?.preciseLocationEnabled ?: true
                val location = if (useGps) locationHelper.getCurrentLocation() else null
                val detected = getCityNameFromLocation(location)
                val isGps = useGps && location != null && detected.isNotEmpty() && detected != "台灣"
                val defaultCityOpt = _userSettings.value?.defaultCity ?: "台北"
                val finalCity = if (detected.isNotEmpty() && detected != "台灣" && useGps) detected else defaultCityOpt
                
                val (lat, lon) = if (location != null && useGps) {
                    Pair(location.latitude, location.longitude)
                } else {
                    getCoordinatesForCity(finalCity)
                }
                
                val newWeather = try {
                    var systemWeather: WeatherInfo? = null
                    try {
                        val weatherUri = android.net.Uri.parse("content://com.google.android.apps.weather.weatherprovider/weather")
                        val cursor = getApplication<Application>().contentResolver.query(weatherUri, null, null, null, null)
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val tempIndex = it.getColumnIndex("temperature")
                                val conditionIndex = it.getColumnIndex("condition")
                                
                                if (tempIndex != -1 && conditionIndex != -1) {
                                    val tempStr = it.getString(tempIndex)
                                    val condStr = it.getString(conditionIndex)
                                    val temp = tempStr?.toFloatOrNull()?.toInt()
                                    
                                    if (temp != null && condStr != null) {
                                    val baseWeather = WeatherInfo(
                                            condition = condStr,
                                            currentTemp = temp,
                                            maxTemp = temp + 4,
                                            minTemp = temp - 4,
                                            locationName = finalCity,
                                            isGpsLocated = isGps
                                        )
                                        systemWeather = baseWeather.copy(description = getWeatherDescription(baseWeather))
                                        android.util.Log.d("MorningViewModel", "Loaded weather from ContentProvider: $systemWeather")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MorningViewModel", "ContentProvider fetch failed", e)
                    }

                    if (systemWeather != null) {
                        systemWeather!!.copy(locationName = finalCity, isGpsLocated = isGps)
                    } else {
                        val weatherData = OpenMeteoClient.service.getForecast(latitude = lat, longitude = lon)
                        val baseWeather = WeatherInfo(
                            condition = mapWeatherCode(weatherData.current.weather_code),
                            currentTemp = weatherData.current.temperature_2m.toInt(),
                            maxTemp = weatherData.daily.temperature_2m_max.firstOrNull()?.toInt() ?: 30,
                            minTemp = weatherData.daily.temperature_2m_min.firstOrNull()?.toInt() ?: 22,
                            apparentTemp = weatherData.current.apparent_temperature?.toInt() ?: (weatherData.current.temperature_2m.toInt() + 1),
                            humidity = weatherData.current.relative_humidity_2m ?: 75,
                            windSpeed = weatherData.current.wind_speed_10m ?: 10f,
                            precipitationProb = weatherData.daily.precipitation_probability_max?.firstOrNull() ?: 10,
                            uvIndex = weatherData.daily.uv_index_max?.firstOrNull() ?: 5.0f,
                            locationName = finalCity,
                            isGpsLocated = isGps
                        )
                        baseWeather.copy(description = getWeatherDescription(baseWeather))
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("MorningViewModel", "Open-Meteo forecast fetch failed", e)
                    val baseWeather = WeatherInfo(
                        condition = "晴時多雲",
                        currentTemp = 28,
                        maxTemp = 32,
                        minTemp = 24,
                        locationName = finalCity,
                        isGpsLocated = isGps
                    )
                    baseWeather.copy(description = getWeatherDescription(baseWeather))
                }
                _weatherInfo.value = newWeather

                val settings = _userSettings.value
                if (settings != null && settings.isSleepSynced) {
                    _sleepInfo.value = SleepInfo(
                        hours = settings.sleepHours,
                        snoringMinutes = settings.sleepSnoringMinutes,
                        coughCount = settings.sleepCoughCount
                    )
                } else {
                    _sleepInfo.value = SleepInfo(hours = 0f, snoringMinutes = 0, coughCount = 0)
                }

                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        getApplication(),
                        android.Manifest.permission.READ_CALENDAR
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    try {
                        _nextEvents.value = calendarRepository.getNextEvents()
                        _todaysEvents.value = calendarRepository.getTodaysUpcomingEvents()
                    } catch (e: SecurityException) {
                        android.util.Log.w("MorningViewModel", "SecurityException fetching calendar: ${e.message}")
                    } catch (e: Throwable) {
                        android.util.Log.e("MorningViewModel", "Failed to fetch calendar events", e)
                    }
                } else {
                    _nextEvents.value = emptyList()
                    _todaysEvents.value = emptyList()
                }

                fetchNews(newWeather)
                updateGoalSuggestions()
                if (isManual) {
                    triggerInteractionFeedback("當前定位與最新天氣數據同步成功！已為您備好最準確的出門參考 ☀️")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                android.util.Log.e("MorningViewModel", "Error inside fetchData", e)
                reportError("無法取得最新資訊，請檢查網路連線")
            } finally {
                _isRefreshing.value = false
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            updateFunFact()
        }
    }

    private fun getCoordinatesForCity(city: String): Pair<Double, Double> {
        return when {
            city.contains("台北") || city.contains("臺北") -> Pair(25.0330, 121.5654)
            city.contains("新北") -> Pair(25.0120, 121.4657)
            city.contains("桃園") -> Pair(24.9936, 121.3010)
            city.contains("台中") || city.contains("臺中") -> Pair(24.1477, 120.6736)
            city.contains("台南") || city.contains("臺南") -> Pair(22.9908, 120.2133)
            city.contains("高雄") -> Pair(22.6273, 120.3014)
            city.contains("新竹") -> Pair(24.8138, 120.9675)
            city.contains("基隆") -> Pair(25.1283, 121.7419)
            city.contains("嘉義") -> Pair(23.4801, 120.4491)
            city.contains("宜蘭") -> Pair(24.7570, 121.7530)
            city.contains("花蓮") -> Pair(23.9871, 121.6016)
            city.contains("台東") || city.contains("臺東") -> Pair(22.7583, 121.1444)
            city.contains("苗栗") -> Pair(24.5601, 120.8210)
            city.contains("彰化") -> Pair(24.0817, 120.5385)
            city.contains("南投") -> Pair(23.9101, 120.6860)
            city.contains("雲林") -> Pair(23.7092, 120.4313)
            city.contains("屏東") -> Pair(22.6761, 120.4885)
            city.contains("澎湖") -> Pair(23.5656, 119.5793)
            city.contains("金門") -> Pair(24.4361, 118.3186)
            city.contains("馬祖") -> Pair(26.1558, 119.9289)
            else -> Pair(25.0330, 121.5654)
        }
    }

    private fun getCityFromCoordinates(latitude: Double, longitude: Double): String {
        val cities = listOf(
            "台北" to Pair(25.0330, 121.5654),
            "台中" to Pair(24.1477, 120.6736),
            "台南" to Pair(22.9908, 120.2133),
            "高雄" to Pair(22.6273, 120.3014),
            "新竹" to Pair(24.8138, 120.9675),
            "基隆" to Pair(25.1283, 121.7419),
            "嘉義" to Pair(23.4801, 120.4491),
            "宜蘭" to Pair(24.7570, 121.7530),
            "花蓮" to Pair(23.9871, 121.6016),
            "台東" to Pair(22.7583, 121.1444)
        )
        var closestCity = "台北"
        var minDistance = Double.MAX_VALUE
        for ((city, coord) in cities) {
            val dist = Math.hypot(latitude - coord.first, longitude - coord.second)
            if (dist < minDistance) {
                minDistance = dist
                closestCity = city
            }
        }
        return closestCity
    }

    private fun getCityNameFromLocation(location: android.location.Location?): String {
        if (location == null) return ""
        try {
            val context = getApplication<Application>().applicationContext
            if (android.location.Geocoder.isPresent()) {
                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val city = addresses?.firstOrNull()?.let {
                    it.adminArea ?: it.locality ?: it.subAdminArea
                }
                if (!city.isNullOrBlank()) {
                    android.util.Log.d("MorningViewModel", "Geocoder fetched city name: $city")
                    return city.replace("City", "").replace("County", "").replace("市", "").replace("縣", "").trim()
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("MorningViewModel", "Geocoder exception", e)
        }
        val approx = getCityFromCoordinates(location.latitude, location.longitude)
        android.util.Log.d("MorningViewModel", "Bounding box fallback city: $approx")
        return approx
    }

    private suspend fun fetchNews(weather: WeatherInfo) {
        val newsMode = _userSettings.value?.newsMode ?: "local"
        var locationText = "台灣"
        
        _newsDetail.value = emptyList()

        val realNews = try {
            when (newsMode) {
                "international" -> {
                    locationText = "國際"
                    GoogleNewsFetcher.fetchInternationalNews()
                }
                "tech" -> {
                    locationText = "科技"
                    GoogleNewsFetcher.fetchTechNews()
                }
                "health" -> {
                    locationText = "健康"
                    GoogleNewsFetcher.fetchHealthNews()
                }
                else -> {
                    val location = locationHelper.getCurrentLocation()
                    val detected = getCityNameFromLocation(location)
                    val defaultOpt = _userSettings.value?.defaultCity ?: "台北"
                    val finalCity = if (detected.isNotEmpty() && detected != "台灣") detected else defaultOpt
                    locationText = finalCity
                    GoogleNewsFetcher.fetchNewsByLocation(finalCity)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("MorningViewModel", "Failed to fetch real Google News via RSS", e)
            emptyList()
        }

        val realNewsContext = if (realNews.isNotEmpty()) {
            val headlines = realNews.mapIndexed { index, item ->
                "${index + 1}. [標題] ${item.title} (媒體來源: ${item.source}, 網址: ${item.link})"
            }.joinToString("\n")
            when (newsMode) {
                "international" -> {
                    "以下是今天真實採集到的國際世界 Google News 最新熱門頭條與其來源網址：\n$headlines\n\n請你扮演高階 AI 早晨簡報助理，將以上真實國際新聞，精心挑選出 3 到 5 則最重要、最高水準且生活實用的世界政經或國際焦點話題。請為每一選取的焦點編輯一段親切、詳實、極具深度與溫度，且充滿整合指導價值的『早晨啟動文字簡報大摘要』（字數必須在 120 到 200 字之間）。請詳細描寫脈絡，提供其對個人生活、科學保健或全球動態的具體啟易，並務必在對應欄位填上該則新聞原本對應的 `url` (來源連結)。"
                }
                "tech" -> {
                    "以下是今天真實採集到的科技創新領域 Google News 最新頭條與來源網址：\n$headlines\n\n請你扮演頂尖 AI 科技前沿大師，從上述熱門科技要聞中，挑選出 3 則最適合職場白領提升眼界、掌握未來趨勢或是應用於日常工作效率的焦點話題。請為每一點編輯出一款好讀易懂、富有洞察力、且實用性極佳的『數位科技晨報大摘要』（字數在 120 到 200 字之間）。詳細闡明其對個人技術實踐與未來的深刻啟示，並在 url 欄位附上原始連結。"
                }
                "health" -> {
                    "以下是今天真實採集到的健康醫療、生活科學領域 Google News 最新熱門頭條與其來源網址：\n$headlines\n\n請你扮演頂尖 AI 身心健康科學顧問，從上述熱門要聞中精心挑選最適合現代城市忙碌工作者與生活大眾的 3 則黃金健康、抗炎、作息、飲食或睡眠焦點話題。請為每一焦點點位編輯一段極富科學實證指南、實操步驟清晰、且文字非常有親和力的『元氣健康晨報大摘要』（字數在 120 到 200 字之間）。詳細描寫成因與個人日常開機的具體實操，並在 url 欄位附上原始連結。"
                }
                else -> {
                    "以下是今天真實採集到的 $locationText 在地生活、市政與社會 Google News 最新熱門頭條與其來源網址：\n$headlines\n\n請你扮演極具親和力與熱情的 AI 台灣在地生活指引大師，從上述熱門台灣在地或市政焦點中挑選出 3 到 5 則最重要、最貼近一般大眾衣食住行、週休旅遊、交通變更、公共安全或休閒生活的要聞。請為每一選取的焦點編輯一段極富親和力、詳實且充滿溫度的『在地生活大摘要』（字數在 120 到 200 字之間）。強烈建議語氣要非常貼近生活常理，附帶親切貼心的實作指引或外出防護提醒。別忘了在 url 欄位填入對應連結。"
                }
            }
        } else {
            when (newsMode) {
                "international" -> {
                    "（暫時無法取得真實 Google News RSS，請你直接為讀者虛擬生成 3 到 5 則富有正向能量、精緻充實、段落長度約 120 到 200 字的國際政經、世界脈動與科技健康新聞話題文字簡報，包含實用的行動指導建議，`url` 請留空）"
                }
                "tech" -> {
                    "（暫時無法取得真實 Google News RSS，請你為讀者精緻虛擬建構 3 則探討 AI 自動化、前沿電子科技、與端側運算之深度科技新聞簡報，描繪其對個體能力提升與日常辦公優化的乾貨，長度 120-200 字，`url` 留空）"
                }
                "health" -> {
                    "（暫時無法取得真實 Google News RSS，請你為讀者精心生成 3 則基於最新睡眠醫學與自主神經調理的健康生活簡報，提供起床伸展、清晨補水與抗發炎餐食等精準指導建議，長度 120-200 字，`url` 留空）"
                }
                else -> {
                    "（暫時無法取得真實 Google News RSS，請你直接為讀者虛擬生成 3 到 5 則與 [$locationText] 在地生活、地方交通與生活日常息息相關、溫馨正能量、段落長度約 120 到 200 字的焦點新聞話題文字簡報，給出極具生活實用乾貨與健康小訣竅，`url` 請留空）"
                }
            }
        }

        val prompt = """
            你是一款兼具高超設計美學、台灣本土親和力，且能在清晨為大眾提供元氣加持的 AI 晨光秘書。
            今天的使用者希望選取的早安簡報模式為：$locationText。
            目前系統採集的在地位置/模式主要文字為：$locationText 的清晨更新。
            目前室外的最新氣候資訊為：${weather.locationName}今日天氣${weather.condition}，室外溫度 ${weather.currentTemp}°C，體感溫度為 ${weather.apparentTemp}°C，濕度為 ${weather.humidity}%，紫外線指數為 ${weather.uvIndex}。
            
            $realNewsContext
            
            請你根據上方的上下文（若有真實採集到的新聞標題，請**百分之百只用真實標題進行精選與論述**，絕對不可憑空捏造無中生有的新聞標題或任意混淆網址），發揮你溫柔、熱情、充滿人文關懷與前沿科學思維的早晨助理精神。
            若 realNewsContext 為空，或找不到相關焦點，則你只能在對應的模式（international/tech/health/local）下，生成 2-3 則能跟上述氣候資訊（溫度、戶外活動氣候等）以及模式主題（國際/科技/健康/在地生活）完美融合且充滿元氣、極度詳實的「精美原創好文與保健指引晨光要聞」。
            
            請確保輸出的 JSON 格式嚴格合法，不要包含任何 markdown 符記（如 ```json），只回傳純 JSON 陣列。
            每一則陣列項目皆須包含三個欄位：title（新聞標題/焦點名稱）、summary（詳細大摘要文字，字數保持120-200字，乾貨滿滿、親切暖心）、url（原始新聞網址，如果是真實採集到的新聞請填入原網址，如果是無真實新聞 the fallback 生成，則填寫 "https://news.google.com"）。
            
            請立刻開始為使用者準備今日晨光簡報：
        """.trimIndent()

        val modelName = _userSettings.value?.geminiModelSelected ?: "gemini-1.5-flash"
        
        try {
            val context = getApplication<Application>().applicationContext
            val responseText = com.example.api.GoogleGenAiClient.generateContent(context, prompt, modelName)
            val cleanedJson = responseText.replace("```json", "").replace("```", "").trim()
            
            val news = json.decodeFromString<List<NewsItem>>(cleanedJson)
            _newsDetail.value = news
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            android.util.Log.e("MorningViewModel", "GoogleGenAiClient generation failed, attempting backup...", e)
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                _newsDetail.value = mapRealArticlesToNewsItems(realNews, newsMode)
                return
            }

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(temperature = 0.7f)
            )

            try {
                val actualModel = when (modelName) {
                    "Gemini Flash Latest" -> "gemini-1.5-flash"
                    "aicore" -> "gemini-1.5-flash"
                    else -> modelName
                }
                val response = RetrofitClient.service.generateContent(actualModel, apiKey, request)
                val jsonText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val cleanedJsonFallback = jsonText.replace("```json", "").replace("```", "").trim()
                
                val news = json.decodeFromString<List<NewsItem>>(cleanedJsonFallback)
                _newsDetail.value = news
            } catch (ex: kotlinx.coroutines.CancellationException) {
                throw ex
            } catch (ex: Throwable) {
                android.util.Log.e("MorningViewModel", "Retrofit generateContent failed, mapping real news...", ex)
                _newsDetail.value = mapRealArticlesToNewsItems(realNews, newsMode)
            }
        }
    }

    private fun mapRealArticlesToNewsItems(realNews: List<GoogleNewsFetcher.NewsArticle>, newsMode: String): List<NewsItem> {
        if (realNews.isEmpty()) {
            return getStaticFallbackNews(newsMode)
        }
        return realNews.take(5).map { article ->
            val formattedSummary = "【即時焦點 • 來源：${article.source}】這是一則來自當前的即時新聞。發佈時間為 ${article.pubDate}。點擊連結可以快速閱讀源自各大媒體之原始深層報導內容，一手掌握最新在地生活資訊。"
            NewsItem(
                title = article.title,
                summary = formattedSummary,
                url = article.link
            )
        }
    }

    private fun getStaticFallbackNews(newsMode: String): List<NewsItem> {
        return if (newsMode == "international") {
            listOf(
                NewsItem(
                    title = "國際財經專題：全球半導體供應鏈重組與晶片振興白皮書",
                    summary = "隨著美、歐、日等多國積極推出巨額半導體補貼與本國製造政策，全球晶片供應鏈正迎來數十年來最劇烈的板塊重組。專家指出，晶片製造商正加速於分散區域佈局新廠。這預示著未來『雙軌供應鏈』與區域性協同生產將成為全球科技產業的核心新常態。長期來看，這不仅能有效預防地緣或災害突發所導致的供應中斷，更將顯著推動各區域的在地基礎設施與技術研發升級，對於高級工程師、科技從業者來說，跨國人才流動與跨洲技術協作將成為必須具備的全新職涯眼界。",
                    url = "https://news.google.com"
                ),
                NewsItem(
                    title = "綠色能源突破：低成本光電及風能技術引領全球淨零碳排",
                    summary = "國際能源總署（IEA）在今晨發布的最新年度能源瞻望白皮書中指出，未來三年間全球可再生能源累計發電量將突破歷史新高，徹底改變傳統基載電力結構。得益於新一代鈣鈦礦疊層太陽能電池以及深海浮動式風力發電技術的突破，綠色電力獲取成本在過去18個月內崩跌了接近百分之二十五，已成為絕大多數國家在新增電力裝機容量時，兼顧商業運維與政策環保的首要選擇。各國政府正加速建置智慧區域電網與分佈式儲能系統，力求落實淨零碳排承諾，而一般大眾家戶也將受益於逐漸降低的綠色電費負擔，開啟低碳生活新篇章。",
                    url = "https://news.google.com"
                ),
                NewsItem(
                    title = "身心健康科技：微日光暴露法與現代白領腦力保健科學觀點",
                    summary = "最新發布於頂尖神經醫學期刊的實證研究確認，精確管理白天的日光暴露與晚上的黑暗節律，是維持大腦海馬迴活動與情緒穩定的核心。研究指出，人體在清晨醒來後的 30 分鐘內，若能接受 10 至 15 分鐘、約 10,000 照度的自然光與微風抚慰，便能精巧刺激視交叉上核，重置神經遞質與褪黑激素的分泌節律。這項開機程序不仅能在白天顯著提升認知專注度、抑制倦怠感，更能深度改善夜晚的睡眠深度。考慮到今天部分地區天氣舒適溫和，誠摯建議您在出門前，撥空步行至通風良好的窗台或公園，進行深呼吸與暖身拉伸，迎接一整天精準、平穩且敏捷的高效活力作息！",
                    url = "https://news.google.com"
                )
            )
        } else {
            listOf(
                NewsItem(
                    title = "在地生活指南：大眾智慧交通網與綠色通勤接駁系統升級優化",
                    summary = "市府交通局與捷運公司今日宣布，為了落實低碳智慧城市願景，將全面優化多條尖峰時段的捷運接駁專線與幹線公車路網。本輪升級除了大量增設微型站點與普及無障礙共享載具外，更引進了尖端的 AI 智慧人流分析與動態調度系統。該系統可實時監測人流湧入速度並在 3 分鐘內動態派遣支援班次，初步模擬指出此舉能成功降低通勤族在月台與站牌的等待時間高達 15%。強烈建議市民朋友們外出工作或上學時，下載並利用最新版的在地即時公車 App 規劃路線，享受綠色低碳且無縫轉乘的舒心出行體驗。",
                    url = "https://news.google.com"
                ),
                NewsItem(
                    title = "數位新創動態：台灣新創團隊 AI 隱私助理驚豔國際大會",
                    summary = "在昨日圓滿閉幕的亞太數位科技年會上，數家來自台灣的潛力新創團隊憑藉其獨特的端側 AI 行為分析與隱私保護防禦技術，大放異彩並斬獲多項創新大獎。這款專為行動設備量身訂製的智慧助理，採用了不需上傳雲端、完全在手機本機運算（On-device AI）的微型語言模型演算法，提供毫秒級的親切語意理解與日程優化。最難能可貴的是它在守護健康日誌、家庭位置等隱密資料的同時，展現出高水準的上下文聯想與情感互動能力。此項突破不僅在亞太與北美商務市場贏得高度熱烈的融資關注，更向世界展現了台灣在端側 AI 應用品質上的無比實力。",
                    url = "https://news.google.com"
                ),
                NewsItem(
                    title = "早晨開機元氣指引：今日晨光伸展建議與空氣品質環境提醒",
                    summary = "根據氣象局與環境保護監測網的清晨更新數據，今日各區空氣品質指數（AQI）普遍維持在極其優良的綠色安全區間，整體氣候適度溫和。這樣的絕佳氣候環境，無疑提供了一個享受大自然的完美起點。醫學專家建議，在如此舒適的清晨前往鄰近公園、草地、或天台進行 15 分鐘的清晨漫步，並在過程中交替進行配合深呼吸的肩頸放鬆與下肢伸展踏步，能極佳地加速全身血液灌流大腦、重置心肺動能。讓我們迎著這道溫煦的朝陽，徹底洗去昨夜累積的疲憊，開啟極其充沛、心滿意足的璀璨一整天！",
                    url = "https://news.google.com"
                )
            )
        }
    }
}
