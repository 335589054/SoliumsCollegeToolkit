package com.solium.Unitoolkit.ui.course

import com.solium.Unitoolkit.core.database.CourseEntity
import com.solium.Unitoolkit.core.utils.FlexibleDateTimeParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 课表 JSON 导入：把大模型 / 其它工具生成的 JSON 文本解析为课程列表。
 *
 * 设计目标是「尽量别丢数据、尽量别崩」：
 * - 顶层可以是数组，也可以是含数组字段的对象；
 * - 字段名兼容中英文多种写法与常见别名；
 * - 周次允许出现 0、-1 等特殊值：原样导入，但课表界面只展示 >= 1 的周次；
 * - 单条数据缺失关键字段时跳过并记录原因，不影响其余数据。
 */
object CourseJsonImport {

    data class Result(val courses: List<CourseEntity>, val warnings: List<String>) {
        val ok: Boolean get() = courses.isNotEmpty()
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** 名称候选字段（按优先级）。 */
    private val nameKeys = listOf("name", "courseName", "course", "title", "课程名称", "课程名", "课程")
    private val teacherKeys = listOf("teacher", "teacherName", "教师", "老师", "任课教师")
    private val locationKeys = listOf("location", "room", "classroom", "place", "教室", "地点", "上课地点")
    private val weekdayKeys = listOf("weekday", "weekdays", "day", "weekDay", "星期", "上课星期", "周几")
    private val weekdayTextKeys = listOf("weekdayText", "weekdaysText", "星期文本", "星期几")
    private val startKeys = listOf("startTime", "start", "begin", "开始时间", "开始")
    private val endKeys = listOf("endTime", "end", "finish", "结束时间", "结束")
    private val timeRangeKeys = listOf("time", "timeRange", "时间", "上课时间")
    private val weeksKeys = listOf("weeks", "week", "weekList", "周次", "上课周次", "周数")
    private val weekTypeKeys = listOf("weekType", "weekMode", "weekPattern", "周类型", "上课模式", "周次类型")
    private val onceWeekKeys = listOf("onceWeek", "singleWeek", "指定周次", "单次周次")
    private val colorKeys = listOf("colorTag", "color", "颜色", "颜色标签")
    private val noteKeys = listOf("note", "remark", "备注", "说明")
    private val dateKeys = listOf("dates", "date", "日期", "上课日期")

    /** 解析入口：容错处理 markdown 代码块与前后说明文字。 */
    fun parse(raw: String): Result {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return Result(emptyList(), listOf("内容为空"))
        val text = extractJsonBlock(trimmed)
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull()
            ?: return Result(emptyList(), listOf("JSON 解析失败，请检查括号、逗号与引号是否完整"))
        val array = when (root) {
            is JsonArray -> root
            is JsonObject -> findCourseArray(root) ?: return Result(emptyList(), listOf("未在对象中找到课程数组字段"))
            else -> return Result(emptyList(), listOf("JSON 顶层应为数组或包含数组的对象"))
        }
        if (array.isEmpty()) return Result(emptyList(), listOf("课程数组为空"))
        val warnings = mutableListOf<String>()
        val courses = array.mapIndexedNotNull { index, element ->
            val obj = element as? JsonObject
            if (obj == null) {
                warnings += "第 ${index + 1} 条不是对象，已跳过"
                null
            } else {
                parseItem(obj, index, warnings)
            }
        }
        return Result(courses, warnings)
    }

    /** 截取首个 `[` / `{` 到最后一个 `]` / `}` 之间的内容，去掉 ```json 包裹与解释性文字。 */
    private fun extractJsonBlock(text: String): String {
        val stripped = text.removePrefix("```json").removePrefix("```JSON").removePrefix("```")
        val start = stripped.indexOfFirst { it == '[' || it == '{' }
        val end = stripped.lastIndexOfAny(charArrayOf(']', '}'))
        return if (start >= 0 && end > start) stripped.substring(start, end + 1) else stripped
    }

