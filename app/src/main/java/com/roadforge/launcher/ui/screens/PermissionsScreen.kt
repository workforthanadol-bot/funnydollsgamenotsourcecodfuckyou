package com.roadforge.launcher.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadforge.launcher.permissions.PermissionsViewModel
import com.roadforge.launcher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onGranted: () -> Unit,
    onBack: () -> Unit,
    vm: PermissionsViewModel = viewModel(),
) {
    val ctx = LocalContext.current
    val hasOverlay    by vm.hasOverlay.collectAsStateWithLifecycle()
    val hasStorage    by vm.hasStorage.collectAsStateWithLifecycle()
    val allGranted    by vm.allGranted.collectAsStateWithLifecycle()

    // auto-advance once all permissions are in
    LaunchedEffect(allGranted) {
        if (allGranted) onGranted()
    }

    // re-check when screen resumes (user comes back from Settings)
    LaunchedEffect(Unit) { vm.recheck() }

    Scaffold(
        containerColor = RFBackground,
        topBar = {
            TopAppBar(
                title = { Text("สิทธิ์การใช้งาน", color = RFTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "กลับ", tint = RFTextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RFBackground),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Explanation banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RFAccent.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                    .border(1.dp, RFAccent.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Default.Info, null, tint = RFAccent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "RoadForge Launcher ต้องการสิทธิ์เหล่านี้เพื่อทำงานได้อย่างถูกต้อง " +
                    "สิทธิ์แต่ละอย่างจำเป็นสำหรับฟังก์ชันที่ระบุไว้",
                    color = RFTextSecondary,
                    fontSize = 13.sp,
                )
            }

            // Overlay permission
            PermissionCard(
                icon = Icons.Default.Layers,
                title = "แสดงทับหน้าจออื่น",
                description = "จำเป็นสำหรับ Touch Overlay ที่แสดงปุ่มเกมทับหน้าจอ Wine",
                required = true,
                granted = hasOverlay,
                onRequest = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${ctx.packageName}")
                    )
                    ctx.startActivity(intent)
                },
            )

            // Storage permission
            PermissionCard(
                icon = Icons.Default.Folder,
                title = "เข้าถึงไฟล์",
                description = "จำเป็นสำหรับอ่านไฟล์เกมจาก Storage ของคุณ",
                required = true,
                granted = hasStorage,
                onRequest = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${ctx.packageName}")
                    )
                    ctx.startActivity(intent)
                },
            )

            Spacer(Modifier.weight(1f))

            // Continue button
            Button(
                onClick = onGranted,
                modifier = Modifier.fillMaxWidth(),
                enabled = allGranted,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RFAccent,
                    disabledContainerColor = RFBorder,
                    contentColor = androidx.compose.ui.graphics.Color(0xFF001F24),
                    disabledContentColor = RFTextDisabled,
                ),
            ) {
                Text(
                    if (allGranted) "ดำเนินการต่อ" else "กรุณาอนุญาตทุกสิทธิ์",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    required: Boolean,
    granted: Boolean,
    onRequest: () -> Unit,
) {
    val borderColor = when {
        granted  -> RFStatusReady.copy(alpha = 0.4f)
        required -> RFStatusWarn.copy(alpha = 0.4f)
        else     -> RFBorder
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RFSurface, RoundedCornerShape(6.dp))
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (granted) RFStatusReady else RFTextSecondary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = RFTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (required) Text("จำเป็น", color = RFStatusWarn, fontSize = 11.sp)
            }
            if (granted) {
                Icon(Icons.Default.CheckCircle, "อนุญาตแล้ว", tint = RFStatusReady, modifier = Modifier.size(20.dp))
            }
        }

        Text(description, color = RFTextSecondary, fontSize = 12.sp)

        if (!granted) {
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = RFSurfaceVariant),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Default.OpenInNew, null, tint = RFAccent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("เปิดการตั้งค่า", color = RFAccent, fontSize = 13.sp)
            }
        }
    }
}
