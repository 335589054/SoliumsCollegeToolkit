package com.unitoolkit.core.utils

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat

data class SystemEvent(
    val id: Long,             // 单次实例唯一 ID（用于列表 key）
    val eventId: Long,        // 日历事件 ID（用于与自建日程去重）
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
)

object CalendarHelper {

    fun hasCalendarPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun dayStartMillis(): Long =
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun defaultCalendarId(context: Context): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val uri = CalendarContract.Calendars.CONTENT_URI
        val cursor = runCatching { context.contentResolver.query(uri, projection, null, null, null) }.getOrNull() ?: return null
        cursor.use {
            if (it.moveToFirst()) return it.getLong(0)
        }
        return null
    }

    fun queryEvents(context: Context, fromMillis: Long, toMillis: Long): List<SystemEvent> {
        if (!hasCalendarPermission(context)) return emptyList()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances._ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
        )
        val uri = Uri.withAppendedPath(CalendarContract.Instances.CONTENT_URI, "$fromMillis/$toMillis")
        val result = mutableListOf<SystemEvent>()
        val cursor = runCatching {
            context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")
        }.getOrNull()
        if (cursor != null) {
            cursor.use {
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val eventId = it.getLong(1)
                    val title = it.getString(2) ?: ""
                    val start = it.getLong(3)
                    val end = it.getLong(4)
                    result.add(SystemEvent(id, eventId, title, start, end))
                }
            }
            return result
        }
        // 个别设备不支持 Instances 查询时回退到事件表
        val fallbackProjection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
        )
        val fallbackUri = CalendarContract.Events.CONTENT_URI
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ? AND ${CalendarContract.Events.DELETED} = 0"
        val args = arrayOf(fromMillis.toString(), toMillis.toString())
        val fallbackCursor = runCatching { context.contentResolver.query(fallbackUri, fallbackProjection, selection, args, "${CalendarContract.Events.DTSTART} ASC") }.getOrNull()
        fallbackCursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val title = it.getString(1) ?: ""
                val start = it.getLong(2)
                val end = it.getLong(3)
                if (result.none { existing -> existing.eventId == id }) result.add(SystemEvent(id, id, title, start, end))
            }
        }
        return result
    }

    fun insertEvent(context: Context, title: String, startMillis: Long, endMillis: Long, reminderMinutes: Int): Long {
        val calId = defaultCalendarId(context) ?: return -1L
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, "Asia/Shanghai")
            put(CalendarContract.Events.HAS_ALARM, if (reminderMinutes > 0) 1 else 0)
        }
        val uri = runCatching { context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) }.getOrNull() ?: return -1L
        val eventId = ContentUris.parseId(uri)
        if (reminderMinutes > 0) {
            val remValues = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, reminderMinutes)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            runCatching { context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, remValues) }
        }
        return eventId
    }

    fun deleteEvent(context: Context, eventId: Long) {
        if (eventId <= 0) return
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        runCatching { context.contentResolver.delete(uri, null, null) }
    }
}
