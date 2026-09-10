package com.solium.Unitoolkit.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.solium.Unitoolkit.R
import com.solium.Unitoolkit.UniToolkitApp
import com.solium.Unitoolkit.core.theme.AppPalette
import com.solium.Unitoolkit.core.theme.UiMode
import com.solium.Unitoolkit.core.theme.resolveTheme
import com.solium.Unitoolkit.service.CourseNotifier
import com.solium.Unitoolkit.ui.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 4×2 圆角「下一节课」小组件：显示课程、开始时间与地点，点击进入课表。 */
class UniWidgetProvider : AppWidgetProvider() {

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
                val palette = appTheme.dark.takeIf { dark } ?: appTheme.light
                val semesterStart = app.settings.semesterStartDate.first()
                val fallbackWeek = app.settings.currentWeek.first()
                val courses = app.database.courseDao().all().first()
                val next = CourseNotifier.nextCourse(courses, System.currentTimeMillis(), semesterStart, fallbackWeek)

                appWidgetIds.forEach { id ->
                    val views = buildViews(context, palette, next)
                    val bgWidth = widgetDp(appWidgetManager, id, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
                    val bgHeight = widgetDp(appWidgetManager, id, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
                    views.setImageViewBitmap(
                        R.id.widget_bg,
                        WidgetRoundedBackground.create(context, bgWidth, bgHeight, palette.primaryContainer.toArgb()),
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

    private fun buildViews(context: Context, palette: AppPalette, next: CourseNotifier.Upcoming?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        val course = next?.course
        val courseText = course?.name ?: "暂无课程"
        val infoText = if (course != null) {
            if (course.location.isNotBlank()) "${course.startTime} · ${course.location}" else course.startTime
        } else {
            "点击查看课表"
        }
        views.setTextViewText(R.id.widget_course, courseText)
        views.setTextViewText(R.id.widget_info, infoText)
        views.setTextColor(R.id.widget_course, palette.primary.toArgb())
        views.setTextColor(R.id.widget_info, palette.primary.toArgb())
        views.setOnClickPendingIntent(
            R.id.widget_root,
            openRoutePendingIntent(context, Routes.COURSE, requestCode = 500),
        )
        return views
    }

    companion object {
        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, UniWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                val intent = Intent(context, UniWidgetProvider::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                context.sendBroadcast(intent)
            }
        }
    }
}

internal fun widgetDp(
    manager: AppWidgetManager,
    widgetId: Int,
    key: String,
    fallback: Int,
): Int {
    val options = manager.getAppWidgetOptions(widgetId)
    return options.getInt(key, -1).takeIf { it > 0 } ?: fallback
}
