package com.solium.Unitoolkit.ui.toolbox

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.solium.Unitoolkit.ui.components.AppIcon
import com.solium.Unitoolkit.ui.components.AppIconBadge
import com.solium.Unitoolkit.ui.navigation.Routes

private data class Tool(val icon: String, val name: String, val route: String)

private data class ToolCategory(val icon: String, val name: String, val tools: List<Tool>)

private val categories = listOf(
    ToolCategory("book-open", "学习", listOf(
        Tool("calendar-days", "课表管理", Routes.COURSE),
        Tool("pen-line", "笔记速记", Routes.NOTE_LIST),
        Tool("square-check", "TodoList", Routes.TODO),
        Tool("timer", "番茄钟", Routes.POMODORO),
        Tool("graduation-cap", "绩点计算器", Routes.GPA),
    )),
    ToolCategory("house", "生活", listOf(
        Tool("zap", "水电费缴交", Routes.UTILITY),
        Tool("calendar-clock", "日程提醒", Routes.SCHEDULE),
        Tool("link", "便捷链接", Routes.LINKS),
        Tool("wallet", "简易账单", Routes.LEDGER),
        Tool("shopping-cart", "采购清单", Routes.SHOPPING),
        Tool("user", "个人名片", Routes.PROFILE_CARD),
    )),
    ToolCategory("gamepad-2", "娱乐", listOf(
        Tool("music", "音游上隐条", Routes.OVERLAY),
    )),
)

@Composable
fun ToolboxScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("工具箱", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        categories.forEach { cat ->
            val expanded = remember { mutableStateOf(true) }
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().clickable { expanded.value = !expanded.value }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (expanded.value) "▼" else "▶", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    AppIcon(cat.icon, size = 20.dp, tint = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.width(8.dp))
                    Text(cat.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
                AnimatedVisibility(visible = expanded.value) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth().height(96.dp * (cat.tools.size / 3 + if (cat.tools.size % 3 == 0) 0 else 1)),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(cat.tools) { tool ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate(tool.route) }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                AppIconBadge(tool.icon, size = 26)
                                Spacer(Modifier.height(6.dp))
                                Text(tool.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}
