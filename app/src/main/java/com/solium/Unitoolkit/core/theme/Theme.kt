package com.solium.Unitoolkit.core.theme

import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import com.solium.Unitoolkit.core.model.CustomTheme

/** 按色调排序的 12 套主题；「爱上雷神」为彩蛋主题。 */
enum class ThemeKey(val key: String, val label: String) {
    INK("ink", "水墨"),
    LEMON("lemon", "淡黄"),
    ORANGE("orange", "橘色"),
    CORAL("coral", "珊瑚红"),
    PINK("pink", "粉色"),
    MOSS("moss", "墨绿"),
    MINT("mint", "薄荷"),
    TEAL("teal", "青色"),
    SKY("sky", "淡蓝"),
    DEEPBLUE("deepblue", "深蓝"),
    LAVENDER("lavender", "淡紫"),
    THOR("thor", "爱上雷神");

    companion object {
        fun fromKey(key: String?): ThemeKey = entries.firstOrNull { it.key == key } ?: INK
    }
}

enum class UiMode { SYSTEM, LIGHT, DARK }

data class AppPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
)

data class AppTheme(val light: AppPalette, val dark: AppPalette)

private fun C(v: Long) = Color(v)

private fun AppPalette.toColorScheme(): ColorScheme = lightColorScheme(
    primary = primary, onPrimary = onPrimary,
    primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = secondary, onSecondary = onSecondary,
    secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
    background = background, onBackground = onBackground,
    surface = surface, onSurface = onSurface,
    surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
    outline = outline,
)

private fun AppPalette.toDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = primary, onPrimary = onPrimary,
    primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = secondary, onSecondary = onSecondary,
    secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
    background = background, onBackground = onBackground,
    surface = surface, onSurface = onSurface,
    surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
    outline = outline,
)

object ThemePresets {
    private val ink = AppTheme(
        light = AppPalette(
            primary = C(0xFF111111), onPrimary = Color.White,
            primaryContainer = C(0xFFE4E4E4), onPrimaryContainer = C(0xFF111111),
            secondary = C(0xFF555555), onSecondary = Color.White,
            secondaryContainer = C(0xFFEDEDED), onSecondaryContainer = C(0xFF222222),
            background = Color.White, onBackground = C(0xFF111111),
            surface = Color.White, onSurface = C(0xFF111111),
            surfaceVariant = C(0xFFF3F3F3), onSurfaceVariant = C(0xFF4A4A4A), outline = C(0xFFD0D0D0),
        ),
        dark = AppPalette(
            primary = Color.White, onPrimary = C(0xFF111111),
            primaryContainer = C(0xFF333333), onPrimaryContainer = Color.White,
            secondary = C(0xFFBBBBBB), onSecondary = C(0xFF111111),
            secondaryContainer = C(0xFF2C2C2C), onSecondaryContainer = Color.White,
            background = C(0xFF111111), onBackground = Color.White,
            surface = C(0xFF1A1A1A), onSurface = Color.White,
            surfaceVariant = C(0xFF242424), onSurfaceVariant = C(0xFFC0C0C0), outline = C(0xFF3A3A3A),
        ),
    )

    private val lemon = AppTheme(
        light = AppPalette(
            primary = C(0xFFC9930A), onPrimary = Color.White,
            primaryContainer = C(0xFFF6E4A8), onPrimaryContainer = C(0xFF3A2E00),
            secondary = C(0xFF7A6A35), onSecondary = Color.White,
            secondaryContainer = C(0xFFF2E7C4), onSecondaryContainer = C(0xFF3A2E00),
            background = C(0xFFFDF8E8), onBackground = C(0xFF3A2E00),
            surface = C(0xFFFDF8E8), onSurface = C(0xFF3A2E00),
            surfaceVariant = C(0xFFF1E9CE), onSurfaceVariant = C(0xFF5C5334), outline = C(0xFFD8CB9A),
        ),
        dark = AppPalette(
            primary = C(0xFFE7C24A), onPrimary = C(0xFF2E2200),
            primaryContainer = C(0xFF5A4700), onPrimaryContainer = C(0xFFF6E4A8),
            secondary = C(0xFFCFBE7C), onSecondary = C(0xFF2E2200),
            secondaryContainer = C(0xFF4A3C08), onSecondaryContainer = C(0xFFF2E7C4),
            background = C(0xFF1C1808), onBackground = C(0xFFF2E6BE),
            surface = C(0xFF1C1808), onSurface = C(0xFFF2E6BE),
            surfaceVariant = C(0xFF2A2510), onSurfaceVariant = C(0xFFD2C48A), outline = C(0xFF4A411E),
        ),
    )

