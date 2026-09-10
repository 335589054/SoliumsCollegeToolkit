package com.solium.Unitoolkit.ui.life

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.solium.Unitoolkit.ui.components.SimpleTopBar
import java.net.URLDecoder

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(navController: NavController) {
    val title = navController.currentBackStackEntry?.arguments?.getString("title")?.let { URLDecoder.decode(it, "UTF-8") } ?: "网页"
    val urlRaw = navController.currentBackStackEntry?.arguments?.getString("url")?.let { URLDecoder.decode(it, "UTF-8") } ?: ""

    Scaffold(topBar = { SimpleTopBar(title = title, onBack = { navController.popBackStack() }) }) { padding ->
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(padding),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    if (urlRaw.isNotBlank()) loadUrl(ensureScheme(urlRaw))
                }
            },
        )
    }
}

private fun ensureScheme(url: String): String =
    if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"