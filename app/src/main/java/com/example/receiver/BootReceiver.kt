package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.util.BriefingAlarmScheduler

/**
 * Receiver that handles device boot.
 * Ensures any necessary background scheduling is active.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted. Ensuring briefing scheduler is ready.")
            
            // Reschedule daily briefing notification if enabled by user
            val prefs = context.getSharedPreferences("briefing_notification_prefs", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("notification_enabled", false)
            if (enabled) {
                val hour = prefs.getInt("notification_hour", 8)
                val minute = prefs.getInt("notification_minute", 0)
                Log.d("BootReceiver", "Rescheduling daily alarm notification at $hour:$minute on boot")
                BriefingAlarmScheduler.scheduleDailyNotification(context, hour, minute)
            }
        }
    }
}