    private val orange = AppTheme(
        light = AppPalette(
            primary = C(0xFFE06614), onPrimary = Color.White,
            primaryContainer = C(0xFFF9D0AC), onPrimaryContainer = C(0xFF3F1E00),
            secondary = C(0xFF8A5A3A), onSecondary = Color.White,
            secondaryContainer = C(0xFFF4DDC8), onSecondaryContainer = C(0xFF3F1E00),
            background = C(0xFFFBF1E9), onBackground = C(0xFF3F1E00),
            surface = C(0xFFFBF1E9), onSurface = C(0xFF3F1E00),
            surfaceVariant = C(0xFFF2E2D2), onSurfaceVariant = C(0xFF5F4A3A), outline = C(0xFFDCC3AC),
        ),
        dark = AppPalette(
            primary = C(0xFFF0A06A), onPrimary = C(0xFF341000),
            primaryContainer = C(0xFF6B3300), onPrimaryContainer = C(0xFFF9D0AC),
            secondary = C(0xFFD5B08A), onSecondary = C(0xFF341000),
            secondaryContainer = C(0xFF523822), onSecondaryContainer = C(0xFFF4DDC8),
            background = C(0xFF201308), onBackground = C(0xFFF4E3D4),
            surface = C(0xFF201308), onSurface = C(0xFFF4E3D4),
            surfaceVariant = C(0xFF2E1F12), onSurfaceVariant = C(0xFFD7BCA6), outline = C(0xFF4E3A2A),
        ),
    )

    private val coral = AppTheme(
        light = AppPalette(
            primary = C(0xFFE0573A), onPrimary = Color.White,
            primaryContainer = C(0xFFF9CFC4), onPrimaryContainer = C(0xFF471100),
            secondary = C(0xFF9A5A4A), onSecondary = Color.White,
            secondaryContainer = C(0xFFF4DDD4), onSecondaryContainer = C(0xFF471100),
            background = C(0xFFFCF0EC), onBackground = C(0xFF471100),
            surface = C(0xFFFCF0EC), onSurface = C(0xFF471100),
            surfaceVariant = C(0xFFF3E0DA), onSurfaceVariant = C(0xFF67493E), outline = C(0xFFDEC4BA),
        ),
        dark = AppPalette(
            primary = C(0xFFF08D70), onPrimary = C(0xFF3C0800),
            primaryContainer = C(0xFF7E2A14), onPrimaryContainer = C(0xFFF9CFC4),
            secondary = C(0xFFD8B3A6), onSecondary = C(0xFF3C0800),
            secondaryContainer = C(0xFF5C3426), onSecondaryContainer = C(0xFFF4DDD4),
            background = C(0xFF24120C), onBackground = C(0xFFF6E4DC),
            surface = C(0xFF24120C), onSurface = C(0xFFF6E4DC),
            surfaceVariant = C(0xFF332019), onSurfaceVariant = C(0xFFDBC0B4), outline = C(0xFF56372C),
        ),
    )