    /** 依次尝试常见字段名，找不到则退回对象中第一个数组值。 */
    private fun findCourseArray(obj: JsonObject): JsonArray? {
        val candidates = listOf("courses", "courseList", "data", "list", "items", "课表", "课程列表")
        candidates.forEach { key ->
            (obj[key] as? JsonArray)?.let { return it }
        }
        return obj.values.firstOrNull { it is JsonArray } as? JsonArray
    }

    private fun parseItem(obj: JsonObject, index: Int, warnings: MutableList<String>): CourseEntity? {
        val label = "第 ${index + 1} 条"
        val name = obj.firstOf(nameKeys)?.toStr().orEmpty()
        if (name.isBlank()) {
            warnings += "$label 缺少课程名称，已跳过"
            return null
        }

        // 时间：支持分开的起止字段，也支持 "08:00-08:45" 合并写法
        var start = obj.firstOf(startKeys)?.toStr().orEmpty()
        var end = obj.firstOf(endKeys)?.toStr().orEmpty()
        if (start.isBlank() || end.isBlank()) {
            val range = obj.firstOf(timeRangeKeys)?.toStr().orEmpty()
            val sep = range.indexOfFirst { it == '-' || it == '~' || it == '—' || it == '–' }
            if (sep > 0) {
                if (start.isBlank()) start = range.substring(0, sep).trim()
                if (end.isBlank()) end = range.substring(sep + 1).trim()
            }
        }
        start = FlexibleDateTimeParser.parseFlexibleTime(start) ?: start
        end = FlexibleDateTimeParser.parseFlexibleTime(end) ?: end
        if (start.isBlank()) {
            start = "08:00"
            warnings += "$label 缺少开始时间，已按 08:00 处理"
        }
        if (end.isBlank()) {
            end = "08:45"
            warnings += "$label 缺少结束时间，已按 08:45 处理"
        }

        val weekdays = parseWeekdays(obj.firstOf(weekdayKeys), obj.firstOf(weekdayTextKeys)?.toStr().orEmpty())
        if (weekdays.isEmpty()) warnings += "$label 未识别到星期，已按「每天」处理"

        val weeks = parseWeeks(obj.firstOf(weeksKeys))
        val rawType = obj.firstOf(weekTypeKeys)?.toStr().orEmpty()
        val weekType = normalizeWeekType(rawType, weeks)
        val onceWeek = obj.firstOf(onceWeekKeys)?.toIntOrNull() ?: weeks.firstOrNull() ?: 1
        val dates = FlexibleDateTimeParser.normalizeDateList(obj.firstOf(dateKeys)?.toStr().orEmpty())

        val color = obj.firstOf(colorKeys)?.toStr().orEmpty().takeIf { isValidHex(it) } ?: "#8B5CF6"

        return CourseEntity(
            name = name,
            teacher = obj.firstOf(teacherKeys)?.toStr().orEmpty(),
            location = obj.firstOf(locationKeys)?.toStr().orEmpty(),
            weekday = weekdays.firstOrNull() ?: 0,
            weekdaysText = weekdays.joinToString(","),
            startTime = start,
            endTime = end,
            colorTag = color,
            note = obj.firstOf(noteKeys)?.toStr().orEmpty(),
            weekType = weekType,
            onceWeek = onceWeek,
            selectedWeeks = if (weekType == "selected") weeks.joinToString(",") else "",
            selectedDates = if (weekType == "dates") dates else "",
            periodSection = 0,
        )
    }

    private fun isValidHex(value: String): Boolean =
        Regex("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$").matches(value.trim())

    /** 周次类型归一化；未显式给出类型时，给出周次列表则视为「多选周次」。 */
    private fun normalizeWeekType(raw: String, weeks: List<Int>): String {
        val t = raw.trim().lowercase()
        return when {
            t.isBlank() -> if (weeks.isNotEmpty()) "selected" else "every"
            t.contains("单") || t == "odd" -> "odd"
            t.contains("双") || t == "even" -> "even"
            t.contains("仅") || t.contains("一次") || t == "once" || t == "single" -> "once"
            t.contains("多选") || t.contains("指定周") || t == "selected" || t == "custom" -> "selected"
            t.contains("日期") || t == "dates" -> "dates"
            t.contains("每周") || t.contains("全部") || t == "every" || t == "all" -> "every"
            weeks.isNotEmpty() -> "selected"
            else -> "every"
        }
    }

