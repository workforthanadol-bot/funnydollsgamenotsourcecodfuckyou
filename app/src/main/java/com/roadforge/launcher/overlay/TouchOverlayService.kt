package com.roadforge.launcher.overlay

import android.app.*
import android.content.Intent
import android.graphics.*
import android.os.Binder
import android.os.IBinder
import android.view.*
import android.view.WindowManager.LayoutParams.*
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.roadforge.launcher.data.TouchLayout
import com.roadforge.launcher.input.InputBackend
import com.roadforge.launcher.ui.overlay.OverlayCanvas
import kotlinx.coroutines.*
import java.io.File

/**
 * Foreground Service สำหรับแสดง Touch Controls ทับหน้าจอ Wine/เกม
 *
 * ใช้ WindowManager.addView ด้วย TYPE_APPLICATION_OVERLAY
 * ต้องการสิทธิ์ SYSTEM_ALERT_WINDOW
 */
class TouchOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    // Lifecycle สำหรับ Compose ใน overlay
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var currentLayout: TouchLayout? = null
    private var inputBackend: InputBackend? = null

    // กดอยู่ขณะนี้ — เก็บไว้เพื่อ releaseAll เมื่อ stop
    private val pressedMappings = mutableSetOf<com.roadforge.launcher.data.InputMapping>()

    inner class LocalBinder : Binder() {
        fun getService(): TouchOverlayService = this@TouchOverlayService
    }
    private val binder = LocalBinder()
    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        super.onCreate()
        createNotificationChannel()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        return START_STICKY
    }

    fun showOverlay(layout: TouchLayout, backend: InputBackend?) {
        currentLayout = layout
        inputBackend = backend
        removeOverlay()

        val params = LayoutParams(
            MATCH_PARENT, MATCH_PARENT,
            TYPE_APPLICATION_OVERLAY,
            FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_IN_SCREEN or FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        val view = OverlayViewHost(this, layout) { mapping, pressed ->
            serviceScope.launch {
                if (pressed) pressedMappings.add(mapping) else pressedMappings.remove(mapping)
                inputBackend?.sendMapping(mapping, pressed)
            }
        }

        windowManager.addView(view, params)
        overlayView = view
    }

    fun hideOverlay() {
        removeOverlay()
    }

    private fun removeOverlay() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            overlayView = null
        }
    }

    override fun onDestroy() {
        serviceScope.launch {
            inputBackend?.releaseAll(pressedMappings.toSet())
        }
        removeOverlay()
        serviceScope.cancel()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Touch Overlay", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("RoadForge Touch Controls")
            .setContentText("Touch overlay กำลังทำงาน")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

    companion object {
        private const val CHANNEL_ID = "roadforge_overlay"
        private const val NOTIF_ID = 1002
    }
}
