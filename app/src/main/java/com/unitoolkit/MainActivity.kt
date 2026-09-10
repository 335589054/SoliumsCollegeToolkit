package com.unitoolkit

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import com.unitoolkit.core.theme.ThemeKey
import com.unitoolkit.core.model.CustomTheme
import com.unitoolkit.core.theme.UiMode
import com.unitoolkit.core.theme.UniToolkitTheme
import com.unitoolkit.core.theme.resolveTheme
import com.unitoolkit.ui.navigation.AppRoot

class MainActivity : ComponentActivity() {

    private var pendingNavRoute by mutableStateOf<String?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingNavRoute = intent.getStringExtra(EXTRA_NAV_ROUTE)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val app = application as UniToolkitApp
            val themeId by app.settings.themeId.collectAsState(initial = ThemeKey.INK.key)
            val customThemes by app.settings.customThemes.collectAsState(initial = emptyList<CustomTheme>())
            val uiMode by app.settings.uiMode.collectAsState(initial = UiMode.SYSTEM)
            UniToolkitTheme(appTheme = resolveTheme(themeId, customThemes), uiMode = uiMode) {
                AppRoot(navRequest = pendingNavRoute) { pendingNavRoute = null }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNavRoute = intent.getStringExtra(EXTRA_NAV_ROUTE)
    }

    companion object {
        const val EXTRA_NAV_ROUTE = "com.unitoolkit.extra.nav_route"
    }
}
