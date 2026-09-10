package com.unitoolkit.core.utils

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** 统一调用 Android 系统日期 / 时间选择器 */
object DateTimePickers {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.CHINA)

    private fun calendarFrom(initial: String?, parse: (String) -> Long?): Calendar {
        val c = Calendar.getInstance()
        if (!initial.isNullOrBlank()) {
            runCatching { parse(initial) }.getOrNull()?.let { c.timeInMillis = it }
        }
        return c
    }

    /** 日期选择（initial 可为 "yyyy-MM-dd" 或 "yyyy-MM-dd HH:mm"） */
    fun pickDate(context: Context, initial: String, onResult: (String) -> Unit) {
        val c = calendarFrom(initial) {
            val s = if (it.length >= 10) it.substring(0, 10) else it
            dateFmt.parse(s)?.time ?: 0L
        }
        DatePickerDialog(
            context,
            { _, y, m, d -> onResult("%04d-%02d-%02d".format(y, m + 1, d)) },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    /** 时间选择（initial 为 "HH:mm"） */
    fun pickTime(context: Context, initial: String, onResult: (String) -> Unit) {
        val c = calendarFrom(initial) {
            val s = it.takeLast(5)
            timeFmt.parse(s)?.time ?: 0L
        }
        TimePickerDialog(
            context,
            { _, h, min -> onResult("%02d:%02d".format(h, min)) },
            c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
            DateFormat.is24HourFormat(context),
        ).show()
    }

    /** 日期 + 时间选择（initial 为 "yyyy-MM-dd HH:mm"） */
    fun pickDateTime(context: Context, initial: String, onResult: (String) -> Unit) {
        pickDate(context, initial) { date ->
            pickTime(context, initial) { time ->
                onResult("$date $time")
            }
        }
    }
}