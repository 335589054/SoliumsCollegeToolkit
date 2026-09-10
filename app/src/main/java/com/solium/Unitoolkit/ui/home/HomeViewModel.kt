package com.solium.Unitoolkit.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solium.Unitoolkit.UniToolkitApp
import com.solium.Unitoolkit.core.database.CourseEntity
import com.solium.Unitoolkit.core.database.LedgerEntryEntity
import com.solium.Unitoolkit.core.database.LinkEntity
import com.solium.Unitoolkit.core.database.TodoEntity
import com.solium.Unitoolkit.core.utils.CalendarHelper
import com.solium.Unitoolkit.core.utils.DateUtils
import com.solium.Unitoolkit.core.utils.SystemEvent
import com.solium.Unitoolkit.core.utils.appFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as UniToolkitApp).database
    private val settings = (app as UniToolkitApp).settings
    private val context = app.applicationContext
    private val weekday = DateUtils.todayWeekday()

    val courses: StateFlow<List<CourseEntity>> =
        db.courseDao().byWeekday(weekday).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todos: StateFlow<List<TodoEntity>> =
        db.todoDao().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ledgerEntries: StateFlow<List<LedgerEntryEntity>> =
        db.ledgerDao().entries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val links: StateFlow<List<LinkEntity>> =
        db.linkDao().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _systemSchedules = MutableStateFlow<List<SystemEvent>>(emptyList())
    val systemSchedules: StateFlow<List<SystemEvent>> = _systemSchedules.asStateFlow()

    val homeCards: StateFlow<List<String>> =
        settings.homeCards.stateIn(viewModelScope, SharingStarted.Eagerly, settings.defaultHomeCards())

    val quickTools: StateFlow<List<String>> =
        settings.quickTools.stateIn(viewModelScope, SharingStarted.Eagerly, settings.defaultQuickTools())

    val currentWeek: StateFlow<Int> =
        settings.currentWeek.stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    fun setHomeCards(cards: List<String>) = viewModelScope.launch { settings.setHomeCards(cards) }

    fun addCard(key: String) {
        val cur = homeCards.value
        if (key !in cur) setHomeCards(cur + key)
    }

    fun removeCard(key: String) {
        setHomeCards(homeCards.value.filterNot { it == key })
    }

    fun moveCard(key: String, delta: Int) {
        val cur = homeCards.value.toMutableList()
        val idx = cur.indexOf(key)
        val target = idx + delta
        if (idx == -1 || target < 0 || target >= cur.size) return
        val tmp = cur[idx]; cur[idx] = cur[target]; cur[target] = tmp
        setHomeCards(cur)
    }

    fun setTodoDone(todo: TodoEntity, done: Boolean) = viewModelScope.launch {
        db.todoDao().update(todo.copy(isDone = done))
    }

    /** 主动同步一次系统日历，供首页日程卡片展示 */
    fun refreshSystemSchedules() {
        if (!CalendarHelper.hasCalendarPermission(context)) return
        val from = CalendarHelper.dayStartMillis()
        _systemSchedules.value = CalendarHelper.queryEvents(context, from, from + 30L * 24 * 3600 * 1000)
    }

    companion object { val Factory = appFactory(::HomeViewModel) }
}
