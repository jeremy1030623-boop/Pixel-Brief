package com.example.api

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.example.BuildConfig

object GoogleGenAiClient {

    /**
     * Instantiates the official Google GenAI GenerativeModel object using the registered keys.
     */
    fun getModel(modelName: String = "gemini-2.5-flash"): GenerativeModel {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val config = generationConfig {
            temperature = 0.7f
        }
        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = config
        )
    }

    /**
     * Executes content generation query using Google Generative AI SDK (com.google.ai.client.generativeai)
     */
    suspend fun generateContent(prompt: String, modelName: String = "gemini-2.5-flash"): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "未設定 valid API 金鑰。"
        }
        return try {
            val model = getModel(modelName)
            val response = model.generateContent(prompt)
            response.text ?: "無內容返回"
        } catch (e: Throwable) {
            Log.e("GoogleGenAiClient", "Google GenAI SDK error", e)
            "SDK 錯誤: ${e.localizedMessage ?: e.message}"
        }
    }
}
