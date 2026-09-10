package com.solium.Unitoolkit.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solium.Unitoolkit.UniToolkitApp
import com.solium.Unitoolkit.core.database.LinkEntity
import com.solium.Unitoolkit.core.utils.appFactory
import com.solium.Unitoolkit.ui.widget.QuickEntryWidgetProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuickEntriesViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = (app as UniToolkitApp).settings
    private val linkDao = (app as UniToolkitApp).database.linkDao()

    /** 首页快捷入口（工具），按展示顺序存储。 */
    val homeTools: StateFlow<List<String>> =
        settings.quickTools.stateIn(viewModelScope, SharingStarted.Eagerly, settings.defaultQuickTools())

    /** 桌面小组件快捷入口（工具），独立于首页，按展示顺序存储。 */
    val widgetTools: StateFlow<List<String>> =
        settings.widgetQuickTools.stateIn(viewModelScope, SharingStarted.Eagerly, settings.defaultQuickTools())

    val links: StateFlow<List<LinkEntity>> =
        linkDao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleTool(key: String) {
        val cur = homeTools.value
        val next = if (key in cur) cur.filterNot { it == key } else cur + key
        viewModelScope.launch { settings.setQuickTools(next) }
    }

    fun toggleWidgetTool(key: String) {
        val cur = widgetTools.value
        val next = if (key in cur) cur.filterNot { it == key } else cur + key
        viewModelScope.launch {
            settings.setWidgetQuickTools(next)
            QuickEntryWidgetProvider.refreshAll(getApplication())
        }
    }

    /** 首页快捷入口排序（±1）。 */
    fun moveTool(key: String, delta: Int) = reorder(homeTools.value, key, delta) { settings.setQuickTools(it) }

    /** 桌面小组件快捷入口排序（±1）。 */
    fun moveWidgetTool(key: String, delta: Int) {
        reorder(widgetTools.value, key, delta) {
            settings.setWidgetQuickTools(it)
            QuickEntryWidgetProvider.refreshAll(getApplication())
        }
    }

    private fun reorder(list: List<String>, key: String, delta: Int, save: suspend (List<String>) -> Unit) {
        val cur = list.toMutableList()
        val idx = cur.indexOf(key)
        val target = idx + delta
        if (idx == -1 || target < 0 || target >= cur.size) return
        val tmp = cur[idx]; cur[idx] = cur[target]; cur[target] = tmp
        viewModelScope.launch { save(cur) }
    }

    fun toggleLinkHome(link: LinkEntity) = viewModelScope.launch {
        linkDao.upsert(link.copy(isHomeCard = !link.isHomeCard))
        QuickEntryWidgetProvider.refreshAll(getApplication())
    }

    companion object { val Factory = appFactory(::QuickEntriesViewModel) }
}