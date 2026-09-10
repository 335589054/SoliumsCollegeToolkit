package com.unitoolkit.ui.life

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
