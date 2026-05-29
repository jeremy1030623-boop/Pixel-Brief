package com.example.data

import android.content.Context
import android.provider.CalendarContract
import com.example.model.AgendaEvent
import java.text.SimpleDateFormat
import java.util.*

object CalendarHelper {
    fun fetchEvents(context: Context): List<AgendaEvent> {
        val events = mutableListOf<AgendaEvent>()
        try {
            val contentResolver = context.contentResolver
            val uri = CalendarContract.Events.CONTENT_URI
            
            // Query events from today till end of today
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startTime = calendar.timeInMillis
            
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val endTime = calendar.timeInMillis
            
            val selection = "(${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?)"
            val selectionArgs = arrayOf(startTime.toString(), endTime.toString())
            
            val projection = arrayOf(
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.ALL_DAY
            )
            
            val cursor = contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )
            
            cursor?.use {
                val titleIndex = it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val startIndex = it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val allDayIndex = it.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)
                
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                
                while (it.moveToNext()) {
                    val title = it.getString(titleIndex)
                    val dtStart = it.getLong(startIndex)
                    val allDay = it.getInt(allDayIndex) == 1
                    
                    val timeStr = if (allDay) "全天" else timeFormat.format(Date(dtStart))
                    events.add(AgendaEvent(title, timeStr, allDay))
                }
            }
        } catch (e: SecurityException) {
            // No permissions, return empty so we can fallback to mocks gracefully
        } catch (e: Exception) {
            // Log or ignore
        }
        
        // If empty, return a set of beautiful morning placeholder events
        if (events.isEmpty()) {
            return listOf(
                AgendaEvent("🌅 晨間伸展與冥想", "08:30"),
                AgendaEvent("☕ 享用美味早餐與咖啡", "09:00"),
                AgendaEvent("💻 核心工作專案開發", "10:30"),
                AgendaEvent("🥗 午餐充電時間 & 散步", "12:30"),
                AgendaEvent("🔋 檢視今日目標與回顧", "17:00")
            )
        }
        return events
    }
}
