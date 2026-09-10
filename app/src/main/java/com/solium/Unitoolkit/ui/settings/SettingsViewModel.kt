package com.solium.Unitoolkit.ui.settings

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solium.Unitoolkit.UniToolkitApp
import com.solium.Unitoolkit.core.model.CustomTheme
import com.solium.Unitoolkit.core.model.ThemePackage
import com.solium.Unitoolkit.core.theme.ThemeKey
import com.solium.Unitoolkit.core.theme.UiMode
import com.solium.Unitoolkit.core.utils.appFactory
import com.solium.Unitoolkit.core.utils.readTextFromUri
import com.solium.Unitoolkit.core.utils.writeTextToUri
import com.solium.Unitoolkit.ui.widget.QuickEntryWidgetProvider
import com.solium.Unitoolkit.ui.widget.UniWidgetProvider
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
    val themeGroupOrder: StateFlow<List<String>> =
        settings.themeGroupOrder.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
            var pkg = Json { ignoreUnknownKeys = true }.decodeFromString(ThemePackage.serializer(), raw)
            // 按文件名自动分组（去掉扩展名），优先级高于包内 name
            val fileGroup = displayNameOf(uri).ifBlank { pkg.name }
            if (fileGroup.isNotBlank()) pkg = pkg.copy(name = fileGroup)
            val withGroup = if (fileGroup.isNotBlank()) {
                pkg.themes.map { it.copy(group = fileGroup) }
            } else pkg.themes
            mergeAndSelectThemes(withGroup)
        }
    }

    /** 读取 Content Uri 的文件显示名（去掉扩展名，用于按文件名分组）。 */
    private fun displayNameOf(uri: Uri): String {
        return runCatching {
            getApplication<UniToolkitApp>().contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            } ?: ""
        }.getOrNull()?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: ""
    }

    /** 重命名分组：把所有属于该分组的主题的 group 改为新名称。 */
    fun renameGroup(from: String, to: String) = viewModelScope.launch {
        val target = to.trim()
        if (from == target || target.isBlank()) return@launch
        val current = settings.customThemes.first()
        settings.setCustomThemes(current.map { if (it.group == from && it.group != target) it.copy(group = target) else it })
        val order = settings.themeGroupOrder.first().map { if (it == from) target else it }
        if (from in settings.themeGroupOrder.first()) settings.setThemeGroupOrder(order)
    }

    /** 新建分组：登记到分组顺序中（可先建空分组再把主题移入）。 */
    fun createGroup(name: String) = viewModelScope.launch {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return@launch
        val order = settings.themeGroupOrder.first().toMutableList()
        if (trimmed !in order) order.add(trimmed)
        settings.setThemeGroupOrder(order)
    }

    /** 调整分组的显示顺序（±1）。 */
    fun moveGroup(group: String, delta: Int) = viewModelScope.launch {
        val themes = settings.customThemes.first()
        val ordered = groupedOrderedItems(themes, settings.themeGroupOrder.first()).keys.filter { it.isNotBlank() }.toMutableList()
        val idx = ordered.indexOf(group)
        val target = idx + delta
        if (idx == -1 || target < 0 || target >= ordered.size) return@launch
        val tmp = ordered[idx]; ordered[idx] = ordered[target]; ordered[target] = tmp
        settings.setThemeGroupOrder(ordered)
    }

    /** 修改某个主题所属的分组。 */
    fun moveThemeToGroup(id: String, newGroup: String) = viewModelScope.launch {
        val trimmed = newGroup.trim()
        val current = settings.customThemes.first()
        settings.setCustomThemes(current.map { if (it.id == id) it.copy(group = trimmed) else it })
        if (trimmed.isNotBlank()) {
            val order = settings.themeGroupOrder.first().toMutableList()
            if (trimmed !in order) order.add(trimmed)
            settings.setThemeGroupOrder(order)
        }
    }

    /** 删除分组：删除该分组内的全部主题。 */
    fun deleteGroup(group: String) = viewModelScope.launch {
        val current = settings.customThemes.first()
        val removing = current.filter { it.group == group }.map { it.id }.toSet()
        val next = current.filterNot { it.group == group }
        settings.setCustomThemes(next)
        if (removing.contains(settings.themeId.first().removePrefix("custom:"))) settings.setThemeId(ThemeKey.INK.key)
        UniWidgetProvider.refreshAll(getApplication())
        QuickEntryWidgetProvider.refreshAll(getApplication())
    }

    /** 导出单个分组为主题包（.sthm）。 */
    fun exportGroupToUri(uri: Uri, group: String) = viewModelScope.launch {
        val themes = settings.customThemes.first().filter { it.group == group }
        if (themes.isEmpty()) return@launch
        val pkg = ThemePackage(name = group.ifBlank { "自定义主题" }, themes = themes)
        val raw = Json { prettyPrint = true; encodeDefaults = true }.encodeToString(ThemePackage.serializer(), pkg)
        writeTextToUri(getApplication(), uri, raw)
    }

    fun exportCustomThemesToUri(uri: Uri) = viewModelScope.launch {
        val themes = settings.customThemes.first()
        val pkgName = themes.asSequence().map { it.group }.firstOrNull { it.isNotBlank() } ?: "自定义主题"
        val pkg = ThemePackage(name = pkgName, themes = themes)
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
