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
    
    private val _userSettings = MutableStateFlow<UserSettings?>(null)
    val userSettings: StateFlow<UserSettings?> = _userSettings.asStateFlow()

    init {
        android.util.Log.d("MorningViewModel", "ViewModel Initializing...")
        viewModelScope.launch {
            try {
                val database = getDb()
                if (database != null) {
                    database.userSettingsDao().getUserSettings().collect { settings ->
                        _userSettings.value = settings ?: UserSettings()
                    }
                } else {
                    _userSettings.value = UserSettings()
                }
            } catch (e: Throwable) {
                 android.util.Log.e("MorningViewModel", "Error loading settings", e)
                 _userSettings.value = UserSettings()
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

    private val json = Json { ignoreUnknownKeys = true }

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
                summary = "今天天氣溫和，非常適合在出門前進行 10 分鐘的深呼吸與輕度伸展。早晨光線有助於重新調整您的生理時鐘。"
            )
        )
    )
    val newsDetail = _newsDetail.asStateFlow()

    data class TimeState(val time: String, val greeting: String)

    private val _timeState = MutableStateFlow(TimeState("", "Good morning,"))
    val timeState = _timeState.asStateFlow()

    private val _currentTimeFlow = flow {
        while (true) {
            try {
                val now = Date()
                val pattern = if (_userSettings.value?.is24HourFormat == true) "HH:mm" else "hh:mm a"
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val greeting = when (hour) {
                    in 5..11 -> "Good morning,"
                    in 12..17 -> "Good afternoon,"
                    in 18..23 -> "Good evening,"
                    else -> "Hi there,"
                }
                
                emit(sdf.format(now) to greeting)
            } catch (e: Throwable) {
                android.util.Log.e("MorningViewModel", "Exception in _currentTimeFlow execution", e)
            }
            delay(60000)
        }
    }.flowOn(Dispatchers.Default)

    val username = _userSettings.map { it?.username ?: "Jeremy" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Jeremy")

    init {
        // fetchData is already setting up data on IO
        fetchData()
        
        viewModelScope.launch {
            try {
                _currentTimeFlow.collect { (time, greeting) ->
                    _timeState.value = TimeState(time, greeting)
                }
            } catch (e: Throwable) {
                android.util.Log.e("MorningViewModel", "Exception collect _currentTimeFlow", e)
            }
        }
        
        viewModelScope.launch {
            try {
                _userSettings.collect { settings ->
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
                }
            } catch (e: Throwable) {
                android.util.Log.e("MorningViewModel", "Exception collect _userSettings", e)
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
                
                var healthData = HealthConnectHelper.HealthData(7.5f, 15, 2, "良好", 8432, 68)
                if (isHCEnabled && HealthConnectHelper.isSdkAvailable(context)) {
                    healthData = HealthConnectHelper.readHealthData(context)
                } else {
                    delay(450)
                }

                val endTime = android.os.SystemClock.elapsedRealtime()
                val syncDuration = endTime - startTime

                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val timeStr = sdf.format(Date())
                
                // Fetch Health Trend using Gemini
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
            } catch (e: Throwable) {
                android.util.Log.e("MorningViewModel", "Exception during syncHealthData", e)
            }
        }
    }

    private suspend fun fetchHealthTrend(data: HealthConnectHelper.HealthData): String {
        val prompt = "你是專業的健康分析助手。請分析以下昨晚的健康數據，並提供一句簡短且具體（20-30字以內）的綜合趨勢報告，鼓勵用戶或給予提醒。數據：睡眠 ${data.sleepHours} 小時（品質：${data.sleepQuality}），步數 ${data.dailySteps} 步，平均心率 ${data.avgHeartRate} bpm。請直接返回報告內容，不要有標題或引號。"
        // Prioritize gemini-nano for on-device AI Core experience
        val modelName = "gemini-nano" 
        
        return try {
            val context = getApplication<Application>().applicationContext
            val responseText = com.example.api.GoogleGenAiClient.generateContent(context, prompt, modelName)
            responseText.trim()
        } catch (e: Throwable) {
            // Fallback trend report
            if (data.sleepHours > 7 && data.dailySteps > 8000) {
                "您今天的活動量不錯，睡眠品質也有所提升，請繼續保持！"
            } else if (data.sleepHours < 6) {
                "昨晚睡眠稍顯不足，建議今天早點休息，並多補充水分。"
            } else {
                "今日各項指標穩定，是充滿活力的一天。"
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
            } catch (e: Throwable) {
                android.util.Log.e("MorningViewModel", "Exception during clearSleepData", e)
            }
        }
    }

    fun updateUserSettings(settings: UserSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getDb()?.userSettingsDao()?.saveUserSettings(settings)
            } catch (e: Throwable) {
                android.util.Log.e("MorningViewModel", "Exception during updateUserSettings", e)
            }
        }
    }

    private fun mapWeatherCode(code: Int): String {
        return when (code) {
            0 -> "晴朗"
            1, 2 -> "多雲時晴"
            3 -> "陰天"
            45, 48 -> "起霧"
            51, 53, 55 -> "毛毛雨"
            61, 63 -> "局部陣雨"
            65 -> "大雨"
            71, 73, 75 -> "降雪"
            80, 81, 82 -> "短暫陣雨"
            95, 96, 99 -> "雷陣雨"
            else -> "多雲"
        }
    }

    fun fetchData() {
        viewModelScope.launch(Dispatchers.IO) {
            // Fetch real location
            val location = locationHelper.getCurrentLocation()
            val lat = location?.latitude ?: 25.0330
            val lon = location?.longitude ?: 121.5654
            
            // Fetch real weather using Open-Meteo
            val newWeather = try {
                val weatherData = OpenMeteoClient.service.getForecast(latitude = lat, longitude = lon)
                WeatherInfo(
                    condition = mapWeatherCode(weatherData.current.weather_code),
                    currentTemp = weatherData.current.temperature_2m.toInt(),
                    maxTemp = weatherData.daily.temperature_2m_max.firstOrNull()?.toInt() ?: 30,
                    minTemp = weatherData.daily.temperature_2m_min.firstOrNull()?.toInt() ?: 22
                )
            } catch (e: Throwable) {
                WeatherInfo(
                    condition = "多雲時晴",
                    currentTemp = 28,
                    maxTemp = 32,
                    minTemp = 24
                )
            }
            _weatherInfo.value = newWeather

            // Read persistent sleep info from userSettings
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

            // Fetch Calendar Events
            try {
                _nextEvents.value = calendarRepository.getNextEvents()
            } catch (e: Throwable) {
                // Handle permission or other errors
            }

            // Fetch News using Gemini with FRESH weather data
            fetchNews(newWeather)
        }
    }

    private suspend fun fetchNews(weather: WeatherInfo) {
        val weatherContext = "目前天氣：${weather.condition}，氣溫 ${weather.currentTemp}°C (最高 ${weather.maxTemp}°C / 最低 ${weather.minTemp}°C)。"
        val prompt = "你是早晨簡報的 AI 助手。請根據目前的虛擬日期 2026年5月22日以及以下即時天氣資訊生成 3-5 則今日簡短新聞重點。$weatherContext 每則新聞需包含標題和摘要。請以繁體中文（台灣）撰寫。請務必讓其中一則新聞與當前天氣的戶外建議相關。請只返回 JSON 數組格式，不要有 Markdown 標記，例如：[{\"title\": \"...\", \"summary\": \"...\"}, ...]"
        // Prioritize gemini-nano for on-device AI Core experience
        val modelName = "gemini-nano"
        
        try {
            val context = getApplication<Application>().applicationContext
            // Use AICore (nano) with fallback to server-side flash
            val responseText = com.example.api.GoogleGenAiClient.generateContent(context, prompt, modelName)
            val cleanedJson = responseText.replace("```json", "").replace("```", "").trim()
            
            val news = json.decodeFromString<List<NewsItem>>(cleanedJson)
            _newsDetail.value = news
        } catch (e: Throwable) {
            // Fault-tolerant secondary fallback using direct Retrofit Client
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") return

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(temperature = 0.7f)
            )

            try {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val jsonText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val cleanedJsonFallback = jsonText.replace("```json", "").replace("```", "").trim()
                
                val news = json.decodeFromString<List<NewsItem>>(cleanedJsonFallback)
                _newsDetail.value = news
            } catch (ex: Throwable) {
                // Ignore error if both fail
            }
        }
    }
}
