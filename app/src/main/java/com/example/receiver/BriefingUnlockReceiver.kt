package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.work.BriefingWorker
import java.util.Calendar

/**
 * Receiver that listens for device unlock (ACTION_USER_PRESENT).
 * Triggers BriefingWorker if conditions are met (e.g., morning time window).
 */
class BriefingUnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            
            // Define "morning" window: 5 AM to 10 AM
            if (hour in 5..10) {
                val prefs = context.getSharedPreferences("briefing_prefs", Context.MODE_PRIVATE)
                val today = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
                val lastRun = prefs.getString("last_briefing_run", "")

                if (lastRun != today) {
                    Log.d("BriefingUnlockReceiver", "Morning unlock detected. Triggering briefing...")
                    
                    val workRequest = OneTimeWorkRequestBuilder<BriefingWorker>().build()
                    WorkManager.getInstance(context).enqueue(workRequest)

                    // Mark as run for today
                    prefs.edit().putString("last_briefing_run", today).apply()
                } else {
                    Log.d("BriefingUnlockReceiver", "Briefing already triggered for today.")
                }
            }
        }
    }
}
