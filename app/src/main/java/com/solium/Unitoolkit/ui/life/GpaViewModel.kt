package com.solium.Unitoolkit.ui.life

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solium.Unitoolkit.UniToolkitApp
import com.solium.Unitoolkit.core.database.GpaCourseEntity
import com.solium.Unitoolkit.core.model.GpaFormula
import com.solium.Unitoolkit.core.utils.appFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GpaViewModel(app: Application) : AndroidViewModel(app) {
    private val uniApp = app as UniToolkitApp
    private val dao = uniApp.database.gpaDao()

    val courses: StateFlow<List<GpaCourseEntity>> =
        dao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val formula: StateFlow<GpaFormula> =
        uniApp.settings.gpaFormula.stateIn(viewModelScope, SharingStarted.Eagerly, GpaFormula())

    fun upsert(course: GpaCourseEntity) = viewModelScope.launch { dao.upsert(course) }
    fun delete(course: GpaCourseEntity) = viewModelScope.launch { dao.delete(course) }
    fun setFormula(f: GpaFormula) = viewModelScope.launch { uniApp.settings.setGpaFormula(f) }

    companion object { val Factory = appFactory(::GpaViewModel) }
}