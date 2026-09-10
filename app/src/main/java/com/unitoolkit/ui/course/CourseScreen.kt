package com.unitoolkit.ui.course

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.unitoolkit.core.database.CourseEntity
import com.unitoolkit.core.database.matchesWeek
import com.unitoolkit.core.database.matchesWeekday
import com.unitoolkit.core.utils.DateTimePickers
import com.unitoolkit.core.utils.DateUtils
import com.unitoolkit.core.utils.parseHexColor
import com.unitoolkit.core.utils.writeTextToUri
import com.unitoolkit.ui.components.EmptyHint
import com.unitoolkit.ui.components.SimpleTopBar
import com.unitoolkit.ui.navigation.Routes

@Composable
fun CourseScreen(navController: NavController, vm: CourseViewModel = viewModel(factory = CourseViewModel.Factory)) {
    val courses by vm.courses.collectAsState()
    val week by vm.currentWeek.collectAsState()
    val semesterWeeks by vm.semesterWeeks.collectAsState()
    val semesterStart by vm.semesterStartDate.collectAsState()
    var selectedDay by remember { mutableStateOf(DateUtils.todayWeekday()) }
    var viewMode by remember { mutableStateOf("three") }
    var menuOpen by remember { mutableStateOf(false) }
    var showSemester by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var pendingExport by remember { mutableStateOf("") }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri != null && pendingExport.isNotBlank()) {
            writeTextToUri(context, uri, pendingExport)
            pendingExport = ""
        }
    }

    Scaffold(
        topBar = {
            SimpleTopBar(
                title = "课表",
                onBack = { navController.popBackStack() },
                trailing = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "导出") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("导出 Markdown") },
                                onClick = {
                                    menuOpen = false
                                    pendingExport = CourseExport.toMarkdown(courses, week)
                                    exportLauncher.launch("课表_第${week}周.md")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("导出 JSON") },
                                onClick = {
                                    menuOpen = false
                                    pendingExport = CourseExport.toJson(courses)
                                    exportLauncher.launch("课表.json")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("课表预设") },
                                onClick = {
                                    menuOpen = false
                                    navController.navigate(Routes.COURSE_PRESETS)
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.courseEdit(-1)) }) {
                Icon(Icons.Default.Add, contentDescription = "添加课程")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            // 周切换
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                WeekArrow("‹") { vm.setWeek((week - 1).coerceAtLeast(1)) }
                Text(
                    "第 $week 周",
                    Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                WeekArrow("›") { vm.setWeek((week + 1).coerceAtMost(semesterWeeks.coerceAtLeast(1))) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (semesterStart.isBlank()) "设置学期起始" else "起始：$semesterStart",
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { DateTimePickers.pickDate(context, semesterStart.ifBlank { DateUtils.today() }) { vm.setSemesterStartDate(it) } }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "学期长度：$semesterWeeks 周",
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showSemester = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // 布局方式
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("day" to "单日列表", "three" to "三日时间轴").forEach { (value, label) ->
                    val selected = viewMode == value
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .clickable { viewMode = value }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            if (viewMode == "three") {
                ThreeDaySchedule(courses, week, Modifier.weight(1f)) { id -> navController.navigate(Routes.courseEdit(id)) }
            } else {
                // 7 天选择
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    (1..7).forEach { d ->
                        val selected = d == selectedDay
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                .clickable { selectedDay = d },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(DateUtils.weekdayZh(d).removePrefix("周"), fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val dayCourses = courses.filter { it.matchesWeekday(selectedDay) && it.matchesWeek(week) }
                if (dayCourses.isEmpty()) {
                    EmptyHint("${DateUtils.weekdayZh(selectedDay)}暂无课程，点击右下角添加")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(dayCourses, key = { it.id }) { c ->
                            CourseItem(c) { navController.navigate(Routes.courseEdit(c.id)) }
                        }
                    }
                }
            }
        }
    }

    if (showSemester) {
        SemesterDialog(
            current = semesterWeeks,
            onDismiss = { showSemester = false },
            onSelect = { vm.setSemesterWeeks(it); showSemester = false },
        )
    }
}

@Composable
private fun WeekArrow(symbol: String, onClick: () -> Unit) {
    Text(
        symbol,
        Modifier.clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 4.dp),
        fontSize = 24.sp,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun SemesterDialog(current: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    val options = listOf(8, 10, 12, 14, 16, 18, 20, 22, 24)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("学期长度") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { w ->
                            val sel = w == current
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onSelect(w) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            ) { Text("${w} 周", fontSize = 13.sp, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun CourseItem(course: CourseEntity, onClick: () -> Unit) {
    val color = remember(course.colorTag) { parseHexColor(course.colorTag) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(16.dp).clip(CircleShape).background(color))
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(course.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${course.startTime} - ${course.endTime}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            val sub = listOf(course.location, course.teacher).filter { it.isNotBlank() }.joinToString(" · ")
            if (sub.isNotBlank()) Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ThreeDaySchedule(
    courses: List<CourseEntity>,
    week: Int,
    modifier: Modifier = Modifier,
    onCourseClick: (Long) -> Unit,
) {
    val dates = (0..2).map { DateUtils.offsetDate(it) }
    val hourHeight = 72.dp
    val timelineHeight = hourHeight * (TimelineEndHour - TimelineStartHour).toFloat()
    Column(
        modifier
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 46.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            dates.forEach { date ->
                val day = DateUtils.parseDateOnly(date)?.let { DateUtils.weekdayOf(it) } ?: 0
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(DateUtils.weekdayZh(day), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text(date.substring(5), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(Modifier.height(timelineHeight).fillMaxWidth()) {
            Column(Modifier.width(46.dp).fillMaxHeight()) {
                (TimelineStartHour until TimelineEndHour).forEach { hour ->
                    Text(
                        "%02d:00".format(hour),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(hourHeight),
                    )
                }
            }
            dates.forEach { date ->
                val day = DateUtils.parseDateOnly(date)?.let { DateUtils.weekdayOf(it) } ?: 0
                val dayCourses = courses.filter {
                    it.matchesWeekday(day) && it.matchesWeek(week, date)
                }.sortedBy { it.startTime }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    // 小时分隔线
                    (TimelineStartHour until TimelineEndHour).forEach { hour ->
                        Box(
                            Modifier
                                .offset(y = hourHeight * (hour - TimelineStartHour).toFloat())
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        )
                    }
                    dayCourses.forEach { course ->
                        TimelineBlock(course, hourHeight) { onCourseClick(course.id) }
                    }
                }
            }
        }
    }
}

private val TimelineStartHour = 7
private val TimelineEndHour = 22

@Composable
private fun TimelineBlock(course: CourseEntity, hourHeight: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val color = remember(course.colorTag) { parseHexColor(course.colorTag) }
    fun toMinutes(value: String): Int = DateUtils.timeToMinutes(value)
    val startRaw = toMinutes(course.startTime)
    val endRaw = toMinutes(course.endTime).coerceAtLeast(startRaw + 10)
    val startMin = startRaw.coerceIn(TimelineStartHour * 60, TimelineEndHour * 60 - 1)
    val duration = (endRaw - startRaw).coerceAtLeast(10)
    val top = (((startMin - TimelineStartHour * 60) / 60f) * hourHeight.value).dp
    val height = ((duration / 60f) * hourHeight.value).dp
    Box(
        Modifier
            .offset(x = 2.dp, y = top.coerceAtLeast(0.dp))
            .fillMaxWidth(0.94f)
            .height(height.coerceAtLeast(26.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.22f))
            .border(1.dp, color, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(course.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${course.startTime} 开始", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            if (course.location.isNotBlank()) {
                Text(course.location, fontSize = 9.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
