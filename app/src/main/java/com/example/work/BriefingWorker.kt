package com.example.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.BriefingRepository
import com.example.util.BriefingNotificationHelper
import kotlinx.coroutines.flow.first

class BriefingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("BriefingWorker", "Starting background briefing generation...")
        
        val db = AppDatabase.getDatabase(applicationContext)
        val repository = BriefingRepository(applicationContext, db)
        
        // Read user settings for personalization
        val settings = db.userSettingsDao().getUserSettings().first()
        val username = settings?.username ?: "用戶"
        
        // Use a personalized morning prompt
        val prompt = "你是專業且溫柔的個人健康規劃與生活大師。請為用戶 ${username} 準備一段今日的晨間問候與簡短的健康提醒。字數約 80-120 字，語氣親切溫暖，使用繁體中文。請包含一段專屬的開場白（例如：早安，${username}！）。"
        
        return try {
            val briefing = repository.getBriefingAction(prompt, forceRefresh = true)
            
            if (briefing != "發生未知錯誤" && !briefing.contains("無法取得")) {
                Log.d("BriefingWorker", "Briefing generated successfully")
                BriefingNotificationHelper.showBriefingNotification(applicationContext, briefing)
                Result.success()
            } else {
                Log.w("BriefingWorker", "Briefing generation returned an error string")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("BriefingWorker", "Error in BriefingWorker", e)
            Result.failure()
        }
    }
}
