package com.example.api

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        val actualModelName = when (modelName) {
            "Gemini Flash Latest" -> "gemini-1.5-flash"
            "aicore" -> "gemini-1.5-flash"
            else -> modelName
        }
        return GenerativeModel(
            modelName = actualModelName,
            apiKey = apiKey,
            generationConfig = config
        )
    }

    /**
     * Executes content generation query using Google Generative AI SDK (com.google.ai.client.generativeai)
     */
    suspend fun generateContentServerSide(prompt: String, modelName: String = "Gemini Flash Latest"): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("未設定 valid API 金鑰。")
        }
        val actualModel = when (modelName) {
            "gemini-nano" -> "Gemini Flash Latest"
            "aicore" -> "gemini-1.5-flash"
            else -> modelName
        }
        try {
            val model = getModel(actualModel)
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
    suspend fun generateContent(prompt: String, modelName: String = "Gemini Flash Latest"): String {
        return generateContentServerSide(prompt, modelName)
    }
}
