package com.unitoolkit.ui.course

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unitoolkit.UniToolkitApp
import com.unitoolkit.core.database.CourseDao
import com.unitoolkit.core.database.CourseEntity
import com.unitoolkit.core.model.CoursePreset
import com.unitoolkit.core.model.GpaMapping
import com.unitoolkit.core.utils.DateUtils
import com.unitoolkit.core.utils.appFactory
import com.unitoolkit.service.CourseNotifier
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
    }
    fun delete(course: CourseEntity) = viewModelScope.launch {
        dao.delete(course)
        CourseNotifier.scheduleAfter(getApplication(), System.currentTimeMillis())
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

    companion object { val Factory = appFactory(::CourseViewModel) }
}
