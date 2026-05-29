package com.example.model

import kotlinx.serialization.Serializable

data class WeatherInfo(
    val condition: String = "晴朗",
    val currentTemp: Int = 26,
    val maxTemp: Int = 30,
    val minTemp: Int = 22,
    val iconRes: Int = 0 // In a real app, this would be an icon ID
)

data class SleepInfo(
    val hours: Float = 7.5f,
    val snoringMinutes: Int = 15,
    val coughCount: Int = 2
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
