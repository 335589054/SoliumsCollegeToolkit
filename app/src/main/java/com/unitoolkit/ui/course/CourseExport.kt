package com.unitoolkit.ui.course

import com.unitoolkit.core.database.CourseEntity
import com.unitoolkit.core.database.matchesWeek
import com.unitoolkit.core.utils.DateUtils
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object CourseExport {

    fun toMarkdown(courses: List<CourseEntity>, week: Int): String {
        val active = courses.filter { it.matchesWeek(week) }
        val weekdays = 1..7
        // 收集所有时间段并按开始时间排序
        val slots = active.map { it.startTime to it.endTime }.distinct()
            .sortedBy { DateUtils.timeToMinutes(it.first) }
        val sb = StringBuilder()
        sb.appendLine("# 我的课表 — 第 $week 周")
        sb.appendLine()
        sb.append("| 时间 |")
        weekdays.forEach { sb.append(" ${DateUtils.weekdayZh(it)} |") }
        sb.appendLine()
        sb.append("|------|")
        weekdays.forEach { sb.append("------|") }
        sb.appendLine()
        slots.forEach { (start, end) ->
            sb.append("| $start-$end |")
            weekdays.forEach { w ->
                val course = active.firstOrNull { it.weekday == w && it.startTime == start && it.endTime == end }
                val cell = course?.let { c ->
                    val loc = if (c.location.isNotBlank()) " (${c.location})" else ""
                    "${c.name}$loc"
                } ?: ""
                sb.append(" $cell |")
            }
            sb.appendLine()
        }
        return sb.toString()
    }

    fun toJson(courses: List<CourseEntity>): String {
        val arr: JsonArray = buildJsonArray {
            courses.forEach { c ->
                add(buildJsonObject {
                    put("name", c.name)
                    put("teacher", c.teacher)
                    put("location", c.location)
                    put("weekday", c.weekday)
                    put("startTime", c.startTime)
                    put("endTime", c.endTime)
                    put("colorTag", c.colorTag)
                    put("note", c.note)
                    put("weekType", c.weekType)
                    put("onceWeek", c.onceWeek)
                    put("selectedWeeks", c.selectedWeeks)
                    put("selectedDates", c.selectedDates)
                })
            }
        }
        return arr.toString()
    }
}
