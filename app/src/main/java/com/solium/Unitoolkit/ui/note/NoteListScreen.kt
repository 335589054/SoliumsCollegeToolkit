package com.solium.Unitoolkit.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.solium.Unitoolkit.core.database.NoteEntity
import com.solium.Unitoolkit.core.utils.DateUtils
import com.solium.Unitoolkit.ui.components.EmptyHint
import com.solium.Unitoolkit.ui.components.SimpleTopBar
import com.solium.Unitoolkit.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoteListScreen(navController: NavController, vm: NoteViewModel = viewModel(factory = NoteViewModel.Factory)) {
    val notes by vm.notes.collectAsState()
    var query by remember { mutableStateOf("") }

    val filtered = if (query.isBlank()) notes else notes.filter { it.title.contains(query, true) || it.content.contains(query, true) }

    Scaffold(
        topBar = {
            SimpleTopBar(
                title = "笔记",
                onBack = { navController.popBackStack() },
                trailing = { IconButton(onClick = { navController.navigate(Routes.noteEdit(-1)) }) { Icon(Icons.Default.Add, contentDescription = "新建") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.noteEdit(-1)) }) { Icon(Icons.Default.Add, contentDescription = "新建笔记") }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("搜索笔记") }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), singleLine = true)
            if (filtered.isEmpty()) {
                EmptyHint("暂无笔记，点击右下角新建")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { n ->
                        NoteItem(n) { navController.navigate(Routes.noteEdit(n.id)) }
                    }
                }
            }
        }
    }
}

private val noteTimeFmt = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)

@Composable
private fun NoteItem(note: NoteEntity, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(note.title, Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(noteTimeFmt.format(Date(note.updatedAt)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        val preview = note.content.lineSequence().firstOrNull { it.isNotBlank() && !it.trimStart().startsWith("#") }?.take(60) ?: ""
        if (preview.isNotBlank()) {
            Text(preview, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}