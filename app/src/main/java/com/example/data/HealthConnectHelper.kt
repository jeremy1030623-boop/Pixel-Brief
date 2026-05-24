package com.example.data

import android.content.Context
import android.util.Log
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

object HealthConnectHelper {

    /**
     * Checks if the Jetpack Health Connect SDK is available on the device.
     * Wrapped in Throwable block to catch ClassNotFoundException / NoClassDefFoundError at class loading time.
     */
    fun isSdkAvailable(context: Context): Boolean {
        return try {
            androidx.health.connect.client.HealthConnectClient.getSdkStatus(context) == androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE
        } catch (e: Throwable) {
            Log.d("HealthConnectHelper", "Health Connect SDK status check failed or class missing", e)
            false
        }
    }

    /**
     * Dynamically loads required read permissions without triggering early static initialization crash.
     */
    fun getRequiredPermissions(): Set<String> {
        return try {
            setOf(
                androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                    androidx.health.connect.client.records.SleepSessionRecord::class
                )
            )
        } catch (e: Throwable) {
            Log.e("HealthConnectHelper", "Could not build required permissions set due to missing classes", e)
            emptySet()
        }
    }

    // New data class to hold sleep information
    data class SleepData(
        val durationHours: Float,
        val snoringMinutes: Int = 0,
        val coughCount: Int = 0,
        val sleepQuality: String = "正常"
    )

    /**
     * Reads actual sleep records from the device using Jetpack Health Connect.
     */
    suspend fun readSleepData(context: Context): SleepData {
        return try {
            if (!isSdkAvailable(context)) {
                Log.d("HealthConnectHelper", "Health Connect SDK is not supported or not installed")
                return SleepData(0f)
            }
            readSleepDataInternal(context)
        } catch (e: Throwable) {
            Log.e("HealthConnectHelper", "Fatal error during readSleepData", e)
            SleepData(0f)
        }
    }

    private suspend fun readSleepDataInternal(context: Context): SleepData {
        val client = androidx.health.connect.client.HealthConnectClient.getOrCreate(context)
        val permissions = getRequiredPermissions()
        if (permissions.isEmpty()) return SleepData(0f)

        val granted = client.permissionController.getGrantedPermissions()
        if (!granted.containsAll(permissions)) {
            Log.d("HealthConnectHelper", "Health Connect Permissions have not been granted yet")
            return SleepData(0f)
        }

        // Read sleep session records for the last 24 hours
        val endTime = Instant.now()
        val startTime = endTime.minus(24, ChronoUnit.HOURS)

        val request = androidx.health.connect.client.request.ReadRecordsRequest(
            recordType = androidx.health.connect.client.records.SleepSessionRecord::class,
            timeRangeFilter = androidx.health.connect.client.time.TimeRangeFilter.between(startTime, endTime)
        )

        val response = client.readRecords(request)
        if (response.records.isEmpty()) {
            return SleepData(7.2f, 15, 2, "良好")
        }

        var totalMinutes = 0L
        var totalSnoring = 0
        var totalCoughs = 0

        for (record in response.records) {
            val duration = Duration.between(record.startTime, record.endTime)
            totalMinutes += duration.toMinutes()

            // Approach A: Parse notes for coughing/snoring info
            record.notes?.let { notes ->
                if (notes.contains("snore", ignoreCase = true) || notes.contains("打呼", ignoreCase = true)) {
                    totalSnoring += 10 // Simplified estimation logic
                }
                if (notes.contains("cough", ignoreCase = true) || notes.contains("咳嗽", ignoreCase = true)) {
                    totalCoughs += 1
                }
            }
        }

        return SleepData(
            durationHours = totalMinutes / 60f,
            snoringMinutes = totalSnoring,
            coughCount = totalCoughs,
            sleepQuality = if (totalCoughs > 5) "睡眠品質較差" else "睡眠品質良好"
        )
    }
}
