package com.solium.Unitoolkit.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solium.Unitoolkit.UniToolkitApp
import com.solium.Unitoolkit.core.database.BackupManager
import com.solium.Unitoolkit.core.utils.appFactory
import com.solium.Unitoolkit.core.utils.readTextFromUri
import com.solium.Unitoolkit.core.utils.writeTextToUri
import com.solium.Unitoolkit.ui.widget.UniWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BackupViewModel(app: Application) : AndroidViewModel(app) {
    private val uniApp = app as UniToolkitApp
    private val manager = BackupManager(uniApp, uniApp.database, uniApp.settings)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun exportTo(uri: Uri) = viewModelScope.launch {
        runCatching {
            val json = manager.export()
            writeTextToUri(getApplication(), uri, json)
        }.onSuccess { _message.value = "数据已导出（图片已内嵌，可跨设备迁移）" }
            .onFailure { _message.value = "导出失败：${it.message}" }
    }

    fun importFrom(uri: Uri, overwrite: Boolean) = viewModelScope.launch {
        runCatching {
            val text = readTextFromUri(getApplication(), uri)
            manager.import(text, overwrite)
        }.onSuccess { _message.value = if (overwrite) "覆盖导入完成" else "增量导入完成" }
            .onFailure { _message.value = "导入失败：${it.message}" }
        // 导入会改写课程等数据，主动刷新「下一节课」小组件
        if (_message.value?.startsWith("导入") == true) UniWidgetProvider.refreshAll(getApplication())
    }

    fun clearAll() = viewModelScope.launch {
        runCatching { manager.clearAll() }
            .onSuccess {
                _message.value = "已清空全部数据"
                UniWidgetProvider.refreshAll(getApplication())
            }
            .onFailure { _message.value = "清空失败：${it.message}" }
    }

    fun consumeMessage() { _message.value = null }

    companion object { val Factory = appFactory(::BackupViewModel) }
}
