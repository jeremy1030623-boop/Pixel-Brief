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
                ),
                androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                    androidx.health.connect.client.records.StepsRecord::class
                ),
                androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                    androidx.health.connect.client.records.HeartRateRecord::class
                )
            )
        } catch (e: Throwable) {
            Log.e("HealthConnectHelper", "Could not build required permissions set due to missing classes", e)
            emptySet()
        }
    }

    // New data class to hold comprehensive health information
    data class HealthData(
        val sleepHours: Float = 0f,
        val snoringMinutes: Int = 0,
        val coughCount: Int = 0,
        val sleepQuality: String = "正常",
        val dailySteps: Int = 0,
        val avgHeartRate: Int = 0
    )

    /**
     * Reads comprehensive health records from the device using Jetpack Health Connect.
     */
    suspend fun readHealthData(context: Context): HealthData {
        return try {
            if (!isSdkAvailable(context)) {
                Log.d("HealthConnectHelper", "Health Connect SDK is not supported or not installed")
                return HealthData()
            }
            readHealthDataInternal(context)
        } catch (e: Throwable) {
            Log.e("HealthConnectHelper", "Fatal error during readHealthData", e)
            HealthData()
        }
    }

    private suspend fun readHealthDataInternal(context: Context): HealthData {
        val client = androidx.health.connect.client.HealthConnectClient.getOrCreate(context)
        val permissions = getRequiredPermissions()
        if (permissions.isEmpty()) return HealthData()

        val granted = client.permissionController.getGrantedPermissions()
        if (!granted.containsAll(permissions)) {
            Log.d("HealthConnectHelper", "Health Connect Permissions have not been granted yet")
            return HealthData()
        }

        // Time range for the last 24 hours
        val endTime = Instant.now()
        val startTime = endTime.minus(24, ChronoUnit.HOURS)
        val timeFilter = androidx.health.connect.client.time.TimeRangeFilter.between(startTime, endTime)

        // 1. Read sleep session records
        val sleepRequest = androidx.health.connect.client.request.ReadRecordsRequest(
            recordType = androidx.health.connect.client.records.SleepSessionRecord::class,
            timeRangeFilter = timeFilter
        )
        val sleepResponse = client.readRecords(sleepRequest)
        
        var totalSleepMinutes = 0L
        var totalSnoring = 0
        var totalCoughs = 0
        for (record in sleepResponse.records) {
            totalSleepMinutes += Duration.between(record.startTime, record.endTime).toMinutes()
            record.notes?.let { notes ->
                if (notes.contains("snore", ignoreCase = true) || notes.contains("打呼", ignoreCase = true)) totalSnoring += 10
                if (notes.contains("cough", ignoreCase = true) || notes.contains("咳嗽", ignoreCase = true)) totalCoughs += 1
            }
        }

        // 2. Read daily steps (Aggregated if possible, or sum records)
        val stepsRequest = androidx.health.connect.client.request.ReadRecordsRequest(
            recordType = androidx.health.connect.client.records.StepsRecord::class,
            timeRangeFilter = timeFilter
        )
        val stepsResponse = client.readRecords(stepsRequest)
        val totalSteps = stepsResponse.records.sumOf { it.count }.toInt()

        // 3. Read heart rate (Average)
        val hrRequest = androidx.health.connect.client.request.ReadRecordsRequest(
            recordType = androidx.health.connect.client.records.HeartRateRecord::class,
            timeRangeFilter = timeFilter
        )
        val hrResponse = client.readRecords(hrRequest)
        val avgHr = if (hrResponse.records.isNotEmpty()) {
            val allSamples = hrResponse.records.flatMap { it.samples }
            if (allSamples.isNotEmpty()) {
                allSamples.map { it.beatsPerMinute }.average().toInt()
            } else 0
        } else 0

        return HealthData(
            sleepHours = totalSleepMinutes / 60f,
            snoringMinutes = totalSnoring,
            coughCount = totalCoughs,
            sleepQuality = if (totalCoughs > 5) "稍差" else "良好",
            dailySteps = totalSteps,
            avgHeartRate = if (avgHr == 0) 70 else avgHr
        )
    }
}
