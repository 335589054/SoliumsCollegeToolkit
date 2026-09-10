package com.unitoolkit.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.unitoolkit.core.database.LinkEntity
import com.unitoolkit.ui.components.AppIconBadge
import com.unitoolkit.ui.components.EmptyHint
import com.unitoolkit.ui.components.SectionLabel
import com.unitoolkit.ui.components.SimpleTopBar
import com.unitoolkit.ui.home.quickTools

@Composable
fun QuickEntriesScreen(navController: NavController, vm: QuickEntriesViewModel = viewModel(factory = QuickEntriesViewModel.Factory)) {
    val enabledTools by vm.enabledTools.collectAsState()
    val links by vm.links.collectAsState()

    Scaffold(
        topBar = { SimpleTopBar(title = "快捷入口管理", onBack = { navController.popBackStack() }) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text("勾选的内容会同时显示在首页快捷入口与桌面「快捷入口」小组件中（桌面小组件最多展示前 4 项）。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp)) }

            item { SectionLabel("工具入口") }
            items(quickTools, key = { it.key }) { tool ->
                val enabled = tool.key in enabledTools
                ToggleRow(icon = tool.icon, title = tool.label, checked = enabled, onToggle = { vm.toggleTool(tool.key) })
            }

            item { SectionLabel("链接入口") }
            if (links.isEmpty()) {
                item { EmptyHint("暂无便捷链接，可到「便捷链接管理」添加") }
            } else {
                items(links, key = { it.id }) { link ->
                    val enabled = link.isHomeCard
                    ToggleRow(icon = link.icon.ifBlank { "link" }, title = link.name, checked = enabled, onToggle = { vm.toggleLinkHome(link) })
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(icon: String, title: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconBadge(icon)
        Text(title, Modifier.weight(1f).padding(start = 12.dp), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}
