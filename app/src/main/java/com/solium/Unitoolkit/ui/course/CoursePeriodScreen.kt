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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.solium.Unitoolkit.core.model.PeriodTiming
import com.solium.Unitoolkit.core.utils.DateTimePickers
import com.solium.Unitoolkit.ui.components.SectionLabel
import com.solium.Unitoolkit.ui.components.SimpleTopBar

/** “第几节课时间”预设编辑：每节课的开始/结束时间，并区分上午（isAfternoon=false）/下午（isAfternoon=true）。 */
@Composable
fun CoursePeriodScreen(navController: NavController, vm: CourseViewModel = viewModel(factory = CourseViewModel.Factory)) {
    val stored by vm.periodTimings.collectAsState()
    var entries by remember { mutableStateOf<List<PeriodTiming>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    if (!loaded && stored.isNotEmpty()) {
        entries = stored
        loaded = true
    }
    val context = LocalContext.current

    fun updateSection(index: String, transform: (PeriodTiming) -> PeriodTiming) {
        entries = entries.map { if ("${it.section}" == index) transform(it) else it }
    }

    Scaffold(
        topBar = {
            SimpleTopBar(
                title = "节次时间预设",
                onBack = { navController.popBackStack() },
                trailing = {
                    TextButton(
                        onClick = { vm.savePeriodTimings(entries); navController.popBackStack() },
                        enabled = entries.any { it.startTime.isNotBlank() && it.endTime.isNotBlank() },
                    ) { Text("保存", fontWeight = FontWeight.Bold) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                "用于定义“第几节课”的起止时间，与“课表预设”相互独立。课程的开始时间命中某节次时，课表会按此时间展示；未命中的课程仍用自身时间。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            PeriodGroup(
                label = "上午",
                entries = entries.filter { !it.isAfternoon }.sortedBy { it.section },
                update = { idx, t -> updateSection(idx) { t } },
                add = { entries = entries + PeriodTiming(section = nextSection(entries), isAfternoon = false, startTime = "08:00", endTime = "08:45") },
                remove = { idx -> entries = entries.filterNot { "${it.section}" == idx } },
                context,
            )
            PeriodGroup(
                label = "下午",
                entries = entries.filter { it.isAfternoon }.sortedBy { it.section },
                update = { idx, t -> updateSection(idx) { t } },
                add = { entries = entries + PeriodTiming(section = nextSection(entries), isAfternoon = true, startTime = "14:00", endTime = "14:45") },
                remove = { idx -> entries = entries.filterNot { "${it.section}" == idx } },
                context,
            )
        }
    }
}

@Composable
private fun PeriodGroup(
    label: String,
    entries: List<PeriodTiming>,
    update: (String, PeriodTiming) -> Unit,
    add: () -> Unit,
    remove: (String) -> Unit,
    context: android.content.Context,
) {
    SectionLabel("$label（${entries.size} 节）")
    if (entries.isEmpty()) {
        Text("暂无${label}课节", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    entries.forEachIndexed { i, p ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$label${i + 1} 节", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(end = 8.dp))
            TimeChip(p.startTime) { DateTimePickers.pickTime(context, p.startTime) { update("${p.section}", p.copy(startTime = it)) } }
            Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 6.dp))
            TimeChip(p.endTime) { DateTimePickers.pickTime(context, p.endTime) { update("${p.section}", p.copy(endTime = it)) } }
            TextButton(onClick = { remove("${p.section}") }) { Text("删", color = MaterialTheme.colorScheme.error) }
        }
    }
    TextButton(onClick = add) { Text("＋ 新增${label}课节", color = MaterialTheme.colorScheme.primary) }
}

@Composable
private fun TimeChip(value: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(if (value.isBlank()) "—" else value, fontSize = 14.sp, color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
    }
}

private fun nextSection(entries: List<PeriodTiming>): Int =
    (entries.maxOfOrNull { it.section } ?: 0) + 1