package com.solium.Unitoolkit.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.solium.Unitoolkit.MainActivity
import com.solium.Unitoolkit.R
import com.solium.Unitoolkit.UniToolkitApp
import com.solium.Unitoolkit.core.database.CourseEntity
import com.solium.Unitoolkit.core.database.matchesWeek
import com.solium.Unitoolkit.core.database.matchesWeekday
import com.solium.Unitoolkit.core.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 课表下一节通知：计算下一节课并推送 / 设置闹钟提醒 */
object CourseNotifier {
    const val CHANNEL_ID = "course"
    const val NOTIFY_ID = 200
    private const val LEAD_MS = 10 * 60 * 1000L // 提前 10 分钟提醒

    data class Upcoming(val course: CourseEntity, val startMillis: Long)

    /** 找到 startMillis >= afterMillis 的最近一节课（考虑单双周 / 仅一次，搜索未来 14 天） */
    fun nextCourse(courses: List<CourseEntity>, afterMillis: Long, semesterStart: String, fallbackWeek: Int): Upcoming? {
        if (courses.isEmpty()) return null
        var best: Upcoming? = null
        for (offset in 0 until 14) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = afterMillis
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val date = cal.time
            val weekday = DateUtils.weekdayOf(date)
            val week = if (semesterStart.isNotBlank()) {
                DateUtils.weekOf(date, semesterStart) ?: (fallbackWeek + offset / 7)
            } else {
                fallbackWeek + offset / 7
            }
            for (c in courses) {
                if (!c.matchesWeekday(weekday) || !c.matchesWeek(week, DateUtils.dateString(date))) continue
                val start = startMillisOf(date, c.startTime)
                if (start < afterMillis) continue
                if (best == null || start < best.startMillis) best = Upcoming(c, start)
            }
        }
        return best
    }

    /** 从 afterMillis 起调度最近一节课的提醒闹钟 */
    fun scheduleAfter(context: Context, afterMillis: Long) {
        val app = context.applicationContext as UniToolkitApp
        CoroutineScope(Dispatchers.IO).launch {
            val courses = app.database.courseDao().all().first()
            val semesterStart = app.settings.semesterStartDate.first()
            val fallbackWeek = app.settings.currentWeek.first()
            val next = nextCourse(courses, afterMillis, semesterStart, fallbackWeek)
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = pendingIntent(context)
            if (next == null) {
                alarmMgr.cancel(pi)
                return@launch
            }
            val trigger = (next.startMillis - LEAD_MS).coerceAtLeast(afterMillis + 1000)
            if (Build.VERSION.SDK_INT >= 23) {
                alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            } else {
                @Suppress("DEPRECATION")
                alarmMgr.set(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        }
    }

    /** 立即推送下一节课通知（通常由闹钟接收器触发） */
    fun postNotification(context: Context, course: CourseEntity, startMillis: Long) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(context)
        val time = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(startMillis))
        val sub = listOf(course.location, course.teacher).filter { it.isNotBlank() }.joinToString(" · ")
        val text = time + if (sub.isNotBlank()) " · $sub" else ""
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("下一节课：${course.name}")
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFY_ID, notif)
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 300, Intent(context, CourseNotificationReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "课表提醒", NotificationManager.IMPORTANCE_HIGH)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun startMillisOf(date: Date, hhmm: String): Long {
        val parts = hhmm.split(":")
        if (parts.size < 2) return -1
        val h = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        val cal = Calendar.getInstance().apply { time = date }
        cal.set(Calendar.HOUR_OF_DAY, h)
        cal.set(Calendar.MINUTE, m)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
