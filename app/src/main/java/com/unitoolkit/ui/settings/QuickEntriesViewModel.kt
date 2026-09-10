package com.unitoolkit.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unitoolkit.UniToolkitApp
import com.unitoolkit.core.database.LinkEntity
import com.unitoolkit.core.utils.appFactory
import com.unitoolkit.ui.widget.QuickEntryWidgetProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuickEntriesViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = (app as UniToolkitApp).settings
    private val linkDao = (app as UniToolkitApp).database.linkDao()

    val enabledTools: StateFlow<List<String>> =
        settings.quickTools.stateIn(viewModelScope, SharingStarted.Eagerly, settings.defaultQuickTools())

    val links: StateFlow<List<LinkEntity>> =
        linkDao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleTool(key: String) {
        val cur = enabledTools.value
        val next = if (key in cur) cur.filterNot { it == key } else cur + key
        viewModelScope.launch {
            settings.setQuickTools(next)
            QuickEntryWidgetProvider.refreshAll(getApplication())
        }
    }

    fun toggleLinkHome(link: LinkEntity) = viewModelScope.launch {
        linkDao.upsert(link.copy(isHomeCard = !link.isHomeCard))
        QuickEntryWidgetProvider.refreshAll(getApplication())
    }

    companion object { val Factory = appFactory(::QuickEntriesViewModel) }
}
