package com.example.api

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.example.BuildConfig

object GoogleGenAiClient {

    /**
     * Executes content generation query using Google Generative AI SDK (com.google.ai.client.generativeai)
     */
    suspend fun generateContent(context: Context, prompt: String, modelName: String = "Gemini Flash Latest"): String {
        return generateContentServerSide(prompt, modelName)
    }

    /**
     * Instantiates the official Google GenAI GenerativeModel object using the registered keys.
     */
    fun getModel(modelName: String = "Gemini Flash Latest"): GenerativeModel {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val config = generationConfig {
            temperature = 0.7f
        }
        val actualModelName = if (modelName == "Gemini Flash Latest") "gemini-1.5-flash-latest" else modelName
        return GenerativeModel(
            modelName = actualModelName,
            apiKey = apiKey,
            generationConfig = config
        )
    }

    /**
     * Executes content generation query using Google Generative AI SDK (com.google.ai.client.generativeai)
     */
    suspend fun generateContentServerSide(prompt: String, modelName: String = "Gemini Flash Latest"): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "未設定 valid API 金鑰。"
        }
        val actualModel = if (modelName == "gemini-nano") "Gemini Flash Latest" else modelName
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
    suspend fun generateContent(prompt: String, modelName: String = "Gemini Flash Latest"): String {
        return generateContentServerSide(prompt, modelName)
    }
}
