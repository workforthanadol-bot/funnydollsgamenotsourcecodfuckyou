package com.roadforge.launcher.runtime

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ตรวจสอบสถานะของ Runtime Components จริง
 *
 * ไม่สร้างสถานะปลอม:
 * - ถ้าไม่พบ binary = MISSING
 * - ถ้า GPU ไม่ใช่ Adreno + ลอง Turnip = UNSUPPORTED
 * - ถ้า Vulkan API ต่ำกว่า 1.1 = UNSUPPORTED สำหรับ DXVK
 */
class RuntimeChecker(private val context: Context) {

    private val filesDir get() = context.filesDir
    private val runtimeDir get() = File(filesDir, "runtime")

    suspend fun checkAll(deviceInfo: DeviceInfo): RuntimeStatus = withContext(Dispatchers.IO) {
        val components = mutableListOf<RuntimeComponent>()
        val warnings = mutableListOf<String>()

        // 1. CPU Architecture
        val cpuSupported = deviceInfo.cpuAbi == "arm64-v8a"
        if (!cpuSupported) {
            warnings.add("CPU ABI '${deviceInfo.cpuAbi}' อาจไม่รองรับ Box64 (ต้องการ arm64-v8a)")
        }

        // 2. Box64
        val box64Binary = File(runtimeDir, "box64")
        components += RuntimeComponent(
            id = "box64",
            name = "Box64",
            description = "แปลงคำสั่ง x86-64 → ARM64 สำหรับรัน Windows binaries",
            status = when {
                !cpuSupported -> ComponentStatus.UNSUPPORTED
                box64Binary.exists() && box64Binary.canExecute() -> ComponentStatus.AVAILABLE
                box64Binary.exists() -> ComponentStatus.ERROR
                else -> ComponentStatus.MISSING
            },
            version = if (box64Binary.exists()) getBox64Version(box64Binary) else null,
            installPath = box64Binary.absolutePath,
            licenseNote = "Box64 — LGPL-2.1 — github.com/ptitSeb/box64",
            requiredForLaunch = true,
        )

        // 3. Wine
        val wineBinary = File(runtimeDir, "wine/bin/wine64")
        components += RuntimeComponent(
            id = "wine",
            name = "Wine",
            description = "Windows API Compatibility Layer",
            status = when {
                wineBinary.exists() && wineBinary.canExecute() -> ComponentStatus.AVAILABLE
                wineBinary.exists() -> ComponentStatus.ERROR
                else -> ComponentStatus.MISSING
            },
            version = null, // detect จาก wine --version เมื่อ exec ได้
            installPath = File(runtimeDir, "wine").absolutePath,
            licenseNote = "Wine — LGPL-2.1 — winehq.org",
            requiredForLaunch = true,
        )

        // 4. Vulkan
        val vulkanAvailable = checkVulkanAvailable()
        val vulkanMeetsMin = deviceInfo.vulkanMinVersion
        components += RuntimeComponent(
            id = "vulkan",
            name = "Vulkan",
            description = "Graphics API ที่ DXVK ต้องการ",
            status = when {
                !vulkanAvailable -> ComponentStatus.UNSUPPORTED
                !vulkanMeetsMin -> ComponentStatus.UNSUPPORTED
                else -> ComponentStatus.COMPATIBLE
            },
            version = deviceInfo.vulkanApiVersion,
            licenseNote = "Vulkan — Khronos",
            requiredForLaunch = true,
        )
        if (vulkanAvailable && !vulkanMeetsMin) {
            warnings.add("Vulkan ${deviceInfo.vulkanApiVersion} ต่ำกว่า 1.1.0 — DXVK อาจไม่ทำงาน")
        }

        // 5. DXVK
        val dxvkD3d11 = File(runtimeDir, "dxvk/d3d11.dll")
        val dxvkDxgi  = File(runtimeDir, "dxvk/dxgi.dll")
        components += RuntimeComponent(
            id = "dxvk",
            name = "DXVK",
            description = "แปลง Direct3D 9/10/11 → Vulkan",
            status = when {
                !vulkanAvailable -> ComponentStatus.UNSUPPORTED
                dxvkD3d11.exists() && dxvkDxgi.exists() -> ComponentStatus.AVAILABLE
                else -> ComponentStatus.MISSING
            },
            installPath = File(runtimeDir, "dxvk").absolutePath,
            licenseNote = "DXVK — zlib/libpng — github.com/doitsujin/dxvk",
            requiredForLaunch = true,
        )

        // 6. Turnip (เฉพาะ Adreno)
        val turnipLib = File(runtimeDir, "turnip/libvulkan_freedreno.so")
        components += RuntimeComponent(
            id = "turnip",
            name = "Turnip (Mesa Adreno)",
            description = "Vulkan Driver สำหรับ Qualcomm Adreno GPU",
            status = when {
                !deviceInfo.isAdreno -> ComponentStatus.UNSUPPORTED
                turnipLib.exists() -> ComponentStatus.AVAILABLE
                else -> ComponentStatus.REQUIRES_INSTALLATION
            },
            installPath = turnipLib.absolutePath,
            licenseNote = "Mesa Turnip — MIT — gitlab.freedesktop.org/mesa/mesa",
            requiredForLaunch = false, // System Vulkan ใช้แทนได้
        )
        if (!deviceInfo.isAdreno) {
            warnings.add("GPU ของอุปกรณ์นี้ไม่ใช่ Qualcomm Adreno — Turnip ไม่รองรับ ใช้ System Vulkan แทน")
        }

        // 7. VKD3D-Proton (optional สำหรับ D3D12)
        val vkd3dLib = File(runtimeDir, "vkd3d-proton/d3d12.dll")
        components += RuntimeComponent(
            id = "vkd3d_proton",
            name = "VKD3D-Proton",
            description = "แปลง Direct3D 12 → Vulkan (BeamNG ใช้ D3D11 จึง optional)",
            status = when {
                !vulkanAvailable -> ComponentStatus.UNSUPPORTED
                vkd3dLib.exists() -> ComponentStatus.AVAILABLE
                else -> ComponentStatus.REQUIRES_INSTALLATION
            },
            installPath = vkd3dLib.absolutePath,
            licenseNote = "VKD3D-Proton — LGPL-2.1 — github.com/HansKristian-Work/vkd3d-proton",
            requiredForLaunch = false,
        )

        // 8. VirGL (fallback)
        val virglLib = File(runtimeDir, "virgl/libvirgl_test_server.so")
        components += RuntimeComponent(
            id = "virgl",
            name = "VirGL",
            description = "OpenGL ผ่าน Virtio-GPU — ประสิทธิภาพต่ำ ใช้เป็น fallback",
            status = if (virglLib.exists()) ComponentStatus.AVAILABLE else ComponentStatus.REQUIRES_INSTALLATION,
            installPath = virglLib.absolutePath,
            licenseNote = "VirGL — MIT — gitlab.freedesktop.org/virgl",
            requiredForLaunch = false,
        )

        val missingRequired = components.filter {
            it.requiredForLaunch && it.status in listOf(
                ComponentStatus.MISSING, ComponentStatus.UNSUPPORTED, ComponentStatus.ERROR
            )
        }

        RuntimeStatus(
            components = components,
            isReadyForLaunch = missingRequired.isEmpty(),
            missingRequired = missingRequired,
            warnings = warnings,
        )
    }

    private fun checkVulkanAvailable(): Boolean {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
        } else {
            File("/proc/driver/mali").exists() ||
            File("/dev/mali0").exists() ||
            File("/dev/kgsl-3d0").exists() // Adreno
        }
    }

    private fun getBox64Version(binary: File): String? = try {
        val proc = ProcessBuilder(binary.absolutePath, "--version")
            .redirectErrorStream(true)
            .start()
        proc.inputStream.bufferedReader().readLine()?.trim()
    } catch (_: Exception) { null }
}
