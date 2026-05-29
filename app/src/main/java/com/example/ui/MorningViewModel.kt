package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.ApiHelper
import com.example.api.RetrofitClient
import com.example.data.CalendarHelper
import com.example.model.AgendaEvent
import com.example.model.BriefData
import com.example.model.HourlyForecast
import com.example.model.WeatherInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class UiState {
    object Loading : UiState()
    data class Success(val data: BriefData) : UiState()
    data class Error(val message: String) : UiState()
}

class MorningViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    // Temperature unit settings: C (Celsius) or F (Fahrenheit)
    private val _tempUnit = MutableStateFlow("C")
    val tempUnit: StateFlow<String> = _tempUnit

    init {
        loadData()
    }

    fun toggleTempUnit() {
        _tempUnit.value = if (_tempUnit.value == "C") "F" else "C"
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // 1. Fetch Location Weather
                val defLat = 25.0330 // Taipei
                val defLon = 121.5654
                val locationName = "台北市"

                val weatherResponse = RetrofitClient.openMeteoService.getForecast(defLat, defLon)
                val currentTemp = weatherResponse.current?.temperature_2m ?: 24.5
                val currentCode = weatherResponse.current?.weather_code ?: 0

                val (conditionStr, isRainyCode) = mapWeatherCode(currentCode)

                // 2. Map hourly forecasts
                val hourlyList = mutableListOf<HourlyForecast>()
                val times = weatherResponse.hourly?.time ?: emptyList()
                val temps = weatherResponse.hourly?.temperature_2m ?: emptyList()
                val codes = weatherResponse.hourly?.weather_code ?: emptyList()

                // Map next 5-6 hours
                val currentHourStr = SimpleDateFormat("HH", Locale.getDefault()).format(Date())
                var startIndex = times.indexOfFirst { it.contains("T$currentHourStr") }
                if (startIndex == -1) startIndex = 0

                for (i in startIndex until (startIndex + 6)) {
                    if (i < times.size && i < temps.size && i < codes.size) {
                        val timeRaw = times[i] // e.g., "2026-05-29T13:00"
                        val formattedTime = timeRaw.substringAfter("T") // "13:00"
                        val tempVal = temps[i]
                        val (condVal, _) = mapWeatherCode(codes[i])
                        hourlyList.add(HourlyForecast(formattedTime, tempVal, condVal))
                    }
                }

                // Max and Min derived
                val todayTemps = if (temps.size >= 24) temps.subList(0, 24) else temps
                val maxTemp = todayTemps.maxOrNull() ?: (currentTemp + 4)
                val minTemp = todayTemps.minOrNull() ?: (currentTemp - 4)

                val weatherInfo = WeatherInfo(
                    condition = conditionStr,
                    currentTemp = currentTemp,
                    maxTemp = maxTemp,
                    minTemp = minTemp,
                    location = locationName,
                    hourlyForecasts = hourlyList
                )

                // 3. Fetch Agenda Events
                val events = CalendarHelper.fetchEvents(getApplication())

                // 4. Construct AI briefing & Fun Fact from Gemini
                val agendaSummaryText = events.joinToString(", ") { "${it.title} 于 ${it.dateTime}" }
                val promptText = "今天是美好的一天。目前台北天氣為：${conditionStr}，氣溫為 ${currentTemp}度。今天的行程有：$agendaSummaryText。請依此生成晨言溫馨簡報。"
                
                val briefText = ApiHelper.generateAiBrief(promptText)
                val factText = ApiHelper.fetchFunFact()

                _uiState.value = UiState.Success(
                    BriefData(
                        aiBrief = briefText,
                        weather = weatherInfo,
                        events = events,
                        funFact = factText
                    )
                )

            } catch (e: Exception) {
                _uiState.value = UiState.Error("載入失敗：${e.localizedMessage ?: "網路連線異常"}")
            }
        }
    }

    private fun mapWeatherCode(code: Int): Pair<String, Boolean> {
        return when (code) {
            0 -> Pair("晴朗無雲", false)
            1, 2 -> Pair("晴時多雲", false)
            3 -> Pair("多雲陰天", false)
            45, 48 -> Pair("濃霧籠罩", false)
            51, 53, 55 -> Pair("毛毛雨", true)
            61, 63, 65 -> Pair("陣雨連連", true)
            71, 73, 75 -> Pair("皚皚白雪", false)
            80, 81, 82 -> Pair("強降雨", true)
            95, 96, 99 -> Pair("雷陣雨", true)
            else -> Pair("大晴天", false)
        }
    }
}
