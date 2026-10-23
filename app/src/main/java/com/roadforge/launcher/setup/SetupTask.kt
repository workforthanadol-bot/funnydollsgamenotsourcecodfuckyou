package com.roadforge.launcher.setup

/** Task แต่ละขั้นตอนใน Auto Setup */
data class SetupTask(
    val id: String,
    val title: String,
    val description: String,
    val weight: Float,          // น้ำหนักสำหรับคำนวณ progress รวม (0.0–1.0)
    val state: TaskState = TaskState.PENDING,
    val errorMessage: String? = null,
    val isSkippable: Boolean = false,
) {
    /** alias สำหรับ UI */
    val displayName: String get() = title
    val message: String?    get() = errorMessage
}

enum class TaskState {
    PENDING,
    RUNNING,
    SUCCESS,
    SKIPPED,
    FAILED,
}

/** คำนวณ overall progress จาก weights ของ tasks ที่เสร็จแล้ว */
fun calculateProgress(tasks: List<SetupTask>): Float {
    val totalWeight = tasks.sumOf { it.weight.toDouble() }.toFloat()
    if (totalWeight == 0f) return 0f
    val doneWeight = tasks
        .filter { it.state == TaskState.SUCCESS || it.state == TaskState.SKIPPED }
        .sumOf { it.weight.toDouble() }
        .toFloat()
    return (doneWeight / totalWeight).coerceIn(0f, 1f)
}

/** รายการ tasks เริ่มต้นสำหรับ BeamNG.drive */
fun defaultSetupTasks(): List<SetupTask> = listOf(
    SetupTask("validate_files",   "ตรวจสอบไฟล์เกม",         "ตรวจสอบ EXE และไฟล์ประกอบ",      weight = 0.08f),
    SetupTask("detect_device",    "ตรวจสอบอุปกรณ์",          "อ่านข้อมูล CPU, RAM, GPU",         weight = 0.06f),
    SetupTask("check_cpu",        "ตรวจสอบ CPU Architecture", "ต้องการ arm64-v8a สำหรับ Box64",  weight = 0.06f),
    SetupTask("check_vulkan",     "ตรวจสอบ Vulkan",          "ตรวจสอบ Vulkan API version",       weight = 0.08f),
    SetupTask("check_runtime",    "ตรวจสอบ Runtime",         "Wine, Box64, DXVK",                weight = 0.10f),
    SetupTask("create_prefix",    "สร้าง Wine Prefix",        "สร้างสภาพแวดล้อม Windows",         weight = 0.15f),
    SetupTask("apply_config",     "ตั้งค่าเริ่มต้น",          "ใส่ค่าที่เหมาะกับอุปกรณ์",          weight = 0.10f),
    SetupTask("validate_runtime", "ตรวจสอบ Runtime ขั้นสุดท้าย","ทดสอบ Wine สามารถ start ได้",   weight = 0.15f),
    SetupTask("save_profile",     "บันทึกโปรไฟล์",           "บันทึกการตั้งค่าทั้งหมด",           weight = 0.10f),
    SetupTask("final_check",      "ตรวจสอบสุดท้าย",          "ยืนยันความพร้อมก่อน PLAY",         weight = 0.12f),
)
