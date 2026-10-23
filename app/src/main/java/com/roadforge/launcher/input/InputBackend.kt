package com.roadforge.launcher.input

import android.util.Log
import com.roadforge.launcher.data.InputMapping
import com.roadforge.launcher.data.MouseButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ส่ง Input ไปยัง Wine/X11 process ผ่าน xdotool
 *
 * ข้อจำกัดที่ชัดเจน:
 * - xdotool ต้องติดตั้งในรูปแบบ binary ใน runtimeDir
 * - ต้องมี DISPLAY :0 running (XServer ใน Wine environment)
 * - หากไม่มี xdotool หรือ DISPLAY → Input จะไม่ถึงเกม แต่แอปจะไม่ crash
 */
class InputBackend(private val runtimeDir: File) {

    private val xdotool = File(runtimeDir, "xdotool")
    private val display = ":0"

    val isAvailable: Boolean
        get() = xdotool.exists() && xdotool.canExecute()

    suspend fun keyDown(keySym: String) = sendXdotool("keydown", "--clearmodifiers", keySym)
    suspend fun keyUp(keySym: String) = sendXdotool("keyup", "--clearmodifiers", keySym)
    suspend fun keyPress(keySym: String) = sendXdotool("key", "--clearmodifiers", keySym)

    suspend fun mouseMove(x: Int, y: Int) = sendXdotool("mousemove", "--sync", "$x", "$y")
    suspend fun mouseClick(button: MouseButton) = sendXdotool(
        "click",
        when (button) { MouseButton.LEFT -> "1"; MouseButton.RIGHT -> "3"; MouseButton.MIDDLE -> "2" }
    )
    suspend fun mouseDown(button: MouseButton) = sendXdotool(
        "mousedown",
        when (button) { MouseButton.LEFT -> "1"; MouseButton.RIGHT -> "3"; MouseButton.MIDDLE -> "2" }
    )
    suspend fun mouseUp(button: MouseButton) = sendXdotool(
        "mouseup",
        when (button) { MouseButton.LEFT -> "1"; MouseButton.RIGHT -> "3"; MouseButton.MIDDLE -> "2" }
    )

    suspend fun sendMapping(mapping: InputMapping, pressed: Boolean) {
        mapping.wineKeySym?.let { sym ->
            if (pressed) keyDown(sym) else keyUp(sym)
        }
        mapping.mouseButton?.let { btn ->
            if (pressed) mouseDown(btn) else mouseUp(btn)
        }
    }

    /** ปล่อยทุก key ที่ค้างอยู่ — เรียกเมื่อ app ถูก pause หรือ game session สิ้นสุด */
    suspend fun releaseAll(activeMappings: Set<InputMapping>) {
        for (mapping in activeMappings) {
            mapping.wineKeySym?.let { keyUp(it) }
            mapping.mouseButton?.let { mouseUp(it) }
        }
    }

    private suspend fun sendXdotool(vararg args: String) = withContext(Dispatchers.IO) {
        if (!isAvailable) {
            Log.w("InputBackend", "xdotool ไม่พร้อม — input ถูก drop: ${args.joinToString(" ")}")
            return@withContext
        }
        try {
            val cmd = listOf(xdotool.absolutePath, *args)
            ProcessBuilder(cmd)
                .apply { environment()["DISPLAY"] = display }
                .start()
                .waitFor()
        } catch (e: Exception) {
            Log.e("InputBackend", "xdotool error: ${e.message}")
        }
    }
}
