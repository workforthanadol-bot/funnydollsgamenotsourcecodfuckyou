package com.roadforge.launcher.runtime

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ข้อมูลอุปกรณ์ที่ตรวจพบจริง
 * ทุกค่าที่แสดงต้องมาจากการอ่าน API จริง ไม่ใช่ค่า hardcode
 */
data class DeviceInfo(
    val cpuAbi: String,
    val cpuCores: Int,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val gpuRenderer: String,
    val gpuVendor: String,
    val gpuVersion: String,
    val vulkanApiVersion: String?,
    val isAdreno: Boolean,
    val adrenoModel: Int?,          // เช่น 740, 650 — null ถ้าไม่ใช่ Adreno
    val isXclipse: Boolean,
    val androidSdk: Int,
    val availableStorageMb: Long,
    val supportsVulkan: Boolean,
    val vulkanMinVersion: Boolean,   // >= 1.1.0 ซึ่งจำเป็นสำหรับ DXVK
)

object DeviceInspector {

    suspend fun inspect(context: Context): DeviceInfo = withContext(Dispatchers.IO) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }

        val cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        val cpuCores = Runtime.getRuntime().availableProcessors()

        // GPU info จาก EGL — ต้องทำบน thread ที่มี EGL context
        // ที่นี่ใช้ค่าที่อ่านได้จาก system properties เป็น fallback
        val gpuRenderer = readSystemProp("ro.hardware.egl")
            ?: readSystemProp("ro.hardware")
            ?: "unknown"
        val gpuVendor = readSystemProp("ro.board.platform") ?: "unknown"
        val gpuVersion = "OpenGL ES ${GLES20.glGetString(GLES20.GL_VERSION) ?: "unknown"}"

        val isAdreno = gpuRenderer.contains("adreno", ignoreCase = true) ||
                gpuVendor.contains("qcom", ignoreCase = true) ||
                Build.HARDWARE.contains("qcom", ignoreCase = true)

        val adrenoModel = if (isAdreno) extractAdrenoModel(gpuRenderer) else null

        val isXclipse = gpuRenderer.contains("xclipse", ignoreCase = true) ||
                gpuVendor.contains("samsung", ignoreCase = true) && gpuVendor.contains("xclipse", ignoreCase = true)

        // ตรวจสอบ Vulkan support
        val vulkanVersion = getVulkanVersion()
        val supportsVulkan = vulkanVersion != null
        val vulkanMinVersion = vulkanVersion != null && vulkanVersion >= 0x00401000 // 1.1.0

        // Storage
        val storagePath = context.filesDir
        val availableStorage = storagePath.freeSpace / (1024 * 1024)

        DeviceInfo(
            cpuAbi = cpuAbi,
            cpuCores = cpuCores,
            totalRamMb = memInfo.totalMem / (1024 * 1024),
            availableRamMb = memInfo.availMem / (1024 * 1024),
            gpuRenderer = gpuRenderer,
            gpuVendor = gpuVendor,
            gpuVersion = gpuVersion,
            vulkanApiVersion = vulkanVersion?.let { formatVulkanVersion(it) },
            isAdreno = isAdreno,
            adrenoModel = adrenoModel,
            isXclipse = isXclipse,
            androidSdk = Build.VERSION.SDK_INT,
            availableStorageMb = availableStorage,
            supportsVulkan = supportsVulkan,
            vulkanMinVersion = vulkanMinVersion,
        )
    }

    private fun readSystemProp(key: String): String? = try {
        val process = Runtime.getRuntime().exec(arrayOf("getprop", key))
        process.inputStream.bufferedReader().readLine()?.trim()?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) { null }

    private fun extractAdrenoModel(renderer: String): Int? {
        val match = Regex("Adreno[\\s(]*(\\d+)", RegexOption.IGNORE_CASE)
            .find(renderer)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun getVulkanVersion(): Int? {
        // ตรวจสอบผ่าน vulkan feature flag
        return try {
            val f = File("/proc/device-tree/model")
            // ใช้ PackageManager feature แทน — ทำใน suspend ต่อ
            null // placeholder — ค่าจริงตรวจจาก PackageManager ใน RuntimeChecker
        } catch (_: Exception) { null }
    }

    private fun formatVulkanVersion(v: Int): String {
        val major = (v shr 22) and 0x3FF
        val minor = (v shr 12) and 0x3FF
        val patch = v and 0xFFF
        return "$major.$minor.$patch"
    }
}
