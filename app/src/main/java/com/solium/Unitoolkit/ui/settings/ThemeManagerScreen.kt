package com.solium.Unitoolkit.ui.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.solium.Unitoolkit.core.model.CustomTheme
import com.solium.Unitoolkit.core.model.ThemeRoleColors
import com.solium.Unitoolkit.core.theme.ThemeRoleDefs
import com.solium.Unitoolkit.core.theme.buildColorScheme
import com.solium.Unitoolkit.core.theme.resolveTheme
import com.solium.Unitoolkit.core.theme.toCustomTheme
import com.solium.Unitoolkit.ui.components.EmptyHint
import com.solium.Unitoolkit.ui.components.PrimaryButton
import com.solium.Unitoolkit.ui.components.SimpleTopBar
import kotlin.math.roundToInt

/** 计算分组的展示顺序：优先 theme_group_order，其余按主题首次出现顺序，未分组固定最后。 */
internal fun groupedOrderedItems(themes: List<CustomTheme>, groupOrder: List<String>): Map<String, List<CustomTheme>> {
    val grouped = themes.groupBy { it.group.ifBlank { "" } }
    val orderedKeys = LinkedHashSet<String>()
    groupOrder.forEach { if (it.isNotBlank()) orderedKeys.add(it) }
    grouped.keys.forEach { if (it.isNotBlank()) orderedKeys.add(it) }
    val result = LinkedHashMap<String, List<CustomTheme>>()
    orderedKeys.forEach { g -> result[g] = grouped[g] ?: emptyList() }
    result[""] = grouped[""] ?: emptyList() // 未分组固定最后
    return result
}

@Composable
fun ThemeManagerScreen(navController: NavController, vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val customThemes by vm.customThemes.collectAsState()
    val themeId by vm.themeId.collectAsState()
    val themeGroupOrder by vm.themeGroupOrder.collectAsState()

    var editing by remember { mutableStateOf<CustomTheme?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importCustomThemesFromUri(uri)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) vm.exportCustomThemesToUri(uri)
    }
    var exportingGroup by remember { mutableStateOf<String?>(null) }
    val exportGroupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) exportingGroup?.let { vm.exportGroupToUri(uri, it) }
        exportingGroup = null
    }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var deleteGroupTarget by remember { mutableStateOf<String?>(null) }
    var creatingGroup by remember { mutableStateOf(false) }
    var moveThemeTarget by remember { mutableStateOf<CustomTheme?>(null) }

    Scaffold(
        topBar = { SimpleTopBar(title = "自定义主题", onBack = { navController.popBackStack() }) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "自定义主题保存为数据化颜色角色，可单独编辑浅色 / 深色的背景、卡片、文字与边框，并可为主页 / 番茄钟 / 课表设置背景图。支持以 .sthm 主题包导入导出。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (customThemes.isEmpty()) {
                EmptyHint("还没有自定义主题")
            } else {
                ThemeGroupedList(
                    themes = customThemes,
                    groupOrder = themeGroupOrder,
                    selectedId = themeId,
                    onSelect = { vm.selectTheme("custom:${it.id}") },
                    onEdit = { editing = it; showEditor = true },
                    onDelete = { vm.deleteCustomTheme(it.id) },
                    onRenameGroup = { renameTarget = it },
                    onExportGroup = { exportingGroup = it; exportGroupLauncher.launch("$it.sthm") },
                    onDeleteGroup = { deleteGroupTarget = it },
                    onMoveGroup = { g, d -> vm.moveGroup(g, d) },
                    onMoveToGroup = { moveThemeTarget = it },
                )
            }

            PrimaryButton(
                label = "新建分组",
                onClick = { creatingGroup = true },
                outlined = true,
            )
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

    renameTarget?.let { from ->
        var newName by remember(from) { mutableStateOf(from) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名分组") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("分组名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { vm.renameGroup(from, newName); renameTarget = null },
                    enabled = newName.isNotBlank(),
                ) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("取消") } },
        )
    }

    deleteGroupTarget?.let { g ->
        AlertDialog(
            onDismissRequest = { deleteGroupTarget = null },
            title = { Text("删除分组") },
            text = { Text("确定删除分组「$g」内的全部主题吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = { vm.deleteGroup(g); deleteGroupTarget = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteGroupTarget = null }) { Text("取消") } },
        )
    }

    if (creatingGroup) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { creatingGroup = false },
            title = { Text("新建分组") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分组名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { vm.createGroup(name); creatingGroup = false },
                    enabled = name.isNotBlank(),
                ) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { creatingGroup = false }) { Text("取消") } },
        )
    }

    moveThemeTarget?.let { theme ->
        MoveThemeDialog(
            groupOrder = groupedOrderedItems(customThemes, themeGroupOrder).keys.toList(),
            currentGroup = theme.group,
            onDismiss = { moveThemeTarget = null },
            onConfirm = { target ->
                vm.moveThemeToGroup(theme.id, target)
                moveThemeTarget = null
            },
        )
    }
}

