package com.example.model

import kotlinx.serialization.Serializable

data class HourlyForecast(
    val time: String,
    val temperature: Int,
    val condition: String
)

data class WeatherInfo(
    val condition: String = "晴朗",
    val currentTemp: Int = 26,
    val maxTemp: Int = 30,
    val minTemp: Int = 22,
    val apparentTemp: Int = 27,
    val humidity: Int = 76,
    val windSpeed: Float = 11.2f,
    val precipitationProb: Int = 10,
    val uvIndex: Float = 5.0f,
    val iconRes: Int = 0,
    val locationName: String = "台北",
    val isGpsLocated: Boolean = false,
    val description: String = "今日天氣晴朗舒適，適合外出活動。",
    val hourlyForecast: List<HourlyForecast> = emptyList()
)

data class SleepInfo(
    val hours: Float = 7.5f,
    val snoringMinutes: Int = 15,
    val coughCount: Int = 2,
    val qualityScore: Int = 85
)

data class HealthInfo(
    val steps: Int = 0,
    val heartRate: Int = 0,
    val trendReport: String = ""
)

@Serializable
data class NewsItem(
    val title: String = "無標題",
    val summary: String = "尚無摘要內容",
    val url: String? = null
)
