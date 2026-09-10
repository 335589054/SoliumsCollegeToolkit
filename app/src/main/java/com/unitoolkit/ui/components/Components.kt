package com.unitoolkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 扁平质感卡片：surface / 主题淡化色背景、24dp 圆角、几乎无阴影，可选描边突出边界 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    tinted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val bg = if (tinted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .then(
                if (tinted) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                        shape = shape,
                    )
                } else Modifier
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(18.dp),
    ) {
        Column(content = content, verticalArrangement = Arrangement.spacedBy(10.dp))
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTopBar(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
        actions = { trailing() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@Composable
fun EmojiBadge(emoji: String, size: Int = 24, background: Color = MaterialTheme.colorScheme.primaryContainer) {
    Box(
        modifier = Modifier
            .size((size + 16).dp)
            .clip(RoundedCornerShape((size / 2 + 8).dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = size.sp)
    }
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

@Composable
fun PriorityDot(priority: Int) {
    val c = when (priority) {
        2 -> Color(0xFFE9747B)
        1 -> Color(0xFFFBBF24)
        else -> Color(0xFF9AA4B0)
    }
    Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(c))
}

@Composable
fun PrimaryButton(label: String, onClick: () -> Unit, outlined: Boolean = false) {
    val bg = if (outlined) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
    val fg = if (outlined) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 13.dp),
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}