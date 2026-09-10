package com.unitoolkit.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unitoolkit.UniToolkitApp
import com.unitoolkit.core.database.BackupManager
import com.unitoolkit.core.utils.appFactory
import com.unitoolkit.core.utils.readTextFromUri
import com.unitoolkit.core.utils.writeTextToUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BackupViewModel(app: Application) : AndroidViewModel(app) {
    private val uniApp = app as UniToolkitApp
    private val manager = BackupManager(uniApp.database, uniApp.settings)
    private val settings = uniApp.settings

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun exportTo(uri: Uri) = viewModelScope.launch {
        runCatching {
            val json = manager.export()
            writeTextToUri(getApplication(), uri, json)
        }.onSuccess {
            val overlayImage = runCatching { settings.overlayConfig.first().imagePath.isNotBlank() }.getOrDefault(false)
            _message.value = if (overlayImage) "数据已导出（上隐条图片无法导出，需重新选择图片）" else "数据已导出"
        }
            .onFailure { _message.value = "导出失败：${it.message}" }
    }

    fun importFrom(uri: Uri, overwrite: Boolean) = viewModelScope.launch {
        runCatching {
            val text = readTextFromUri(getApplication(), uri)
            manager.import(text, overwrite)
        }.onSuccess { _message.value = if (overwrite) "覆盖导入完成" else "增量导入完成" }
            .onFailure { _message.value = "导入失败：${it.message}" }
    }

    fun clearAll() = viewModelScope.launch {
        runCatching { manager.clearAll() }
            .onSuccess { _message.value = "已清空全部数据" }
            .onFailure { _message.value = "清空失败：${it.message}" }
    }

    fun consumeMessage() { _message.value = null }

    companion object { val Factory = appFactory(::BackupViewModel) }
}
