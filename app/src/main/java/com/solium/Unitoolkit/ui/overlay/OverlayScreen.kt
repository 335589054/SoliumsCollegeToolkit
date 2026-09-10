package com.solium.Unitoolkit.ui.overlay

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.solium.Unitoolkit.core.model.OverlayConfig
import com.solium.Unitoolkit.core.utils.parseHexColor
import com.solium.Unitoolkit.service.OverlayManager
import com.solium.Unitoolkit.ui.components.PrimaryButton
import com.solium.Unitoolkit.ui.components.SimpleTopBar
import kotlinx.coroutines.delay

@Composable
fun OverlayScreen(navController: NavController, vm: OverlayViewModel = viewModel(factory = OverlayViewModel.Factory)) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val running by OverlayManager.running.collectAsState()
    var config by remember { mutableStateOf(OverlayConfig()) }
    var pickedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var initialized by remember { mutableStateOf(false) }

    // 直接读取 DataStore 最新配置恢复（图片路径已复制到应用私有目录）
    LaunchedEffect(Unit) {
        vm.load { loaded ->
            config = loaded
            if (loaded.imagePath.isNotBlank()) {
                runCatching { BitmapFactory.decodeFile(loaded.imagePath) }.getOrNull()?.let {
                    pickedBitmap = it.asImageBitmap()
                }
            }
            initialized = true
        }
    }
    // 离开页面时立即保存，避免修改还未落盘
    val latestConfig by rememberUpdatedState(config)
    DisposableEffect(Unit) {
        onDispose {
            if (initialized) vm.save(latestConfig)
        }
    }
    // 设置变更后自动保存（离开页面时也会再保存一次）
    LaunchedEffect(config) {
        if (initialized) {
            delay(350)
            vm.save(config)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val path = copyImageToInternal(context, uri)
            if (path != null) {
                config = config.copy(imagePath = path, x = 0, y = 0)
                pickedBitmap = runCatching { BitmapFactory.decodeFile(path).asImageBitmap() }.getOrNull()
            }
        }
    }

    val canDraw = Settings.canDrawOverlays(context)
    val screenWidthPx = context.resources.displayMetrics.widthPixels
    val widthDp = with(density) { if (config.width > 0) config.width.toDp() else screenWidthPx.toDp() }
    val heightDp = with(density) {
        if (config.height > 0) config.height.toDp() else (context.resources.displayMetrics.heightPixels * 0.2f).toInt().toDp()
    }

    if (running && canDraw) {
        LaunchedEffect(config.width, config.height, config.opacity, config.mode, config.colorHex, config.text, config.textColorHex, config.textSizePx, config.imageScale) {
            delay(200)
            OverlayManager.update(context, config)
        }
    }

    Scaffold(
        topBar = { SimpleTopBar(title = "音游上隐条", onBack = { navController.popBackStack() }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "上隐条用于遮挡游戏画面上方音符。支持图片、纯色或文字；图片与配置已保存，无需每次重新选择。注意：导出数据备份不会包含上隐条图片文件。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!canDraw) {
                PermissionCard(onRequest = {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
                    )
                })
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("内容类型", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("image" to "图片", "color" to "纯色", "text" to "文字").forEach { (value, label) ->
                        val selected = config.mode == value
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                .clickable { config = config.copy(mode = value) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                label,
                                fontSize = 13.sp,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            when (config.mode) {
                "color" -> {
                    ColorField(label = "颜色值", hex = config.colorHex, onHex = { config = config.copy(colorHex = it) })
                    Box(
                        Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(14.dp))
                            .background(runCatching { Color(android.graphics.Color.parseColor(config.colorHex)) }.getOrDefault(Color.Black)),
                    )
                }
                "text" -> {
                    OutlinedTextField(
                        value = config.text,
                        onValueChange = { config = config.copy(text = it) },
                        label = { Text("上隐条文字") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ColorField("文字颜色", config.textColorHex) { config = config.copy(textColorHex = it) }
                }
                else -> {
                    Text("上隐条图片", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center,
                    ) {
                        val bmp = pickedBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = "上隐条预览",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = if (config.imageScale == "crop") ContentScale.Crop else ContentScale.Fit,
                            )
                        } else {
                            Text("点击选择图片（PNG / JPG）", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("fit" to "拉伸填充", "crop" to "居中裁切").forEach { (value, label) ->
                            val selected = config.imageScale == value
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                    .clickable { config = config.copy(imageScale = value) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("尺寸与透明度", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                SliderRow("宽度", widthDp.value, 60f..screenWidthPx.toFloat() / density.density) { v ->
                    config = config.copy(width = with(density) { v.dp.roundToPx() })
                }
                SliderRow("高度", heightDp.value, 40f..(context.resources.displayMetrics.heightPixels.toFloat() / density.density * 0.5f)) { v ->
                    config = config.copy(height = with(density) { v.dp.roundToPx() })
                }
                SliderRow("透明度", config.opacity * 100f, 10f..100f) { v ->
                    config = config.copy(opacity = v / 100f)
                }
            }

            val canStart = when (config.mode) {
                "color" -> true
                "text" -> config.text.isNotBlank()
                else -> config.imagePath.isNotBlank()
            }
            if (running) {
                PrimaryButton("停止悬浮窗", onClick = { OverlayManager.stop(context) }, outlined = true)
            } else if (canStart) {
                PrimaryButton("启动悬浮窗", onClick = { if (canDraw) OverlayManager.start(context, config) })
            }
        }
    }
}

@Composable
private fun PermissionCard(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("需要「悬浮窗」权限", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
        Text("开启后即可在游戏画面之上叠加你的上隐条。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
        PrimaryButton("去开启权限", onClick = onRequest)
    }
}

@Composable
private fun ColorField(label: String, hex: String, onHex: (String) -> Unit) {
    OutlinedTextField(
        value = hex,
        onValueChange = onHex,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text(
                if (label == "透明度") "${value.toInt()}%" else "${value.toInt()} dp",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

/** 将所选图片复制到应用私有目录，返回可长期访问的文件路径 */
private fun copyImageToInternal(context: android.content.Context, uri: Uri): String? {
    return runCatching {
        val dir = context.filesDir.resolve("overlay").apply { mkdirs() }
        val outFile = dir.resolve("hide_bar.png")
        context.contentResolver.openInputStream(uri)?.use { input ->
            outFile.outputStream().use { output -> input.copyTo(output) }
        }
        outFile.absolutePath
    }.getOrNull()
}
