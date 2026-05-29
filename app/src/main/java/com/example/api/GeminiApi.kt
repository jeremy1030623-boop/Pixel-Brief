package com.example.api

import com.example.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini Request/Response Models for Gson ---
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GenerationConfig(
    val temperature: Float? = null,
    val responseMimeType: String? = null
)

data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?
)

// --- OpenMeteo Models ---
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentWeather?,
    val hourly: HourlyData?
)

data class CurrentWeather(
    val time: String,
    val temperature_2m: Double,
    val weather_code: Int
)

data class HourlyData(
    val time: List<String>,
    val temperature_2m: List<Double>,
    val weather_code: List<Int>
)

// --- Retrofit Service Interfaces ---
interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

interface OpenMeteoApiService {
    @POST("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,weather_code",
        @Query("hourly") hourly: String = "temperature_2m,weather_code",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse
}

// --- Retrofit Setup ---
object RetrofitClient {
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val OPEN_METEO_BASE_URL = "https://api.open-meteo.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val geminiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }

    val openMeteoService: OpenMeteoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(OPEN_METEO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoApiService::class.java)
    }
}

// --- API Helpers ---
object ApiHelper {
    suspend fun generateAiBrief(prompt: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "（請在 Secrets 設置 API 金鑰以啟用「AI 每日簡報」功能）\n這是一個美觀、本地運行的 Pixel Style 晨間簡報工具。今天天氣宜人，以下是您的行程安排！"
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.7f),
            systemInstruction = Content(parts = listOf(Part(text = "你是一個貼心、溫暖的 Pixel 精靈助理。請用優雅、溫馨的繁體中文提供天氣與日程簡報，語氣親切，130字以內。")))
        )

        return try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "早安！今天也是美好的一天。別忘了查看您的行程規劃！"
        } catch (e: Exception) {
            "早安！我們今天也一起加油。請查看您今天的詳細行程！"
        }
    }

    suspend fun fetchFunFact(): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "冷知識：水熊蟲是已知唯一能在太空真空環境中生存的動物！"
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "提供一個有趣、字數50字以內的冷知識或激勵語，繁體中文。 不需要標題，只要冷知識內容。"))))
        )

        return try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "冷知識：貓咪有 32 隻肌肉控制每隻耳朵！"
        } catch (e: Exception) {
            "今日激勵：你比自己想像的更勇敢、更堅強！"
        }
    }
}
