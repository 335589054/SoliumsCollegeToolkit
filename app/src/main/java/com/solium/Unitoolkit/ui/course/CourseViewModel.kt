package com.solium.Unitoolkit.ui.course

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solium.Unitoolkit.UniToolkitApp
import com.solium.Unitoolkit.core.database.CourseDao
import com.solium.Unitoolkit.core.database.CourseEntity
import com.solium.Unitoolkit.core.model.CoursePreset
import com.solium.Unitoolkit.core.model.GpaMapping
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

    private val _currentWeek = MutableStateFlow(1)
    val currentWeek: StateFlow<Int> = _currentWeek.asStateFlow()

    private val _semesterWeeks = MutableStateFlow(20)
    val semesterWeeks: StateFlow<Int> = _semesterWeeks.asStateFlow()

    private val _semesterStartDate = MutableStateFlow("")
    val semesterStartDate: StateFlow<String> = _semesterStartDate.asStateFlow()

    init {
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

    companion object { val Factory = appFactory(::CourseViewModel) }
}
