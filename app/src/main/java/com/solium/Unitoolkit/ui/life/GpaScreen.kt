package com.solium.Unitoolkit.ui.life

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
import androidx.compose.material.icons.filled.Settings
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
import com.solium.Unitoolkit.core.database.GpaCourseEntity
import com.solium.Unitoolkit.core.model.GpaFormula
import com.solium.Unitoolkit.core.model.GpaMapping
import com.solium.Unitoolkit.core.model.gradePointOf
import com.solium.Unitoolkit.ui.components.EmptyHint
import com.solium.Unitoolkit.ui.components.SimpleTopBar
import java.util.Locale

@Composable
fun GpaScreen(navController: NavController, vm: GpaViewModel = viewModel(factory = GpaViewModel.Factory)) {
    val courses by vm.courses.collectAsState()
    val formula by vm.formula.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var showFormula by remember { mutableStateOf(false) }
    var semesterFilter by remember { mutableStateOf<String?>(null) }

    val semesters = courses.map { it.semester }.distinct()
    val filtered = semesterFilter?.let { s -> courses.filter { it.semester == s } } ?: courses
    val totalCredits = filtered.sumOf { it.credits }
    val weightedGp = filtered.sumOf { it.gradePoint * it.credits }
    val gpa = if (totalCredits == 0.0) 0.0 else weightedGp / totalCredits
    val weightedScore = filtered.sumOf { it.rawScore * it.credits }

    Scaffold(
        topBar = {
            SimpleTopBar(
                title = "绩点计算器",
                onBack = { navController.popBackStack() },
                trailing = { IconButton(onClick = { showFormula = true }) { Icon(Icons.Default.Settings, contentDescription = "公式设置") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, contentDescription = "添加课程") }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            // 汇总卡片
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("GPA ${String.format(Locale.US, "%.2f", gpa)}", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column { Text("已修学分", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer); Text(String.format(Locale.US, "%.1f", totalCredits), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                    Column { Text("加权平均分", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer); Text(if (totalCredits == 0.0) "-" else String.format(Locale.US, "%.1f", weightedScore / totalCredits), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                }
            }

            Spacer(Modifier.height(12.dp))
            // 学期筛选
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                val allSel = semesterFilter == null
                Box(Modifier.clip(RoundedCornerShape(10.dp)).background(if (allSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface).clickable { semesterFilter = null }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text("全部", fontSize = 13.sp, color = if (allSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                }
                semesters.forEach { s ->
                    val sel = semesterFilter == s
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface).clickable { semesterFilter = s }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(s, fontSize = 13.sp, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            if (filtered.isEmpty()) {
                EmptyHint("暂无课程记录，点击右下角添加")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { c ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(c.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                Text("${c.semester} · ${formatDouble(c.credits)} 学分 · ${formatDouble(c.rawScore)} 分", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(String.format(Locale.US, "%.2f", c.gradePoint), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { vm.delete(c) }) { Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) AddCourseDialog(
        defaultSemester = semesters.firstOrNull(),
        onDismiss = { showAdd = false },
        onSave = { semester, name, credits, score ->
            vm.upsert(GpaCourseEntity(semester = semester, name = name, credits = credits, rawScore = score, gradePoint = formula.gradePointOf(score)))
            showAdd = false
        },
    )
    if (showFormula) FormulaDialog(formula = formula, onDismiss = { showFormula = false }, onSave = { vm.setFormula(it); showFormula = false })
}

private fun formatDouble(d: Double): String = String.format(Locale.US, "%g", d)

@Composable
private fun AddCourseDialog(
    defaultSemester: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double) -> Unit,
) {
    var semester by remember { mutableStateOf(defaultSemester ?: "2026-秋") }
    var name by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("添加课程", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("课程名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = semester, onValueChange = { semester = it }, label = { Text("学期") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = credits, onValueChange = { credits = it }, label = { Text("学分") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = score, onValueChange = { score = it }, label = { Text("成绩") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = {
                    val c = credits.toDoubleOrNull() ?: 0.0
                    val s = score.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && c > 0) {
                        onSave(semester.trim(), name.trim(), c, s)
                    }
                }) { Text("保存", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun FormulaDialog(formula: GpaFormula, onDismiss: () -> Unit, onSave: (GpaFormula) -> Unit) {
    var mode by remember { mutableStateOf(formula.mode) }
    var mappings by remember { mutableStateOf(formula.mappings) }
    var newMin by remember { mutableStateOf("") }
    var newPoint by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("绩点公式", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            listOf("percent4" to "4.0 制（百分区间）", "percent5" to "5.0 制（线性）", "custom" to "自定义映射").forEach { (m, label) ->
                val sel = mode == m
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface).clickable { mode = m }.padding(14.dp)) {
                    Text(label, fontSize = 14.sp, color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                }
            }

            if (mode == "custom") {
                Text("自定义映射（分数下限 → 绩点）", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                mappings.sortedByDescending { it.min }.forEach { mp ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("分数 ≥ ${formatDouble(mp.min)}  →  绩点 ${formatDouble(mp.point)}", Modifier.weight(1f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = { mappings = mappings.filter { it != mp } }) { Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newMin, onValueChange = { newMin = it }, label = { Text("下限") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = newPoint, onValueChange = { newPoint = it }, label = { Text("绩点") }, modifier = Modifier.weight(1f), singleLine = true)
                    TextButton(onClick = {
                        val mn = newMin.toDoubleOrNull(); val p = newPoint.toDoubleOrNull()
                        if (mn != null && p != null) { mappings = mappings + GpaMapping(mn, p); newMin = ""; newPoint = "" }
                    }) { Text("添加") }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = { onSave(GpaFormula(mode = mode, mappings = mappings)) }) { Text("确定", fontWeight = FontWeight.Bold) }
            }
        }
    }
}