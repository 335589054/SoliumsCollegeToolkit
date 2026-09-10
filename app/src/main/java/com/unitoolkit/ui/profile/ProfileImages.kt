package com.unitoolkit.ui.profile

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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
