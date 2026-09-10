package com.unitoolkit.ui.pomodoro

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.unitoolkit.core.model.PomodoroPreset
import com.unitoolkit.service.PomodoroManager
import com.unitoolkit.ui.components.PrimaryButton
import com.unitoolkit.ui.components.SimpleTopBar
import com.unitoolkit.ui.navigation.Routes

@Composable
fun PomodoroScreen(navController: NavController, vm: PomodoroViewModel = viewModel(factory = PomodoroViewModel.Factory)) {
    val state by vm.state.collectAsState()
    val presets by vm.presets.collectAsState()
    val keepScreenOn by vm.keepScreenOn.collectAsState()
    val stats by vm.todayStats.collectAsState()
    val selected by vm.selectedPreset.collectAsState()

    var showCustom by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activePreset = selected ?: presets.firstOrNull()
    val running = state.running

    DisposableEffect(running, keepScreenOn) {
        val activity = context as? Activity
        if (running && keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    Scaffold(
        topBar = { SimpleTopBar(title = "番茄钟", onBack = { navController.popBackStack() }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 预设选择（仅切换，不自动开始）
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(presets) { p ->
                    val sel = p == activePreset
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .clickable { vm.selectPreset(p) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                            Text("${p.workMinutes}/${p.shortBreakMinutes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 计时环
            val progress = if (state.totalSeconds == 0) 0f else 1f - state.remainingSeconds.toFloat() / state.totalSeconds.toFloat()
            val arcBaseColor = MaterialTheme.colorScheme.surfaceVariant
            val arcColor = if (state.phase == PomodoroManager.Phase.BREAK) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(260.dp)) {
                    val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    val inset = 20.dp.toPx()
                    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                    drawArc(color = arcBaseColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
                    drawArc(color = arcColor, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        when (state.phase) {
                            PomodoroManager.Phase.WORK -> "专注中"
                            PomodoroManager.Phase.BREAK -> "休息中"
                            else -> "准备开始"
                        },
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(PomodoroManager.formatRemaining(), fontSize = 56.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onBackground)
                    Text("第 ${state.completedWorkCount + 1} 轮", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 控制按钮
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (state.phase == PomodoroManager.Phase.IDLE) {
                    PrimaryButton(label = "开始专注", onClick = { activePreset?.let { vm.start(it) } })
                } else {
                    PrimaryButton(label = if (running) "暂停" else "继续", onClick = { if (running) vm.pause() else vm.resume() })
                    PrimaryButton(label = "停止", onClick = { vm.stop() }, outlined = true)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("保持亮屏", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
                Switch(checked = keepScreenOn, onCheckedChange = { vm.setKeepScreenOn(it) })
            }

            PrimaryButton(label = "进入沉浸模式", onClick = { navController.navigate(Routes.POMODORO_IMMERSIVE) }, outlined = true)
            PrimaryButton(label = "＋ 自定义预设", onClick = { showCustom = true }, outlined = true)

            // 今日统计
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("今日统计", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("完成轮数", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${stats.count} 轮", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("累计专注", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${stats.totalMinutes} 分钟", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showCustom) {
        CustomPresetDialog(
            onDismiss = { showCustom = false },
            onSave = { p -> vm.saveCustomPreset(p); showCustom = false },
        )
    }
}

@Composable
private fun CustomPresetDialog(onDismiss: () -> Unit, onSave: (PomodoroPreset) -> Unit) {
    var name by remember { mutableStateOf("") }
    var work by remember { mutableStateOf("25") }
    var short by remember { mutableStateOf("5") }
    var long by remember { mutableStateOf("15") }
    var interval by remember { mutableStateOf("4") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义预设") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("预设名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = work, onValueChange = { work = it }, label = { Text("专注(分)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = short, onValueChange = { short = it }, label = { Text("短休(分)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = long, onValueChange = { long = it }, label = { Text("长休(分)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = interval, onValueChange = { interval = it }, label = { Text("长休间隔") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val w = work.toIntOrNull() ?: 25
                val s = short.toIntOrNull() ?: 5
                val l = long.toIntOrNull() ?: 15
                val i = interval.toIntOrNull() ?: 4
                if (name.isNotBlank()) {
                    onSave(PomodoroPreset(name.trim(), w, s, l, i))
                }
            }) { Text("保存", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}