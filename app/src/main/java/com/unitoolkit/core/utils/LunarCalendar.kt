package com.unitoolkit.core.utils

/**
 * 基于《绕月历》公历→农历换算（1900–2100）与 24 节气近似计算。
 * 数据表为广泛使用的 lunarInfo 编码表。
 */
object LunarCalendar {

    private val lunarInfo = intArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5d0, 0x14573, 0x052d0, 0x0a9a8, 0x0e950, 0x06aa0,
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b5a0, 0x195a6,
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0,
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
        0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252, 0x0d520
    )

    private val monthNames = arrayOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")

    private val termNames = arrayOf(
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分", "清明", "谷雨", "立夏", "小满",
        "芒种", "夏至", "小暑", "大暑", "立秋", "处暑", "白露", "秋分", "寒露", "霜降",
        "立冬", "小雪", "大雪", "冬至"
    )
    private val termInfo = arrayOf(
        "0106", "0120", "0204", "0219", "0306", "0321", "0405", "0420", "0506", "0521",
        "0606", "0621", "0707", "0723", "0808", "0823", "0908", "0923", "1008", "1024",
        "1108", "1122", "1207", "1222"
    )

    data class Lunar(val year: Int, val month: Int, val day: Int, val isLeap: Boolean)

    private fun lunarYearDays(y: Int): Int {
        var i = 0x8000
        var sum = 348
        while (i > 0x8) {
            if (lunarInfo[y - 1900] and i != 0) sum += 1
            i = i shr 1
        }
        return sum + leapDays(y)
    }

    private fun leapMonth(y: Int): Int = lunarInfo[y - 1900] and 0xf

    private fun leapDays(y: Int): Int =
        if (leapMonth(y) != 0) (if (lunarInfo[y - 1900] and 0x10000 != 0) 30 else 29) else 0

    private fun monthDays(y: Int, m: Int): Int =
        if (lunarInfo[y - 1900] and (0x10000 shr m) != 0) 30 else 29

    /** 公历 → 儒略日 */
    private fun jdn(y: Int, m: Int, d: Int): Long {
        val a = (14 - m) / 12
        val yy = y + 4800 - a
        val mm = m + 12 * a - 3
        return d + (153 * mm + 2) / 5 + 365L * yy + yy / 4 - yy / 100 + yy / 400 - 32045
    }

    fun solarToLunar(sy: Int, sm: Int, sd: Int): Lunar {
        var offset = (jdn(sy, sm, sd) - jdn(1900, 1, 31)).toInt()
        var i = 1900
        var temp = 0
        while (i < 2101 && offset > 0) {
            temp = lunarYearDays(i)
            offset -= temp
            i++
        }
        if (offset < 0) { offset += temp; i-- }
        val year = i
        val leap = leapMonth(year)
        var isLeap = false
        i = 1
        while (i < 13 && offset > 0) {
            if (leap > 0 && i == leap + 1 && !isLeap) {
                i--
                isLeap = true
                temp = leapDays(year)
            } else {
                temp = monthDays(year, i)
            }
            if (isLeap && i == leap + 1) isLeap = false
            offset -= temp
            i++
        }
        if (offset == 0 && leap > 0 && i == leap + 1) {
            if (isLeap) isLeap = false else { isLeap = true; i-- }
        }
        if (offset < 0) { offset += temp; i-- }
        return Lunar(year, i, offset + 1, isLeap)
    }

    private fun dayName(d: Int): String = when (d) {
        1 -> "初一"; 2 -> "初二"; 3 -> "初三"; 4 -> "初四"; 5 -> "初五"
        6 -> "初六"; 7 -> "初七"; 8 -> "初八"; 9 -> "初九"; 10 -> "初十"
        11 -> "十一"; 12 -> "十二"; 13 -> "十三"; 14 -> "十四"; 15 -> "十五"
        16 -> "十六"; 17 -> "十七"; 18 -> "十八"; 19 -> "十九"; 20 -> "二十"
        21 -> "廿一"; 22 -> "廿二"; 23 -> "廿三"; 24 -> "廿四"; 25 -> "廿五"
        26 -> "廿六"; 27 -> "廿七"; 28 -> "廿八"; 29 -> "廿九"; 30 -> "三十"
        else -> d.toString()
    }

    /** 返回如「四月初八」（闰月前置「闰」）的农历月日文本 */
    fun lunarText(sy: Int, sm: Int, sd: Int): String {
        val l = solarToLunar(sy, sm, sd)
        val month = monthNames[(l.month - 1).coerceIn(0, 11)] + "月"
        return (if (l.isLeap) "闰" else "") + month + dayName(l.day)
    }

    /** 返回当前所处节气（近似），如「立夏」 */
    fun solarTerm(sy: Int, sm: Int, sd: Int): String {
        var idx = -1
        for (i in termInfo.indices) {
            val mm = termInfo[i].substring(0, 2).toInt()
            val dd = termInfo[i].substring(2, 4).toInt()
            if (mm < sm || (mm == sm && dd <= sd)) idx = i else break
        }
        if (idx < 0) idx = termNames.size - 1
        return termNames[idx]
    }
}