package com.solium.Unitoolkit.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.solium.Unitoolkit.core.database.CourseEntity
import com.solium.Unitoolkit.core.database.LinkEntity
import com.solium.Unitoolkit.core.database.TodoEntity
import com.solium.Unitoolkit.core.database.matchesWeek
import com.solium.Unitoolkit.core.theme.LocalCustomTheme
import com.solium.Unitoolkit.core.theme.themeBackground
import com.solium.Unitoolkit.core.utils.DateUtils
import com.solium.Unitoolkit.core.utils.SystemEvent
import com.solium.Unitoolkit.core.utils.parseHexColor
import com.solium.Unitoolkit.ui.components.Card
import com.solium.Unitoolkit.ui.components.AppIconBadge
import com.solium.Unitoolkit.ui.components.EmptyHint
import com.solium.Unitoolkit.ui.components.PrimaryButton
import com.solium.Unitoolkit.ui.navigation.Routes
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val homeTimeFmt = SimpleDateFormat("HH:mm", Locale.CHINA)

private data class CardSpec(val key: String, val icon: String, val title: String)

private val allCards = listOf(
    CardSpec("course", "book-open", "课表"),
    CardSpec("ledger", "wallet", "账单"),
    CardSpec("todo", "square-check", "TodoList"),
    CardSpec("links", "zap", "快捷入口"),
    CardSpec("schedule", "calendar-clock", "日程"),
)

