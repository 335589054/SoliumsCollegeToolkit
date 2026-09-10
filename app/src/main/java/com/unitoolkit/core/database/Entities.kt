package com.unitoolkit.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// ============ 课程（按具体时间，非节次） ============
@Serializable
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val teacher: String = "",
    val location: String = "",
    val weekday: Int,               // 1-7 周一至周日
    val weekdaysText: String = "",  // 逗号分隔 1-7；为空时使用 weekday（兼容旧数据）
    val startTime: String,          // "HH:mm"
    val endTime: String,            // "HH:mm"
    val colorTag: String = "#8B5CF6",
    val note: String = "",
    val weekType: String = "every",  // every / odd(单周) / even(双周) / once(仅一次)
    val onceWeek: Int = 1,           // weekType == "once" 时的具体周次
    val selectedWeeks: String = "",  // weekType == "selected"：逗号分隔周次
    val selectedDates: String = "",  // weekType == "dates"：逗号分隔日期 yyyy-MM-dd
    val createdAt: Long = System.currentTimeMillis(),
)

fun CourseEntity.selectedWeekList(): List<Int> =
    selectedWeeks.split(',').mapNotNull { it.trim().toIntOrNull() }

fun CourseEntity.selectedDateList(): List<String> =
    selectedDates.split(',').map { it.trim() }.filter { it.isNotBlank() }

fun CourseEntity.weekdayList(): List<Int> {
    val list = weekdaysText.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 }
    return if (list.isEmpty() && weekday in 1..7) listOf(weekday) else list
}

fun CourseEntity.matchesWeekday(day: Int): Boolean = weekdayList().isEmpty() || day in weekdayList()

/** 判断课程在指定周次是否上课 */
fun CourseEntity.matchesWeek(week: Int, date: String? = null): Boolean = when (weekType) {
    "odd" -> week % 2 == 1
    "even" -> week % 2 == 0
    "once" -> week == onceWeek
    "selected" -> week in selectedWeekList()
    "dates" -> date != null && date in selectedDateList()
    else -> true
}

// ============ 笔记 ============
@Serializable
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
)

// ============ 待办 ============
@Serializable
@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val priority: Int = 1,          // 0低 1中 2高
    val dueDate: String = "",       // "yyyy-MM-dd HH:mm" 或空
    val tags: String = "",          // 逗号分隔
    val isDone: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

// ============ 番茄钟历史 ============
@Serializable
@Entity(tableName = "pomodoro_records")
data class PomodoroRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val presetName: String,
    val focusMinutes: Int,
    val completedAt: Long = System.currentTimeMillis(),
)

// ============ 绩点课程 ============
@Serializable
@Entity(tableName = "gpa_courses")
data class GpaCourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val semester: String,
    val name: String,
    val credits: Double,
    val rawScore: Double,
    val gradePoint: Double,
)

// ============ 便捷链接 ============
@Serializable
@Entity(tableName = "links")
data class LinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val category: String = "自定义",
    val icon: String = "",
    val sortOrder: Int = 0,
    val isHomeCard: Boolean = false,
)

// ============ 账单 ============
@Serializable
@Entity(tableName = "ledger_entries")
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,               // "income" / "expense"
    val amount: Double,
    val category: String,
    val note: String = "",
    val date: String,               // "yyyy-MM-dd HH:mm"
    val paymentMethod: String = "", // 微信 / 支付宝 / 银行卡 / 自定义
)

@Serializable
@Entity(tableName = "ledger_categories")
data class LedgerCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,               // "income" / "expense"
    val icon: String = "",
    val isCustom: Boolean = false,
)

// ============ 应用内日程 ============
@Serializable
@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceEventId: String = "", // 系统日历事件 ID（关联）
    val title: String,
    val startTime: String,          // "yyyy-MM-dd HH:mm"
    val endTime: String,            // "yyyy-MM-dd HH:mm"
    val reminderMinutes: Int = 15,
)

// ============ 悬浮窗预设 ============
@Serializable
@Entity(tableName = "overlay_presets")
data class OverlayPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val jsonData: String,
)

// ============ 采购清单 ============
@Serializable
@Entity(tableName = "shopping_activities")
data class ShoppingActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,               // 生活超市 / 朴朴 / 山姆 / 奶茶店 / 面包店 / 淘宝网购 / 线下菜市场
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: Long,
    val name: String,
    val isPostponed: Boolean = false, // 暂缓采购：下次同类活动自动带入
    val isDone: Boolean = false,      // 已购
    val createdAt: Long = System.currentTimeMillis(),
)
