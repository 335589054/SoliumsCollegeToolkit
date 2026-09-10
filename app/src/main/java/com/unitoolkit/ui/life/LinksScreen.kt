package com.unitoolkit.ui.life

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.unitoolkit.core.database.LinkEntity
import com.unitoolkit.ui.components.AppIconBadge
import com.unitoolkit.ui.components.EmptyHint
import com.unitoolkit.ui.components.SimpleTopBar
import com.unitoolkit.ui.navigation.Routes

private val categories = listOf("学习", "生活", "娱乐", "自定义")

@Composable
fun LinksScreen(navController: NavController, vm: LinksViewModel = viewModel(factory = LinksViewModel.Factory)) {
    val links by vm.links.collectAsState()
    var editing by remember { mutableStateOf<LinkEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SimpleTopBar(title = "便捷链接", onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) { Icon(Icons.Default.Add, contentDescription = "添加链接") }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Text("点击链接可在应用内打开；开启「首页」可展示在概览页（最多 3 个）", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            if (links.isEmpty()) {
                EmptyHint("暂无链接，点击右下角添加")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(links, key = { it.id }) { link ->
                        LinkItem(
                            link = link,
                            onClick = { navController.navigate(Routes.webview(link.name, link.url)) },
                            onEdit = { editing = link; showDialog = true },
                            onToggleHome = { vm.upsert(link.copy(isHomeCard = !link.isHomeCard)) },
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        LinkEditDialog(
            existing = editing,
            onDismiss = { showDialog = false },
            onSave = { l -> vm.upsert(l); showDialog = false },
            onDelete = { l -> vm.delete(l); showDialog = false },
        )
    }
}

@Composable
private fun LinkItem(link: LinkEntity, onClick: () -> Unit, onEdit: () -> Unit, onToggleHome: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconBadge(link.icon.ifBlank { "link" })
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(link.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${link.category} · ${link.url}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("首页", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Switch(checked = link.isHomeCard, onCheckedChange = { onToggleHome() })
        }
        IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun LinkEditDialog(existing: LinkEntity?, onDismiss: () -> Unit, onSave: (LinkEntity) -> Unit, onDelete: (LinkEntity) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var url by remember { mutableStateOf(existing?.url ?: "") }
    var icon by remember { mutableStateOf(existing?.icon ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "自定义") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (existing == null) "添加链接" else "编辑链接", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("网址 (https://…)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = icon, onValueChange = { icon = it }, label = { Text("图标名称（lucide，可选）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { c ->
                    val sel = category == c
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .clickable { category = c }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) { Text(c, fontSize = 12.sp, color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (existing != null) TextButton(onClick = { onDelete(existing) }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = {
                    if (name.isNotBlank() && url.isNotBlank()) {
                        onSave(
                            LinkEntity(
                                id = existing?.id ?: 0,
                                name = name.trim(),
                                url = url.trim(),
                                category = category,
                                icon = icon.trim(),
                                sortOrder = existing?.sortOrder ?: 0,
                                isHomeCard = existing?.isHomeCard ?: false,
                            )
                        )
                    }
                }) { Text("保存", fontWeight = FontWeight.Bold) }
            }
        }
    }
}