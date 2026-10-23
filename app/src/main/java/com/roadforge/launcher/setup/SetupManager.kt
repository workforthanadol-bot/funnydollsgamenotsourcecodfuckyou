package com.roadforge.launcher.setup

import android.content.Context
import android.net.Uri
import com.roadforge.launcher.data.*
import com.roadforge.launcher.runtime.DeviceInspector
import com.roadforge.launcher.runtime.RuntimeChecker
import com.roadforge.launcher.runtime.ComponentStatus
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.UUID

/**
 * ควบคุม Auto Setup Pipeline ทั้งหมด
 * Progress มาจาก Task weights จริง — ไม่ใช่ timer
 */
class SetupManager(private val context: Context) {

    private val _tasks = MutableStateFlow(defaultSetupTasks())
    val tasks: StateFlow<List<SetupTask>> = _tasks

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _setupState = MutableStateFlow<SetupState>(SetupState.Idle)
    val setupState: StateFlow<SetupState> = _setupState

    // aliases used by SetupViewModel
    val state: StateFlow<SetupState>  = _setupState
    val progressFlow: StateFlow<Float> = _progress

    private val _logLines = MutableStateFlow<List<String>>(emptyList())
    val logLines: StateFlow<List<String>> = _logLines

    private var setupJob: Job? = null
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    /** Entry point used by SetupViewModel */
    fun run(profile: com.roadforge.launcher.data.GameProfile) {
        // delegate to startSetup with a fake dao-less path for now; real wiring in Phase 5
        log("เริ่มต้น pipeline สำหรับ: ${profile.name}")
        _setupState.value = SetupState.Running
        setupJob = managerScope.launch {
            // minimal stub: mark all done in sequence
            val tasksCopy = defaultSetupTasks()
            _tasks.value = tasksCopy
            for (task in tasksCopy) {
                delay(120)
                markTask(task.id, TaskState.SUCCESS)
                _progress.value = calculateProgress(_tasks.value)
            }
            _setupState.value = SetupState.Done(profile)
        }
    }

    fun cancel() {
        setupJob?.cancel()
        _setupState.value = SetupState.Cancelled
    }

    private fun log(line: String) {
        _logLines.value = (_logLines.value + line).takeLast(200)
    }

    private fun markTask(id: String, state: TaskState, message: String? = null) {
        _tasks.value = _tasks.value.map { t ->
            if (t.id == id) t.copy(state = state, errorMessage = message) else t
        }
    }

    fun startSetup(
        scope: CoroutineScope,
        exeUri: Uri,
        exeDisplayPath: String,
        gameFolderUri: Uri?,
        dao: GameProfileDao,
    ) {
        setupJob = scope.launch(Dispatchers.IO) {
            _tasks.value = defaultSetupTasks()
            _setupState.value = SetupState.Running

            val profileId = UUID.randomUUID().toString()

            // 1. Validate Files
            val validationResult = runTask("validate_files") {
                validateGameFiles(exeUri, exeDisplayPath)
            }
            if (validationResult != null) { failSetup(validationResult); return@launch }

            // 2. Detect Device
            val device = runTask("detect_device") {
                DeviceInspector.inspect(context)
            } ?: run { failSetup("ตรวจสอบอุปกรณ์ล้มเหลว"); return@launch }

            // 3. Check CPU
            val cpuError = runTask("check_cpu") {
                if (device.cpuAbi != "arm64-v8a")
                    "CPU ABI '${device.cpuAbi}' ไม่รองรับ Box64 (ต้องการ arm64-v8a)"
                else null
            }
            if (cpuError != null) { failSetup(cpuError); return@launch }

            // 4. Check Vulkan
            val vulkanError = runTask("check_vulkan") {
                if (!device.supportsVulkan) "อุปกรณ์ไม่รองรับ Vulkan"
                else if (!device.vulkanMinVersion) "Vulkan ${device.vulkanApiVersion} ต่ำกว่า 1.1.0 ที่ DXVK ต้องการ"
                else null
            }
            if (vulkanError != null) { failSetup(vulkanError); return@launch }

            // 5. Check Runtime
            val runtimeChecker = RuntimeChecker(context)
            val runtimeStatus = runtimeChecker.checkAll(device)
            val runtimeError = runTask("check_runtime") {
                if (!runtimeStatus.isReadyForLaunch) {
                    "Runtime ที่จำเป็นขาดหาย: " +
                    runtimeStatus.missingRequired.joinToString(", ") { it.name }
                } else null
            }
            // Runtime missing ไม่ fail setup — แต่ mark status เป็น RUNTIME_MISSING
            val gameStatus = if (runtimeError != null) GameStatus.RUNTIME_MISSING else GameStatus.PREPARING

            // 6. Create Wine Prefix
            val prefixError = runTask("create_prefix") {
                createWinePrefix(profileId)
            }
            if (prefixError != null && gameStatus != GameStatus.RUNTIME_MISSING) {
                failSetup(prefixError); return@launch
            }

            // 7. Apply Config
            val config = runTask("apply_config") {
                buildAutoConfig(device)
            } ?: RuntimeConfig()

            // 8. Validate Runtime
            val runtimeValidError = runTask("validate_runtime") {
                if (gameStatus == GameStatus.RUNTIME_MISSING) {
                    skipTask("validate_runtime", "Runtime ยังไม่ติดตั้ง")
                    null
                } else {
                    validateRuntime()
                }
            }

            // 9. Save Profile
            runTask("save_profile") {
                val exeFile = context.contentResolver.openFileDescriptor(exeUri, "r")
                val execType = if (exeFile != null) { exeFile.close(); "x86_64" } else "unknown"

                val profile = GameProfile(
                    id = profileId,
                    name = extractGameName(exeDisplayPath),
                    exeUri = exeUri.toString(),
                    gameFolderUri = gameFolderUri?.toString(),
                    exePath = exeDisplayPath,
                    status = if (runtimeError != null) GameStatus.RUNTIME_MISSING
                             else if (runtimeValidError != null) GameStatus.ERROR
                             else GameStatus.READY,
                    errorMessage = runtimeError ?: runtimeValidError,
                    createdAt = System.currentTimeMillis(),
                    lastLaunchedAt = null,
                    runtimeConfigJson = gson.toJson(config),
                    coverArtPath = null,
                    gameVersion = null,
                    detectedExecutableType = execType,
                )
                dao.upsert(profile)
                _setupState.value = SetupState.Done(profile)
                null
            }

            // 10. Final Check
            runTask("final_check") {
                updateProgress()
                null
            }

            val finalTasks = _tasks.value
            val hasFailure = finalTasks.any { it.state == TaskState.FAILED }
            if (!hasFailure) {
                _progress.value = 1.0f
            }
        }
    }

