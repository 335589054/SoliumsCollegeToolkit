package com.unitoolkit.ui.note

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.unitoolkit.core.database.NoteEntity
import com.unitoolkit.core.utils.writeTextToUri
import com.unitoolkit.ui.components.AppIcon
import com.unitoolkit.ui.components.MarkdownText
import com.unitoolkit.ui.components.SimpleTopBar
import com.unitoolkit.ui.navigation.Routes

private enum class NoteMode { EDIT, PREVIEW, SPLIT }

@Composable
fun NoteEditScreen(navController: NavController, vm: NoteViewModel = viewModel(factory = NoteViewModel.Factory)) {
    val noteId = navController.currentBackStackEntry?.arguments?.getLong("id") ?: -1L
    val notes by vm.notes.collectAsState()
    val existing = notes.firstOrNull { it.id == noteId }

    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var text by remember { mutableStateOf(TextFieldValue(existing?.content ?: "")) }
    var mode by rememberSaveable { mutableStateOf(NoteMode.EDIT) }
    var menuOpen by remember { mutableStateOf(false) }

    val save = {
        vm.upsert(
            NoteEntity(
                id = noteId.coerceAtLeast(0),
                title = title,
                content = text.text,
                updatedAt = System.currentTimeMillis(),
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            )
        )
        navController.popBackStack()
    }

    var pendingExport by remember { mutableStateOf("") }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri != null && pendingExport.isNotBlank()) { writeTextToUri(context, uri, pendingExport); pendingExport = "" }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val content = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            }.getOrDefault("")
            text = TextFieldValue(content)
            if (title.isBlank()) title = content.lines().firstOrNull { it.isNotBlank() }?.trim()?.take(30) ?: ""
        }
    }

    Scaffold(
        topBar = {
            SimpleTopBar(
                title = if (existing == null) "新建笔记" else "编辑笔记",
                onBack = { navController.popBackStack() },
                trailing = {
                    TextButton(onClick = { save() }) { Text("保存", fontWeight = FontWeight.Bold) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "更多") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("导出 Markdown") }, onClick = { menuOpen = false; pendingExport = "# ${title.ifBlank { "无标题" }}\n\n${text.text}"; exportLauncher.launch("${title.ifBlank { "笔记" }}.md") })
                            DropdownMenuItem(text = { Text("导入 Markdown") }, onClick = { menuOpen = false; importLauncher.launch(arrayOf("text/*", "text/markdown", "application/octet-stream")) })
                            if (existing != null) DropdownMenuItem(text = { Text("删除笔记") }, onClick = { menuOpen = false; vm.delete(existing); navController.popBackStack() })
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), singleLine = true)

            // 模式切换
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(NoteMode.EDIT to "编辑", NoteMode.PREVIEW to "预览", NoteMode.SPLIT to "分屏").forEach { (m, label) ->
                    val sel = mode == m
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .clickable { mode = m }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) { Text(label, fontSize = 13.sp, color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) }
                }
            }

            when (mode) {
                NoteMode.EDIT -> {
                    MarkdownToolbar { wrap -> text = wrap(text) }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                        placeholder = { Text("在此输入 Markdown 内容…") },
                    )
                }
                NoteMode.PREVIEW -> {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp)) {
                        MarkdownText(text.text)
                    }
                }
                NoteMode.SPLIT -> {
                    Column(Modifier.fillMaxSize().padding(top = 8.dp)) {
                        OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(0.5f), placeholder = { Text("编辑…") })
                        Box(Modifier.weight(0.5f).verticalScroll(rememberScrollState()).padding(top = 12.dp)) { MarkdownText(text.text) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownToolbar(onAction: (TextFieldValue.() -> TextFieldValue) -> Unit) {
    val style = MaterialTheme.colorScheme.onSurface
    data class Btn(val label: String?, val icon: String?, val action: TextFieldValue.() -> TextFieldValue)
    val buttons = listOf(
        Btn("B", null) { wrapInline(this, "**", "**") },
        Btn("I", null) { wrapInline(this, "*", "*") },
        Btn("`", null) { wrapInline(this, "`", "`") },
        Btn("H", null) { prefixLine(this, "## ") },
        Btn("-", null) { prefixLine(this, "- ") },
        Btn(">", null) { prefixLine(this, "> ") },
        Btn(null, "link") { wrapInline(this, "[", "](url)") },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        buttons.forEach { b ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onAction(b.action) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                if (b.icon != null) AppIcon(b.icon, size = 16.dp, tint = style)
                else Text(b.label ?: "", fontWeight = FontWeight.Bold, color = style, fontSize = 14.sp)
            }
        }
    }
}

private fun wrapInline(v: TextFieldValue, before: String, after: String): TextFieldValue {
    val sel = v.selection
    val selText = v.text.substring(sel.min, sel.max)
    val newText = v.text.substring(0, sel.min) + before + selText + after + v.text.substring(sel.max)
    val newSel = TextRange(sel.min + before.length, sel.min + before.length + selText.length)
    return TextFieldValue(newText, newSel)
}

private fun prefixLine(v: TextFieldValue, prefix: String): TextFieldValue {
    val pos = v.selection.min
    val lineStart = v.text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0)) + 1
    val newText = v.text.substring(0, lineStart) + prefix + v.text.substring(lineStart)
    val newPos = pos + prefix.length
    return TextFieldValue(newText, TextRange(newPos, newPos))
}