    private val pink = AppTheme(
        light = AppPalette(
            primary = C(0xFFE9749B), onPrimary = Color.White,
            primaryContainer = C(0xFFFBDCE5), onPrimaryContainer = C(0xFF4A1C2E),
            secondary = C(0xFFEE9BB5), onSecondary = Color.White,
            secondaryContainer = C(0xFFFCE2EA), onSecondaryContainer = C(0xFF4A1C2E),
            background = C(0xFFFFF5F7), onBackground = C(0xFF4A1C2E),
            surface = C(0xFFFFF5F7), onSurface = C(0xFF4A1C2E),
            surfaceVariant = C(0xFFFBE9EE), onSurfaceVariant = C(0xFF5C3C47), outline = C(0xFFEBCBD5),
        ),
        dark = AppPalette(
            primary = C(0xFFF5A7C0), onPrimary = C(0xFF3A0A1B),
            primaryContainer = C(0xFF6B3A4B), onPrimaryContainer = C(0xFFFBDCE5),
            secondary = C(0xFFF3B7CB), onSecondary = C(0xFF3F0E20),
            secondaryContainer = C(0xFF70404F), onSecondaryContainer = C(0xFFFCE2EA),
            background = C(0xFF201318), onBackground = C(0xFFF6E0E7),
            surface = C(0xFF201318), onSurface = C(0xFFF6E0E7),
            surfaceVariant = C(0xFF2C1C22), onSurfaceVariant = C(0xFFD4B3BE), outline = C(0xFF4C353C),
        ),
    )

    private val moss = AppTheme(
        light = AppPalette(
            primary = C(0xFF3B6B4F), onPrimary = Color.White,
            primaryContainer = C(0xFFD3E8DA), onPrimaryContainer = C(0xFF1F2D25),
            secondary = C(0xFF7A947F), onSecondary = Color.White,
            secondaryContainer = C(0xFFE2EEDF), onSecondaryContainer = C(0xFF1F2D25),
            background = C(0xFFF4F7F4), onBackground = C(0xFF1F2D25),
            surface = C(0xFFF4F7F4), onSurface = C(0xFF1F2D25),
            surfaceVariant = C(0xFFE7EDE7), onSurfaceVariant = C(0xFF49554D), outline = C(0xFFC3CFC4),
        ),
        dark = AppPalette(
            primary = C(0xFF8FBF9F), onPrimary = C(0xFF0E1F15),
            primaryContainer = C(0xFF2A4C38), onPrimaryContainer = C(0xFFD3E8DA),
            secondary = C(0xFFA9C4AE), onSecondary = C(0xFF15241A),
            secondaryContainer = C(0xFF34483A), onSecondaryContainer = C(0xFFE2EEDF),
            background = C(0xFF12181E), onBackground = C(0xFFE0E9E2),
            surface = C(0xFF12181E), onSurface = C(0xFFE0E9E2),
            surfaceVariant = C(0xFF1E2822), onSurfaceVariant = C(0xFFB7C4BC), outline = C(0xFF3A4A40),
        ),
    )

    private val mint = AppTheme(
        light = AppPalette(
            primary = C(0xFF2E8B6A), onPrimary = Color.White,
            primaryContainer = C(0xFFC9F0DC), onPrimaryContainer = C(0xFF0C2A1E),
            secondary = C(0xFF6A9A84), onSecondary = Color.White,
            secondaryContainer = C(0xFFDCF2E6), onSecondaryContainer = C(0xFF0C2A1E),
            background = C(0xFFF0FAF4), onBackground = C(0xFF0C2A1E),
            surface = C(0xFFF0FAF4), onSurface = C(0xFF0C2A1E),
            surfaceVariant = C(0xFFE1F1E8), onSurfaceVariant = C(0xFF3F5B4C), outline = C(0xFFBED9CA),
        ),
        dark = AppPalette(
            primary = C(0xFF82D8AE), onPrimary = C(0xFF05301F),
            primaryContainer = C(0xFF1E5A41), onPrimaryContainer = C(0xFFC9F0DC),
            secondary = C(0xFFA2CCB4), onSecondary = C(0xFF0B2E1E),
            secondaryContainer = C(0xFF30483B), onSecondaryContainer = C(0xFFDCF2E6),
            background = C(0xFF0F1C16), onBackground = C(0xFFDBEFE3),
            surface = C(0xFF0F1C16), onSurface = C(0xFFDBEFE3),
            surfaceVariant = C(0xFF1A2A22), onSurfaceVariant = C(0xFFACC6B6), outline = C(0xFF34503E),
        ),
    )

