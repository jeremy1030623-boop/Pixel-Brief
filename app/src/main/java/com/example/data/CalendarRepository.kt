package com.example.data

import android.content.ContentResolver
import android.provider.CalendarContract
import java.util.Calendar

data class CalendarEvent(
    val title: String,
    val startTime: Long,
    val endTime: Long
)

class CalendarRepository(private val contentResolver: ContentResolver) {
    fun getNextEvents(limit: Int = 3): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        try {
            val startTime = System.currentTimeMillis()
            val endTime = startTime + 24 * 60 * 60 * 1000 // Next 24 hours

            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END
            )

            val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            android.content.ContentUris.appendId(uriBuilder, startTime)
            android.content.ContentUris.appendId(uriBuilder, endTime)

            val cursor = contentResolver.query(
                uriBuilder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            cursor?.use {
                val titleIndex = it.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIndex = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIndex = it.getColumnIndex(CalendarContract.Instances.END)

                while (it.moveToNext() && events.size < limit) {
                    val title = if (titleIndex >= 0) it.getString(titleIndex) ?: "無標題" else "無標題"
                    val start = if (beginIndex >= 0) it.getLong(beginIndex) else System.currentTimeMillis()
                    val end = if (endIndex >= 0) it.getLong(endIndex) else start
                    events.add(
                        CalendarEvent(
                            title = title,
                            startTime = start,
                            endTime = end
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Catch all exceptions securely
        }
        return events
    }
}
