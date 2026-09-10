package com.unitoolkit.ui.home

import com.unitoolkit.ui.navigation.Routes

/** 首页「快捷入口」中的内置工具入口（与便捷链接区分） */
data class QuickTool(val key: String, val icon: String, val label: String, val route: String)

val quickTools = listOf(
    QuickTool("course", "calendar-days", "课表", Routes.COURSE),
    QuickTool("note", "pen-line", "笔记", Routes.NOTE_LIST),
    QuickTool("todo", "square-check", "待办", Routes.TODO),
    QuickTool("pomodoro", "timer", "番茄钟", Routes.POMODORO),
    QuickTool("ledger", "wallet", "账单", Routes.LEDGER),
    QuickTool("utility", "zap", "水电费", Routes.UTILITY),
    QuickTool("schedule", "calendar-clock", "日程", Routes.SCHEDULE),
    QuickTool("gpa", "graduation-cap", "绩点", Routes.GPA),
    QuickTool("links", "link", "链接", Routes.LINKS),
    QuickTool("shopping", "shopping-cart", "采购", Routes.SHOPPING),
    QuickTool("overlay", "music", "上隐条", Routes.OVERLAY),
)