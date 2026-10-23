package com.roadforge.launcher.runtime

/**
 * แต่ละ Component ของ Windows Compatibility Runtime
 *
 * สถานะที่เป็นไปได้ของแต่ละ component ถูกกำหนดไว้อย่างชัดเจน
 * Launcher ไม่สร้างสถานะปลอม — ถ้าไม่พบ binary จริงคือ MISSING
 */
data class RuntimeComponent(
    val id: String,
    val name: String,
    val description: String,
    val status: ComponentStatus,
    val version: String? = null,
    val requiredForLaunch: Boolean = true,
    val installPath: String? = null,
    val downloadUrl: String? = null,        // null = ต้องติดตั้งเองจากภายนอก
    val licenseNote: String = "",
)

enum class ComponentStatus {
    AVAILABLE,      // พบ binary / library พร้อมใช้
    INSTALLED,      // ติดตั้งแล้ว รอตรวจสอบ
    COMPATIBLE,     // ตรวจสอบแล้วว่าใช้ได้กับอุปกรณ์นี้
    MISSING,        // ไม่พบ — ต้องติดตั้ง
    UNSUPPORTED,    // อุปกรณ์ไม่รองรับ (เช่น Turnip บน non-Adreno GPU)
    REQUIRES_INSTALLATION,  // รองรับแต่ยังไม่ได้ติดตั้ง
    CHECKING,       // กำลังตรวจสอบ
    ERROR           // ตรวจสอบไม่ได้ด้วยเหตุผลอื่น
}

/** สถานะรวมของ Runtime ทั้งหมด */
data class RuntimeStatus(
    val components: List<RuntimeComponent>,
    val isReadyForLaunch: Boolean,
    val missingRequired: List<RuntimeComponent>,
    val warnings: List<String>,
)
