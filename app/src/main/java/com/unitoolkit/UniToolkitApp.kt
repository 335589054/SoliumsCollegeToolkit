package com.unitoolkit

import android.app.Application
import com.unitoolkit.core.database.AppDatabase
import com.unitoolkit.core.storage.SettingsRepository
import com.unitoolkit.service.CourseNotifier
import com.unitoolkit.service.PomodoroManager
import com.unitoolkit.ui.widget.QuickEntryWidgetProvider
import com.unitoolkit.ui.widget.UniWidgetProvider

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
        UniWidgetProvider.refreshAll(this)
        QuickEntryWidgetProvider.refreshAll(this)
    }
}
