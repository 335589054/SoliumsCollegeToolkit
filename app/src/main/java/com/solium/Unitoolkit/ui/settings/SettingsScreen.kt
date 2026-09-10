package com.solium.Unitoolkit.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.solium.Unitoolkit.core.theme.ThemeKey
import com.solium.Unitoolkit.core.theme.ThemePresets
import com.solium.Unitoolkit.core.theme.UiMode
import com.solium.Unitoolkit.ui.components.AppIconBadge
import com.solium.Unitoolkit.ui.components.SectionLabel
import com.solium.Unitoolkit.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(navController: NavController, vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory), bvm: BackupViewModel = viewModel(factory = BackupViewModel.Factory)) {
    val context = LocalContext.current
    val themeId by vm.themeId.collectAsState()
    val customThemes by vm.customThemes.collectAsState()
    val uiMode by vm.uiMode.collectAsState()
    val themeGroupOrder by vm.themeGroupOrder.collectAsState()
    var showAbout by remember { mutableStateOf(false) }

    // 备份数据相关状态
    var importing by remember { mutableStateOf(false) }
    var importOverwrite by remember { mutableStateOf(true) }
    var clearStep by remember { mutableStateOf(0) } // 0 无 / 1 首次确认 / 2 二次确认

    val message by bvm.message.collectAsState()
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            bvm.consumeMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) bvm.exportTo(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) bvm.importFrom(uri, importOverwrite)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("设置", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))

        SectionLabel("外观 · 配色主题")
        Text("滚动查看更多主题", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().height(140.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(ThemeKey.entries.toList()) { key ->
                val palette = ThemePresets.paletteFor(key)
                val selected = key.key == themeId
                Column(
                    Modifier.fillMaxWidth().clickable { vm.selectTheme(key.key) }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette.light.primary)
                            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape) else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(key.label, fontSize = 12.sp, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        SectionLabel("自定义主题")
        if (customThemes.isEmpty()) {
            Text("尚未创建自定义主题", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
        } else {
            val grouped = groupedOrderedItems(customThemes, themeGroupOrder)
            var expandedGroups by remember(grouped) { mutableStateOf(emptySet<String>()) }
            grouped.forEach { (g, groupItems) ->
                if (g == "" && groupItems.isEmpty()) return@forEach
                val label = g.ifBlank { "未分组" }
                val expanded = g in expandedGroups
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { expandedGroups = if (expanded) expandedGroups - g else expandedGroups + g }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (expanded) "▾" else "▸", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(6.dp))
                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f, fill = false))
                    Text("${groupItems.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (expanded) {
                    groupItems.forEach { theme ->
                        val selected = "custom:${theme.id}" == themeId
                        val fallback = MaterialTheme.colorScheme.primary
                        val primaryColor = remember(theme, fallback) {
                            val hex = theme.colors["primary"]?.lightHex
                            if (hex.isNullOrBlank()) fallback
                            else runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
                        }
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.selectTheme("custom:${theme.id}") }.padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = (theme.colors["primary"]?.lightAlpha ?: 1f).coerceIn(0f, 1f)))
                                    .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape) else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(theme.name, Modifier.weight(1f), fontSize = 14.sp, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            if (selected) Text("使用中", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        SettingsEntry("settings", "管理自定义主题", "分组管理 / 新建 / 移动 · 导入导出 .sthm") { navController.navigate(Routes.THEMES) }

        SectionLabel("外观 · 深色模式")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UiMode.entries.forEach { mode ->
                val label = when (mode) { UiMode.SYSTEM -> "跟随系统"; UiMode.LIGHT -> "浅色"; UiMode.DARK -> "深色" }
                val selected = mode == uiMode
                Box(
                    Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                        .clickable { vm.setUiMode(mode) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(label, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        SectionLabel("首页")
        SettingsEntry("zap", "首页快捷入口", "选择并排序首页展示的工具与链接入口") { navController.navigate(Routes.QUICK_ENTRIES) }
        SettingsEntry("layout-grid", "桌面快捷入口小组件", "长按桌面添加；独立选择并排序入口") { navController.navigate(Routes.QUICK_ENTRIES) }
        SettingsEntry("link", "便捷链接管理", "添加 / 编辑 / 排序自定义链接") { navController.navigate(Routes.LINKS) }

        Spacer(Modifier.height(8.dp))

        SectionLabel("个人名片")
        SettingsEntry("user", "个人名片信息", "头像 / 手机号 / QQ / 微信二维码 / B站") { navController.navigate(Routes.PROFILE_EDIT) }

        Spacer(Modifier.height(8.dp))

        SectionLabel("数据管理")
        Text(
            "v1.1.0 起应用包名变更为 com.solium.Unitoolkit，旧版本数据不会自动迁移。升级前请先「导出数据」得到 .stbc 备份，安装新版本后再「导入数据」恢复。",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        SettingsEntry("download", "导出数据（备份）", "全部数据与图片资源内嵌，导出为 .stbc") {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            exportLauncher.launch("unitoolkit_backup_$stamp.stbc")
        }
        SettingsEntry("upload", "导入数据", "增量合并或覆盖还原本地数据") { importing = true }
        SettingsEntry("trash-2", "清空数据", "删除本地全部数据（不可恢复）") { clearStep = 1 }

        Spacer(Modifier.height(8.dp))

        SectionLabel("系统")
        SettingsEntry("scroll-text", "更新日志", "查看版本更新记录") { navController.navigate(Routes.CHANGELOG) }
        SettingsEntry("info", "关于 Solium的工具箱", "v1.1.0 · 大学生一站式工具") { showAbout = true }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("关于 Solium的工具箱") },
            text = {
                Text(
                    "Solium的工具箱 v1.1.0\n\n" +
                        "面向大学生的一站式便捷工具 APP，覆盖学习、生活、娱乐三大场景。\n\n" +
                        "本地优先 · 轻量无感 · 可扩展 · 沉浸专注\n\n" +
                        "开发者：Solium\n" +
                        "邮箱：335589054@qq.com\n" +
                        "B站：UID 1214293355",
                )
            },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("好的") } },
        )
    }

    if (importing) {
        AlertDialog(
            onDismissRequest = { importing = false },
            title = { Text("导入数据") },
            text = { Text("选择导入模式：\n\n· 增量：保留现有数据，按 ID 合并导入\n· 覆盖：先清空全部数据，再导入备份") },
            confirmButton = {
                TextButton(onClick = {
                    importOverwrite = false
                    importing = false
                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
                }) { Text("增量导入") }
            },
            dismissButton = {
                TextButton(onClick = {
                    importOverwrite = true
                    importing = false
                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
                }) { Text("覆盖导入", color = MaterialTheme.colorScheme.error) }
            },
        )
    }

    if (clearStep == 1) {
        AlertDialog(
            onDismissRequest = { clearStep = 0 },
            title = { Text("清空全部数据") },
            text = { Text("将删除本地全部数据（课表、笔记、待办、账单、采购清单等），此操作不可恢复。确定继续？") },
            confirmButton = { TextButton(onClick = { clearStep = 2 }) { Text("继续", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { clearStep = 0 }) { Text("取消") } },
        )
    } else if (clearStep == 2) {
        AlertDialog(
            onDismissRequest = { clearStep = 0 },
            title = { Text("再次确认") },
            text = { Text("真的要清空全部数据吗？建议先「导出数据」备份。") },
            confirmButton = {
                TextButton(onClick = { clearStep = 0; bvm.clearAll() }) { Text("确认清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { clearStep = 0 }) { Text("取消") } },
        )
    }
}

@Composable
private fun SettingsEntry(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconBadge(icon)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
