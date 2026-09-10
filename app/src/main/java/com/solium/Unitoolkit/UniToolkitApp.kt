package com.solium.Unitoolkit

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.InvalidationTracker
import com.solium.Unitoolkit.core.database.AppDatabase
import com.solium.Unitoolkit.core.database.BackupManager
import com.solium.Unitoolkit.core.model.ThemePackage
import com.solium.Unitoolkit.core.storage.SettingsRepository
import com.solium.Unitoolkit.core.utils.readTextFromUri
import com.solium.Unitoolkit.service.CourseNotifier
import com.solium.Unitoolkit.service.PomodoroManager
import com.solium.Unitoolkit.ui.widget.QuickEntryWidgetProvider
import com.solium.Unitoolkit.ui.widget.UniWidgetProvider
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class UniToolkitApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var settings: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.get(this)
        settings = SettingsRepository(this)
        PomodoroManager.init(this, database)
        CourseNotifier.scheduleAfter(this, System.currentTimeMillis())
        // 课程数据变化（新增/编辑/删除/备份导入/清空）时主动刷新「下一节课」小组件，
        // 避免依赖 30 分钟的 updatePeriodMillis 而导致显示过期数据。
        database.invalidationTracker.addObserver(
            object : InvalidationTracker.Observer(arrayOf("courses")) {
                override fun onInvalidated(tables: Set<String>) {
                    UniWidgetProvider.refreshAll(this@UniToolkitApp)
                    QuickEntryWidgetProvider.refreshAll(this@UniToolkitApp)
                }
            },
        )
        UniWidgetProvider.refreshAll(this)
        QuickEntryWidgetProvider.refreshAll(this)
    }

    /** 通过系统「打开方式」打开 .sthm 主题包：按文件名分组合并导入。 */
    suspend fun importThemePackageFromUri(uri: Uri) {
        runCatching {
            val raw = readTextFromUri(this, uri)
            var pkg = Json { ignoreUnknownKeys = true }.decodeFromString(ThemePackage.serializer(), raw)
            val fileGroup = displayNameOf(uri).ifBlank { pkg.name }
            if (fileGroup.isNotBlank()) pkg = pkg.copy(name = fileGroup)
            val incoming = if (fileGroup.isNotBlank()) pkg.themes.map { it.copy(group = fileGroup) } else pkg.themes
            if (incoming.isEmpty()) return@runCatching
            val current = settings.customThemes.first()
            val merged = current.toMutableList()
            incoming.forEach { t ->
                val idx = merged.indexOfFirst { it.id == t.id }
                if (idx >= 0) merged[idx] = t else merged.add(t)
            }
            settings.setCustomThemes(merged)
            settings.setThemeId("custom:${incoming.first().id}")
        }
        UniWidgetProvider.refreshAll(this)
        QuickEntryWidgetProvider.refreshAll(this)
    }

    /** 通过系统「打开方式」打开 .stbc 备份：增量导入并内嵌图片资源。 */
    suspend fun importBackupFromUri(uri: Uri) {
        runCatching {
            val raw = readTextFromUri(this, uri)
            BackupManager(this, database, settings).import(raw, overwrite = false)
        }
        UniWidgetProvider.refreshAll(this)
        QuickEntryWidgetProvider.refreshAll(this)
    }

    /** 读取 Content Uri 的文件显示名（去掉扩展名）。 */
    private fun displayNameOf(uri: Uri): String {
        return runCatching {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            } ?: ""
        }.getOrNull()?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: ""
    }
}
