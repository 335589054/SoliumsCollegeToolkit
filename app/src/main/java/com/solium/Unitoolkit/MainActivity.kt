package com.solium.Unitoolkit

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.solium.Unitoolkit.core.theme.ThemeKey
import com.solium.Unitoolkit.core.model.CustomTheme
import com.solium.Unitoolkit.core.theme.UiMode
import com.solium.Unitoolkit.core.theme.UniToolkitTheme
import com.solium.Unitoolkit.core.theme.findCurrentCustomTheme
import com.solium.Unitoolkit.core.theme.resolveTheme
import com.solium.Unitoolkit.ui.navigation.AppRoot
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingNavRoute by mutableStateOf<String?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 仅在冷启动（非旋转/配置变更导致的 Activity 重建）时消费导航路由，
        // 避免屏幕朝向变化后自动跳转到课表。
        if (savedInstanceState == null) {
            pendingNavRoute = intent.getStringExtra(EXTRA_NAV_ROUTE)
            handleOpenIntent(intent)
        }
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val app = application as UniToolkitApp
            val themeId by app.settings.themeId.collectAsState(initial = ThemeKey.INK.key)
            val customThemes by app.settings.customThemes.collectAsState(initial = emptyList<CustomTheme>())
            val uiMode by app.settings.uiMode.collectAsState(initial = UiMode.SYSTEM)
            UniToolkitTheme(
                appTheme = resolveTheme(themeId, customThemes),
                uiMode = uiMode,
                customTheme = findCurrentCustomTheme(themeId, customThemes),
            ) {
                AppRoot(navRequest = pendingNavRoute) { pendingNavRoute = null }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNavRoute = intent.getStringExtra(EXTRA_NAV_ROUTE)
        handleOpenIntent(intent)
    }

    /** 处理通过系统「打开方式」打开 .sthm 主题包 / .stbc 备份文件。 */
    private fun handleOpenIntent(intent: Intent) {
        val uri = intent.data ?: return
        val name = uri.lastPathSegment?.substringBefore('?') ?: return
        val app = application as UniToolkitApp
        lifecycleScope.launch {
            when {
                name.endsWith(".sthm", ignoreCase = true) -> {
                    app.importThemePackageFromUri(uri)
                    Toast.makeText(this@MainActivity, "已导入主题包", Toast.LENGTH_SHORT).show()
                }
                name.endsWith(".stbc", ignoreCase = true) -> {
                    app.importBackupFromUri(uri)
                    Toast.makeText(this@MainActivity, "已导入备份", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        const val EXTRA_NAV_ROUTE = "com.solium.Unitoolkit.extra.nav_route"
    }
}
