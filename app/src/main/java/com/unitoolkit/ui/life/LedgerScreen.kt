package com.unitoolkit.ui.life

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
import androidx.compose.material.icons.filled.List
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.unitoolkit.core.database.LedgerCategoryEntity
import com.unitoolkit.core.database.LedgerEntryEntity
import com.unitoolkit.core.utils.DateTimePickers
import com.unitoolkit.core.utils.DateUtils
import com.unitoolkit.ui.components.EmptyHint
import com.unitoolkit.ui.components.SimpleTopBar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val ExpenseRed = Color(0xFFE9747B)
private val IncomeGreen = Color(0xFF3FA96F)

private val paymentOptions = listOf("微信", "支付宝", "银行卡", "自定义")

@Composable
fun LedgerScreen(navController: NavController, vm: LedgerViewModel = viewModel(factory = LedgerViewModel.Factory)) {
    val entries by vm.entries.collectAsState()
    val expenseCats by vm.expenseCategories.collectAsState()
    val incomeCats by vm.incomeCategories.collectAsState()

    var filter by remember { mutableStateOf("all") } // all / expense / income
    var showAdd by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }
    var currentMonth by remember { mutableStateOf(DateUtils.monthNow()) }
    var entryToDelete by remember { mutableStateOf<LedgerEntryEntity?>(null) }
    var showClear by remember { mutableStateOf(false) }

    val monthly = entries.filter { it.date.startsWith(currentMonth) }
    val income = monthly.filter { it.type == "income" }.sumOf { it.amount }
    val expense = monthly.filter { it.type == "expense" }.sumOf { it.amount }

    val monthEntries = entries.filter { it.date.startsWith(currentMonth) }
    val filtered = when (filter) {
        "expense" -> monthEntries.filter { it.type == "expense" }
        "income" -> monthEntries.filter { it.type == "income" }
        else -> monthEntries
    }
    val grouped = filtered.groupBy { it.date.substring(0, 10) }
        .toList()
        .sortedByDescending { it.first }

    Scaffold(
        topBar = {
            SimpleTopBar(
                title = "简易账单",
                onBack = { navController.popBackStack() },
                trailing = {
                    IconButton(onClick = { showClear = true }) { Icon(Icons.Default.Delete, contentDescription = "一键清除", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton(onClick = { showCategories = true }) { Icon(Icons.Default.List, contentDescription = "分类管理") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, contentDescription = "记一笔") }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            // 月份切换
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.clip(RoundedCornerShape(10.dp)).clickable { currentMonth = shiftMonth(currentMonth, -1) }.padding(horizontal = 12.dp, vertical = 6.dp)) { Text("‹", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(currentMonth, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Box(Modifier.clip(RoundedCornerShape(10.dp)).clickable { currentMonth = shiftMonth(currentMonth, 1) }.padding(horizontal = 12.dp, vertical = 6.dp)) { Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            // 本月汇总
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SummaryCell("本月收入", income, IncomeGreen)
                SummaryCell("本月支出", expense, ExpenseRed)
                SummaryCell("结余", income - expense, MaterialTheme.colorScheme.onPrimaryContainer)
            }

            Spacer(Modifier.height(12.dp))

            // 类型筛选
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                listOf("all" to "全部", "expense" to "支出", "income" to "收入").forEach { (k, label) ->
                    val sel = filter == k
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .clickable { filter = k }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(label, fontSize = 13.sp, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            if (grouped.isEmpty()) {
                EmptyHint("${currentMonth} 暂无账单记录")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    grouped.forEach { (date, dayEntries) ->
                        item(key = "header_$date") {
                            val dayIncome = dayEntries.filter { it.type == "income" }.sumOf { it.amount }
                            val dayExpense = dayEntries.filter { it.type == "expense" }.sumOf { it.amount }
                            Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(date, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("   支出 ¥${fmt(dayExpense)}  收入 ¥${fmt(dayIncome)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        items(dayEntries, key = { "entry_${it.id}" }) { e ->
                            EntryRow(e, onDelete = { entryToDelete = e })
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddEntryDialog(
            expenseCats = expenseCats,
            incomeCats = incomeCats,
            onDismiss = { showAdd = false },
            onSave = { e -> vm.insert(e); showAdd = false },
        )
    }
    if (showCategories) {
        CategoryDialog(
            expenseCats = expenseCats,
            incomeCats = incomeCats,
            onDismiss = { showCategories = false },
            onAdd = { vm.addCategory(it) },
        )
    }
    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("确认删除") },
            text = { Text("旧的不去，新的不来 —— 是否删除这条账单记录？") },
            confirmButton = {
                TextButton(onClick = { entryToDelete?.let { vm.delete(it) }; entryToDelete = null }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { entryToDelete = null }) { Text("取消") } },
        )
    }
    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("一键清除") },
            text = { Text("清除后将删除全部账单记录（开发用）。覆水难收，确认清空吗？") },
            confirmButton = {
                TextButton(onClick = { vm.clearAll(); showClear = false }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } },
        )
    }
}

private fun shiftMonth(month: String, delta: Int): String {
    val fmt = SimpleDateFormat("yyyy-MM", Locale.CHINA)
    val c = Calendar.getInstance()
    runCatching { fmt.parse(month) }.getOrNull()?.let { c.time = it }
    c.add(Calendar.MONTH, delta)
    return fmt.format(c.time)
}

@Composable
private fun SummaryCell(label: String, value: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text("¥${fmt(value)}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun EntryRow(e: LedgerEntryEntity, onDelete: () -> Unit) {
    val isIncome = e.type == "income"
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("${e.category}${if (e.note.isBlank()) "" else " · ${e.note}"}", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            val meta = listOf(e.date.substring(11, 16), e.paymentMethod.ifBlank { "" }).filter { it.isNotBlank() }.joinToString(" · ")
            Text(meta, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "${if (isIncome) "+" else "-"}¥${fmt(e.amount)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isIncome) IncomeGreen else ExpenseRed,
        )
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun AddEntryDialog(
    expenseCats: List<LedgerCategoryEntity>,
    incomeCats: List<LedgerCategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (LedgerEntryEntity) -> Unit,
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf("expense") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var noteEdited by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(DateUtils.today()) }
    var paymentMethod by remember { mutableStateOf("") }
    var customPayment by remember { mutableStateOf("") }

    val cats = if (type == "expense") expenseCats else incomeCats
    val selectedCat = category.takeIf { c -> cats.any { it.name == c } } ?: cats.firstOrNull()?.name ?: ""

    LaunchedEffect(type, selectedCat) {
        if (!noteEdited && note.isBlank()) {
            note = if (selectedCat == "餐饮") {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                "${DateUtils.today()} ${mealName(hour)}"
            } else {
                ""
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("记一笔", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("expense" to "支出", "income" to "收入").forEach { (k, label) ->
                    val sel = type == k
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .clickable { type = k }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) { Text(label, fontSize = 13.sp, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
                }
            }

            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("金额") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            // 分类选择
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                cats.forEach { c ->
                    val sel = selectedCat == c.name
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                category = c.name
                                if (!noteEdited) {
                                    note = if (c.name == "餐饮") {
                                        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                                        "${DateUtils.today()} ${mealName(hour)}"
                                    } else {
                                        ""
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) { Text(c.name, fontSize = 13.sp, color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) }
                }
            }

            // 支付方式
            Text("支付方式", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                paymentOptions.forEach { p ->
                    val sel = paymentMethod == p
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { paymentMethod = p }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) { Text(p, fontSize = 13.sp, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
                }
            }
            if (paymentMethod == "自定义") {
                OutlinedTextField(value = customPayment, onValueChange = { customPayment = it }, label = { Text("自定义支付方式") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }

            OutlinedTextField(value = note, onValueChange = { noteEdited = true; note = it }, label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            // 日期选择
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { DateTimePickers.pickDate(context, date) { date = it } }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("日期", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(date, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = {
                    val a = amount.toDoubleOrNull() ?: 0.0
                    if (a > 0 && selectedCat.isNotBlank() && date.isNotBlank()) {
                        onSave(
                            LedgerEntryEntity(
                                type = type,
                                amount = a,
                                category = selectedCat,
                                note = note.trim(),
                                date = "$date ${DateUtils.nowTime()}",
                                paymentMethod = if (paymentMethod == "自定义") customPayment.trim() else paymentMethod,
                            )
                        )
                    }
                }) { Text("保存", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun CategoryDialog(
    expenseCats: List<LedgerCategoryEntity>,
    incomeCats: List<LedgerCategoryEntity>,
    onDismiss: () -> Unit,
    onAdd: (LedgerCategoryEntity) -> Unit,
) {
    var type by remember { mutableStateOf("expense") }
    var name by remember { mutableStateOf("") }
    val cats = if (type == "expense") expenseCats else incomeCats

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("分类管理", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("expense" to "支出", "income" to "收入").forEach { (k, label) ->
                    val sel = type == k
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .clickable { type = k }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) { Text(label, fontSize = 13.sp, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
                }
            }

            Text(cats.joinToString("  ") { it.name }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("新分类名") }, modifier = Modifier.weight(1f), singleLine = true)
                TextButton(onClick = {
                    if (name.isNotBlank() && cats.none { it.name == name.trim() }) {
                        onAdd(LedgerCategoryEntity(name = name.trim(), type = type, isCustom = true))
                        name = ""
                    }
                }) { Text("添加") }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("完成", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

private fun fmt(d: Double): String = String.format(Locale.US, "%.2f", d)

private fun mealName(hour: Int): String = when {
    hour < 10 -> "早饭"
    hour < 15 -> "午饭"
    hour < 18 -> "晚饭"
    else -> "夜宵"
}
