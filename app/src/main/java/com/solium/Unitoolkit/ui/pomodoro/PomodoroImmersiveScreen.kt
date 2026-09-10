package com.solium.Unitoolkit.ui.pomodoro

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.solium.Unitoolkit.core.theme.LocalCustomTheme
import com.solium.Unitoolkit.core.theme.themeBackground
import com.solium.Unitoolkit.service.PomodoroManager
import kotlinx.coroutines.delay

@Composable
fun PomodoroImmersiveScreen(onBack: () -> Unit) {
    val state by PomodoroManager.state.collectAsState()
    var showMillis by remember { mutableStateOf(false) }
    var centi by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(state.remainingSeconds) {
        centi = 0
        while (true) { delay(100); centi = (centi + 1) % 10 }
    }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        window?.let { w ->
            WindowCompat.setDecorFitsSystemWindows(w, false)
            WindowInsetsControllerCompat(w, w.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            window?.let { w ->
                WindowCompat.setDecorFitsSystemWindows(w, true)
                WindowInsetsControllerCompat(w, w.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler { onBack() }

    val customTheme = LocalCustomTheme.current
    val bgBase = if (customTheme?.pomodoroImmersiveBgImage.isNullOrBlank()) Modifier.background(Color.Black) else Modifier
    Column(
        bgBase
            .fillMaxSize()
            .themeBackground(customTheme?.pomodoroImmersiveBgImage, 1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { showMillis = !showMillis },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            when (state.phase) {
                PomodoroManager.Phase.WORK -> "专注"
                PomodoroManager.Phase.BREAK -> "休息"
                else -> "待机"
            },
            color = Color(0xFF9E9E9E),
            fontSize = 18.sp,
        )
        val main = PomodoroManager.formatRemaining()
        val full = if (showMillis) "$main.${(10 - centi) % 10}" else main
        Text(full, color = Color.White, fontSize = 84.sp, fontWeight = FontWeight.Light)
        Text("点击切换毫秒 · 按返回键退出", Modifier.padding(top = 24.dp), color = Color(0xFF616161), fontSize = 13.sp)
    }
}