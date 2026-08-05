package com.example.api

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GoogleGenAiClient {

    private fun mapToActualModelName(modelName: String): String {
        return when (modelName.lowercase()) {
            "gemini flash latest", "gemini-1.5-flash", "gemini-nano", "aicore" -> "gemini-3.5-flash"
            "gemini-1.5-pro" -> "gemini-3.1-pro-preview"
            else -> modelName
        }
    }

    /**
     * Executes content generation query using Google Generative AI SDK (com.google.ai.client.generativeai)
     */
    suspend fun generateContent(context: Context, prompt: String, modelName: String = "gemini-3.5-flash"): String {
        return generateContentServerSide(prompt, modelName)
    }

    /**
     * Instantiates the official Google GenAI GenerativeModel object using the registered keys.
     */
    fun getModel(modelName: String = "gemini-3.5-flash"): GenerativeModel {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val config = generationConfig {
            temperature = 0.7f
        }
        val actualModelName = mapToActualModelName(modelName)
        return GenerativeModel(
            modelName = actualModelName,
            apiKey = apiKey,
            generationConfig = config
        )
    }

    /**
     * Executes content generation query using Google Generative AI SDK (com.google.ai.client.generativeai)
     */
    suspend fun generateContentServerSide(prompt: String, modelName: String = "gemini-3.5-flash"): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("未設定 valid API 金鑰。")
        }
        try {
            val model = getModel(modelName)
            val response = model.generateContent(prompt)
            response.text ?: throw IllegalStateException("無內容返回")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            val cause = e.cause
            if (cause is kotlinx.coroutines.CancellationException) {
                throw cause
            }
            Log.e("GoogleGenAiClient", "Google GenAI SDK error", e)
            throw e
        }
    }

    // Deprecated or compatibility wrapper
    suspend fun generateContent(prompt: String, modelName: String = "gemini-3.5-flash"): String {
        return generateContentServerSide(prompt, modelName)
    }
}
