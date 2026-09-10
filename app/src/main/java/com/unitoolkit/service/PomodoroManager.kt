package com.unitoolkit.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.unitoolkit.MainActivity
import com.unitoolkit.R
import com.unitoolkit.core.database.AppDatabase
import com.unitoolkit.core.database.PomodoroRecordEntity
import com.unitoolkit.core.model.PomodoroPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 番茄钟全局计时核心：进程级协程计时，前台服务负责通知展示 */
object PomodoroManager {
    enum class Phase { IDLE, WORK, BREAK }

    data class State(
        val phase: Phase = Phase.IDLE,
        val running: Boolean = false,
        val remainingSeconds: Int = 0,
        val totalSeconds: Int = 0,
        val preset: PomodoroPreset? = null,
        val completedWorkCount: Int = 0,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var db: AppDatabase? = null
    private var appContext: Context? = null
    private var ticker: Job? = null

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    internal val isRunning: Boolean get() = _state.value.running

    fun init(context: Context, database: AppDatabase) {
        appContext = context.applicationContext
        db = database
    }

    fun start(preset: PomodoroPreset) {
        ticker?.cancel()
        val secs = preset.workMinutes * 60
        _state.value = State(Phase.WORK, true, secs, secs, preset, 0)
        startTicker()
        launchService()
    }

    fun pause() {
        _state.value = _state.value.copy(running = false)
        updateNotification()
    }

    fun resume() {
        _state.value = _state.value.copy(running = true)
        startTicker()
        updateNotification()
    }

    fun stop() {
        ticker?.cancel()
        _state.value = State(phase = Phase.IDLE)
        appContext?.let { ctx ->
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            runCatching { nm.cancel(PomodoroService.NOTIFY_ID) }
            ctx.stopService(Intent(ctx, PomodoroService::class.java))
        }
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (true) {
                delay(1000)
                val s = _state.value
                if (!s.running) continue
                val next = s.remainingSeconds - 1
                if (next <= 0) {
                    onPhaseComplete(s)
                } else {
                    _state.value = s.copy(remainingSeconds = next)
                }
            }
        }
    }

    private fun onPhaseComplete(s: State) {
        val preset = s.preset ?: return
        when (s.phase) {
            Phase.WORK -> {
                // 记录统计
                val minutes = preset.workMinutes
                db?.pomodoroDao()?.let { dao ->
                    scope.launch { dao.insert(PomodoroRecordEntity(presetName = preset.name, focusMinutes = minutes)) }
                }
                val completed = s.completedWorkCount + 1
                val longBreak = completed % preset.longBreakInterval == 0
                val breakSecs = (if (longBreak) preset.longBreakMinutes else preset.shortBreakMinutes) * 60
                _state.value = s.copy(
                    phase = Phase.BREAK,
                    remainingSeconds = breakSecs,
                    totalSeconds = breakSecs,
                    completedWorkCount = completed,
                )
                notify("🎉 专注结束", "休息一下吧", breakSecs)
                playAlert()
            }
            Phase.BREAK -> {
                _state.value = State(phase = Phase.IDLE, preset = preset, completedWorkCount = s.completedWorkCount)
                notify("✅ 休息结束", "可以开始下一轮专注", 0)
                playAlert()
                appContext?.let { it.stopService(Intent(it, PomodoroService::class.java)) }
            }
            else -> Unit
        }
    }

    private fun playAlert() {
        val ctx = appContext ?: return
        try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90).startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700)
        } catch (_: Exception) {}
        try {
            val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(600, VibrationEffect.DEFAULT_AMPLITUDE))
                else @Suppress("DEPRECATION") vibrator.vibrate(600)
            }
        } catch (_: Exception) {}
    }

    fun formatRemaining(): String {
        val secs = _state.value.remainingSeconds
        val m = secs / 60
        val s = secs % 60
        return "%02d:%02d".format(m, s)
    }

    private fun launchService() {
        val ctx = appContext ?: return
        val intent = Intent(ctx, PomodoroService::class.java)
        ContextCompat.startForegroundService(ctx, intent)
    }

    fun updateNotification() {
        appContext?.let { ctx -> PomodoroService.updateNotificationIfRunning(ctx, _state.value) }
    }

    private fun notify(title: String, text: String, secs: Int) {
        val ctx = appContext ?: return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notif = NotificationCompat.Builder(ctx, PomodoroService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(PomodoroService.NOTIFY_ID + 1, notif) }
    }
}
