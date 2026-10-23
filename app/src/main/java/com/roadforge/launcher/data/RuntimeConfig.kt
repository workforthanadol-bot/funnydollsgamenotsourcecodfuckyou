package com.roadforge.launcher.data

/**
 * การตั้งค่า Runtime สำหรับแต่ละเกม
 * serialize เป็น JSON เก็บใน GameProfile.runtimeConfigJson
 */
data class RuntimeConfig(
    val wineVersion: String = "auto",
    val windowsVersion: String = "win10",
    val box64Preset: Box64Preset = Box64Preset.DEFAULT,
    val graphicsDriver: GraphicsDriver = GraphicsDriver.AUTO,
    val graphicsApi: GraphicsApi = GraphicsApi.DXVK,
    val resolution: Resolution = Resolution(1280, 720),
    val fpsLimit: Int = 60,
    val vsync: Boolean = false,
    val lowMemoryMode: Boolean = false,
    val shaderCacheEnabled: Boolean = true,
    val extraWineArgs: String = "",
    val extraEnvVars: Map<String, String> = emptyMap(),
)

enum class Box64Preset(val label: String, val description: String) {
    DEFAULT("Default", "การตั้งค่าสมดุล เหมาะกับเกมทั่วไป"),
    PERFORMANCE("Performance", "ปิดการตรวจสอบบางส่วนเพื่อความเร็ว อาจไม่เสถียร"),
    COMPATIBILITY("Compatibility", "เปิด Dynarec เพิ่มเติม ช้ากว่าแต่เสถียรกว่า"),
    LOW_MEMORY("Low Memory", "ลด Cache ของ Dynarec เพื่อลด RAM ใช้กับอุปกรณ์ RAM น้อย")
}

enum class GraphicsDriver(val label: String, val description: String) {
    AUTO("Auto", "ให้ Launcher เลือก Driver ที่เหมาะที่สุดอัตโนมัติ"),
    TURNIP("Turnip (Adreno)", "Mesa Turnip Driver — รองรับ Qualcomm Adreno เท่านั้น"),
    SYSTEM_VULKAN("System Vulkan", "ใช้ Vulkan Driver ที่ติดตั้งในระบบ Android"),
    VIRGL("VirGL (OpenGL)", "OpenGL ผ่าน VirGL — ประสิทธิภาพต่ำกว่า Vulkan"),
}

enum class GraphicsApi(val label: String) {
    DXVK("DXVK (D3D9/10/11 → Vulkan)"),
    VKD3D_PROTON("VKD3D-Proton (D3D12 → Vulkan)"),
    WINED3D("WineD3D (fallback — OpenGL)"),
}

data class Resolution(
    val width: Int,
    val height: Int,
) {
    override fun toString() = "${width}×${height}"

    companion object {
        val PRESETS = listOf(
            Resolution(960, 540),
            Resolution(1280, 720),
            Resolution(1600, 900),
            Resolution(1920, 1080),
        )
    }
}
