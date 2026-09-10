package com.solium.Unitoolkit.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

/** 从应用内部保存的路径加载本地图片；文件不存在或为空时不绘制。 */
@Composable
fun LocalFileImage(
    path: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val bitmap: ImageBitmap? by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            val p = path ?: return@withContext null
            if (p.isBlank() || !File(p).exists()) null else BitmapFactory.decodeFile(p)?.asImageBitmap()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = contentDescription,
            modifier = modifier,
        )
    }
}

/** 采样解码 Uri 图片，长边不超过 maxDimension，降低内存占用。失败返回 null。 */
suspend fun decodeSampledBitmap(
    context: Context,
    uri: Uri,
    maxDimension: Int,
): Bitmap? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: return@withContext null
    var sample = 1
    val maxSide = max(bounds.outWidth, bounds.outHeight)
    while (maxSide / (sample * 2) >= maxDimension && sample < 16) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
}

/** 基于 Compose 的正方形裁剪对话框，支持拖动裁剪框。确认回调返回裁剪后的 Bitmap。 */
@Composable
fun CropDialog(
    bitmap: ImageBitmap,
    title: String,
    onConfirm: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
) {
    val bmpW = bitmap.width.toFloat()
    val bmpH = bitmap.height.toFloat()
    val side = remember(bmpW, bmpH) { min(bmpW, bmpH) }
    var cropLeft by remember(bmpW, bmpH) { mutableStateOf(((bmpW - side) / 2f).coerceAtLeast(0f)) }
    var cropTop by remember(bmpW, bmpH) { mutableStateOf(((bmpH - side) / 2f).coerceAtLeast(0f)) }
    val density = LocalDensity.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(20.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                        .clipToBounds(),
                ) {
                    val avW = with(density) { maxWidth.toPx() }
                    val avH = with(density) { maxHeight.toPx() }
                    val scale = min(avW / bmpW, avH / bmpH)
                    val drawW = bmpW * scale
                    val drawH = bmpH * scale
                    val offX = (avW - drawW) / 2f
                    val offY = (avH - drawH) / 2f
                    val rectLeft = offX + cropLeft * scale
                    val rectTop = offY + cropTop * scale
                    val rectSide = side * scale
                    val dragScale = scale
                    val scrim = Color.Black.copy(alpha = 0.55f)
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(bmpW, bmpH) {
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    val nx = (cropLeft + drag.x / dragScale)
                                        .coerceIn(0f, (bmpW - side).coerceAtLeast(0f))
                                    val ny = (cropTop + drag.y / dragScale)
                                        .coerceIn(0f, (bmpH - side).coerceAtLeast(0f))
                                    cropLeft = nx
                                    cropTop = ny
                                }
                            },
                    ) {
                        drawImage(
                            bitmap,
                            dstOffset = IntOffset(offX.toInt(), offY.toInt()),
                            dstSize = IntSize(drawW.toInt(), drawH.toInt()),
                        )
                        if (rectTop > 0f) drawRect(scrim, topLeft = Offset(0f, 0f), size = Size(size.width, rectTop))
                        if (rectTop + rectSide < size.height) {
                            drawRect(scrim, topLeft = Offset(0f, rectTop + rectSide), size = Size(size.width, size.height - rectTop - rectSide))
                        }
                        if (rectLeft > 0f) drawRect(scrim, topLeft = Offset(0f, rectTop), size = Size(rectLeft, rectSide))
                        if (rectLeft + rectSide < size.width) {
                            drawRect(scrim, topLeft = Offset(rectLeft + rectSide, rectTop), size = Size(size.width - rectLeft - rectSide, rectSide))
                        }
                        drawRect(
                            Color.White,
                            topLeft = Offset(rectLeft, rectTop),
                            size = Size(rectSide, rectSide),
                            style = Stroke(width = 3f),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消（保存原图）") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val cropped = Bitmap.createBitmap(
                            bitmap.asAndroidBitmap(),
                            cropLeft.toInt(),
                            cropTop.toInt(),
                            side.toInt(),
                            side.toInt(),
                        )
                        onConfirm(cropped)
                    }) { Text("保存裁剪") }
                }
            }
        }
    }
}
