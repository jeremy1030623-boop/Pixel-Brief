package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.util.BriefingNotificationHelper
import com.example.util.BriefingAlarmScheduler
import com.example.work.BriefingWorker
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm trigger received! Action: ${intent.action}")

        // 1. Immediately show a beautiful morning greeting notification so it feels responsive and real-time.
        BriefingNotificationHelper.showBriefingNotification(
            context,
            "您的早晨簡報已經準備好囉！快來開啟今日專屬健康與生活智慧分析 ☀️"
        )

        // 2. Trigger the BriefingWorker in the background to fetch/pre-generate Gemini AI briefings,
        // so that when they tap and open the app, it's already completely ready.
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<BriefingWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d("AlarmReceiver", "Successfully enqueued BriefingWorker from alarm trigger.")
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to enqueue BriefingWorker", e)
        }

        // 3. Reschedule for the next day to keep the alarm loop going
        val prefs = context.getSharedPreferences("briefing_notification_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("notification_enabled", false)
        if (enabled) {
            val hour = prefs.getInt("notification_hour", 8)
            val minute = prefs.getInt("notification_minute", 0)
            BriefingAlarmScheduler.scheduleDailyNotification(context, hour, minute)
        }
    }
}
