package com.solium.Unitoolkit.ui.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.solium.Unitoolkit.core.model.CoursePreset
import com.solium.Unitoolkit.ui.components.EmptyHint
import com.solium.Unitoolkit.ui.components.SimpleTopBar
import com.solium.Unitoolkit.ui.components.SectionLabel

@Composable
fun CoursePresetScreen(navController: NavController, vm: CourseViewModel = viewModel(factory = CourseViewModel.Factory)) {
    val presets by vm.coursePresets.collectAsState()
    var editing by remember { mutableStateOf<CoursePreset?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SimpleTopBar(title = "课表预设", onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "新建预设")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "预设只保存已填写的字段；新建课程时可一键套用。禁止保存完全空白的预设。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            if (presets.isEmpty()) {
                EmptyHint("暂无预设，点击右下角新建")
            } else {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    presets.forEach { p ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable {
                                editing = p; showDialog = true
                            }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                val parts = listOf(p.courseName, p.teacher, p.location).filter { it.isNotBlank() }
                                Text(if (parts.isEmpty()) "仅部分基础信息" else parts.joinToString(" · "), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { vm.deletePreset(p.id) }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        CoursePresetDialog(
            preset = editing,
            onDismiss = { showDialog = false },
            onSave = { p ->
                vm.savePreset(p)
                showDialog = false
            },
        )
    }
}

@Composable
private fun CoursePresetDialog(
    preset: CoursePreset?,
    onDismiss: () -> Unit,
    onSave: (CoursePreset) -> Unit,
) {
    var name by remember { mutableStateOf(preset?.name ?: "") }
    var courseName by remember { mutableStateOf(preset?.courseName ?: "") }
    var teacher by remember { mutableStateOf(preset?.teacher ?: "") }
    var location by remember { mutableStateOf(preset?.location ?: "") }
    var weekday by remember { mutableStateOf(preset?.weekday ?: 0) }
    var weekdaysText by remember { mutableStateOf(preset?.weekdaysText ?: "") }
    var startTime by remember { mutableStateOf(preset?.startTime ?: "") }
    var endTime by remember { mutableStateOf(preset?.endTime ?: "") }
    var colorTag by remember { mutableStateOf(preset?.colorTag ?: "") }
    var note by remember { mutableStateOf(preset?.note ?: "") }
    var weekType by remember { mutableStateOf(preset?.weekType ?: "") }
    var onceWeek by remember { mutableStateOf(preset?.onceWeek ?: 0) }
    var selectedWeeks by remember { mutableStateOf(preset?.selectedWeeks ?: "") }
    var selectedDates by remember { mutableStateOf(preset?.selectedDates ?: "") }

    val filled = listOf(name.trim(), courseName.trim(), teacher.trim(), location.trim(), startTime.trim(), endTime.trim(), colorTag.trim(), note.trim(), weekType.trim(), selectedWeeks.trim(), selectedDates.trim()).any { it.isNotBlank() } || weekday != 0 || onceWeek != 0

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(if (preset == null) "新建课表预设" else "编辑课表预设", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(name, { name = it }, label = { Text("预设名称（必填，用于识别）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(courseName, { courseName = it }, label = { Text("课程名称（可留空）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(teacher, { teacher = it }, label = { Text("教师") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(location, { location = it }, label = { Text("教室") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            SectionLabel("上课星期（点击“不限”清除）")
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(if (weekday == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface).clickable { weekday = 0 }.padding(horizontal = 8.dp, vertical = 6.dp),
                ) { Text("不限", fontSize = 12.sp, color = if (weekday == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
                (1..7).forEach { d ->
                    val selected = weekday == d
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp)).background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface).clickable { weekday = d }.padding(horizontal = 8.dp, vertical = 6.dp),
                    ) { Text("周$d", fontSize = 12.sp, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
                }
            }
            OutlinedTextField(
                value = weekdaysText,
                onValueChange = { weekdaysText = it },
                label = { Text("多选星期，逗号分隔（留空=不限/每天）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(startTime, { startTime = it }, label = { Text("开始 HH:mm") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(endTime, { endTime = it }, label = { Text("结束 HH:mm") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(colorTag, { colorTag = it }, label = { Text("颜色 #RRGGBB") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(note, { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(weekType, { weekType = it }, label = { Text("周次类型（可留空：every/odd/even/once/selected/dates）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(if (onceWeek == 0) "" else onceWeek.toString(), { onceWeek = it.toIntOrNull() ?: 0 }, label = { Text("仅一次周次（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(selectedWeeks, { selectedWeeks = it }, label = { Text("多选周次，逗号分隔") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(selectedDates, { selectedDates = it }, label = { Text("指定日期，逗号分隔 yyyy-MM-dd") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(
                    onClick = {
                        onSave(
                            CoursePreset(
                                id = preset?.id ?: "",
                                name = name.trim(),
                                courseName = courseName.trim(),
                                teacher = teacher.trim(),
                                location = location.trim(),
                                weekday = weekday,
                                weekdaysText = weekdaysText.trim(),
                                startTime = startTime.trim(),
                                endTime = endTime.trim(),
                                colorTag = colorTag.trim(),
                                note = note.trim(),
                                weekType = weekType.trim(),
                                onceWeek = onceWeek,
                                selectedWeeks = selectedWeeks.trim(),
                                selectedDates = selectedDates.trim(),
                            ),
                        )
                    },
                    enabled = name.isNotBlank() && filled,
                ) { Text("保存", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
