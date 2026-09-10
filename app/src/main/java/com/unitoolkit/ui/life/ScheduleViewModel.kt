package com.unitoolkit.ui.life

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unitoolkit.core.utils.CalendarHelper
import com.unitoolkit.core.utils.DateUtils
import com.unitoolkit.core.utils.SystemEvent
import com.unitoolkit.core.utils.appFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class ScheduleViewModel(app: Application) : AndroidViewModel(app) {
    private val context = app.applicationContext

    private val _systemEvents = MutableStateFlow<List<SystemEvent>>(emptyList())
    val systemEvents: StateFlow<List<SystemEvent>> = _systemEvents.asStateFlow()

    /** 读取系统日历（今天起 30 天内） */
    fun refreshSystemEvents() {
        if (!CalendarHelper.hasCalendarPermission(context)) return
        val from = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val to = from + 30L * 24 * 3600 * 1000
        _systemEvents.value = CalendarHelper.queryEvents(context, from, to)
    }

    /** 新增日程：直接写入系统日历，不保留本地副本 */
    fun addEvent(title: String, startText: String, endText: String, reminderMinutes: Int) = viewModelScope.launch {
        val start = DateUtils.parse(startText)?.time ?: return@launch
        val end = DateUtils.parse(endText)?.time ?: return@launch
        CalendarHelper.insertEvent(context, title, start, end, reminderMinutes)
        refreshSystemEvents()
    }

    /** 删除系统日历事件 */
    fun deleteEvent(event: SystemEvent) = viewModelScope.launch {
        CalendarHelper.deleteEvent(context, event.eventId)
        refreshSystemEvents()
    }

    companion object { val Factory = appFactory(::ScheduleViewModel) }
}
