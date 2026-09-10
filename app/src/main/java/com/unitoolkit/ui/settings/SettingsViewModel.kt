package com.unitoolkit.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unitoolkit.UniToolkitApp
import com.unitoolkit.core.model.CustomTheme
import com.unitoolkit.core.model.ThemePackage
import com.unitoolkit.core.theme.ThemeKey
import com.unitoolkit.core.theme.UiMode
import com.unitoolkit.core.utils.appFactory
import com.unitoolkit.core.utils.readTextFromUri
import com.unitoolkit.core.utils.writeTextToUri
import com.unitoolkit.ui.widget.QuickEntryWidgetProvider
import com.unitoolkit.ui.widget.UniWidgetProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = (app as UniToolkitApp).settings

    val themeId: StateFlow<String> =
        settings.themeId.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeKey.INK.key)
    val customThemes: StateFlow<List<CustomTheme>> =
        settings.customThemes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val uiMode: StateFlow<UiMode> =
        settings.uiMode.stateIn(viewModelScope, SharingStarted.Eagerly, UiMode.SYSTEM)

    fun selectTheme(id: String) = viewModelScope.launch {
        settings.setThemeId(id)
        UniWidgetProvider.refreshAll(getApplication())
        QuickEntryWidgetProvider.refreshAll(getApplication())
    }

    fun saveCustomTheme(theme: CustomTheme) = viewModelScope.launch {
        val current = settings.customThemes.first()
        val id = theme.id.ifBlank { System.currentTimeMillis().toString() }
        settings.setCustomThemes(
            if (theme.id.isBlank()) current + theme.copy(id = id)
            else current.map { if (it.id == id) theme else it },
        )
        settings.setThemeId("custom:$id")
    }

    fun deleteCustomTheme(id: String) = viewModelScope.launch {
        val current = settings.customThemes.first()
        val next = current.filterNot { it.id == id }
        settings.setCustomThemes(next)
        if (settings.themeId.first() == "custom:$id") settings.setThemeId(ThemeKey.INK.key)
        UniWidgetProvider.refreshAll(getApplication())
        QuickEntryWidgetProvider.refreshAll(getApplication())
    }

    fun importCustomThemes(themes: List<CustomTheme>) = viewModelScope.launch {
        mergeAndSelectThemes(themes)
    }

    fun importCustomThemesFromUri(uri: Uri) = viewModelScope.launch {
        runCatching {
            val raw = readTextFromUri(getApplication(), uri)
            val pkg = Json { ignoreUnknownKeys = true }.decodeFromString(ThemePackage.serializer(), raw)
            mergeAndSelectThemes(pkg.themes)
        }
    }

    fun exportCustomThemesToUri(uri: Uri) = viewModelScope.launch {
        val themes = settings.customThemes.first()
        val pkg = ThemePackage(themes = themes)
        val raw = Json { prettyPrint = true; encodeDefaults = true }.encodeToString(ThemePackage.serializer(), pkg)
        writeTextToUri(getApplication(), uri, raw)
    }

    private suspend fun mergeAndSelectThemes(themes: List<CustomTheme>) {
        if (themes.isEmpty()) return
        val current = settings.customThemes.first()
        val merged = current.toMutableList()
        themes.forEach { incoming ->
            val idx = merged.indexOfFirst { it.id == incoming.id }
            if (idx >= 0) merged[idx] = incoming else merged.add(incoming)
        }
        settings.setCustomThemes(merged)
        settings.setThemeId("custom:${themes.first().id}")
        UniWidgetProvider.refreshAll(getApplication())
        QuickEntryWidgetProvider.refreshAll(getApplication())
    }

    fun setUiMode(mode: UiMode) = viewModelScope.launch {
        settings.setUiMode(mode)
        UniWidgetProvider.refreshAll(getApplication())
        QuickEntryWidgetProvider.refreshAll(getApplication())
    }

    companion object { val Factory = appFactory(::SettingsViewModel) }
}
