package com.solium.Unitoolkit.ui.course

import com.solium.Unitoolkit.core.database.CourseEntity
import com.solium.Unitoolkit.core.database.matchesWeek
import com.solium.Unitoolkit.core.model.HolidayAdjustment
import com.solium.Unitoolkit.core.model.HolidayPackage
import com.solium.Unitoolkit.core.utils.DateUtils
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
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

    /**
     * 导出给大模型使用的提示词：说明字段含义与 JSON 模板，用户可粘贴到任意大模型后
     * 让它把自己的课表整理成可直接导入的 JSON。
     */
    fun aiPromptTemplate(): String = """
        # 课表整理提示词（Uni ToolKit）

        请把下面提供的课表文本整理成 **JSON 数组**，只输出 JSON 本身，不要额外解释、不要用 markdown 代码块包裹。

        ## 字段说明

        | 字段 | 类型 | 说明 |
        |------|------|------|
        | name | string | 课程名称（必填） |
        | teacher | string | 教师（可省略） |
        | location | string | 教室 / 地点（可省略） |
        | weekday | number 或 number[] | 星期，1=周一 … 7=周日；多天上课用数组，如 [1,3] |
        | startTime | string | 开始时间，格式 HH:mm（必填） |
        | endTime | string | 结束时间，格式 HH:mm（必填） |
        | weeks | number 或 number[] 或 string | 上课周次，如 [1,2,3] / "1-16" / "1,3,5" |
        | weekType | string | every(每周) / odd(单周) / even(双周) / once(仅一次) / selected(按 weeks 指定) |
        | onceWeek | number | weekType 为 once 时的周次 |
        | colorTag | string | 颜色，形如 "#8B5CF6"（可省略） |
        | note | string | 备注（可省略） |

        ## 要求

        1. 周次请按原文保留，允许出现 0、-1 之类的特殊周次，不要丢弃、不要改写。
        2. 时间统一补成两位小时，例如 8:00 写成 08:00。
        3. 没有把握的字段可以省略；name、weekday、startTime、endTime 请尽量给出。
        4. 输出必须是合法的 JSON 数组。

        ## JSON 模板

        ```json
        [
          {
            "name": "高等数学A",
            "teacher": "张三",
            "location": "教B-201",
            "weekday": 1,
            "startTime": "08:00",
            "endTime": "08:45",
            "weeks": [1, 2, 3, 4, 5, 6, 7, 8],
            "weekType": "selected",
            "colorTag": "#8B5CF6",
            "note": ""
          },
          {
            "name": "大学英语",
            "teacher": "李四",
            "location": "外语楼305",
            "weekday": [2, 4],
            "startTime": "10:00",
            "endTime": "10:45",
            "weekType": "every"
          }
        ]
        ```

        ## 我的课表

        （在此粘贴你的课表文本）
    """.trimIndent()

    /** 调休表序列化（.sltx）。 */
    fun holidayToJson(adjustments: List<HolidayAdjustment>, name: String = "调休表"): String {
        val pkg = HolidayPackage(name = name, adjustments = adjustments)
        return Json { prettyPrint = true; encodeDefaults = true }.encodeToString(HolidayPackage.serializer(), pkg)
    }

    /** 调休表解析；兼容裸数组写法。 */
    fun holidayFromJson(raw: String): List<HolidayAdjustment> {
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val element = runCatching { json.parseToJsonElement(raw.trim()) }.getOrNull() ?: return emptyList()
        return when (element) {
            is JsonArray -> runCatching {
                json.decodeFromJsonElement(ListSerializer(HolidayAdjustment.serializer()), element)
            }.getOrDefault(emptyList())

            is JsonObject -> runCatching {
                json.decodeFromJsonElement(HolidayPackage.serializer(), element).adjustments
            }.getOrDefault(emptyList())

            else -> emptyList()
        }
    }
}
