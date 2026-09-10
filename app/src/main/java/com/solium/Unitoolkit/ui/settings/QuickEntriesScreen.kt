package com.solium.Unitoolkit.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.solium.Unitoolkit.core.database.LinkEntity
import com.solium.Unitoolkit.ui.components.AppIconBadge
import com.solium.Unitoolkit.ui.components.EmptyHint
import com.solium.Unitoolkit.ui.components.SectionLabel
import com.solium.Unitoolkit.ui.components.SimpleTopBar
import com.solium.Unitoolkit.ui.home.QuickTool
import com.solium.Unitoolkit.ui.home.quickTools

@Composable
fun QuickEntriesScreen(navController: NavController, vm: QuickEntriesViewModel = viewModel(factory = QuickEntriesViewModel.Factory)) {
    val homeTools by vm.homeTools.collectAsState()
    val widgetTools by vm.widgetTools.collectAsState()
    val links by vm.links.collectAsState()

    Scaffold(
        topBar = { SimpleTopBar(title = "快捷入口管理", onBack = { navController.popBackStack() }) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text("首页快捷入口与桌面「快捷入口」小组件已各自独立设置：分别勾选要展示的工具并可按 ↑ ↓ 调整显示顺序。链接入口两边共用展示。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp)) }

            item { SectionLabel("首页 · 工具入口") }
            item {
                ToolSection(
                    tools = quickTools,
                    enabledKeys = homeTools,
                    onToggle = { vm.toggleTool(it) },
                    onMove = { k, d -> vm.moveTool(k, d) },
                )
            }

            item { SectionLabel("桌面小组件 · 工具入口") }
            item { Text("桌面小组件最多展示最前面的 4 项。", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item {
                ToolSection(
                    tools = quickTools,
                    enabledKeys = widgetTools,
                    onToggle = { vm.toggleWidgetTool(it) },
                    onMove = { k, d -> vm.moveWidgetTool(k, d) },
                )
            }

            item { SectionLabel("链接入口") }
            if (links.isEmpty()) {
                item { EmptyHint("暂无便捷链接，可到「便捷链接管理」添加") }
            } else {
                items(links, key = { it.id }) { link ->
                    val enabled = link.isHomeCard
                    LinkToggleRow(icon = link.icon.ifBlank { "link" }, title = link.name, checked = enabled, onToggle = { vm.toggleLinkHome(link) })
                }
            }
        }
    }
}

/** 一组工具入口：启用的按当前顺序展示并可排序，关闭的排在末尾仅用于重新勾选。 */
@Composable
private fun ToolSection(
    tools: List<QuickTool>,
    enabledKeys: List<String>,
    onToggle: (String) -> Unit,
    onMove: (String, Int) -> Unit,
) {
    val enabledSet = enabledKeys.toSet()
    val enabledItems = enabledKeys.mapNotNull { k -> tools.firstOrNull { it.key == k } }
    val disabledItems = tools.filter { it.key !in enabledSet }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        enabledItems.forEachIndexed { i, tool ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIconBadge(tool.icon)
                Text(tool.label, Modifier.weight(1f).padding(start = 12.dp), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                IconButton(enabled = i > 0, onClick = { onMove(tool.key, -1) }) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移")
                }
                IconButton(enabled = i < enabledItems.lastIndex, onClick = { onMove(tool.key, 1) }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移")
                }
                Spacer(Modifier.width(4.dp))
                Switch(checked = true, onCheckedChange = { onToggle(tool.key) })
            }
        }
        disabledItems.forEach { tool ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIconBadge(tool.icon)
                Text(tool.label, Modifier.weight(1f).padding(start = 12.dp), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Switch(checked = false, onCheckedChange = { onToggle(tool.key) })
            }
        }
    }
}

@Composable
private fun LinkToggleRow(icon: String, title: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconBadge(icon)
        Text(title, Modifier.weight(1f).padding(start = 12.dp), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}