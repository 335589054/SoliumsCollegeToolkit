package com.solium.Unitoolkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val iconCache = HashMap<String, ImageVector>()

/** 按名称构建 lucide 风格画笔 SVG 的 ImageVector（统一色调由 tint 控制） */
fun lucideVector(name: String): ImageVector = iconCache.getOrPut(name) {
    val paths = lucidePaths[name] ?: lucidePaths.getValue("info")
    ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        paths.forEach { d ->
            addPath(
                pathData = addPathNodes(d),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
}

@Composable
fun AppIcon(
    name: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 24.dp,
) {
    val vector = remember(name) { lucideVector(name) }
    Icon(imageVector = vector, contentDescription = null, modifier = modifier.size(size), tint = tint)
}

/** 圆角底 + 图标，替代原 EmojiBadge 的图标版本 */
@Composable
fun AppIconBadge(
    name: String,
    size: Int = 24,
    background: Color = MaterialTheme.colorScheme.primaryContainer,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = Modifier
            .size((size + 16).dp)
            .clip(RoundedCornerShape((size / 2 + 8).dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(name = name, size = size.dp, tint = tint)
    }
}