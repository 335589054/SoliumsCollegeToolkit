package com.solium.Unitoolkit.core.utils

import java.util.Calendar

/**
 * 全局统一的日期 / 时间「文本输入框」容错解析工具。
 *
 * 针对用户在设置 / 表单里直接输入日期、时间的场景（区别于系统 DialogPicker），
 * 支持中文与英文符号分隔（`-` `/` `.` `年/月/日` `时/分` `点` 空格、冒号、逗号等），
 * 也支持纯数字输入：
 *  - 日期 4 位 = 月日（如 0930 = 9 月 30 日，年份用当前年份）
 *  - 日期 6 位 = 年缩写+月日（如 260910 = 2026 年 09 月 10 日）
 *  - 日期 8 位 = 年月日（如 20260910 = 2026 年 09 月 10 日）
 *  - 时间 `HH:mm`、`HHmm`（4 位自动加冒号）、`H点m分` 等
 */
object FlexibleDateTimeParser {

    /** 日期输入框的默认/空值填充：当前日期（yyyy-MM-dd） */
    fun todayDefault(): String = DateUtils.today()

    /** 时间输入框的默认/空值填充：当前时间（HH:mm） */
    fun nowDefault(): String = DateUtils.nowTime()

    /**
     * 解析日期文本为规范 `yyyy-MM-dd`；解析失败返回 null（由调用方决定是否用 [todayDefault] 兜底）。
     * [relative] 用于在缺少年份/月份时补齐（默认当前日期）。
     */
    fun parseFlexibleDate(input: String?, relative: Calendar = Calendar.getInstance()): String? {
        val s = input?.trim().orEmpty()
        if (s.isEmpty()) return null
        val (year, month, day) = parts(relative)
        return if (Regex("[年月日]").containsMatchIn(s)) {
            parseChineseDate(s, year, month, day)
        } else {
            parseNumericDate(s, year)
        }
    }

    /** 解析时间文本为规范 `HH:mm`；解析失败返回 null（由调用方决定是否用 [nowDefault] 兜底）。 */
    fun parseFlexibleTime(input: String?): String? {
        val s = input?.trim().orEmpty()
        if (s.isEmpty()) return null
        if (Regex("[时点分]").containsMatchIn(s)) {
            val h = Regex("(\\d{1,2})\\s*[时点]").find(s)?.groupValues?.get(1)?.toIntOrNull()
            val m = Regex("(\\d{1,2})\\s*分").find(s)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("[时点]\\s*(\\d{1,2})").find(s)?.groupValues?.get(1)?.toIntOrNull()
            return buildTime(h, m)
        }
        val tokens = Regex("\\d+").findAll(s).map { it.value }.toList()
        if (tokens.isNotEmpty() && tokens.size >= 2) {
            return buildTime(tokens[0].toIntOrNull(), tokens[1].toIntOrNull())
        }
        val dg = tokens.getOrNull(0) ?: return null
        return when (dg.length) {
            4 -> buildTime(dg.substring(0, 2).toIntOrNull(), dg.substring(2, 4).toIntOrNull())
            3 -> buildTime(dg.substring(0, 1).toIntOrNull(), dg.substring(1, 3).toIntOrNull())
            1, 2 -> buildTime(dg.toIntOrNull(), 0)
            else -> null
        }
    }

    /** 解析单个日期文本，失败 / 空白返回 [todayDefault]（空值自动补当前日期）。 */
    fun normalizeDate(input: String?, relative: Calendar = Calendar.getInstance()): String =
        parseFlexibleDate(input, relative) ?: todayDefault()

    /** 解析单个时间文本，失败 / 空白返回 [nowDefault]（空值自动补当前时间）。 */
    fun normalizeTime(input: String?): String =
        parseFlexibleTime(input) ?: nowDefault()

    /** 解析以逗号分隔的多个日期（课表「指定日期」等），规范化为 `yyyy-MM-dd` 去重列表。 */
    fun normalizeDateList(input: String?, relative: Calendar = Calendar.getInstance()): String {
        if (input.isNullOrBlank()) return ""
        return input.split(',', '，', '、', ';', '；', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { parseFlexibleDate(it, relative) }
            .distinct()
            .joinToString(", ")
    }

    // ---- 内部实现 ----

    private fun parts(now: Calendar) = Triple(
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH) + 1,
        now.get(Calendar.DAY_OF_MONTH),
    )

    private fun parseChineseDate(s: String, cy: Int, cm: Int, cd: Int): String? {
        val year = Regex("(\\d{1,4})\\s*年").find(s)?.groupValues?.get(1)?.toIntOrNull()
        val month = Regex("(\\d{1,2})\\s*月").find(s)?.groupValues?.get(1)?.toIntOrNull()
        val day = Regex("(\\d{1,2})\\s*日").find(s)?.groupValues?.get(1)?.toIntOrNull()
        if (year == null && month == null && day == null) return null
        return buildDate(year ?: cy, month ?: cm, day ?: cd)
    }

    private fun parseNumericDate(s: String, cy: Int): String? {
        val tokens = Regex("\\d+").findAll(s).map { it.value }.toList()
        if (tokens.isEmpty()) return null
        return when {
            tokens.size >= 3 -> {
                buildDate(expandYearToken(tokens[0]), tokens[1].toIntOrNull() ?: 1, tokens[2].toIntOrNull() ?: 1)
            }
            tokens.size == 2 -> buildDate(cy, tokens[0].toIntOrNull() ?: 1, tokens[1].toIntOrNull() ?: 1)
            else -> {
                val dg = tokens[0]
                when (dg.length) {
                    8 -> buildDate(
                        dg.substring(0, 4).toInt(), dg.substring(4, 6).toInt(), dg.substring(6, 8).toInt(),
                    )
                    6 -> {
                        val yy = dg.substring(0, 2).toInt()
                        buildDate(expandTwoDigitYear(yy), dg.substring(2, 4).toInt(), dg.substring(4, 6).toInt())
                    }
                    4 -> buildDate(cy, dg.substring(0, 2).toInt(), dg.substring(2, 4).toInt())
                    else -> null
                }
            }
        }
    }

    /** 年份 token：2 位缩写则按是否 < 70 推断世纪；否则取原值 */
    private fun expandYearToken(t: String): Int =
        if (t.length == 2) expandTwoDigitYear(t.toIntOrNull() ?: 0) else t.toIntOrNull() ?: 0

    private fun expandTwoDigitYear(yy: Int): Int = if (yy < 70) 2000 + yy else 1900 + yy

    private fun buildDate(y: Int, m: Int, d: Int): String? {
        if (m !in 1..12) return null
        val year = y.coerceIn(1900, 2100)
        val day = d.takeIf { it in 1..daysInMonth(year, m) } ?: return null
        return "%04d-%02d-%02d".format(year, m, day)
    }

    private fun buildTime(h: Int?, m: Int?): String? {
        val hh = h ?: return null
        if (hh !in 0..23) return null
        val mm = m ?: 0
        if (mm !in 0..59) return null
        return "%02d:%02d".format(hh, mm)
    }

    private fun daysInMonth(y: Int, m: Int): Int = when (m) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0) 29 else 28
        else -> 0
    }
}