package com.unitoolkit.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import com.unitoolkit.core.model.CustomTheme
import com.unitoolkit.core.theme.ThemeRoleDefs
import com.unitoolkit.core.theme.resolveTheme
import com.unitoolkit.core.theme.toCustomTheme
import com.unitoolkit.ui.components.EmptyHint
import com.unitoolkit.ui.components.PrimaryButton
import com.unitoolkit.ui.components.SimpleTopBar

@Composable
fun ThemeManagerScreen(navController: NavController, vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val customThemes by vm.customThemes.collectAsState()
    val themeId by vm.themeId.collectAsState()

    var editing by remember { mutableStateOf<CustomTheme?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importCustomThemesFromUri(uri)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportCustomThemesToUri(uri)
    }

    Scaffold(
        topBar = { SimpleTopBar(title = "自定义主题", onBack = { navController.popBackStack() }) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "自定义主题保存为数据化颜色角色，可单独编辑浅色 / 深色的背景、卡片、文字与边框等。支持以 .sthm 主题包导入导出。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (customThemes.isEmpty()) {
                EmptyHint("还没有自定义主题")
            } else {
                customThemes.forEach { theme ->
                    val selected = "custom:${theme.id}" == themeId
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .clickable { vm.selectTheme("custom:${theme.id}") }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(theme.name, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        TextButton(onClick = { editing = theme; showEditor = true }) { Text("编辑") }
                        TextButton(onClick = { vm.deleteCustomTheme(theme.id) }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }

            PrimaryButton(
                label = "新建自定义主题",
                onClick = {
                    val base = resolveTheme(themeId, customThemes)
                    editing = base.toCustomTheme(id = "", name = "自定义主题")
                    showEditor = true
                },
            )
            PrimaryButton(
                label = "导入 .sthm 主题包",
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                outlined = true,
            )
            PrimaryButton(
                label = "导出全部自定义主题（.sthm）",
                onClick = { exportLauncher.launch("自定义主题包.sthm") },
                outlined = true,
            )
        }
    }

    if (showEditor && editing != null) {
        ThemeEditorDialog(
            initial = editing!!,
            onDismiss = { showEditor = false },
            onSave = { vm.saveCustomTheme(it); showEditor = false },
        )
    }
}

@Composable
private fun ThemeEditorDialog(
    initial: CustomTheme,
    onDismiss: () -> Unit,
    onSave: (CustomTheme) -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    var picking by remember { mutableStateOf<PickTarget?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(if (initial.id.isBlank()) "新建自定义主题" else "编辑自定义主题", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it) },
                label = { Text("主题名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ThemeRoleDefs.forEach { role ->
                val colors = draft.colors[role.key]
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(role.label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
                    ColorSwatchButton("浅色", colors?.lightHex ?: "", Modifier.weight(1f)) {
                        picking = PickTarget(role.key, light = true)
                    }
                    ColorSwatchButton("深色", colors?.darkHex ?: "", Modifier.weight(1f)) {
                        picking = PickTarget(role.key, light = false)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = { onSave(draft.copy(name = draft.name.trim())) }, enabled = draft.name.isNotBlank()) {
                    Text("保存", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    picking?.let { target ->
        val targetColors = draft.colors[target.role] ?: com.unitoolkit.core.model.ThemeRoleColors()
        ColorPickerDialog(
            title = if (target.light) "浅色 · ${ThemeRoleDefs.first { it.key == target.role }.label}" else "深色 · ${ThemeRoleDefs.first { it.key == target.role }.label}",
            initialHex = if (target.light) targetColors.lightHex else targetColors.darkHex,
            onDismiss = { picking = null },
            onPick = { hex ->
                val old = draft.colors[target.role] ?: com.unitoolkit.core.model.ThemeRoleColors()
                draft = draft.copy(
                    colors = draft.colors + (
                        target.role to if (target.light) old.copy(lightHex = hex) else old.copy(darkHex = hex)
                        ),
                )
                picking = null
            },
        )
    }
}

private data class PickTarget(val role: String, val light: Boolean)

@Composable
private fun ColorSwatchButton(label: String, hex: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(6.dp))
                .background(runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)),
        )
        Text("$label ${hex.ifBlank { "未设置" }}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun ColorPickerDialog(
    title: String,
    initialHex: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val hsv = remember(initialHex) {
        FloatArray(3).also {
            runCatching {
                android.graphics.Color.colorToHSV(
                    android.graphics.Color.parseColor(initialHex.ifBlank { "#000000" }),
                    it,
                )
            }
        }
    }
    var hue by remember(initialHex) { mutableStateOf(hsv[0]) }
    var saturation by remember(initialHex) { mutableStateOf(hsv[1] * 100f) }
    var value by remember(initialHex) { mutableStateOf(hsv[2] * 100f) }
    val currentColorInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation / 100f, value / 100f))
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Box(
                Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color(currentColorInt)),
            )
            HslSliderRow("色调", hue, 0f..360f) { hue = it }
            HslSliderRow("饱和度", saturation, 0f..100f) { saturation = it }
            HslSliderRow("亮度", value, 0f..100f) { value = it }
            Text("#%06X".format(currentColorInt and 0xFFFFFF), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = { onPick("#%06X".format(currentColorInt and 0xFFFFFF)) }) {
                    Text("确定", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HslSliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(48.dp))
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
        Text("${value.toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(34.dp))
    }
}