@Composable
fun HomeScreen(navController: NavController, vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val courses by vm.courses.collectAsState()
    val todos by vm.todos.collectAsState()
    val ledger by vm.ledgerEntries.collectAsState()
    val links by vm.links.collectAsState()
    val systemSchedules by vm.systemSchedules.collectAsState()
    val cards by vm.homeCards.collectAsState()
    val enabledTools by vm.quickTools.collectAsState()
    val currentWeek by vm.currentWeek.collectAsState()

    var managing by remember { mutableStateOf(false) }
    var nowMinutes by remember { mutableStateOf(DateUtils.timeToMinutes(DateUtils.nowTime())) }
    var clockTime by remember { mutableStateOf(DateUtils.nowTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMinutes = DateUtils.timeToMinutes(DateUtils.nowTime())
            clockTime = DateUtils.nowTime()
            delay(1_000)
        }
    }

    val balance = ledger.filter { it.type == "income" }.sumOf { it.amount } -
        ledger.filter { it.type == "expense" }.sumOf { it.amount }
    val today = DateUtils.today()
    val todayExpense = ledger.filter { it.type == "expense" && it.date.startsWith(today) }.sumOf { it.amount }

    // 当天接下来的课程
    val allTodayCourses = courses.filter { it.matchesWeek(currentWeek, DateUtils.today()) }.sortedBy { it.startTime }
    val upcomingCourses = allTodayCourses.filter { DateUtils.timeToMinutes(it.startTime) >= nowMinutes }.take(3)
    val nextCourse = allTodayCourses.filter { DateUtils.timeToMinutes(it.startTime) >= nowMinutes }.minByOrNull { it.startTime }
    val minutesUntilNext = nextCourse?.let { DateUtils.timeToMinutes(it.startTime) - nowMinutes }

    val undoneTodos = todos.filter { !it.isDone }

    LaunchedEffect(Unit) { vm.refreshSystemSchedules() }

    val customTheme = LocalCustomTheme.current
    val cardOpacity = customTheme?.homeCardOpacity ?: 1f

    Column(
        Modifier
            .fillMaxSize()
            .themeBackground(customTheme?.homeBgImage, 1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 头部：居中时钟
        ClockHeader(clockTime)

        cards.forEach { key ->
            val spec = allCards.firstOrNull { it.key == key } ?: return@forEach
            HomeCardContainer(
                spec = spec,
                managing = managing,
                isFirst = cards.first() == key,
                isLast = cards.last() == key,
                cardOpacity = cardOpacity,
                onMoveUp = { vm.moveCard(key, -1) },
                onMoveDown = { vm.moveCard(key, 1) },
                onRemove = { vm.removeCard(key) },
            ) {
                when (key) {
                    "course" -> CourseCard(upcomingCourses, nextCourse, minutesUntilNext, onClick = { navController.navigate(Routes.COURSE) })
                    "ledger" -> LedgerCard(balance, todayExpense, onAdd = { navController.navigate(Routes.LEDGER) })
                    "todo" -> TodoHomeCard(undoneTodos, onToggle = { t -> vm.setTodoDone(t, !t.isDone) }, onOpen = { navController.navigate(Routes.TODO) })
                    "links" -> QuickEntriesCard(visibleTools = enabledTools.mapNotNull { k -> quickTools.firstOrNull { it.key == k } }, links = links, navController)
                    "schedule" -> ScheduleHomeCard(systemSchedules, today, onClick = { navController.navigate(Routes.SCHEDULE) })
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // 卡片管理（底部）
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { managing = !managing }) {
                Text(if (managing) "完成" else "管理卡片", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (managing) {
            ManageCardsRow(cards = cards, onAdd = { vm.addCard(it) })
        }
    }
}

@Composable
private fun ClockHeader(clockTime: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            clockTime,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            "${DateUtils.today()} ${DateUtils.weekdayZh(DateUtils.todayWeekday())}",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "农历 ${DateUtils.lunarToday()} · ${DateUtils.solarTermToday()}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeCardContainer(
    spec: CardSpec,
    managing: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    cardOpacity: Float = 1f,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column {
        Card(modifier = Modifier.graphicsLayer { alpha = cardOpacity.coerceIn(0f, 1f) }, tinted = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIconBadge(spec.icon, background = MaterialTheme.colorScheme.surface)
                Spacer(Modifier.width(10.dp))
                Text(spec.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (managing) {
                    Spacer(Modifier.weight(1f))
                    IconButton(enabled = !isFirst, onClick = onMoveUp) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移") }
                    IconButton(enabled = !isLast, onClick = onMoveDown) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移") }
                    IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = "移除") }
                }
            }
            content()
        }
    }
}

@Composable
private fun ManageCardsRow(cards: List<String>, onAdd: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("添加卡片", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            allCards.forEach { spec ->
                val added = spec.key in cards
                Box(
                    Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (added) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer)
                        .clickable(enabled = !added) { onAdd(spec.key) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        if (added) "${spec.title} ✓" else "+ ${spec.title}",
                        fontSize = 13.sp,
                        color = if (added) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        Text("点击卡片右侧 ↑ ↓ 可排序，× 可移除；重复卡片不允许添加。", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------- 课表卡片 ----------
@Composable
private fun CourseCard(upcoming: List<CourseEntity>, next: CourseEntity?, minutesUntil: Int?, onClick: () -> Unit) {
    if (upcoming.isEmpty()) {
        EmptyHint("今天已没有剩余课程")
    } else {
        upcoming.forEach { c ->
            CourseRow(c)
        }
        if (minutesUntil != null && minutesUntil in 0..9 && next != null) {
            Text(
                "距下节课「${next.name}」还有 $minutesUntil 分钟",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
    TextButton(onClick = onClick) { Text("查看完整课表") }
}

@Composable
private fun CourseRow(c: CourseEntity) {
    val color = remember(c.colorTag) { parseHexColor(c.colorTag) }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(c.name, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("${c.startTime} - ${c.endTime}${if (c.location.isNotBlank()) " · ${c.location}" else ""}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------- 账单卡片 ----------
@Composable
private fun LedgerCard(balance: Double, todayExpense: Double, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("总余额 ¥${fmt(balance)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("今日支出 ¥${fmt(todayExpense)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PrimaryButton(label = "+ 记一笔", onClick = onAdd)
    }
}

// ---------- TodoList 卡片 ----------
@Composable
private fun TodoHomeCard(undone: List<TodoEntity>, onToggle: (TodoEntity) -> Unit, onOpen: () -> Unit) {
    if (undone.isEmpty()) {
        EmptyHint("暂无待办事项")
    } else {
        undone.take(4).forEach { t ->
            Row(Modifier.fillMaxWidth().clickable { onToggle(t) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = false, onCheckedChange = { onToggle(t) })
                Text(
                    t.title,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                    textDecoration = if (t.isDone) TextDecoration.LineThrough else TextDecoration.None,
                )
            }
        }
    }
    TextButton(onClick = onOpen) { Text("进入 TodoList") }
}

// ---------- 快捷入口卡片 ----------
@Composable
private fun QuickEntriesCard(visibleTools: List<QuickTool>, links: List<LinkEntity>, navController: NavController) {
    if (visibleTools.isEmpty() && links.none { it.isHomeCard }) {
        EmptyHint("暂无快捷入口，可到设置添加")
        return
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        visibleTools.forEach { t ->
            Column(
                Modifier.clip(MaterialTheme.shapes.medium).clickable { navController.navigate(t.route) }.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppIconBadge(t.icon, background = MaterialTheme.colorScheme.surface)
                Spacer(Modifier.height(6.dp))
                Text(t.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        links.filter { it.isHomeCard }.forEach { l ->
            Column(
                Modifier.clip(MaterialTheme.shapes.medium).clickable { navController.navigate(Routes.webview(l.name, l.url)) }.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppIconBadge(l.icon.ifBlank { "link" }, background = MaterialTheme.colorScheme.surface)
                Spacer(Modifier.height(6.dp))
                Text(l.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// ---------- 日程卡片 ----------
@Composable
private fun ScheduleHomeCard(events: List<SystemEvent>, today: String, onClick: () -> Unit) {
    val now = System.currentTimeMillis()
    val todaySchedules = events.filter {
        DateUtils.dateString(Date(it.startMillis)) == today && it.endMillis >= now
    }.sortedBy { it.startMillis }.take(3)
    if (todaySchedules.isEmpty()) {
        EmptyHint("今天没有待办日程")
    } else {
        Column(Modifier.fillMaxWidth().heightIn(min = 92.dp)) {
            todaySchedules.forEach { s ->
                Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Text(s.title, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "${homeTimeFmt.format(Date(s.startMillis))} - ${homeTimeFmt.format(Date(s.endMillis))}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    TextButton(onClick = onClick) { Text("进入日程") }
}

private fun fmt(d: Double): String = String.format(java.util.Locale.US, "%.2f", d)