    private fun parseWeekdays(el: JsonElement?, fallbackText: String): List<Int> {
        val out = LinkedHashSet<Int>()
        val tokens = mutableListOf<String>()
        when (el) {
            null -> Unit
            is JsonArray -> el.forEach { tokens += it.toStr() }
            else -> tokens += el.toStr()
        }
        if (fallbackText.isNotBlank()) tokens += fallbackText
        tokens.flatMap { splitTokens(it) }.forEach { token ->
            token.trim().toIntOrNull()?.let { v ->
                // 星期约定 1-7；少数数据用 0 表示周日，这里归一为 7
                out += if (v == 0) 7 else v
                return@forEach
            }
            weekdayFromText(token)?.let { out += it }
        }
        return out.filter { it in 1..7 }
    }

    private fun parseWeeks(el: JsonElement?): List<Int> {
        if (el == null) return emptyList()
        val out = LinkedHashSet<Int>()
        val tokens = mutableListOf<String>()
        when (el) {
            is JsonArray -> el.forEach { tokens += it.toStr() }
            else -> tokens += el.toStr()
        }
        tokens.flatMap { splitTokens(it) }.forEach { expandWeekToken(it, out) }
        return out.toList()
    }

    /** 按常见分隔符（逗号 / 顿号 / 空格 / 分号 / 斜杠）拆分。 */
    private fun splitTokens(raw: String): List<String> =
        raw.replace('，', ',').replace('、', ',').replace('；', ',').replace(';', ',')
            .replace('/', ',').replace(' ', ',')
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }

    /** 展开单个周次片段：支持范围（1-16 / 1~16 / 1至16）与单值；<=0 的周次同样保留。 */
    private fun expandWeekToken(token: String, out: MutableSet<Int>) {
        var t = token.trim().removeSuffix("周").removePrefix("第").trim()
        if (t.isBlank()) return
        t = t.replace("—", "-").replace("–", "-").replace("~", "-").replace("至", "-")
        val range = Regex("^(-?\\d+)\\s*-\\s*(-?\\d+)$").find(t)
        if (range != null) {
            val a = range.groupValues[1].toIntOrNull() ?: return
            val b = range.groupValues[2].toIntOrNull() ?: return
            val lo = minOf(a, b)
            val hi = maxOf(a, b)
            // 防御：范围过大时截断，避免异常数据撑爆数据量
            if (hi - lo > 200) return
            for (w in lo..hi) out += w
            return
        }
        t.toIntOrNull()?.let { out += it }
    }

    private fun weekdayFromText(token: String): Int? {
        val t = token.trim().lowercase()
            .replace("星期", "").replace("周", "").replace("礼拜", "").trim()
        return when {
            t.isBlank() -> null
            t.startsWith("mon") || t.contains("一") -> 1
            t.startsWith("tue") || t.contains("二") -> 2
            t.startsWith("wed") || t.contains("三") -> 3
            t.startsWith("thu") || t.contains("四") -> 4
            t.startsWith("fri") || t.contains("五") -> 5
            t.startsWith("sat") || t.contains("六") -> 6
            t.startsWith("sun") || t.contains("日") || t.contains("天") -> 7
            else -> null
        }
    }

    private fun JsonObject.firstOf(keys: List<String>): JsonElement? {
        keys.forEach { k -> this[k]?.let { return it } }
        return null
    }

    private fun JsonElement.toStr(): String = when (this) {
        is JsonPrimitive -> content
        is JsonArray -> joinToString(",") { (it as? JsonPrimitive)?.content ?: "" }
        is JsonObject -> ""
    }

    private fun JsonElement.toIntOrNull(): Int? =
        (this as? JsonPrimitive)?.content?.trim()?.let { Regex("^-?\\d+$").find(it)?.value?.toIntOrNull() }
}