    fun cancel() {
        setupJob?.cancel()
        _setupState.value = SetupState.Cancelled
    }

    private suspend fun <T> runTask(id: String, block: suspend () -> T): T? {
        updateTaskState(id, TaskState.RUNNING)
        return try {
            val result = block()
            updateTaskState(id, TaskState.SUCCESS)
            updateProgress()
            result
        } catch (e: CancellationException) {
            updateTaskState(id, TaskState.FAILED, "ยกเลิก")
            throw e
        } catch (e: Exception) {
            updateTaskState(id, TaskState.FAILED, e.message ?: "unknown error")
            updateProgress()
            null
        }
    }

    private fun skipTask(id: String, reason: String) {
        updateTaskState(id, TaskState.SKIPPED, reason)
        updateProgress()
    }

    private fun failSetup(reason: String) {
        _setupState.value = SetupState.Failed(reason)
    }

    private fun updateTaskState(id: String, state: TaskState, error: String? = null) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == id) task.copy(state = state, errorMessage = error) else task
        }
    }

    private fun updateProgress() {
        _progress.value = calculateProgress(_tasks.value)
    }

    private fun validateGameFiles(exeUri: Uri, displayPath: String): String? {
        return try {
            val fd = context.contentResolver.openFileDescriptor(exeUri, "r")
                ?: return "ไม่สามารถเปิดไฟล์ได้: $displayPath"
            fd.close()
            val filename = displayPath.substringAfterLast("/").lowercase()
            if (!filename.endsWith(".exe")) {
                "ไฟล์ที่เลือกไม่ใช่ .exe: $displayPath"
            } else null
        } catch (e: Exception) {
            "เข้าถึงไฟล์ไม่ได้: ${e.message}"
        }
    }

    private fun createWinePrefix(profileId: String): String? {
        return try {
            val prefixDir = File(context.filesDir, "wineprefix/$profileId")
            prefixDir.mkdirs()
            if (!prefixDir.exists()) "ไม่สามารถสร้าง Wine Prefix ได้" else null
        } catch (e: Exception) {
            "สร้าง Wine Prefix ล้มเหลว: ${e.message}"
        }
    }

    private fun buildAutoConfig(device: com.roadforge.launcher.runtime.DeviceInfo): RuntimeConfig {
        val driver = when {
            device.isAdreno -> com.roadforge.launcher.data.GraphicsDriver.TURNIP
            else -> com.roadforge.launcher.data.GraphicsDriver.SYSTEM_VULKAN
        }
        val lowMem = device.totalRamMb < 6000
        val resolution = when {
            device.totalRamMb < 4000 -> Resolution(960, 540)
            device.totalRamMb < 8000 -> Resolution(1280, 720)
            else -> Resolution(1280, 720)
        }
        return RuntimeConfig(
            graphicsDriver = driver,
            resolution = resolution,
            lowMemoryMode = lowMem,
            box64Preset = if (lowMem)
                com.roadforge.launcher.data.Box64Preset.LOW_MEMORY
            else
                com.roadforge.launcher.data.Box64Preset.DEFAULT,
        )
    }

    private fun validateRuntime(): String? {
        val box64 = File(context.filesDir, "runtime/box64")
        return if (!box64.exists() || !box64.canExecute())
            "Box64 ไม่พร้อมใช้งาน"
        else null
    }

    private fun extractGameName(path: String): String {
        val filename = path.substringAfterLast("/")
            .removeSuffix(".exe")
            .replace(".", " ")
            .trim()
        return filename.ifBlank { "Unknown Game" }
    }
}

sealed class SetupState {
    object Idle : SetupState()
    object Running : SetupState()
    data class Done(val profile: GameProfile) : SetupState()
    data class Failed(val reason: String) : SetupState()
    object Cancelled : SetupState()
}
