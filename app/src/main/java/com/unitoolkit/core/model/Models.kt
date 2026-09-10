package com.unitoolkit.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PomodoroPreset(
    val name: String,
    val workMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    val longBreakInterval: Int,
)

fun defaultPomodoroPresets(): List<PomodoroPreset> = listOf(
    PomodoroPreset("专注 25/5", 25, 5, 15, 4),
    PomodoroPreset("深度 50/10", 50, 10, 30, 3),
    PomodoroPreset("快速 15/3", 15, 3, 10, 4),
)

@Serializable
data class OverlayConfig(
    val mode: String = "image",     // image / color / text
    val imagePath: String = "",  // 本地图片文件路径（遮罩上隐条）
    val imageScale: String = "fit", // fit 拉伸 / crop 裁切
    val colorHex: String = "#000000",
    val text: String = "",
    val textColorHex: String = "#FFFFFF",
    val textSizePx: Int = 0,
    val width: Int = 0,          // px，0 表示占满屏幕宽度
    val height: Int = 0,         // px，0 表示约 40% 屏幕高度
    val opacity: Float = 1f,
    val x: Int = 0,              // 屏幕坐标（水平）
    val y: Int = 0,              // 屏幕坐标（垂直）
)

@Serializable
data class GpaMapping(val min: Double, val point: Double)

@Serializable
data class GpaFormula(
    val mode: String = "percent4", // percent4 / percent5 / custom
    val mappings: List<GpaMapping> = emptyList(),
)

fun defaultGpaFormula(): GpaFormula = GpaFormula(mode = "percent4")

@Serializable
data class PersonalProfile(
    val name: String = "",
    val phone: String = "",
    val qq: String = "",
    val qqQrPath: String = "",
    val wechatQrPath: String = "",
    val bilibiliName: String = "",
    val bilibiliUid: String = "",
    val avatarPath: String = "",
    val customInfo: List<PersonalInfoItem> = emptyList(),
)

@Serializable
data class PersonalInfoItem(
    val label: String = "",
    val value: String = "",
)

/** 课表预设：只保存已填写的字段，空白字段表示不套用。 */
@Serializable
data class CoursePreset(
    val id: String = "",
    val name: String = "",
    val courseName: String = "",
    val teacher: String = "",
    val location: String = "",
    val weekday: Int = 0,          // 0=未指定
    val weekdaysText: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val colorTag: String = "",
    val note: String = "",
    val weekType: String = "",
    val onceWeek: Int = 0,
    val selectedWeeks: String = "",
    val selectedDates: String = "",
)

@Serializable
data class ThemeRoleColors(
    val lightHex: String = "",
    val darkHex: String = "",
)

/** 自定义主题：colors 中每个键对应一个 UI 角色（primary/background/surface/…）。 */
@Serializable
data class CustomTheme(
    val id: String = "",
    val name: String = "",
    val colors: Map<String, ThemeRoleColors> = emptyMap(),
)

/** .sthm 主题包：本质是 JSON，可包含多个主题。 */
@Serializable
data class ThemePackage(
    val format: String = "unitoolkit-theme",
    val version: Int = 1,
    val themes: List<CustomTheme> = emptyList(),
)

/** 根据公式把原始分数换算成绩点 */
fun GpaFormula.gradePointOf(score: Double): Double {
    return when (mode) {
        "percent5" -> ((score - 50.0) / 10.0).coerceIn(0.0, 5.0)
        "custom" -> mappings.sortedByDescending { it.min }
            .firstOrNull { score >= it.min }?.point ?: 0.0
        else -> when {
            score >= 90 -> 4.0
            score >= 85 -> 3.7
            score >= 82 -> 3.3
            score >= 78 -> 3.0
            score >= 75 -> 2.7
            score >= 72 -> 2.3
            score >= 68 -> 2.0
            score >= 64 -> 1.5
            score >= 60 -> 1.0
            else -> 0.0
        }
    }
}
