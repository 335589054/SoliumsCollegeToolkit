package com.unitoolkit.ui.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.unitoolkit.MainActivity
import com.unitoolkit.R
import com.unitoolkit.UniToolkitApp
import com.unitoolkit.core.theme.AppPalette
import com.unitoolkit.core.theme.UiMode
import com.unitoolkit.core.theme.resolveTheme
import com.unitoolkit.ui.home.quickTools
import com.unitoolkit.ui.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val MAX_ENTRIES = 4
private val entryViewIds = intArrayOf(
    R.id.quick_entry_0,
    R.id.quick_entry_1,
    R.id.quick_entry_2,
    R.id.quick_entry_3,
)

private val entryIconIds = intArrayOf(
    R.id.quick_entry_0_icon,
    R.id.quick_entry_1_icon,
    R.id.quick_entry_2_icon,
    R.id.quick_entry_3_icon,
)

private data class QuickEntryView(val icon: String, val route: String)

/** 2×2 圆角快捷入口小组件：展示设置中勾选的内置工具与便捷链接。 */
class QuickEntryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val app = context.applicationContext as UniToolkitApp
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val themeId = app.settings.themeId.first()
                val customThemes = app.settings.customThemes.first()
                val uiMode = app.settings.uiMode.first()
                val dark = uiMode == UiMode.DARK
                val appTheme = resolveTheme(themeId, customThemes)
                val colors = if (dark) appTheme.dark else appTheme.light
                val enabledTools = app.settings.quickTools.first().toSet()
                val homeLinks = app.database.linkDao().homeCards().first()
                val entries = buildEntries(enabledTools, homeLinks)

                appWidgetIds.forEach { id ->
                    val views = buildViews(context, colors, entries)
                    val bgWidth = widgetDp(appWidgetManager, id, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
                    val bgHeight = widgetDp(appWidgetManager, id, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
                    views.setImageViewBitmap(
                        R.id.quick_entry_bg,
                        WidgetRoundedBackground.create(context, bgWidth, bgHeight, colors.primaryContainer.toArgb()),
                    )
                    appWidgetManager.updateAppWidget(id, views)
                }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    private fun buildEntries(enabledTools: Set<String>, homeLinks: List<com.unitoolkit.core.database.LinkEntity>): List<QuickEntryView> {
        val tools = quickTools.filter { it.key in enabledTools }.map { QuickEntryView(it.icon, it.route) }
        val links = homeLinks.map { link ->
            val icon = link.icon.ifBlank { "link" }
            QuickEntryView(icon, Routes.webview(link.name, link.url))
        }
        val entries = (tools + links).take(MAX_ENTRIES)
        return if (entries.isEmpty()) {
            listOf(QuickEntryView("layout-grid", Routes.QUICK_ENTRIES))
        } else {
            entries
        }
    }

    private fun buildViews(context: Context, palette: AppPalette, entries: List<QuickEntryView>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.quick_entry_widget_layout)
        val iconColor = palette.primary.toArgb()
        val iconPx = (22 * context.resources.displayMetrics.density).roundToInt()

        entries.forEachIndexed { index, entry ->
            if (index >= MAX_ENTRIES) return@forEachIndexed
            val viewId = entryViewIds[index]
            views.setImageViewBitmap(
                entryIconIds[index],
                tintedIconBitmap(context, widgetIconResource(entry.icon), iconColor, iconPx),
            )
            views.setViewVisibility(viewId, View.VISIBLE)
            views.setOnClickPendingIntent(
                viewId,
                openRoutePendingIntent(context, entry.route, requestCode = 1000 + index),
            )
        }
        for (index in entries.size until MAX_ENTRIES) {
            views.setViewVisibility(entryViewIds[index], View.INVISIBLE)
        }
        return views
    }

    private fun tintedIconBitmap(context: Context, resId: Int, color: Int, sizePx: Int): Bitmap {
        val size = sizePx.coerceAtLeast(4)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val drawable = context.getDrawable(resId) ?: return bitmap
        drawable.setTint(color)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    private fun widgetIconResource(iconName: String): Int = when (iconName) {
        "calendar-days" -> R.drawable.widget_icon_calendar_days
        "pen-line" -> R.drawable.widget_icon_pen_line
        "square-check" -> R.drawable.widget_icon_square_check
        "timer" -> R.drawable.widget_icon_timer
        "wallet" -> R.drawable.widget_icon_wallet
        "zap" -> R.drawable.widget_icon_zap
        "calendar-clock" -> R.drawable.widget_icon_calendar_clock
        "graduation-cap" -> R.drawable.widget_icon_graduation_cap
        "shopping-cart" -> R.drawable.widget_icon_shopping_cart
        "music" -> R.drawable.widget_icon_music
        else -> R.drawable.widget_icon_link
    }

    companion object {
        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, QuickEntryWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                val intent = Intent(context, QuickEntryWidgetProvider::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                context.sendBroadcast(intent)
            }
        }
    }
}

internal fun openRoutePendingIntent(context: Context, route: String, requestCode: Int): PendingIntent {
    val intent = Intent(context, MainActivity::class.java)
        .setAction("com.unitoolkit.action.OPEN_QUICK_ENTRY")
        .putExtra(MainActivity.EXTRA_NAV_ROUTE, route)
    return PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
