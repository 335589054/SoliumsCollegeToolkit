package com.unitoolkit.ui.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unitoolkit.UniToolkitApp
import com.unitoolkit.core.database.TodoEntity
import com.unitoolkit.core.utils.appFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as UniToolkitApp).database.todoDao()

    val todos: StateFlow<List<TodoEntity>> =
        dao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun upsert(todo: TodoEntity) = viewModelScope.launch { dao.upsert(todo) }
    fun setDone(todo: TodoEntity, done: Boolean) = viewModelScope.launch { dao.update(todo.copy(isDone = done)) }
    fun delete(todo: TodoEntity) = viewModelScope.launch { dao.delete(todo) }

    companion object { val Factory = appFactory(::TodoViewModel) }
}