package com.example.api

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.example.BuildConfig

object GoogleGenAiClient {

    /**
     * Attempts to generate content. In a real environment, this would call 
     * the Gemini Nano model via AICore on supported devices.
     * Here we utilize the 1.5 Flash model which offers similar latency profiles.
     */
    suspend fun generateContent(context: Context, prompt: String, modelName: String = "gemini-1.5-flash"): String {
        // Fallback to server-side for the applet demo, 
        // with the restriction check performed at the Activity level.
        return generateContentServerSide(prompt, modelName)
    }

    /**
     * Instantiates the official Google GenAI GenerativeModel object using the registered keys.
     */
    fun getModel(modelName: String = "gemini-1.5-flash"): GenerativeModel {
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
    suspend fun generateContentServerSide(prompt: String, modelName: String = "gemini-1.5-flash"): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "未設定 valid API 金鑰。"
        }
        val actualModel = if (modelName == "gemini-nano") "gemini-1.5-flash" else modelName
        return try {
            val model = getModel(actualModel)
            val response = model.generateContent(prompt)
            response.text ?: "無內容返回"
        } catch (e: Throwable) {
            Log.e("GoogleGenAiClient", "Google GenAI SDK error", e)
            "SDK 錯誤: ${e.localizedMessage ?: e.message}"
        }
    }

    // Deprecated or compatibility wrapper
    suspend fun generateContent(prompt: String, modelName: String = "gemini-1.5-flash"): String {
        return generateContentServerSide(prompt, modelName)
    }
}
