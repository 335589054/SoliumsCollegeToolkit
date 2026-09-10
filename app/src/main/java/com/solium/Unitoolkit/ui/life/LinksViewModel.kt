package com.solium.Unitoolkit.ui.life

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

class LinksViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as UniToolkitApp).database.linkDao()

    val links: StateFlow<List<LinkEntity>> =
        dao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun upsert(link: LinkEntity) = viewModelScope.launch {
        dao.upsert(link)
        QuickEntryWidgetProvider.refreshAll(getApplication())
    }
    fun delete(link: LinkEntity) = viewModelScope.launch {
        dao.delete(link)
        QuickEntryWidgetProvider.refreshAll(getApplication())
    }

    companion object { val Factory = appFactory(::LinksViewModel) }
}
