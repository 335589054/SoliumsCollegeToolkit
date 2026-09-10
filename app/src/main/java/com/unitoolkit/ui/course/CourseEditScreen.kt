package com.unitoolkit.ui.course

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
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
import com.unitoolkit.core.database.CourseEntity
import com.unitoolkit.core.model.CoursePreset
import com.unitoolkit.core.utils.DateUtils
import com.unitoolkit.ui.components.SectionLabel
import com.unitoolkit.ui.components.SimpleTopBar

private val colorOptions = listOf("#8B5CF6", "#3B6B4F", "#5B9BD5", "#9B7FB8", "#E9749B", "#F59E0B", "#EF4444", "#10B981")

private val weekTypeOptions = listOf(
    "every" to "每周",
    "odd" to "单周",
    "even" to "双周",
    "once" to "仅一次",
    "selected" to "多选周次",
    "dates" to "指定日期",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CourseEditScreen(navController: NavController, vm: CourseViewModel = viewModel(factory = CourseViewModel.Factory)) {
    val courseId = navController.currentBackStackEntry?.arguments?.getLong("id") ?: -1L
    val courses by vm.courses.collectAsState()
    val presets by vm.coursePresets.collectAsState()
    val semesterWeeks by vm.semesterWeeks.collectAsState()
    val existing = courses.firstOrNull { it.id == courseId }

    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var teacher by rememberSaveable { mutableStateOf(existing?.teacher ?: "") }
    var location by rememberSaveable { mutableStateOf(existing?.location ?: "") }
    var weekday by rememberSaveable { mutableStateOf(existing?.weekday ?: DateUtils.todayWeekday()) }
    var startTime by rememberSaveable { mutableStateOf(existing?.startTime ?: "08:00") }
    var endTime by rememberSaveable { mutableStateOf(existing?.endTime ?: "08:45") }
    var colorTag by rememberSaveable { mutableStateOf(existing?.colorTag ?: "#8B5CF6") }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var weekType by rememberSaveable { mutableStateOf(existing?.weekType ?: "every") }
    var onceWeek by rememberSaveable { mutableStateOf(existing?.onceWeek ?: 1) }
    var selectedWeeks by rememberSaveable { mutableStateOf(existing?.selectedWeeks ?: "") }
    var selectedDates by rememberSaveable { mutableStateOf(existing?.selectedDates ?: "") }
    var selectedDays by rememberSaveable {
        mutableStateOf(
            existing?.weekdaysText?.takeIf { it.isNotBlank() }
                ?: existing?.weekday?.toString()
                ?: DateUtils.todayWeekday().toString(),
        )
    }
    var showSavePreset by remember { mutableStateOf(false) }

    // 编辑时课程异步加载，初次组合 existing 为空；加载完成后回填一次，避免用户需重新输入
    var initialized by rememberSaveable { mutableStateOf(courseId == -1L) }
    LaunchedEffect(existing?.id) {
        if (!initialized && existing != null) {
            name = existing.name
            teacher = existing.teacher
            location = existing.location
            weekday = existing.weekday
            startTime = existing.startTime
            endTime = existing.endTime
            colorTag = existing.colorTag
            note = existing.note
            weekType = existing.weekType
            onceWeek = existing.onceWeek
            selectedWeeks = existing.selectedWeeks
            selectedDates = existing.selectedDates
            selectedDays = existing.weekdaysText.ifBlank { existing.weekday.toString() }
            initialized = true
        }
    }

    val canSave = name.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()

    Scaffold(
        topBar = {
            SimpleTopBar(
                title = if (existing == null) "添加课程" else "编辑课程",
                onBack = { navController.popBackStack() },
                trailing = {
                    TextButton(
                        onClick = {
                            vm.upsert(
                                CourseEntity(
                                    id = courseId.coerceAtLeast(0),
                                    name = name.trim(),
                                    teacher = teacher.trim(),
                                    location = location.trim(),
                                    weekday = selectedDays.split(',').mapNotNull { it.trim().toIntOrNull() }.firstOrNull() ?: 0,
                                    weekdaysText = selectedDays,
                                    startTime = startTime,
                                    endTime = endTime,
                                    colorTag = colorTag,
                                    note = note.trim(),
                                    weekType = weekType,
                                    onceWeek = onceWeek,
                                    selectedWeeks = selectedWeeks,
                                    selectedDates = selectedDates,
                                )
                            )
                            navController.popBackStack()
                        },
                        enabled = canSave,
                    ) { Text("保存", fontWeight = FontWeight.Bold) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (presets.isNotEmpty()) {
                SectionLabel("套用预设")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { p ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable {
                                    if (p.courseName.isNotBlank()) name = p.courseName
                                    if (p.teacher.isNotBlank()) teacher = p.teacher
                                    if (p.location.isNotBlank()) location = p.location
                                    if (p.weekday > 0) weekday = p.weekday
                                    if (p.weekdaysText.isNotBlank()) selectedDays = p.weekdaysText
                                    else if (p.weekday > 0) selectedDays = p.weekday.toString()
                                    if (p.startTime.isNotBlank()) startTime = p.startTime
                                    if (p.endTime.isNotBlank()) endTime = p.endTime
                                    if (p.colorTag.isNotBlank()) colorTag = p.colorTag
                                    if (p.note.isNotBlank()) note = p.note
                                    if (p.weekType.isNotBlank()) weekType = p.weekType
                                    if (p.onceWeek > 0) onceWeek = p.onceWeek
                                    if (p.selectedWeeks.isNotBlank()) selectedWeeks = p.selectedWeeks
                                    if (p.selectedDates.isNotBlank()) selectedDates = p.selectedDates
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) { Text(p.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface) }
                    }
                }
            }
            TextButton(onClick = { showSavePreset = true }) { Text("＋ 将当前填写保存为预设", color = MaterialTheme.colorScheme.primary) }
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("课程名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = teacher, onValueChange = { teacher = it }, label = { Text("教师") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("教室") }, modifier = Modifier.weight(1f), singleLine = true)
            }

            SectionLabel("上课星期（可多选；不选表示每天/由日期决定）")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedDays.isBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .clickable { selectedDays = "" }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Text("每天", fontSize = 12.sp, color = if (selectedDays.isBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                }
                (1..7).forEach { d ->
                    val selected = d.toString() in selectedDays.split(',').map { it.trim() }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .clickable { selectedDays = toggleCsvValue(selectedDays, d.toString()) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(DateUtils.weekdayZh(d), fontSize = 12.sp, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            SectionLabel("上课周次")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                weekTypeOptions.forEach { (value, label) ->
                    val selected = weekType == value
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .clickable { weekType = value }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) { Text(label, fontSize = 13.sp, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
                }
            }
            if (weekType == "once") {
                Text("选择本学期第几周上课", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..semesterWeeks.coerceAtLeast(1)).forEach { w ->
                        val selected = onceWeek == w
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onceWeek = w },
                            contentAlignment = Alignment.Center,
                        ) { Text("$w", fontSize = 13.sp, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
                    }
                }
            }
            if (weekType == "selected") {
                Text("可多选：本学期第几周上课", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..semesterWeeks.coerceAtLeast(1)).forEach { w ->
                        val selected = w.toString() in selectedWeeks.split(',').map { it.trim() }
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    selectedWeeks = toggleCsvValue(selectedWeeks, w.toString())
                                },
                            contentAlignment = Alignment.Center,
                        ) { Text("$w", fontSize = 13.sp, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
                    }
                }
            }
            if (weekType == "dates") {
                OutlinedTextField(
                    value = selectedDates,
                    onValueChange = { selectedDates = it },
                    label = { Text("上课日期（多个用逗号分隔，格式 2026-09-10）") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionLabel("具体时间（HH:mm）")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("开始") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("结束") }, modifier = Modifier.weight(1f), singleLine = true)
            }

            SectionLabel("颜色标签")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colorOptions.forEach { c ->
                    val selected = c == colorTag
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(parseHex(c))
                            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape) else Modifier)
                            .clickable { colorTag = c },
                    )
                }
            }

            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            if (existing != null) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { vm.delete(existing); navController.popBackStack() }) {
                    Text("删除课程", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (showSavePreset) {
        SavePresetDialog(
            defaultName = name.trim(),
            onDismiss = { showSavePreset = false },
            onSave = { presetName ->
                vm.savePreset(
                    CoursePreset(
                        name = presetName,
                        courseName = name.trim(),
                        teacher = teacher.trim(),
                        location = location.trim(),
                        weekday = selectedDays.split(',').mapNotNull { it.trim().toIntOrNull() }.firstOrNull() ?: 0,
                        weekdaysText = selectedDays,
                        startTime = startTime,
                        endTime = endTime,
                        colorTag = colorTag,
                        note = note.trim(),
                        weekType = weekType,
                        onceWeek = onceWeek,
                        selectedWeeks = selectedWeeks,
                        selectedDates = selectedDates,
                    ),
                )
                showSavePreset = false
            },
        )
    }
}

@Composable
private fun SavePresetDialog(
    defaultName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var presetName by remember { mutableStateOf(defaultName) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("保存为课表预设", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(
                value = presetName,
                onValueChange = { presetName = it },
                label = { Text("预设名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("预设会保存当前已填写的属性，之后新建课程可一键套用。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = { if (presetName.isNotBlank()) onSave(presetName.trim()) }, enabled = presetName.isNotBlank()) {
                    Text("保存", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun parseHex(hex: String): Color = com.unitoolkit.core.utils.parseHexColor(hex)

private fun toggleCsvValue(current: String, value: String): String {
    val list = current.split(',').map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
    if (value in list) list.remove(value) else list.add(value)
    return list.joinToString(",")
}
