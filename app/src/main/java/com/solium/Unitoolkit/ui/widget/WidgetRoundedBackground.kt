package com.solium.Unitoolkit.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.roundToInt

/** 生成一张带圆角的纯色位图，作为小组件背景（可继续跟随主题配色）。 */
internal object WidgetRoundedBackground {

    fun create(context: Context, widthDp: Int, heightDp: Int, color: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val width = (widthDp.coerceAtLeast(1) * density).roundToInt()
        val height = (heightDp.coerceAtLeast(1) * density).roundToInt()
        val radius = (22 * density).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val translucentColor = (color and 0x00FFFFFF) or 0xA6000000.toInt()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = translucentColor }
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius.toFloat(), radius.toFloat(), paint)
        return bitmap
    }
}
