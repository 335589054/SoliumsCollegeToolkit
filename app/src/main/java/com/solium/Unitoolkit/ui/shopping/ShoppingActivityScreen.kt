package com.solium.Unitoolkit.ui.shopping

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.solium.Unitoolkit.core.database.ShoppingItemEntity
import com.solium.Unitoolkit.ui.components.AppIcon
import com.solium.Unitoolkit.ui.components.EmptyHint
import com.solium.Unitoolkit.ui.components.SimpleTopBar

private val shoppingTemplate = "牛奶\n鸡蛋\n面包\n蔬菜\n水果"

@Composable
fun ShoppingActivityScreen(navController: NavController, vm: ShoppingViewModel = viewModel(factory = ShoppingViewModel.Factory)) {
    val context = LocalContext.current
    val activityId = navController.currentBackStackEntry?.arguments?.getLong("id") ?: -1L
    val activities by vm.activities.collectAsState()
    val activity = activities.firstOrNull { it.id == activityId }
    val items by vm.items(activityId).collectAsState()

    var showAdd by remember { mutableStateOf(false) }
    var showComplete by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<ShoppingItemEntity?>(null) }
    var confirmDeleteActivity by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val names = readTextLines(context, uri)
            if (names.isEmpty()) {
                Toast.makeText(context, "未解析到有效项目", Toast.LENGTH_SHORT).show()
            } else {
                vm.addItems(activityId, names)
                Toast.makeText(context, "已导入 ${names.size} 项", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun copyTemplate() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("采购清单模板", shoppingTemplate))
        Toast.makeText(context, "模板已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            SimpleTopBar(
                title = activity?.name ?: "采购清单",
                onBack = { navController.popBackStack() },
                trailing = {
                    if (activity != null) {
                        IconButton(onClick = { confirmDeleteActivity = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除活动", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, contentDescription = "添加项目") }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                ActionChip("copy", "复制模板", Modifier.weight(1f)) { copyTemplate() }
                ActionChip("upload", "导入 txt", Modifier.weight(1f)) { importLauncher.launch("text/*") }
            }
            if (activity != null && items.isNotEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { showComplete = true }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        AppIcon("square-check", size = 18.dp, tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("采购完成", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            val todo = items.filterNot { it.isDone }
            val postponed = items.filter { it.isPostponed }
            val done = items.filter { it.isDone }
            if (items.isEmpty()) {
                EmptyHint("暂无项目，点击 + 添加或导入 txt")
            } else if (todo.isEmpty() && done.isNotEmpty()) {
                EmptyHint("已全部采购完成")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(todo.filterNot { it.isPostponed }, key = { it.id }) { item -> ItemRow(item, vm) }
                    if (postponed.isNotEmpty()) {
                        item(key = "postponed_header") { Text("暂缓采购（下次同类型自动带入）", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)) }
                    }
                    items(postponed, key = { it.id }) { item -> ItemRow(item, vm) }
                    if (done.isNotEmpty()) {
                        item(key = "done_header") { Text("已采购", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)) }
                    }
                    items(done, key = { it.id }) { item -> ItemRow(item, vm) }
                }
            }
        }
    }

    if (showAdd) {
        AddItemDialog(onDismiss = { showAdd = false }, onSave = { name -> vm.addItem(activityId, name); showAdd = false })
    }
    if (showComplete && activity != null) {
        CompleteDialog(
            activityName = activity.name,
            onDismiss = { showComplete = false },
            onConfirm = { amount ->
                vm.completeActivity(activity, amount)
                showComplete = false
                Toast.makeText(context, "已标记完成，未购项目已移入暂缓采购", Toast.LENGTH_SHORT).show()
            },
        )
    }
    if (confirmDeleteActivity && activity != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteActivity = false },
            title = { Text("删除采购活动") },
            text = { Text("确定删除「${activity.name}」及其全部采购项目吗？此操作不可撤销。") },
            confirmButton = { TextButton(onClick = { vm.deleteActivity(activity); navController.popBackStack() }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDeleteActivity = false }) { Text("取消") } },
        )
    }
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("删除项目") },
            text = { Text("确定删除「${itemToDelete?.name}」吗？") },
            confirmButton = { TextButton(onClick = { itemToDelete?.let { vm.deleteItem(it) }; itemToDelete = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun ItemRow(item: ShoppingItemEntity, vm: ShoppingViewModel) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.isDone, onCheckedChange = { vm.setDone(item, !item.isDone) })
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                item.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None,
            )
        }
        Box(
            Modifier.clip(RoundedCornerShape(10.dp))
                .background(if (item.isPostponed) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                .clickable { vm.togglePostponed(item) }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text("暂缓", fontSize = 12.sp, color = if (item.isPostponed) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActionChip(icon: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(icon, size = 16.dp, tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AddItemDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("添加采购项目", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("项目名（如：牛奶 ×2）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim()) }) { Text("添加", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun CompleteDialog(activityName: String, onDismiss: () -> Unit, onConfirm: (Double?) -> Unit) {
    var amount by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("「${activityName}」采购完成", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("未购项目将移入「暂缓采购区」，并在下次同类采购时自动带入。", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("实际支付金额（可选）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            if ((amount.toDoubleOrNull() ?: 0.0) > 0.0) {
                Text("将同步一笔支出到账单 · 分类「购物」", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = { onConfirm(amount.toDoubleOrNull()?.takeIf { it > 0.0 }) }) { Text("完成", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

private fun readTextLines(context: Context, uri: Uri): List<String> {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            reader.readLines().map { it.trim() }.filter { it.isNotBlank() }
        } ?: emptyList()
    }.getOrDefault(emptyList())
}