package com.example.model

import java.io.Serializable

data class WeatherInfo(
    val condition: String,
    val currentTemp: Double,
    val maxTemp: Double,
    val minTemp: Double,
    val location: String,
    val hourlyForecasts: List<HourlyForecast> = emptyList()
) : Serializable

data class HourlyForecast(
    val time: String,
    val temp: Double,
    val condition: String
) : Serializable

data class AgendaEvent(
    val title: String,
    val dateTime: String,
    val isAllDay: Boolean = false
) : Serializable

data class BriefData(
    val aiBrief: String,
    val weather: WeatherInfo,
    val events: List<AgendaEvent>,
    val funFact: String
) : Serializable