    private val teal = AppTheme(
        light = AppPalette(
            primary = C(0xFF00838F), onPrimary = Color.White,
            primaryContainer = C(0xFFBEE9EE), onPrimaryContainer = C(0xFF002F34),
            secondary = C(0xFF5A9297), onSecondary = Color.White,
            secondaryContainer = C(0xFFD2EBEE), onSecondaryContainer = C(0xFF002F34),
            background = C(0xFFEEF8F8), onBackground = C(0xFF002F34),
            surface = C(0xFFEEF8F8), onSurface = C(0xFF002F34),
            surfaceVariant = C(0xFFDFECEC), onSurfaceVariant = C(0xFF3E5355), outline = C(0xFFB9D0D2),
        ),
        dark = AppPalette(
            primary = C(0xFF5AC7D1), onPrimary = C(0xFF00292E),
            primaryContainer = C(0xFF00616A), onPrimaryContainer = C(0xFFBEE9EE),
            secondary = C(0xFF9ACBD0), onSecondary = C(0xFF032E32),
            secondaryContainer = C(0xFF2E5357), onSecondaryContainer = C(0xFFD2EBEE),
            background = C(0xFF0C1B1C), onBackground = C(0xFFD4ECEE),
            surface = C(0xFF0C1B1C), onSurface = C(0xFFD4ECEE),
            surfaceVariant = C(0xFF17292A), onSurfaceVariant = C(0xFFA3C2C5), outline = C(0xFF2F4C4F),
        ),
    )

    private val sky = AppTheme(
        light = AppPalette(
            primary = C(0xFF5B9BD5), onPrimary = Color.White,
            primaryContainer = C(0xFFD5E8F7), onPrimaryContainer = C(0xFF1E3A52),
            secondary = C(0xFF7BA9CE), onSecondary = Color.White,
            secondaryContainer = C(0xFFE2EFF9), onSecondaryContainer = C(0xFF1E3A52),
            background = C(0xFFF2F7FC), onBackground = C(0xFF1E3A52),
            surface = C(0xFFF2F7FC), onSurface = C(0xFF1E3A52),
            surfaceVariant = C(0xFFE5EEF6), onSurfaceVariant = C(0xFF45586A), outline = C(0xFFC1D4E3),
        ),
        dark = AppPalette(
            primary = C(0xFF8FBDE6), onPrimary = C(0xFF0B2233),
            primaryContainer = C(0xFF2E506E), onPrimaryContainer = C(0xFFD5E8F7),
            secondary = C(0xFFA6CBE6), onSecondary = C(0xFF13283A),
            secondaryContainer = C(0xFF38566F), onSecondaryContainer = C(0xFFE2EFF9),
            background = C(0xFF0F1A24), onBackground = C(0xFFDBE8F2),
            surface = C(0xFF0F1A24), onSurface = C(0xFFDBE8F2),
            surfaceVariant = C(0xFF1B2A38), onSurfaceVariant = C(0xFFADC2D2), outline = C(0xFF374E60),
        ),
    )

