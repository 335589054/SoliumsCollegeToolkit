package com.unitoolkit.ui.pomodoro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unitoolkit.UniToolkitApp
import com.unitoolkit.core.database.PomodoroStats
import com.unitoolkit.core.model.PomodoroPreset
import com.unitoolkit.core.utils.appFactory
import com.unitoolkit.service.PomodoroManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class PomodoroViewModel(app: Application) : AndroidViewModel(app) {
    private val uniApp = app as UniToolkitApp
    private val settings = uniApp.settings
    private val dao = uniApp.database.pomodoroDao()

    val state: StateFlow<PomodoroManager.State> = PomodoroManager.state
    val presets: StateFlow<List<PomodoroPreset>> =
        settings.pomodoroPresets.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val keepScreenOn: StateFlow<Boolean> =
        settings.keepScreenOn.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _selectedPreset = MutableStateFlow<PomodoroPreset?>(null)
    val selectedPreset: StateFlow<PomodoroPreset?> = _selectedPreset.asStateFlow()

    val todayStats: StateFlow<PomodoroStats> =
        PomodoroManager.state.flatMapLatest {
            flow { emit(dao.todayStats(startOfToday())) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, PomodoroStats(0, 0))

    /** 仅切换预设，不自动开始计时 */
    fun selectPreset(preset: PomodoroPreset) {
        _selectedPreset.value = preset
    }

    fun start(preset: PomodoroPreset) = PomodoroManager.start(preset)
    fun pause() = PomodoroManager.pause()
    fun resume() = PomodoroManager.resume()
    fun stop() = PomodoroManager.stop()
    fun setKeepScreenOn(v: Boolean) = viewModelScope.launch { settings.setKeepScreenOn(v) }

    /** 保存 / 更新自定义预设 */
    fun saveCustomPreset(preset: PomodoroPreset) = viewModelScope.launch {
        val current = settings.pomodoroPresets.first().toMutableList()
        val index = current.indexOfFirst { it.name == preset.name }
        if (index != -1) current[index] = preset else current.add(preset)
        settings.setPomodoroPresets(current)
        _selectedPreset.value = preset
    }

    private fun startOfToday(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    companion object { val Factory = appFactory(::PomodoroViewModel) }
}