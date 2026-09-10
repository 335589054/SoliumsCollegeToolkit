package com.unitoolkit.ui.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.unitoolkit.core.database.TodoEntity
import com.unitoolkit.core.utils.DateUtils
import com.unitoolkit.ui.components.EmptyHint
import com.unitoolkit.ui.components.PriorityDot
import com.unitoolkit.ui.components.SimpleTopBar

private enum class Filter { ALL, TODAY, DONE }

@Composable
fun TodoScreen(navController: NavController, vm: TodoViewModel = viewModel(factory = TodoViewModel.Factory)) {
    val todos by vm.todos.collectAsState()
    var filter by remember { mutableStateOf(Filter.ALL) }
    var editing by remember { mutableStateOf<TodoEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    val today = DateUtils.today()

    val filtered = when (filter) {
        Filter.ALL -> todos
        Filter.TODAY -> todos.filter { it.dueDate.startsWith(today) }
        Filter.DONE -> todos.filter { it.isDone }
    }

    Scaffold(
        topBar = { SimpleTopBar(title = "TodoList", onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) { Icon(Icons.Default.Add, contentDescription = "添加待办") }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 10.dp)) {
                listOf(Filter.ALL to "全部", Filter.TODAY to "今日到期", Filter.DONE to "已完成").forEach { (f, label) ->
                    val sel = filter == f
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .clickable { filter = f }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) { Text(label, fontSize = 13.sp, color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) }
                }
            }
            if (filtered.isEmpty()) {
                EmptyHint("暂无待办，点击右下角添加")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { todo ->
                        TodoItem(todo, onToggle = { vm.setDone(todo, !todo.isDone) }, onClick = { editing = todo; showDialog = true })
                    }
                }
            }
        }
    }

    if (showDialog) {
        TodoDialog(
            existing = editing,
            onDismiss = { showDialog = false },
            onSave = { t -> vm.upsert(t); showDialog = false },
            onDelete = { t -> vm.delete(t); showDialog = false },
        )
    }
}

@Composable
private fun TodoItem(todo: TodoEntity, onToggle: () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = todo.isDone, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PriorityDot(todo.priority)
                Text(
                    todo.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (todo.isDone) TextDecoration.LineThrough else TextDecoration.None,
                )
            }
            val meta = listOf(todo.dueDate.ifBlank { "" }, todo.tags.ifBlank { "" }).filter { it.isNotBlank() }.joinToString("  ")
            if (meta.isNotBlank()) Text(meta, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TodoDialog(existing: TodoEntity?, onDismiss: () -> Unit, onSave: (TodoEntity) -> Unit, onDelete: (TodoEntity) -> Unit) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var priority by remember { mutableStateOf(existing?.priority ?: 1) }
    var dueDate by remember { mutableStateOf(existing?.dueDate ?: "") }
    var tags by remember { mutableStateOf(existing?.tags ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (existing == null) "新建待办" else "编辑待办", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("截止日期 (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("标签（逗号分隔）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "低", 1 to "中", 2 to "高").forEach { (p, label) ->
                    val sel = priority == p
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .clickable { priority = p }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) { Text(label, fontSize = 13.sp, color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (existing != null) {
                    TextButton(onClick = { onDelete(existing) }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            TodoEntity(
                                id = existing?.id ?: 0,
                                title = title.trim(),
                                note = note.trim(),
                                priority = priority,
                                dueDate = dueDate.trim(),
                                tags = tags.trim(),
                                isDone = existing?.isDone ?: false,
                                sortOrder = existing?.sortOrder ?: 0,
                                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            )
                        )
                    }
                }) { Text("保存", fontWeight = FontWeight.Bold) }
            }
        }
    }
}