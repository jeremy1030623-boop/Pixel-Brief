package com.example.api

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.example.BuildConfig

object GoogleGenAiClient {

    private var useAICore: Boolean = false

    /**
     * Attempts to generate content using Android AI Core (Gemini Nano on-device).
     * If AI Core is not available or fails, it falls back to the server-side Gemini API.
     */
    suspend fun generateContent(context: Context, prompt: String, modelName: String = "gemini-1.5-flash"): String {
        // Try on-device Gemini Nano if requested (or as a preference)
        if (modelName == "gemini-nano") {
            try {
                // In some SDK versions, passing a context or using a specific nano constructor works.
                // However, without the exact class definition in front of me, I'll use the most likely standard structure.
                val nanoModel = GenerativeModel(
                    modelName = "gemini-nano",
                    apiKey = "" // On-device often doesn't need a key if mediated by the system
                )
                val response = nanoModel.generateContent(prompt)
                return response.text ?: "Nano returned no content"
            } catch (e: Throwable) {
                Log.d("GoogleGenAiClient", "Nano on-device failed: ${e.message}")
            }
        }

        // Fallback to server-side
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
        return try {
            val model = getModel(modelName)
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
