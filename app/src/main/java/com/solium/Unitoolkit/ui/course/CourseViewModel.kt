package com.solium.Unitoolkit.ui.course

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solium.Unitoolkit.UniToolkitApp
import com.solium.Unitoolkit.core.database.CourseDao
import com.solium.Unitoolkit.core.database.CourseEntity
import com.solium.Unitoolkit.core.model.CoursePreset
import com.solium.Unitoolkit.core.model.GpaMapping
import com.solium.Unitoolkit.core.model.HolidayAdjustment
import com.solium.Unitoolkit.core.model.PeriodTiming
import com.solium.Unitoolkit.core.utils.DateUtils
import com.solium.Unitoolkit.core.utils.appFactory
import com.solium.Unitoolkit.service.CourseNotifier
import com.solium.Unitoolkit.ui.widget.UniWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

class CourseViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as UniToolkitApp).database
    private val settings = (app as UniToolkitApp).settings
    private val dao: CourseDao = db.courseDao()

    val courses: StateFlow<List<CourseEntity>> =
        dao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val coursePresets: StateFlow<List<CoursePreset>> =
        settings.coursePresets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val periodTimings: StateFlow<List<PeriodTiming>> =
        settings.periodTimings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val holidayAdjustments: StateFlow<List<HolidayAdjustment>> =
        settings.holidayAdjustments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentWeek = MutableStateFlow(1)
    val currentWeek: StateFlow<Int> = _currentWeek.asStateFlow()

    private val _semesterWeeks = MutableStateFlow(20)
    val semesterWeeks: StateFlow<Int> = _semesterWeeks.asStateFlow()

    private val _semesterStartDate = MutableStateFlow("")
    val semesterStartDate: StateFlow<String> = _semesterStartDate.asStateFlow()

    init {
        // 打开课表时自动对齐到「今天」所在的周次：设置了学期起始日期则按起始日期推算，
        // 否则沿用上次记录的周次。修复此前打开课表停留在第 1 周、不自动跳转的问题。
        viewModelScope.launch {
            val start = settings.semesterStartDate.first()
            val weeks = settings.semesterWeeks.first()
            val stored = settings.currentWeek.first()
            val todayWeek = if (start.isNotBlank()) DateUtils.weekOf(Date(), start) else null
            val target = if (todayWeek != null && todayWeek >= 1) {
                todayWeek.coerceAtMost(weeks.coerceAtLeast(1))
            } else stored
            _currentWeek.value = target
            if (target != stored) settings.setCurrentWeek(target)
        }
        viewModelScope.launch { settings.currentWeek.collect { _currentWeek.value = it } }
        viewModelScope.launch { settings.semesterWeeks.collect { _semesterWeeks.value = it } }
        viewModelScope.launch { settings.semesterStartDate.collect { _semesterStartDate.value = it } }
    }

    fun setWeek(w: Int) = viewModelScope.launch {
        _currentWeek.value = w
        settings.setCurrentWeek(w)
    }

    fun setSemesterWeeks(weeks: Int) = viewModelScope.launch {
        _semesterWeeks.value = weeks
        settings.setSemesterWeeks(weeks)
    }

    fun setSemesterStartDate(date: String) = viewModelScope.launch {
        _semesterStartDate.value = date
        settings.setSemesterStartDate(date)
        // 设置了学期起始后，自动把当前周对齐到今天的实际周次
        val todayWeek = DateUtils.weekOf(Date(), date) ?: return@launch
        if (todayWeek >= 1) setWeek(todayWeek.coerceAtMost(_semesterWeeks.value.coerceAtLeast(1)))
    }

    fun upsert(course: CourseEntity) = viewModelScope.launch {
        dao.upsert(course)
        CourseNotifier.scheduleAfter(getApplication(), System.currentTimeMillis())
        UniWidgetProvider.refreshAll(getApplication())
    }
    fun delete(course: CourseEntity) = viewModelScope.launch {
        dao.delete(course)
        CourseNotifier.scheduleAfter(getApplication(), System.currentTimeMillis())
        UniWidgetProvider.refreshAll(getApplication())
    }

    fun savePreset(preset: CoursePreset) = viewModelScope.launch {
        val current = settings.coursePresets.first()
        val id = preset.id.ifBlank { System.currentTimeMillis().toString() }
        settings.setCoursePresets(
            if (preset.id.isBlank()) current + preset.copy(id = id)
            else current.map { if (it.id == id) preset else it },
        )
    }

    fun deletePreset(id: String) = viewModelScope.launch {
        settings.setCoursePresets(settings.coursePresets.first().filterNot { it.id == id })
    }

    fun savePeriodTimings(list: List<PeriodTiming>) = viewModelScope.launch {
        settings.setPeriodTimings(
            list.filter { it.startTime.isNotBlank() }.sortedBy { it.section }
        )
    }

    /** 一键清空课表：删除全部课程（不影响预设 / 节次 / 调休）。 */
    fun clearCourses() = viewModelScope.launch {
        dao.clearAll()
        CourseNotifier.scheduleAfter(getApplication(), System.currentTimeMillis())
        UniWidgetProvider.refreshAll(getApplication())
    }

    /** 从 JSON 导入课程；overwrite=true 时先清空已有课程。 */
    fun importCourses(list: List<CourseEntity>, overwrite: Boolean) = viewModelScope.launch {
        if (overwrite) dao.clearAll()
        dao.insertAll(list)
        CourseNotifier.scheduleAfter(getApplication(), System.currentTimeMillis())
        UniWidgetProvider.refreshAll(getApplication())
    }

    fun saveHolidayAdjustments(list: List<HolidayAdjustment>) = viewModelScope.launch {
        settings.setHolidayAdjustments(list.filter { it.date.isNotBlank() && it.sourceDate.isNotBlank() })
    }

    companion object { val Factory = appFactory(::CourseViewModel) }
}