    private val deepblue = AppTheme(
        light = AppPalette(
            primary = C(0xFF2F4FBF), onPrimary = Color.White,
            primaryContainer = C(0xFFD5E0FF), onPrimaryContainer = C(0xFF0B2160),
            secondary = C(0xFF6A7ECB), onSecondary = Color.White,
            secondaryContainer = C(0xFFE2E8FF), onSecondaryContainer = C(0xFF0B2160),
            background = C(0xFFF1F4FF), onBackground = C(0xFF0B2160),
            surface = C(0xFFF1F4FF), onSurface = C(0xFF0B2160),
            surfaceVariant = C(0xFFE3E8F8), onSurfaceVariant = C(0xFF3C4A78), outline = C(0xFFBBC6E8),
        ),
        dark = AppPalette(
            primary = C(0xFF8FA5FF), onPrimary = C(0xFF00134F),
            primaryContainer = C(0xFF20357E), onPrimaryContainer = C(0xFFD5E0FF),
            secondary = C(0xFFA4B2EF), onSecondary = C(0xFF112049),
            secondaryContainer = C(0xFF39457F), onSecondaryContainer = C(0xFFE2E8FF),
            background = C(0xFF0D1226), onBackground = C(0xFFDCE2FA),
            surface = C(0xFF0D1226), onSurface = C(0xFFDCE2FA),
            surfaceVariant = C(0xFF1A2137), onSurfaceVariant = C(0xFFA9B4DE), outline = C(0xFF34406E),
        ),
    )

    private val lavender = AppTheme(
        light = AppPalette(
            primary = C(0xFF9B7FB8), onPrimary = Color.White,
            primaryContainer = C(0xFFE9DFF3), onPrimaryContainer = C(0xFF3C2E4A),
            secondary = C(0xFFB49ACD), onSecondary = Color.White,
            secondaryContainer = C(0xFFEEE8F6), onSecondaryContainer = C(0xFF3C2E4A),
            background = C(0xFFF6F2F9), onBackground = C(0xFF3C2E4A),
            surface = C(0xFFF6F2F9), onSurface = C(0xFF3C2E4A),
            surfaceVariant = C(0xFFECE7F2), onSurfaceVariant = C(0xFF544A60), outline = C(0xFFD2C8DD),
        ),
        dark = AppPalette(
            primary = C(0xFFC4A6DE), onPrimary = C(0xFF241839),
            primaryContainer = C(0xFF53406B), onPrimaryContainer = C(0xFFE9DFF3),
            secondary = C(0xFFCBB3E3), onSecondary = C(0xFF2A1D3D),
            secondaryContainer = C(0xFF564875), onSecondaryContainer = C(0xFFEEE8F6),
            background = C(0xFF191327), onBackground = C(0xFFE8E0F2),
            surface = C(0xFF191327), onSurface = C(0xFFE8E0F2),
            surfaceVariant = C(0xFF241D33), onSurfaceVariant = C(0xFFC0B2D2), outline = C(0xFF443855),
        ),
    )

    private val thor = AppTheme(
        light = AppPalette(
            primary = C(0xFFF6BD00), onPrimary = C(0xFF422F00),
            primaryContainer = C(0xFFFFE57F), onPrimaryContainer = C(0xFF422F00),
            secondary = C(0xFF8B5CF6), onSecondary = Color.White,
            secondaryContainer = C(0xFFEDE3FF), onSecondaryContainer = C(0xFF2E1660),
            background = C(0xFFFFFBF2), onBackground = C(0xFF2E1F00),
            surface = C(0xFFFFFBF2), onSurface = C(0xFF2E1F00),
            surfaceVariant = C(0xFFF3EADD), onSurfaceVariant = C(0xFF5C5036), outline = C(0xFFD9C9A2),
        ),
        dark = AppPalette(
            primary = C(0xFFFFD52E), onPrimary = C(0xFF3A2C00),
            primaryContainer = C(0xFF6A5E00), onPrimaryContainer = C(0xFFFFE57F),
            secondary = C(0xFFC9A6FF), onSecondary = C(0xFF301A66),
            secondaryContainer = C(0xFF543390), onSecondaryContainer = C(0xFFEDE3FF),
            background = C(0xFF17120A), onBackground = C(0xFFF3E5C0),
            surface = C(0xFF17120A), onSurface = C(0xFFF3E5C0),
            surfaceVariant = C(0xFF252013), onSurfaceVariant = C(0xFFD2C49A), outline = C(0xFF4A4020),
        ),
    )

