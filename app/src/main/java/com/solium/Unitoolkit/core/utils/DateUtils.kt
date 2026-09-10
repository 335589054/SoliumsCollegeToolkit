package com.solium.Unitoolkit.core.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.CHINA)
    private val dateTimeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    private val monthFmt = SimpleDateFormat("yyyy-MM", Locale.CHINA)
    private val dateOnlyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

    fun nowTime(): String = timeFmt.format(Date())
    fun today(): String = dateFmt.format(Date())
    fun nowDateTime(): String = dateTimeFmt.format(Date())
    fun monthOf(date: String): String = if (date.length >= 7) date.substring(0, 7) else ""
    fun monthNow(): String = monthFmt.format(Date())

    fun timeToMinutes(hhmm: String): Int {
        return try {
            val p = hhmm.split(":")
            p[0].toInt() * 60 + p[1].toInt()
        } catch (e: Exception) { 0 }
    }

    fun minutesToTime(mins: Int): String {
        val h = (mins / 60).coerceIn(0, 23)
        val m = mins % 60
        return "%02d:%02d".format(h, m)
    }

    /** 今天星期几，1=周一 ... 7=周日 */
    fun todayWeekday(): Int {
        val c = Calendar.getInstance()
        val d = c.get(Calendar.DAY_OF_WEEK) // 1=周日 ... 7=周六
        return if (d == 1) 7 else d - 1
    }

    /** 指定日期星期几，1=周一 ... 7=周日 */
    fun weekdayOf(date: Date): Int {
        val c = Calendar.getInstance().apply { time = date }
        val d = c.get(Calendar.DAY_OF_WEEK)
        return if (d == 1) 7 else d - 1
    }

    /** 解析 "yyyy-MM-dd"，失败返回 null */
    fun parseDateOnly(date: String): Date? = runCatching { dateOnlyFmt.parse(date) }.getOrNull()

    /** 根据学期第一周起始日（周一）计算指定日期所属周次；起始日为空则返回 null */
    fun weekOf(date: Date, semesterStart: String?): Int? {
        val start = parseDateOnly(semesterStart ?: return null) ?: return null
        val diffDays = ((date.time - start.time) / (1000L * 60 * 60 * 24)).toInt()
        return diffDays / 7 + 1
    }

    fun weekdayZh(w: Int): String = when (w) {
        1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"; 5 -> "周五"; 6 -> "周六"; 7 -> "周日"
        else -> ""
    }

    fun formatDate(date: String): String {
        return if (date.length >= 10) date.substring(0, 10) else date
    }

    fun dateString(date: Date): String = dateFmt.format(date)

    /** 返回相对今天的日期，offset=0 今天、1 明天… */
    fun offsetDate(offset: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, offset)
        return dateFmt.format(c.time)
    }

    /** 解析 "yyyy-MM-dd HH:mm"，失败返回 null */
    fun parse(dateTime: String): Date? = runCatching { dateTimeFmt.parse(dateTime) }.getOrNull()

    /** 农历月日文本，如「四月初八」 */
    fun lunarToday(): String {
        val c = Calendar.getInstance()
        return LunarCalendar.lunarText(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    /** 当前节气，如「立夏」 */
    fun solarTermToday(): String {
        val c = Calendar.getInstance()
        return LunarCalendar.solarTerm(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    /** 距今天数（正=未来） */
    fun daysUntil(date: String): Long? {
        val d = runCatching { dateFmt.parse(date) }.getOrNull() ?: return null
        val diff = d.time - System.currentTimeMillis()
        return diff / (1000 * 60 * 60 * 24)
    }
}