/** 移动主题到其它分组的对话框：可在已有分组中选择，或输入一个新分组名。 */
@Composable
private fun MoveThemeDialog(
    groupOrder: List<String>,
    currentGroup: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var selected by remember { mutableStateOf(currentGroup.ifBlank { "" }) }
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("移动主题到分组") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                groupOrder.forEach { g ->
                    val label = g.ifBlank { "未分组" }
                    val chosen = selected == g && newName.isBlank()
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selected = g; newName = "" }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("或输入新分组名") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(if (newName.isNotBlank()) newName else selected) },
                enabled = newName.isNotBlank() || groupOrder.contains(selected),
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 按 group（主题包）分组显示为可折叠列表，未设置 group 归入“未分组”。 */
@Composable
private fun ThemeGroupedList(
    themes: List<CustomTheme>,
    groupOrder: List<String>,
    selectedId: String,
    onSelect: (CustomTheme) -> Unit,
    onEdit: (CustomTheme) -> Unit,
    onDelete: (CustomTheme) -> Unit,
    onRenameGroup: (String) -> Unit,
    onExportGroup: (String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onMoveGroup: (String, Int) -> Unit,
    onMoveToGroup: (CustomTheme) -> Unit,
) {
    val grouped = groupedOrderedItems(themes, groupOrder)
    val nonEmptyNames = grouped.keys.filter { it.isNotBlank() }
    var expandedGroups by remember(grouped) { mutableStateOf(grouped.keys.toSet()) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        grouped.forEach { (g, items) ->
            if (g == "" && items.isEmpty()) return@forEach
            val label = g.ifBlank { "未分组" }
            val expanded = g in expandedGroups
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        expandedGroups = if (expanded) expandedGroups - g else expandedGroups + g
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (expanded) "▾" else "▸", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(6.dp))
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f, fill = false))
                Text("${items.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                if (g.isNotEmpty()) {
                    val idx = nonEmptyNames.indexOf(g)
                    IconButton(onClick = { onMoveGroup(g, -1) }, enabled = idx > 0, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "分组上移", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { onMoveGroup(g, 1) }, enabled = idx < nonEmptyNames.lastIndex, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "分组下移", modifier = Modifier.size(18.dp))
                    }
                    TextButton(onClick = { onRenameGroup(g) }, contentPadding = PaddingValues(horizontal = 2.dp)) { Text("改名", fontSize = 12.sp) }
                    TextButton(onClick = { onExportGroup(g) }, contentPadding = PaddingValues(horizontal = 2.dp)) { Text("导出", fontSize = 12.sp) }
                    TextButton(onClick = { onDeleteGroup(g) }, contentPadding = PaddingValues(horizontal = 2.dp)) { Text("删除", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
                }
            }
            if (expanded) {
                items.forEach { theme ->
                    val selected = "custom:${theme.id}" == selectedId
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .clickable { onSelect(theme) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(theme.name, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        TextButton(onClick = { onMoveToGroup(theme) }, contentPadding = PaddingValues(horizontal = 2.dp)) { Text("移动", fontSize = 13.sp) }
                        TextButton(onClick = { onEdit(theme) }) { Text("编辑") }
                        TextButton(onClick = { onDelete(theme) }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
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
    var showPreview by remember { mutableStateOf(false) }
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
            OutlinedTextField(
                value = draft.group,
                onValueChange = { draft = draft.copy(group = it) },
                label = { Text("主题包名（用于列表分组）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("背景图片（内嵌 base64，可跨设备迁移）", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 6.dp))
            BgImageEditorRow(
                label = "主页背景",
                value = draft.homeBgImage,
                onChange = { draft = draft.copy(homeBgImage = it) },
                onClear = { draft = draft.copy(homeBgImage = "") },
            )
            BgImageEditorRow(
                label = "番茄钟背景",
                value = draft.pomodoroBgImage,
                onChange = { draft = draft.copy(pomodoroBgImage = it) },
                onClear = { draft = draft.copy(pomodoroBgImage = "") },
            )
            BgImageEditorRow(
                label = "番茄钟沉浸背景",
                value = draft.pomodoroImmersiveBgImage,
                onChange = { draft = draft.copy(pomodoroImmersiveBgImage = it) },
                onClear = { draft = draft.copy(pomodoroImmersiveBgImage = "") },
            )
            BgImageEditorRow(
                label = "课表背景",
                value = draft.courseBgImage,
                onChange = { draft = draft.copy(courseBgImage = it) },
                onClear = { draft = draft.copy(courseBgImage = "") },
            )
            OpacitySliderRow("课表背景透明度", draft.courseBgOpacity) { draft = draft.copy(courseBgOpacity = it) }
            OpacitySliderRow("主页卡片透明度", draft.homeCardOpacity) { draft = draft.copy(homeCardOpacity = it) }

            Text("颜色角色", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 6.dp))
            ThemeRoleDefs.forEach { role ->
                val colors = draft.colors[role.key]
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(role.label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
                    ColorSwatchButton("浅色", colors?.lightHex ?: "", colors?.lightAlpha ?: 1f, Modifier.weight(1f)) {
                        picking = PickTarget(role.key, light = true)
                    }
                    ColorSwatchButton("深色", colors?.darkHex ?: "", colors?.darkAlpha ?: 1f, Modifier.weight(1f)) {
                        picking = PickTarget(role.key, light = false)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { showPreview = true }) { Text("预览", fontWeight = FontWeight.Medium) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = { onSave(draft.copy(name = draft.name.trim())) }, enabled = draft.name.isNotBlank()) {
                    Text("保存", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    picking?.let { target ->
        val targetColors = draft.colors[target.role] ?: ThemeRoleColors()
        ColorPickerDialog(
            title = if (target.light) "浅色 · ${ThemeRoleDefs.first { it.key == target.role }.label}" else "深色 · ${ThemeRoleDefs.first { it.key == target.role }.label}",
            initialHex = if (target.light) targetColors.lightHex else targetColors.darkHex,
            initialAlpha = if (target.light) targetColors.lightAlpha else targetColors.darkAlpha,
            onDismiss = { picking = null },
            onPick = { hex, alpha ->
                val old = draft.colors[target.role] ?: ThemeRoleColors()
                draft = draft.copy(
                    colors = draft.colors + (
                        target.role to if (target.light) old.copy(lightHex = hex, lightAlpha = alpha) else old.copy(darkHex = hex, darkAlpha = alpha)
                        ),
                )
                picking = null
            },
        )
    }
    if (showPreview) {
        ThemePreviewDialog(theme = draft) { showPreview = false }
    }
}

private data class PickTarget(val role: String, val light: Boolean)

@Composable
private fun ColorSwatchButton(label: String, hex: String, alpha: Float, modifier: Modifier, onClick: () -> Unit) {
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
                .background(runCatching { Color(android.graphics.Color.parseColor(hex)).copy(alpha = alpha.coerceIn(0f, 1f)) }.getOrDefault(Color.Gray)),
        )
        Text("$label ${hex.ifBlank { "未设置" }}  A${(alpha.coerceIn(0f, 1f) * 100).roundToInt()}%", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun ColorPickerDialog(
    title: String,
    initialHex: String,
    initialAlpha: Float = 1f,
    onDismiss: () -> Unit,
    onPick: (String, Float) -> Unit,
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
    var alphaPct by remember(initialAlpha) { mutableStateOf((initialAlpha.coerceIn(0f, 1f) * 100f).roundToInt().coerceIn(0, 100).toFloat()) }
    val currentColorInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation / 100f, value / 100f))
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Box(
                Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color(currentColorInt).copy(alpha = alphaPct / 100f)),
            )
            HslSliderRow("色调", hue, 0f..360f) { hue = it }
            HslSliderRow("饱和度", saturation, 0f..100f) { saturation = it }
            HslSliderRow("亮度", value, 0f..100f) { value = it }
            HslSliderRow("透明度", alphaPct, 0f..100f) { alphaPct = it }
            Text("#%06X".format(currentColorInt and 0xFFFFFF), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = { onPick("#%06X".format(currentColorInt and 0xFFFFFF), alphaPct / 100f) }) {
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

@Composable
private fun OpacitySliderRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text("${(value.coerceIn(0f, 1f) * 100).roundToInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value.coerceIn(0f, 1f), onValueChange = onChange, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
    }
}

/** 单行背景图编辑：选择图片（转 base64）、预览缩略图、清除。 */
@Composable
private fun BgImageEditorRow(label: String, value: String, onChange: (String) -> Unit, onClear: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            readUriAsBase64(context, uri)?.let(onChange)
        }
    }
    val bitmap = remember(value) { value.takeIf { it.isNotBlank() }?.let { base64ToBitmap(it) } }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            TextButton(onClick = { launcher.launch(arrayOf("image/*")) }) { Text("选择图片") }
            if (value.isNotBlank()) {
                TextButton(onClick = onClear) { Text("清除", color = MaterialTheme.colorScheme.error) }
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(8.dp)),
            )
        } else if (value.isBlank()) {
            Text("未设置背景图", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 预览：用当前草稿主题渲染一个示例界面并标注各颜色角色改的是哪里。 */
@Composable
private fun ThemePreviewDialog(theme: CustomTheme, onDismiss: () -> Unit) {
    var dark by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        // 预览主题提升为最外层：切换深 / 浅色时，整个预览页背景（surface）随预览主题一起变化，
        // 而不沿用 app 当前全局主题的背景。
        MaterialTheme(colorScheme = theme.buildColorScheme(dark)) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("主题预览", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        TextButton(onClick = { dark = !dark }) { Text(if (dark) "浅色" else "深色") }
                    }
                    PreviewItem("主色·按钮（primary）", MaterialTheme.colorScheme.primary) {
                        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                            Text("示例按钮", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    PreviewItem("页面背景（background / surface）", MaterialTheme.colorScheme.surface) {
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(10.dp)) {
                            Text("卡片内容", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        }
                    }
                    PreviewItem("页面主文字（onBackground）", MaterialTheme.colorScheme.onBackground) {
                        Text("示例正文文字", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp)
                    }
                    PreviewItem("输入框背景（surfaceVariant）", MaterialTheme.colorScheme.surfaceVariant) {
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(10.dp),
                        ) {
                            Text("输入框占位文字", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                    PreviewItem("边框/分隔线（outline）", MaterialTheme.colorScheme.outline) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("关闭") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewItem(roleLabel: String, color: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
            Spacer(Modifier.width(6.dp))
            Text(roleLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

private fun readUriAsBase64(context: Context, uri: Uri): String? = runCatching {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
}.getOrNull()

private fun base64ToBitmap(b64: String): Bitmap? = runCatching {
    val bytes = android.util.Base64.decode(b64, android.util.Base64.NO_WRAP)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}.getOrNull()