    private val themes = mapOf(
        ThemeKey.INK to ink,
        ThemeKey.LEMON to lemon,
        ThemeKey.ORANGE to orange,
        ThemeKey.CORAL to coral,
        ThemeKey.PINK to pink,
        ThemeKey.MOSS to moss,
        ThemeKey.MINT to mint,
        ThemeKey.TEAL to teal,
        ThemeKey.SKY to sky,
        ThemeKey.DEEPBLUE to deepblue,
        ThemeKey.LAVENDER to lavender,
        ThemeKey.THOR to thor,
    )

    fun paletteFor(key: ThemeKey): AppTheme = themes.getValue(key)
}

data class ThemeRoleDef(val key: String, val label: String)

val ThemeRoleDefs = listOf(
    ThemeRoleDef("primary", "主色 / 按钮"),
    ThemeRoleDef("onPrimary", "主色上的文字"),
    ThemeRoleDef("primaryContainer", "主色容器 / 选中背景"),
    ThemeRoleDef("onPrimaryContainer", "容器内文字"),
    ThemeRoleDef("secondary", "辅助色"),
    ThemeRoleDef("onSecondary", "辅助色文字"),
    ThemeRoleDef("secondaryContainer", "辅助容器"),
    ThemeRoleDef("onSecondaryContainer", "辅助容器文字"),
    ThemeRoleDef("background", "页面背景"),
    ThemeRoleDef("onBackground", "页面主文字"),
    ThemeRoleDef("surface", "卡片背景"),
    ThemeRoleDef("onSurface", "卡片文字 / 标题"),
    ThemeRoleDef("surfaceVariant", "分组 / 输入框背景"),
    ThemeRoleDef("onSurfaceVariant", "次要文字 / 标题说明"),
    ThemeRoleDef("outline", "边框 / 分隔线"),
)

fun AppPalette.roleColors(): Map<String, Color> = mapOf(
    "primary" to primary,
    "onPrimary" to onPrimary,
    "primaryContainer" to primaryContainer,
    "onPrimaryContainer" to onPrimaryContainer,
    "secondary" to secondary,
    "onSecondary" to onSecondary,
    "secondaryContainer" to secondaryContainer,
    "onSecondaryContainer" to onSecondaryContainer,
    "background" to background,
    "onBackground" to onBackground,
    "surface" to surface,
    "onSurface" to onSurface,
    "surfaceVariant" to surfaceVariant,
    "onSurfaceVariant" to onSurfaceVariant,
    "outline" to outline,
)

fun Color.toHexString(): String = "#%08X".format(toArgb())

private fun parseColorOr(hex: String?, alpha: Float, fallback: Color): Color {
    val base = hex?.takeIf { it.isNotBlank() }?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
        ?: fallback
    return base.copy(alpha = alpha.coerceIn(0f, 1f))
}

private fun rolePalette(
    theme: CustomTheme,
    default: AppPalette,
    light: Boolean,
): AppPalette {
    val defaults = default.roleColors()
    fun c(role: String): Color {
        val pair = theme.colors[role]
        val hex = if (light) pair?.lightHex else pair?.darkHex
        val alpha = if (light) pair?.lightAlpha ?: 1f else pair?.darkAlpha ?: 1f
        return parseColorOr(hex, alpha, defaults[role] ?: Color.Black)
    }
    return AppPalette(
        primary = c("primary"), onPrimary = c("onPrimary"),
        primaryContainer = c("primaryContainer"), onPrimaryContainer = c("onPrimaryContainer"),
        secondary = c("secondary"), onSecondary = c("onSecondary"),
        secondaryContainer = c("secondaryContainer"), onSecondaryContainer = c("onSecondaryContainer"),
        background = c("background"), onBackground = c("onBackground"),
        surface = c("surface"), onSurface = c("onSurface"),
        surfaceVariant = c("surfaceVariant"), onSurfaceVariant = c("onSurfaceVariant"),
        outline = c("outline"),
    )
}

