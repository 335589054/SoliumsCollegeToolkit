package com.unitoolkit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.unitoolkit.MainActivity
import com.unitoolkit.R
import com.unitoolkit.core.model.OverlayConfig

/** 音游上隐条悬浮窗：支持图片（拉伸/裁切）、纯色与文字，可拖拽。 */
class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var overlay: View? = null
    private var currentMode: String = ""
    private var downRawX = 0f
    private var downRawY = 0f
    private var downX = 0
    private var downY = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            removeOverlay()
            OverlayManager.syncStopped(false)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFY_ID, buildNotification(this))
        applyOverlay(OverlayManager.config)
        return START_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    private fun applyOverlay(cfg: OverlayConfig) {
        if (!Settings.canDrawOverlays(this)) return
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val width = if (cfg.width > 0) cfg.width else screenW
        val height = if (cfg.height > 0) cfg.height else (screenH * 0.2f).toInt()

        if (overlay == null || cfg.mode != currentMode) {
            removeOverlay()
            val view = when (cfg.mode) {
                "color" -> View(this).apply { setBackgroundColor(parseColor(cfg.colorHex, Color.BLACK)) }
                "text" -> TextView(this).apply {
                    text = cfg.text
                    setTextColor(parseColor(cfg.textColorHex, Color.WHITE))
                    textSize = if (cfg.textSizePx > 0) cfg.textSizePx.toFloat() else (height * 0.35f).coerceAtLeast(24f)
                    gravity = Gravity.CENTER
                }
                else -> ImageView(this)
            }
            view.alpha = cfg.opacity.coerceIn(0.05f, 1f)
            view.setOnTouchListener { _, event -> onDrag(event) }
            currentMode = cfg.mode
            overlay = view
        } else {
            val view = overlay ?: return
            when (cfg.mode) {
                "color" -> view.setBackgroundColor(parseColor(cfg.colorHex, Color.BLACK))
                "text" -> (view as? TextView)?.let {
                    it.text = cfg.text
                    it.setTextColor(parseColor(cfg.textColorHex, Color.WHITE))
                    it.textSize = if (cfg.textSizePx > 0) cfg.textSizePx.toFloat() else (height * 0.35f).coerceAtLeast(24f)
                }
                else -> {}
            }
            view.alpha = cfg.opacity.coerceIn(0.05f, 1f)
        }

        if (cfg.mode == "image") {
            val iv = overlay as? ImageView ?: return
            val bitmap: Bitmap? = runCatching { BitmapFactory.decodeFile(cfg.imagePath) }.getOrNull()
            iv.setImageDrawable(bitmap?.let { BitmapDrawable(resources, it) })
            iv.scaleType = if (cfg.imageScale == "crop") ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_XY
        }

        val view = overlay ?: return
        val params = WindowManager.LayoutParams(
            width,
            height,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = cfg.x
        params.y = cfg.y
        runCatching {
            if (view.parent == null) wm.addView(view, params) else wm.updateViewLayout(view, params)
        }
    }

    private fun parseColor(hex: String, fallback: Int): Int =
        runCatching { Color.parseColor(hex) }.getOrDefault(fallback)

    private fun onDrag(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                downX = OverlayManager.config.x
                downY = OverlayManager.config.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val v = overlay ?: return true
                val lp = v.layoutParams as WindowManager.LayoutParams
                lp.x = (downX + (event.rawX - downRawX)).toInt()
                lp.y = (downY + (event.rawY - downRawY)).toInt()
                OverlayManager.config = OverlayManager.config.copy(x = lp.x, y = lp.y)
                runCatching { wm.updateViewLayout(v, lp) }
                return true
            }
        }
        return false
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun removeOverlay() {
        overlay?.let { runCatching { wm.removeView(it) } }
        overlay = null
        currentMode = ""
    }

    companion object {
        const val CHANNEL_ID = "overlay"
        const val NOTIFY_ID = 200
        const val ACTION_STOP = "com.unitoolkit.action.STOP_OVERLAY"
        const val ACTION_SHOW = "com.unitoolkit.action.SHOW_OVERLAY"
        const val ACTION_UPDATE = "com.unitoolkit.action.UPDATE_OVERLAY"

        fun buildNotification(ctx: Context): Notification {
            val openIntent = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val stopIntent = PendingIntent.getService(
                ctx, 1, Intent(ctx, OverlayService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            return NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("音游上隐条运行中")
                .setContentText("点击停止关闭悬浮窗")
                .setContentIntent(openIntent)
                .setOngoing(true)
                .addAction(0, "停止", stopIntent)
                .build()
        }

        private fun ensureChannel(ctx: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, "音游上隐条", NotificationManager.IMPORTANCE_LOW)
                val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(channel)
            }
        }
    }
}
