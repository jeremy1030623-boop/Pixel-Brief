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
    private val db = AppDatabase.getDatabase(application)
    private val calendarRepository = CalendarRepository(application.contentResolver)
    
    private val _userSettings = db.userSettingsDao().getUserSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())
    val userSettings = _userSettings

    private val _weatherInfo = MutableStateFlow(WeatherInfo())
    val weatherInfo = _weatherInfo.asStateFlow()

    private val _sleepInfo = MutableStateFlow(SleepInfo())
    val sleepInfo = _sleepInfo.asStateFlow()

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

    private val _currentTime = MutableStateFlow("")
    val currentTime = _currentTime.asStateFlow()

    private val _currentTimeFlow = flow {
        while (true) {
            val pattern = if (_userSettings.value?.is24HourFormat == true) "HH:mm" else "hh:mm a"
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            emit(sdf.format(Date()))
            delay(1000)
        }
    }.flowOn(Dispatchers.Default)

    val username = _userSettings.map { it?.username ?: "Jeremy" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Jeremy")

    init {
        // fetchData is already setting up data on IO
        fetchData()
        
        viewModelScope.launch {
            _currentTimeFlow.collect {
                _currentTime.value = it
            }
        }
        
        viewModelScope.launch {
            _userSettings.collect { settings ->
                if (settings != null && settings.isSleepSynced) {
                    _sleepInfo.value = SleepInfo(
                        hours = settings.sleepHours,
                        snoringMinutes = settings.sleepSnoringMinutes,
                        coughCount = settings.sleepCoughCount
                    )
                } else {
                    _sleepInfo.value = SleepInfo(hours = 0f, snoringMinutes = 0, coughCount = 0)
                }
            }
        }
    }

    fun syncGoogleClockSleepData() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSettings = _userSettings.value ?: UserSettings()
            val context = getApplication<Application>().applicationContext
            val isHCEnabled = currentSettings.isHealthSyncEnabled
            var sleepHours = 7.5f
            if (isHCEnabled && HealthConnectHelper.isSdkAvailable(context)) {
                val duration = HealthConnectHelper.readSleepDurationHours(context)
                if (duration > 0f) {
                    sleepHours = duration
                }
            }

            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeStr = sdf.format(Date())
            
            val updatedSettings = currentSettings.copy(
                isSleepSynced = true,
                sleepHours = sleepHours,
                sleepSnoringMinutes = 15,
                sleepCoughCount = 2,
                lastSyncTime = timeStr
            )
            db.userSettingsDao().saveUserSettings(updatedSettings)
        }
    }

    fun clearSleepData() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSettings = _userSettings.value ?: UserSettings()
            val updatedSettings = currentSettings.copy(
                isSleepSynced = false,
                sleepHours = 0f,
                sleepSnoringMinutes = 0,
                sleepCoughCount = 0,
                lastSyncTime = ""
            )
            db.userSettingsDao().saveUserSettings(updatedSettings)
        }
    }

    fun updateUserSettings(settings: UserSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            db.userSettingsDao().saveUserSettings(settings)
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
            // Fetch real weather using Open-Meteo
            try {
                val weatherData = OpenMeteoClient.service.getForecast()
                _weatherInfo.value = WeatherInfo(
                    condition = mapWeatherCode(weatherData.current.weather_code),
                    currentTemp = weatherData.current.temperature_2m.toInt(),
                    maxTemp = weatherData.daily.temperature_2m_max.firstOrNull()?.toInt() ?: 30,
                    minTemp = weatherData.daily.temperature_2m_min.firstOrNull()?.toInt() ?: 22
                )
            } catch (e: Throwable) {
                _weatherInfo.value = WeatherInfo(
                    condition = "多雲時晴",
                    currentTemp = 28,
                    maxTemp = 32,
                    minTemp = 24
                )
            }

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

            // Fetch News using Gemini
            fetchNews()
        }
    }

    private suspend fun fetchNews() {
        val prompt = "你是早晨簡報的 AI 助手。請根據目前的虛擬日期 2026年5月22日，生成 3-5 則今日簡短新聞重點。每則新聞需包含標題和摘要。請以簡體/繁體中文（台灣）撰寫。請只返回 JSON 數組格式，不要有 Markdown 標記，例如：[{\"title\": \"...\", \"summary\": \"...\"}, ...]"
        val modelName = _userSettings.value?.geminiModelSelected ?: "gemini-1.5-flash"
        
        try {
            // Primary client: Google GenAI SDK (com.google.ai.client.generativeai)
            val responseText = com.example.api.GoogleGenAiClient.generateContent(prompt, modelName)
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
