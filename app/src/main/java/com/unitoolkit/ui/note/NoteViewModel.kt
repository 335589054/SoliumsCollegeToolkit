package com.unitoolkit.ui.note

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unitoolkit.UniToolkitApp
import com.unitoolkit.core.database.NoteEntity
import com.unitoolkit.core.utils.appFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as UniToolkitApp).database.noteDao()

    val notes: StateFlow<List<NoteEntity>> =
        dao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun upsert(note: NoteEntity) = viewModelScope.launch {
        var n = note
        if (n.title.isBlank()) {
            n = n.copy(title = note.content.lines().firstOrNull { it.isNotBlank() }?.trim()?.take(30) ?: "无标题")
        }
        dao.upsert(n.copy(updatedAt = System.currentTimeMillis()))
    }

    fun delete(note: NoteEntity) = viewModelScope.launch { dao.delete(note) }

    companion object { val Factory = appFactory(::NoteViewModel) }
}