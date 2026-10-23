package com.roadforge.launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.roadforge.launcher.navigation.RoadForgeNavGraph
import com.roadforge.launcher.ui.theme.RoadForgeTheme

/**
 * MainActivity — single-activity host.
 *
 * ความรับผิดชอบ:
 * - ติดตั้ง RoadForgeTheme + NavHost
 * - จัดการ SAF pickers (folder + EXE)
 * - ขอ SYSTEM_ALERT_WINDOW ก่อนเปิด overlay
 */
class MainActivity : ComponentActivity() {

    // ─── SAF Launchers ──────────────────────────────────────────

    /** Callback ที่ Library/ViewModel จะ register เพื่อรับ folder URI */
    var onFolderPicked: ((Uri?) -> Unit)? = null

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        onFolderPicked?.invoke(uri)
        onFolderPicked = null
    }

    /** Callback สำหรับ EXE picker */
    var onExePicked: ((Uri?) -> Unit)? = null

    private val exePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        onExePicked?.invoke(uri)
        onExePicked = null
    }

    // ─── Overlay permission launcher ────────────────────────────

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // re-check handled by PermissionsViewModel.recheck() in LaunchedEffect
    }

    // ─── Entry point ─────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RoadForgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    RoadForgeNavGraph(navController = navController)
                }
            }
        }
    }

    // ─── Public helpers called by ViewModels / screens ──────────

    fun launchFolderPicker(callback: (Uri?) -> Unit) {
        onFolderPicked = callback
        folderPickerLauncher.launch(null)
    }

    fun launchExePicker(callback: (Uri?) -> Unit) {
        onExePicked = callback
        exePickerLauncher.launch(arrayOf("application/octet-stream", "*/*"))
    }

    fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }
}
