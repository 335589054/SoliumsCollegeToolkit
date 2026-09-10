package com.unitoolkit.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.unitoolkit.core.model.OverlayConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 音游上隐条全局状态：进程级配置与运行状态，前台服务负责渲染图片悬浮窗 */
object OverlayManager {
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    @Volatile
    var config: OverlayConfig = OverlayConfig()

    fun start(context: Context, cfg: OverlayConfig) {
        config = cfg
        _running.value = true
        ContextCompat.startForegroundService(
            context, Intent(context, OverlayService::class.java).setAction(OverlayService.ACTION_SHOW)
        )
    }

    fun update(context: Context, cfg: OverlayConfig) {
        config = cfg
        ContextCompat.startForegroundService(
            context, Intent(context, OverlayService::class.java).setAction(OverlayService.ACTION_UPDATE)
        )
    }

    fun stop(context: Context) {
        _running.value = false
        context.stopService(Intent(context, OverlayService::class.java))
    }

    internal fun syncStopped(value: Boolean) { _running.value = value }
}