package com.solium.Unitoolkit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.solium.Unitoolkit.MainActivity
import com.solium.Unitoolkit.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PomodoroService : Service() {

    private var collectJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            PomodoroManager.stop()
            return START_NOT_STICKY
        }
        startForeground(NOTIFY_ID, buildNotification(this, PomodoroManager.state.value))
        if (collectJob == null) {
            collectJob = CoroutineScope(Dispatchers.Main).launch {
                PomodoroManager.state.collect { s ->
                    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFY_ID, buildNotification(this@PomodoroService, s))
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        collectJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFY_ID)
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "pomodoro"
        const val NOTIFY_ID = 100
        private const val ACTION_STOP = "com.solium.Unitoolkit.action.STOP_POMODORO"

        fun buildNotification(ctx: Context, s: PomodoroManager.State): Notification {
            val phaseText = when (s.phase) {
                PomodoroManager.Phase.WORK -> "专注中"
                PomodoroManager.Phase.BREAK -> "休息中"
                else -> "待机"
            }
            val contentText = if (s.phase == PomodoroManager.Phase.IDLE) "已停止" else "${formatSeconds(s.remainingSeconds)} · $phaseText"

            val openIntent = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val stopIntent = PendingIntent.getService(
                ctx, 1, Intent(ctx, PomodoroService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            return NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Solium的工具箱 · 番茄钟")
                .setContentText(contentText)
                .setContentIntent(openIntent)
                .setOngoing(s.running)
                .addAction(0, "停止", stopIntent)
                .build()
        }

        fun updateNotificationIfRunning(ctx: Context, s: PomodoroManager.State) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (s.phase == PomodoroManager.Phase.IDLE) {
                nm.cancel(NOTIFY_ID)
                return
            }
            if (s.running || s.phase != PomodoroManager.Phase.IDLE) {
                nm.notify(NOTIFY_ID, buildNotification(ctx, s))
            }
        }

        private fun ensureChannel(ctx: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, "番茄钟", NotificationManager.IMPORTANCE_LOW)
                val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(channel)
            }
        }
    }
}

private fun formatSeconds(secs: Int): String {
    val m = secs / 60
    val s = secs % 60
    return "%02d:%02d".format(m, s)
}
