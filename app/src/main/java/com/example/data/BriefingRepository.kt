package com.example.data

import android.content.Context
import android.util.Log
import com.example.api.GoogleGenAiClient
import com.example.api.LocalAiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Repository that manages AI-generated briefings with a dual-path logic:
 * 1. Checks for local AI Core (Gemini Nano) availability.
 * 2. Uses local inference if supported.
 * 3. Falls back to Gemini Cloud API if local is unsupported or fails.
 * 4. Implements caching to reduce redundant API calls.
 */
class BriefingRepository(private val context: Context, private val db: AppDatabase) {

    private fun md5(input: String): String {
        return MessageDigest.getInstance("MD5").digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates content with local-first preference and caching.
     */
    suspend fun getBriefingAction(prompt: String, forceRefresh: Boolean = false): String = withContext(Dispatchers.IO) {
        val hash = md5(prompt)
        
        // 1. Cache Check
        if (!forceRefresh) {
            try {
                val cached = db.aiCacheDao().getCachedResponse(hash)
                if (cached != null) {
                    val age = System.currentTimeMillis() - cached.timestamp
                    if (age < 6 * 60 * 60 * 1000) { // 6 hours cache for briefings
                        Log.d("BriefingRepository", "Cache Hit for prompt hash: $hash")
                        return@withContext cached.response
                    }
                }
            } catch (e: Exception) {
                Log.e("BriefingRepository", "Cache read error", e)
            }
        }

        // 2. Dual-Path Logic
        var result: String? = null

        // Try Local AI Core (Gemini Nano) first
        if (LocalAiClient.isAiCoreAvailable(context)) {
            Log.d("BriefingRepository", "AI Core detected. Attempting local inference...")
            result = LocalAiClient.generateContentLocal(context, prompt)
        }

        // Fallback to Cloud Gemini API
        if (result == null) {
            Log.d("BriefingRepository", "Local inference unavailable or failed. Falling back to Cloud API.")
            result = try {
                GoogleGenAiClient.generateContentServerSide(prompt)
            } catch (e: Exception) {
                Log.e("BriefingRepository", "Cloud API failed", e)
                "無法取得 AI 摘要。請檢查網路連線或稍後再試。"
            }
        }

        // 3. Save to Cache
        if (result != null && result.isNotEmpty() && result != "無法取得 AI 摘要。請檢查網路連線或稍後再試。") {
            try {
                db.aiCacheDao().saveToCache(AiCache(promptHash = hash, response = result))
                Log.d("BriefingRepository", "Saved response to cache")
            } catch (e: Exception) {
                Log.e("BriefingRepository", "Cache save error", e)
            }
        }

        return@withContext result ?: "發生未知錯誤"
    }

    /**
     * Specialized briefing for News
     */
    suspend fun summarizeNews(newsContent: String): String {
        val prompt = "請將以下新聞內容摘要成 3 個重點，使用繁體中文：\n\n$newsContent"
        return getBriefingAction(prompt)
    }

    /**
     * Specialized briefing for Sleep Analysis
     */
    suspend fun analyzeSleep(sleepData: String): String {
        val prompt = "根據以下睡眠數據提供一項具體的建議，使用繁體中文：\n\n$sleepData"
        return getBriefingAction(prompt)
    }

    /**
     * Specialized briefing for Schedule
     */
    suspend fun organizeSchedule(events: String): String {
        val prompt = "請整理以下行程並給予簡單的提醒，使用繁體中文：\n\n$events"
        return getBriefingAction(prompt)
    }
}