fun CustomTheme.toAppTheme(default: AppTheme): AppTheme =
    AppTheme(
        light = rolePalette(this, default.light, light = true),
        dark = rolePalette(this, default.dark, light = false),
    )

fun AppTheme.toCustomTheme(id: String, name: String): CustomTheme {
    val light = light.roleColors()
    val dark = dark.roleColors()
    return CustomTheme(
        id = id,
        name = name,
        colors = ThemeRoleDefs.associate { role ->
            role.key to com.solium.Unitoolkit.core.model.ThemeRoleColors(
                lightHex = light[role.key]?.toHexString() ?: "",
                lightAlpha = 1f,
                darkHex = dark[role.key]?.toHexString() ?: "",
                darkAlpha = 1f,
            )
        },
    )
}

fun resolveTheme(id: String, customThemes: List<CustomTheme>): AppTheme {
    if (id.startsWith("custom:")) {
        return customThemes.firstOrNull { "custom:${it.id}" == id }?.toAppTheme(ThemePresets.paletteFor(ThemeKey.INK))
            ?: ThemePresets.paletteFor(ThemeKey.INK)
    }
    return ThemePresets.paletteFor(ThemeKey.fromKey(id))
}

/** 返回当前选中的自定义主题（仅当 themeId 指向 custom: 主题时返回）；否则 null 表示使用内置主题。 */
fun findCurrentCustomTheme(themeId: String, customThemes: List<CustomTheme>): CustomTheme? =
    if (themeId.startsWith("custom:")) customThemes.firstOrNull { "custom:${it.id}" == themeId } else null

/** 根据自定义主题搭建一个可见的 ColorScheme，用于“预览”等需要脱离当前主题色渲染的场景。 */
fun CustomTheme.buildColorScheme(
    dark: Boolean,
    default: AppTheme = ThemePresets.paletteFor(ThemeKey.INK),
): ColorScheme {
    val pal = toAppTheme(default)
    return if (dark) pal.dark.toDarkColorScheme() else pal.light.toColorScheme()
}

val LocalAppPalette = compositionLocalOf { ThemePresets.paletteFor(ThemeKey.INK) }

/** 当前生效的自定义主题扩展属性（背景图 / 透明度 / 分组等）；非自定义主题时为 null。 */
val LocalCustomTheme = compositionLocalOf<CustomTheme?> { null }

/** 把 base64 图片解码为 Bitmap；无效或空串时返回 null。 */
fun decodeBase64Image(base64: String?): Bitmap? {
    val data = base64?.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        val bytes = android.util.Base64.decode(data, android.util.Base64.NO_WRAP)
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

/** 背景图绘制修饰符：在内容最底层铺满绘制 base64 背景图（无图时保持不变）。 */
@Composable
fun Modifier.themeBackground(base64: String?, opacity: Float = 1f): Modifier {
    val bitmap = remember(base64) { decodeBase64Image(base64) }
    return if (bitmap == null) this
    else this.then(
        Modifier.drawBehind {
            val img = bitmap.asImageBitmap()
            drawImage(
                img,
                srcSize = IntSize(img.width, img.height),
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                alpha = opacity.coerceIn(0f, 1f),
                filterQuality = FilterQuality.Low,
            )
        }
    )
}

@Composable
fun UniToolkitTheme(
    appTheme: AppTheme = ThemePresets.paletteFor(ThemeKey.INK),
    uiMode: UiMode = UiMode.SYSTEM,
    customTheme: CustomTheme? = null,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (uiMode) {
        UiMode.SYSTEM -> systemDark
        UiMode.LIGHT -> false
        UiMode.DARK -> true
    }
    val palette = appTheme
    val scheme = if (dark) palette.dark.toDarkColorScheme() else palette.light.toColorScheme()
    CompositionLocalProvider(LocalAppPalette provides palette, LocalCustomTheme provides customTheme) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
