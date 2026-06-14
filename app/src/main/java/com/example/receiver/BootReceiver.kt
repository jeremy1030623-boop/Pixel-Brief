package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver that handles device boot.
 * Ensures any necessary background scheduling is active.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted. Ensuring briefing scheduler is ready.")
            // Since we use manifest-registered USER_PRESENT receiver, 
            // the system automatically handles it.
            // We could use this to schedule a fallback periodic work if needed.
        }
    }
}
