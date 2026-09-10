package com.unitoolkit.ui.life

import android.Manifest
import android.os.Build
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.unitoolkit.core.utils.CalendarHelper
import com.unitoolkit.core.utils.SystemEvent
import com.unitoolkit.ui.components.EmptyHint
import com.unitoolkit.ui.components.SimpleTopBar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ScheduleScreen(navController: NavController, vm: ScheduleViewModel = viewModel(factory = ScheduleViewModel.Factory)) {
    val systemEvents by vm.systemEvents.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasPermission = it.values.all { granted -> granted }
        if (hasPermission) vm.refreshSystemEvents()
    }

    fun requestPermission() {
        val perms = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        } else {
            arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        }
        permissionLauncher.launch(perms)
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (CalendarHelper.hasCalendarPermission(context)) vm.refreshSystemEvents()
        else requestPermission()
    }

    Scaffold(
        topBar = {
            SimpleTopBar(
                title = "日程提醒",
                onBack = { navController.popBackStack() },
                trailing = {
                    TextButton(onClick = {
                        if (CalendarHelper.hasCalendarPermission(context)) vm.refreshSystemEvents()
                        else requestPermission()
                    }) { Text("刷新系统日程") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, contentDescription = "添加日程") }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Text("日程直接读写系统日历，不保留本地副本（需日历读写权限）", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            if (systemEvents.isEmpty()) {
                EmptyHint("系统日历近 30 天暂无日程，点击右下角添加")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(systemEvents) { e -> SystemEventRow(e, onDelete = { vm.deleteEvent(e) }) }
                }
            }
        }
    }

    if (showAdd) {
        AddScheduleDialog(
            onDismiss = { showAdd = false },
            onSave = { title, start, end ->
                vm.addEvent(title, start, end, reminderMinutes = 0)
                showAdd = false
            },
        )
    }
}

private val dtFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

@Composable
private fun SystemEventRow(event: SystemEvent, onDelete: () -> Unit) {
    val fmt = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(event.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "${fmt.format(Date(event.startMillis))} → ${SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(event.endMillis))}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "删除日程", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, start: String, end: String) -> Unit,
) {
    val now = Calendar.getInstance()
    now.set(Calendar.MINUTE, 0)
    now.set(Calendar.SECOND, 0)
    now.set(Calendar.MILLISECOND, 0)
    val startDefault = dtFmt.format(now.time)
    now.add(Calendar.HOUR_OF_DAY, 1)
    val endDefault = dtFmt.format(now.time)

    var title by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(startDefault) }
    var end by remember { mutableStateOf(endDefault) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("添加日程", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("开始 (yyyy-MM-dd HH:mm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("结束 (yyyy-MM-dd HH:mm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        onSave(title.trim(), start.trim(), end.trim())
                    }
                }) { Text("保存", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
