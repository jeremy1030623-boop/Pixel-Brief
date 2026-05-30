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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

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

    private val _goalSuggestion = MutableStateFlow<String>("")
    val goalSuggestion = _goalSuggestion.asStateFlow()

    private val _funFact = MutableStateFlow<String>("你知道嗎？每天適量喝水，能顯著提升專注力與新陳代謝。")
    val funFact = _funFact.asStateFlow()

    init {
        // fetchData is already setting up data on IO
        fetchData()
        
        // Periodic update every 30 minutes
        viewModelScope.launch {
            while (true) {
                delay(1800000L) // 30 minutes in milliseconds
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
                _currentTimeFlow.collect { (time, greeting) ->
                    _timeState.value = TimeState(time, greeting)
                }
            } catch (e: Throwable) {
                android.util.Log.e("MorningViewModel", "Exception collect _currentTimeFlow", e)
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
                    // Trigger suggestion update when sleep/health syncing changes
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
        } catch (e: Throwable) {
            "你知道嗎？每天適量喝水，能顯著提升專注力與新陳代謝。"
        }
    }

    private suspend fun updateGoalSuggestions() {
        val sleep = _sleepInfo.value
        val health = _healthInfo.value
        val weather = _weatherInfo.value
        
        val prompt = "你是專業生活規劃簡報大師。請根據以下數據：天氣 ${weather.condition} (${weather.currentTemp}°C)，昨晚睡眠 ${sleep.hours} 小時，今日步數 ${health.steps}。請給出一段針對今日生活目標的個人化建議，包含戶外活動調整建議與休息規劃，字數約 60-80 字。語氣溫馨、充滿正能量、務實，不要條列式，以簡報大摘要形式呈現。"
        val modelName = _userSettings.value?.geminiModelSelected ?: "Gemini Flash Latest"
        
        _goalSuggestion.value = try {
            val context = getApplication<Application>().applicationContext
            val responseText = com.example.api.GoogleGenAiClient.generateContent(context, prompt, modelName)
            responseText.trim()
        } catch (e: Throwable) {
             if (sleep.hours < 6f) {
                "昨晚睡眠較少，今日請務必 prioritize 休息，減少高強度活動，並適時補充水分與小憩。"
            } else if (weather.condition.contains("晴") && health.steps < 5000) {
                "今天天氣絕佳，且最近活動量較少，強烈建議您安排 20 分鐘戶外漫步，吸收陽光恢復活力。"
            } else {
                "今天持續維持均衡作息，保持心情愉悅即可，隨時關注身體狀態，適量休息。"
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
        val prompt = "你是專業且溫柔的個人健康規劃與生活大師。請分析以下昨晚到今天的健康數據，並提供一段字數約 80-120 字的『深度健康生活綜合指導文字簡報』，包含具體的身體狀態評估、今日飲食與運動之科學建議，以及晨間開機的精神小叮嚀。請務必溫馨且充滿細節，直接返回簡報文字，不要有任何標題或外層引號。數據：睡眠 ${data.sleepHours} 小時（品質：${data.sleepQuality}，打鼾 ${data.snoringMinutes} 分鐘，咳嗽 ${data.coughCount} 次），今日步數 ${data.dailySteps} 步，平均心率 ${data.avgHeartRate} bpm。"
        // Prioritize gemini-nano for on-device AI Core experience
        val modelName = "gemini-nano" 
        
        return try {
            val context = getApplication<Application>().applicationContext
            val responseText = com.example.api.GoogleGenAiClient.generateContent(context, prompt, modelName)
            responseText.trim()
        } catch (e: Throwable) {
            // Fallback trend report
            if (data.sleepHours > 7 && data.dailySteps > 8000) {
                "【活力滿分】您昨晚的睡眠時數長達 ${data.sleepHours} 小時，深層睡眠品質卓越；搭配今日高達 ${data.dailySteps} 步的活躍步伐，您的心肺功能與代謝正處於極佳狀態。建議清晨多攝取高蛋白與富含維生素的膳食，維持一整天高能量釋放。今天非常適合進行戶外快走，讓充足陽光調節您的生理時鐘！"
            } else if (data.sleepHours < 6) {
                "【溫馨守護】您昨晚的睡眠時數僅 ${data.sleepHours} 小時，身體開機稍顯疲憊，且心律偏高。今日建議在飲食中補充足夠的純水與複合性碳水化合物，保持體液平衡與專注。今天請避免挑戰極限強度的訓練，改為 15 分鐘的溫和拉伸與深呼吸，晚上提早入睡以極速修護元氣。"
            } else {
                "【元氣平衡】今日您的各項健康指標整體表現平穩。睡眠時數達 ${data.sleepHours} 小時，心率穩定在 ${data.avgHeartRate} bpm。建議中午進行 10 分鐘的靜坐冥想或深呼吸，並在工作時每隔一小時起身活動，能大幅改善下半身循環。保持平和心境，迎接充實、健康而自信的一天！"
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
                android.util.Log.d("MorningViewModel", "User settings updated, reloading data actively...")
                fetchData()
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
            _isRefreshing.value = true
            try {
                // Determine city name and coordinates
                val location = locationHelper.getCurrentLocation()
                val detected = getCityNameFromLocation(location)
                val defaultCityOpt = _userSettings.value?.defaultCity ?: "台北"
                val finalCity = if (detected.isNotEmpty() && detected != "台灣") detected else defaultCityOpt
                
                // Get lat/lon: prioritize GPS location, fallback to mapped city coordinates
                val (lat, lon) = if (location != null) {
                    Pair(location.latitude, location.longitude)
                } else {
                    getCoordinatesForCity(finalCity)
                }
                
                // Fetch real weather using System Cache first, fallback to Open-Meteo
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
                                        systemWeather = WeatherInfo(
                                            condition = condStr,
                                            currentTemp = temp,
                                            maxTemp = temp + 4,
                                            minTemp = temp - 4,
                                            locationName = finalCity
                                        )
                                        android.util.Log.d("MorningViewModel", "Loaded weather from ContentProvider: $systemWeather")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MorningViewModel", "ContentProvider fetch failed", e)
                    }

                    if (systemWeather != null) {
                        systemWeather!!.copy(locationName = finalCity)
                    } else {
                        val weatherData = OpenMeteoClient.service.getForecast(latitude = lat, longitude = lon)
                        WeatherInfo(
                            condition = mapWeatherCode(weatherData.current.weather_code),
                            currentTemp = weatherData.current.temperature_2m.toInt(),
                            maxTemp = weatherData.daily.temperature_2m_max.firstOrNull()?.toInt() ?: 30,
                            minTemp = weatherData.daily.temperature_2m_min.firstOrNull()?.toInt() ?: 22,
                            apparentTemp = weatherData.current.apparent_temperature?.toInt() ?: (weatherData.current.temperature_2m.toInt() + 1),
                            humidity = weatherData.current.relative_humidity_2m ?: 75,
                            windSpeed = weatherData.current.wind_speed_10m ?: 10f,
                            precipitationProb = weatherData.daily.precipitation_probability_max?.firstOrNull() ?: 10,
                            uvIndex = weatherData.daily.uv_index_max?.firstOrNull() ?: 5.0f,
                            locationName = finalCity
                        )
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("MorningViewModel", "Open-Meteo forecast fetch failed", e)
                    WeatherInfo(
                        condition = "多雲時晴",
                        currentTemp = 28,
                        maxTemp = 32,
                        minTemp = 24,
                        locationName = finalCity
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
                    android.util.Log.e("MorningViewModel", "Failed to fetch calendar events", e)
                }

                // Fetch News using Gemini with FRESH weather data
                fetchNews(newWeather)
            } catch (e: Throwable) {
                android.util.Log.e("MorningViewModel", "Error inside fetchData", e)
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
            city.contains("基隆") -> Pair(25.1283, 121.7392)
            city.contains("新竹") -> Pair(24.8138, 120.9675)
            city.contains("苗栗") -> Pair(24.5601, 120.8206)
            city.contains("彰化") -> Pair(24.0517, 120.5161)
            city.contains("南投") -> Pair(23.9155, 120.6860)
            city.contains("雲林") -> Pair(23.7092, 120.4313)
            city.contains("嘉義") -> Pair(23.4801, 120.4491)
            city.contains("屏東") -> Pair(22.6660, 120.4859)
            city.contains("宜蘭") -> Pair(24.7021, 121.7377)
            city.contains("花蓮") -> Pair(23.9872, 121.6016)
            city.contains("台東") || city.contains("臺東") -> Pair(22.7583, 121.1444)
            city.contains("澎湖") -> Pair(23.5711, 119.5793)
            city.contains("金門") -> Pair(24.4494, 118.3773)
            city.contains("馬祖") -> Pair(26.1558, 119.9519)
            else -> Pair(25.0330, 121.5654) // default to Taipei
        }
    }

    private fun getCityFromCoordinates(lat: Double, lon: Double): String {
        return when {
            lat in 24.95..25.25 && lon in 121.45..121.65 -> "台北"
            lat in 24.85..25.10 && lon in 121.20..121.49 -> "新北"
            lat in 24.90..25.15 && lon in 121.00..121.35 -> "桃園"
            lat in 24.68..24.90 && lon in 120.90..121.15 -> "新竹"
            lat in 24.30..24.60 && lon in 120.70..120.95 -> "苗栗"
            lat in 24.05..24.35 && lon in 120.55..120.80 -> "台中"
            lat in 23.95..24.15 && lon in 120.40..120.65 -> "彰化"
            lat in 23.70..24.00 && lon in 120.65..121.10 -> "南投"
            lat in 23.60..23.85 && lon in 120.15..120.55 -> "雲林"
            lat in 23.35..23.60 && lon in 120.10..120.50 -> "嘉義"
            lat in 22.85..23.25 && lon in 120.10..120.45 -> "台南"
            lat in 22.45..22.85 && lon in 120.20..120.60 -> "高雄"
            lat in 21.85..22.45 && lon in 120.35..120.90 -> "屏東"
            lat in 24.40..24.99 && lon in 121.60..121.90 -> "宜蘭"
            lat in 23.40..24.30 && lon in 121.25..121.65 -> "花蓮"
            lat in 22.30..23.20 && lon in 120.75..121.20 -> "台東"
            else -> "台灣"
        }
    }

    private fun getCityNameFromLocation(location: android.location.Location?): String {
        if (location == null) return ""
        try {
            val context = getApplication<Application>().applicationContext
            if (android.location.Geocoder.isPresent()) {
                val geocoder = android.location.Geocoder(context, Locale.getDefault())
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
            if (newsMode == "international") {
                locationText = "國際"
                GoogleNewsFetcher.fetchInternationalNews()
            } else {
                val location = locationHelper.getCurrentLocation()
                val detected = getCityNameFromLocation(location)
                val defaultOpt = _userSettings.value?.defaultCity ?: "台北"
                val finalCity = if (detected.isNotEmpty() && detected != "台灣") detected else defaultOpt
                locationText = finalCity
                GoogleNewsFetcher.fetchNewsByLocation(finalCity)
            }
        } catch (e: Throwable) {
            android.util.Log.e("MorningViewModel", "Failed to fetch real Google News via RSS", e)
            emptyList()
        }

        val realNewsContext = if (realNews.isNotEmpty()) {
            val headlines = realNews.mapIndexed { index, item ->
                "${index + 1}. [標題] ${item.title} (媒體來源: ${item.source}, 網址: ${item.link})"
            }.joinToString("\n")
            if (newsMode == "international") {
                "以下是今天真實採集到的國際世界 Google News 最新熱門頭條與其來源網址：\n$headlines\n\n請你扮演高階 AI 早晨簡報助理，將以上真實國際新聞，精心挑選出 3 到 5 則最重要、最高水準且生活實用的世界政經或國際焦點話題。請為每一選取的焦點編輯一段親切、詳實、極具深度與溫度，且充滿整合指導價值的『早晨啟動文字簡報大摘要』（字數必須在 120 到 200 字之間）。請詳細描寫脈絡，提供其對個人生活、科學保健或全球動態的具體啟示，並務必在對應欄位填上該則新聞原本對應的 `url` (來源連結)。"
            } else {
                "以下是今天真實採集到的 [$locationText] 地方與在地 Google News 最新熱門頭條與其來源網址：\n$headlines\n\n請你扮演高階 AI 早晨簡報助理，將以上真實新聞，精心挑選出 3 到 5 則與 [$locationText] 地方生活、交通、發展或周邊生活息息相關的在地重要話題。請為每一選取的焦點編輯一段親切、詳實、極具深度與溫度，且充滿整合指導價值的『早晨啟動文字簡報大摘要』（字數必須在 120 到 200 字之間）。請結合在地人的日常作息、防護、通勤或週末出行，深入拓展報導細節。並務必在對應欄位填上該則新聞原本對應的 `url` (來源連結)。"
            }
        } else {
            if (newsMode == "international") {
                "（暫時無法取得真實 Google News RSS，請你直接為讀者虛擬生成 3 到 5 則富有正向能量、精緻充實、段落長度約 120 到 200 字的國際政經、世界脈動與科技健康新聞話題文字簡報，包含實用的行動指導建議，`url` 請留空）"
            } else {
                "（暫時無法取得真實 Google News RSS，請你直接為讀者虛擬生成 3 到 5 則與 [$locationText] 在地生活、地方交通與生活日常息息相關、溫馨正能量、段落長度約 120 到 200 字的焦點新聞話題文字簡報，給出極具生活實用乾貨與健康小訣竅，`url` 請留空）"
            }
        }

        val isSunny = weather.condition.contains("晴") 
        val isHot = weather.currentTemp > 28
        val sunProtectionAdvice = if (isSunny && isHot) {
            "\n！！特別提醒：今日天氣晴朗且溫度較高，曬太陽時間不宜過長，請務必加強防曬措施，攜帶遮陽傘帽，並多補充水分，避免中暑。"
        } else {
            "\n請務必讓其中一則簡報重點與今日天氣、穿著、紫外線防護或戶外活動建議深度呼應並給予極其溫馨的貼心指引。"
        }

        val prompt = "$realNewsContext\n\n目前天氣環境資訊如下：\n目前天氣：${weather.condition}，氣溫 ${weather.currentTemp}°C (最高 ${weather.maxTemp}°C / 最低 ${weather.minTemp}°C)。$sunProtectionAdvice\n\n請以繁體中文（台灣）撰寫。請只返回 JSON 數組格式的字串，不要包含 ```json 或 ``` 標記，也不要有任何其他引導敘述文字，嚴格遵守以下範例格式：\n[{\"title\": \"焦點標題\", \"summary\": \"親切深入的早安大摘要文字，字數保持120-200字，乾貨滿滿...\", \"url\": \"該則新聞對應的原始/來源連結或空\"}, ...]"
        val modelName = _userSettings.value?.geminiModelSelected ?: "gemini-1.5-flash"
        
        try {
            val context = getApplication<Application>().applicationContext
            val responseText = com.example.api.GoogleGenAiClient.generateContent(context, prompt, modelName)
            val cleanedJson = responseText.replace("```json", "").replace("```", "").trim()
            
            val news = json.decodeFromString<List<NewsItem>>(cleanedJson)
            _newsDetail.value = news
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
                    else -> modelName
                }
                val response = RetrofitClient.service.generateContent(actualModel, apiKey, request)
                val jsonText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val cleanedJsonFallback = jsonText.replace("```json", "").replace("```", "").trim()
                
                val news = json.decodeFromString<List<NewsItem>>(cleanedJsonFallback)
                _newsDetail.value = news
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
