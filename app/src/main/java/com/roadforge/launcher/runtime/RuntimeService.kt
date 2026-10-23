package com.roadforge.launcher.runtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.roadforge.launcher.R
import com.roadforge.launcher.data.GameProfile
import com.roadforge.launcher.data.RuntimeConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Foreground Service สำหรับรัน Wine + Box64 + BeamNG.drive
 *
 * ข้อจำกัดที่ชัดเจน:
 * - Service นี้สร้าง Environment และเตรียม command line
 * - การรัน EXE จริงต้องการ Wine binary ที่ติดตั้งใน runtimeDir
 * - หาก Runtime ไม่พร้อม Service จะ report RUNTIME_MISSING และหยุด
 */
class RuntimeService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakelock: PowerManager.WakeLock? = null
    private var gameProcess: Process? = null

    private val _launchState = MutableStateFlow<LaunchState>(LaunchState.Idle)
    val launchState: StateFlow<LaunchState> = _launchState

    inner class LocalBinder : Binder() {
        fun getService(): RuntimeService = this@RuntimeService
    }
    private val binder = LocalBinder()
    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("กำลังเตรียม…"))
        return START_NOT_STICKY
    }

    fun launchGame(profile: GameProfile, config: RuntimeConfig) {
        serviceScope.launch {
            _launchState.value = LaunchState.Checking("ตรวจสอบ Runtime…")

            val runtimeDir = java.io.File(filesDir, "runtime")
            val box64 = java.io.File(runtimeDir, "box64")
            val wine64 = java.io.File(runtimeDir, "wine/bin/wine64")

            if (!box64.exists() || !box64.canExecute()) {
                _launchState.value = LaunchState.Failed("Box64 ไม่พบหรือไม่สามารถรันได้\nPath: ${box64.absolutePath}")
                stopSelf()
                return@launch
            }
            if (!wine64.exists() || !wine64.canExecute()) {
                _launchState.value = LaunchState.Failed("Wine ไม่พบหรือไม่สามารถรันได้\nPath: ${wine64.absolutePath}")
                stopSelf()
                return@launch
            }

            _launchState.value = LaunchState.Checking("เตรียม Wine Prefix…")
            val winePrefix = java.io.File(filesDir, "wineprefix/${profile.id}")
            winePrefix.mkdirs()

            _launchState.value = LaunchState.Launching

            val env = buildEnvironment(config, runtimeDir, winePrefix)
            val cmd = buildCommand(box64, wine64, profile, config)

            updateNotification("กำลังรัน ${profile.name}")

            acquireWakelock()

            try {
                gameProcess = ProcessBuilder(cmd)
                    .apply { environment().putAll(env) }
                    .redirectErrorStream(true)
                    .start()

                _launchState.value = LaunchState.Running(profile.name)

                // stream stdout ไปยัง log file
                val logFile = java.io.File(filesDir, "logs/${profile.id}_launch.log")
                logFile.parentFile?.mkdirs()
                gameProcess!!.inputStream.use { input ->
                    logFile.outputStream().use { out -> input.copyTo(out) }
                }

                val exitCode = gameProcess!!.waitFor()
                _launchState.value = if (exitCode == 0) {
                    LaunchState.Exited(exitCode)
                } else {
                    LaunchState.Failed("เกมปิดด้วย exit code $exitCode\nดู Log: ${logFile.absolutePath}")
                }
            } catch (e: Exception) {
                _launchState.value = LaunchState.Failed("Launch failed: ${e.message}")
            } finally {
                releaseWakelock()
                stopSelf()
            }
        }
    }

    fun stopGame() {
        gameProcess?.destroy()
        _launchState.value = LaunchState.Idle
        stopSelf()
    }

    private fun buildEnvironment(
        config: RuntimeConfig,
        runtimeDir: java.io.File,
        winePrefix: java.io.File,
    ): Map<String, String> {
        val env = mutableMapOf(
            "WINEPREFIX" to winePrefix.absolutePath,
            "WINEDLLPATH" to "${runtimeDir.absolutePath}/dxvk",
            "WINEDLLOVERRIDES" to buildDllOverrides(config),
            "BOX64_DYNAREC" to "1",
            "BOX64_BASH" to "${runtimeDir.absolutePath}/wine/bin/bash",
            "DISPLAY" to ":0",
            "MESA_LOADER_DRIVER_OVERRIDE" to resolveDriver(config),
            "TU_DEBUG" to "noconform",
            "DXVK_STATE_CACHE_PATH" to "${filesDir.absolutePath}/shader_cache",
            "DXVK_LOG_PATH" to "${filesDir.absolutePath}/logs",
            "WINEARCH" to "win64",
            "BOX64_DYNAREC_BIGBLOCK" to when (config.box64Preset) {
                com.roadforge.launcher.data.Box64Preset.PERFORMANCE -> "2"
                com.roadforge.launcher.data.Box64Preset.COMPATIBILITY -> "0"
                com.roadforge.launcher.data.Box64Preset.LOW_MEMORY -> "1"
                else -> "1"
            },
            "BOX64_DYNAREC_STRONGMEM" to when (config.box64Preset) {
                com.roadforge.launcher.data.Box64Preset.COMPATIBILITY -> "3"
                else -> "1"
            },
        )
        env.putAll(config.extraEnvVars)
        return env
    }

    private fun buildDllOverrides(config: RuntimeConfig): String {
        val overrides = mutableListOf("d3d11=n,b", "dxgi=n,b")
        if (config.graphicsApi == com.roadforge.launcher.data.GraphicsApi.VKD3D_PROTON) {
            overrides.add("d3d12=n,b")
        }
        return overrides.joinToString(";")
    }

    private fun resolveDriver(config: RuntimeConfig): String = when (config.graphicsDriver) {
        com.roadforge.launcher.data.GraphicsDriver.TURNIP -> "zink"
        com.roadforge.launcher.data.GraphicsDriver.VIRGL -> "virpipe"
        com.roadforge.launcher.data.GraphicsDriver.SYSTEM_VULKAN -> ""
        com.roadforge.launcher.data.GraphicsDriver.AUTO -> "zink"
    }

    private fun buildCommand(
        box64: java.io.File,
        wine64: java.io.File,
        profile: GameProfile,
        config: RuntimeConfig,
    ): List<String> = buildList {
        add(box64.absolutePath)
        add(wine64.absolutePath)
        add(profile.exePath)
        add("--no-update")
        if (config.resolution.width > 0) {
            add("-w"); add("${config.resolution.width}")
            add("-h"); add("${config.resolution.height}")
        }
        if (config.extraWineArgs.isNotBlank()) {
            addAll(config.extraWineArgs.split(" ").filter { it.isNotBlank() })
        }
    }

    private fun acquireWakelock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RoadForge:GameRunning")
            .also { it.acquire(4 * 60 * 60 * 1000L) } // max 4 ชั่วโมง
    }

    private fun releaseWakelock() {
        wakelock?.takeIf { it.isHeld }?.release()
        wakelock = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "RoadForge Game", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "แสดงสถานะเกมที่กำลังรัน" }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RoadForge")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    override fun onDestroy() {
        gameProcess?.destroyForcibly()
        releaseWakelock()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "roadforge_game"
        private const val NOTIF_ID = 1001
        const val ACTION_LAUNCH  = "com.roadforge.launcher.ACTION_LAUNCH"
        const val EXTRA_GAME_ID  = "extra_game_id"
    }
}

sealed class LaunchState {
    object Idle : LaunchState()
    data class Checking(val step: String) : LaunchState()
    object Launching : LaunchState()
    data class Running(val gameName: String) : LaunchState()
    data class Exited(val exitCode: Int) : LaunchState()
    data class Failed(val reason: String) : LaunchState()
}
