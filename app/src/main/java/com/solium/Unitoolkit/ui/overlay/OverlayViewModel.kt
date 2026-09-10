package com.solium.Unitoolkit.ui.overlay

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solium.Unitoolkit.UniToolkitApp
import com.solium.Unitoolkit.core.model.OverlayConfig
import com.solium.Unitoolkit.core.utils.appFactory
import com.solium.Unitoolkit.service.OverlayManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OverlayViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = (app as UniToolkitApp).settings

    val config: StateFlow<OverlayConfig> =
        settings.overlayConfig.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverlayConfig())

    fun save(cfg: OverlayConfig) = viewModelScope.launch {
        settings.setOverlayConfig(cfg)
        OverlayManager.config = cfg
    }

    /** 直接等待 DataStore 的最新值，避免重进时只读到默认空配置。 */
    fun load(onLoaded: (OverlayConfig) -> Unit) = viewModelScope.launch {
        onLoaded(settings.overlayConfig.first())
    }

    companion object { val Factory = appFactory(::OverlayViewModel) }
}
