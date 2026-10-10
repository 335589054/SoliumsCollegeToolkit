package com.solium.Unitoolkit.ui.course

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.solium.Unitoolkit.core.model.HolidayAdjustment
import com.solium.Unitoolkit.core.utils.DateTimePickers
import com.solium.Unitoolkit.core.utils.DateUtils
import com.solium.Unitoolkit.core.utils.readTextFromUri
import com.solium.Unitoolkit.core.utils.writeTextToUri
import com.solium.Unitoolkit.ui.components.EmptyHint
import com.solium.Unitoolkit.ui.components.PrimaryButton
import com.solium.Unitoolkit.ui.components.SimpleTopBar

/** 调休编辑器：维护「某天补某天的课」，课表会按调休自动映射课程。 */
@Composable
fun HolidayAdjustScreen(navController: NavController, vm: CourseViewModel = viewModel(factory = CourseViewModel.Factory)) {
    val adjustments by vm.holidayAdjustments.collectAsState()
    val context = LocalContext.current

    var editing by remember { mutableStateOf<HolidayAdjustment?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<HolidayAdjustment?>(null) }
    var hint by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val list = CourseExport.holidayFromJson(readTextFromUri(context, uri))
            if (list.isEmpty()) {
                hint = "导入失败：文件中没有可识别的调休数据"
            } else {
                // 按「实际日期」去重合并，避免重复导入产生冲突
                val merged = adjustments.toMutableList()
                list.forEach { incoming ->
                    val idx = merged.indexOfFirst { it.date == incoming.date }
                    if (idx >= 0) merged[idx] = incoming else merged.add(incoming)
                }
                vm.saveHolidayAdjustments(merged)
                hint = "已导入 ${list.size} 条调休"
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) writeTextToUri(context, uri, CourseExport.holidayToJson(adjustments))
    }

    Scaffold(
        topBar = { SimpleTopBar(title = "调休编辑器", onBack = { navController.popBackStack() }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Text(
                "例如「10-10 补 10-07 的课」：添加后，10-10 当天会显示 10-07（周二）的课程。调休表可导出为 .sltx 分享或备份。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            if (hint.isNotBlank()) {
                Text(hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 6.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                PrimaryButton(label = "新增调休", onClick = { adding = true })
                Box(Modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { importLauncher.launch(arrayOf("application/json", "*/*")) }.padding(horizontal = 16.dp, vertical = 13.dp)) {
                    Text("导入 .sltx", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Box(Modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { if (adjustments.isNotEmpty()) exportLauncher.launch("调休表.sltx") }.padding(horizontal = 16.dp, vertical = 13.dp)) {
                    Text("导出 .sltx", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            if (adjustments.isEmpty()) {
                EmptyHint("还没有调休记录")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(adjustments.sortedBy { it.date }, key = { it.id.ifBlank { it.date } }) { a ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { editing = a }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${a.date}（${DateUtils.weekdayZh(weekdayOf(a.date))}）补 ${a.sourceDate}（${DateUtils.weekdayZh(weekdayOf(a.sourceDate))}）的课",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (a.note.isNotBlank()) {
                                    Text(a.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { deleting = a }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (adding || editing != null) {
        HolidayDialog(
            initial = editing,
            onDismiss = { adding = false; editing = null },
            onSave = { a ->
                val next = if (editing == null) {
                    adjustments + a.copy(id = System.currentTimeMillis().toString())
                } else {
                    adjustments.map { if (it.id == a.id) a else it }
                }
                vm.saveHolidayAdjustments(next)
                adding = false; editing = null
            },
        )
    }

    deleting?.let { a ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除调休") },
            text = { Text("确定删除「${a.date} 补 ${a.sourceDate}」这条调休记录吗？") },
            confirmButton = {
                TextButton(onClick = { vm.saveHolidayAdjustments(adjustments.filterNot { it.id == a.id }); deleting = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun HolidayDialog(initial: HolidayAdjustment?, onDismiss: () -> Unit, onSave: (HolidayAdjustment) -> Unit) {
    val context = LocalContext.current
    var date by remember { mutableStateOf(initial?.date ?: DateUtils.today()) }
    var sourceDate by remember { mutableStateOf(initial?.sourceDate ?: DateUtils.today()) }
    var note by remember { mutableStateOf(initial?.note ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(if (initial == null) "新增调休" else "编辑调休", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("选择「实际上课的日期」与「借用哪一天课表」，例如 10-10 补 10-07。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            DatePickRow("实际上课日期", date) { DateTimePickers.pickDate(context, date) { date = it } }
            DatePickRow("借用课表日期", sourceDate) { DateTimePickers.pickDate(context, sourceDate) { sourceDate = it } }
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(
                    onClick = { onSave(HolidayAdjustment(id = initial?.id ?: "", date = date, sourceDate = sourceDate, note = note.trim())) },
                    enabled = date.isNotBlank() && sourceDate.isNotBlank(),
                ) { Text("保存", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun DatePickRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text("$value ${DateUtils.weekdayZh(weekdayOf(value))}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun weekdayOf(date: String): Int =
    DateUtils.parseDateOnly(date)?.let { DateUtils.weekdayOf(it) } ?: 0
