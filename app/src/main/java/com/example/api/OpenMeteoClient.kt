package com.example.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Serializable
data class OpenMeteoResponse(
    val current: CurrentWeatherData,
    val daily: DailyWeatherData
)

@Serializable
data class CurrentWeatherData(
    val temperature_2m: Float,
    val weather_code: Int
)

@Serializable
data class DailyWeatherData(
    val temperature_2m_max: List<Float>,
    val temperature_2m_min: List<Float>
)

interface OpenMeteoApiService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double = 25.0330,
        @Query("longitude") longitude: Double = 121.5654,
        @Query("current") current: String = "temperature_2m,weather_code",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "Asia/Taipei"
    ): OpenMeteoResponse
}

object OpenMeteoClient {
    private const val BASE_URL = "https://api.open-meteo.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    val service: OpenMeteoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenMeteoApiService::class.java)
    }
}
