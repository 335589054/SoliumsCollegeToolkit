package com.solium.Unitoolkit.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.solium.Unitoolkit.UniToolkitApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 接收课表提醒闹钟：推送下一节课通知并调度后续课程 */
class CourseNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as UniToolkitApp
        CoroutineScope(Dispatchers.IO).launch {
            val now = System.currentTimeMillis()
            val courses = app.database.courseDao().all().first()
            val semesterStart = app.settings.semesterStartDate.first()
            val fallbackWeek = app.settings.currentWeek.first()
            val next = CourseNotifier.nextCourse(courses, now, semesterStart, fallbackWeek)
            if (next != null) {
                CourseNotifier.postNotification(context, next.course, next.startMillis)
                // 调度下一节（严格晚于本节）
                CourseNotifier.scheduleAfter(context, next.startMillis + 1)
            } else {
                CourseNotifier.scheduleAfter(context, now)
            }
        }
    }
}
