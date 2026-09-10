package com.unitoolkit.ui.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unitoolkit.UniToolkitApp
import com.unitoolkit.core.model.PersonalProfile
import com.unitoolkit.core.utils.appFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ProfileImageKind { AVATAR, QQ_QR, WECHAT_QR }

class ProfileCardViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = (app as UniToolkitApp).settings

    val profile: StateFlow<PersonalProfile> =
        settings.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PersonalProfile())

    fun save(profile: PersonalProfile) = viewModelScope.launch {
        settings.setProfile(profile)
    }

    fun importImage(kind: ProfileImageKind, uri: Uri, onImported: (String) -> Unit) = viewModelScope.launch {
        val path = copyToFiles(kind, uri)
        if (path.isNotBlank()) onImported(path)
    }

    private suspend fun copyToFiles(kind: ProfileImageKind, uri: Uri): String = withContext(Dispatchers.IO) {
        val app = getApplication<Application>()
        val fileName = when (kind) {
            ProfileImageKind.AVATAR -> "profile_avatar"
            ProfileImageKind.QQ_QR -> "profile_qq_qr"
            ProfileImageKind.WECHAT_QR -> "profile_wechat_qr"
        }
        val file = File(app.filesDir, "$fileName.png")
        runCatching {
            app.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
        if (file.exists() && file.length() > 0) file.absolutePath else ""
    }

    companion object { val Factory = appFactory(::ProfileCardViewModel) }
}
