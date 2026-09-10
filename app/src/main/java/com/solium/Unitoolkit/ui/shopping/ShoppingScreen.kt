package com.solium.Unitoolkit.ui.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.solium.Unitoolkit.core.database.PostponedItemRow
import com.solium.Unitoolkit.core.database.ShoppingItemEntity
import com.solium.Unitoolkit.ui.components.AppIconBadge
import com.solium.Unitoolkit.ui.components.EmptyHint
import com.solium.Unitoolkit.ui.components.SimpleTopBar
import com.solium.Unitoolkit.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val shoppingTypes = listOf("生活超市", "朴朴", "山姆", "奶茶店", "面包店", "淘宝网购", "线下菜市场")

fun shoppingTypeIcon(type: String): String = when (type) {
    "生活超市" -> "shopping-cart"
    "朴朴" -> "leaf"
    "山姆" -> "store"
    "奶茶店" -> "cup-soda"
    "面包店" -> "croissant"
    "淘宝网购" -> "package"
    "线下菜市场" -> "shopping-basket"
    else -> "shopping-cart"
}

@Composable
fun ShoppingScreen(navController: NavController, vm: ShoppingViewModel = viewModel(factory = ShoppingViewModel.Factory)) {
    val activities by vm.activities.collectAsState()
    val postponedRows by vm.postponedRows.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) } // 0=采购活动 1=暂缓采购区
    var itemToDelete by remember { mutableStateOf<ShoppingItemEntity?>(null) }

    Scaffold(
        topBar = { SimpleTopBar(title = "采购清单", onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            if (tab == 0) {
                FloatingActionButton(onClick = { showCreate = true }) { Icon(Icons.Default.Add, contentDescription = "新建采购活动") }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SegTab("采购活动", tab == 0) { tab = 0 }
                SegTab("暂缓采购区 (${postponedRows.size})", tab == 1) { tab = 1 }
            }

            if (tab == 0) {
                Text("按采购活动管理清单；「暂缓采购」的项目会自动带入下次同类型采购。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                if (activities.isEmpty()) {
                    EmptyHint("暂无采购活动，点击右下角新建")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                        items(activities, key = { it.id }) { activity ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface)
                                    .clickable { navController.navigate(Routes.shoppingActivity(activity.id)) }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIconBadge(shoppingTypeIcon(activity.type))
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(activity.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(activity.type, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                Text("暂缓采购的项目会在下次新建同类型采购时自动带入；可在此移回或删除。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                if (postponedRows.isEmpty()) {
                    EmptyHint("暂缓采购区为空")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                        items(postponedRows, key = { it.item.id }) { row ->
                            PostponedRow(
                                row = row,
                                onRestore = { vm.togglePostponed(row.item) },
                                onDelete = { itemToDelete = row.item },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateActivityDialog(onDismiss = { showCreate = false }, onCreate = { name, type ->
            vm.createActivity(name, type)
            showCreate = false
        })
    }
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("删除暂缓项目") },
            text = { Text("确定删除「${itemToDelete?.name}」吗？") },
            confirmButton = { TextButton(onClick = { itemToDelete?.let { vm.deleteItem(it) }; itemToDelete = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun RowScope.SegTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PostponedRow(row: PostponedItemRow, onRestore: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconBadge(shoppingTypeIcon(row.activityType))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(row.item.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(row.activityType, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onRestore) { Text("移回", color = MaterialTheme.colorScheme.primary) }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateActivityDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var type by remember { mutableStateOf("生活超市") }
    val dateStr = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date())
    var name by remember { mutableStateOf("生活超市 $dateStr") }
    var nameEdited by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("新建采购活动", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

            Text("① 先选择类型（会自动填入名称）", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                shoppingTypes.forEach { t ->
                    val sel = type == t
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                type = t
                                if (!nameEdited) name = "$t $dateStr"
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) { Text(t, fontSize = 13.sp, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
                }
            }

            Text("② 可修改活动名", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = name,
                onValueChange = {
                    nameEdited = true
                    name = it
                },
                label = { Text("活动名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = {
                    val n = name.trim().ifBlank { "$type $dateStr" }
                    onCreate(n, type)
                }) { Text("创建", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
