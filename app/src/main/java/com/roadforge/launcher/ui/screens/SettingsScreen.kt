package com.roadforge.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadforge.launcher.settings.SettingsViewModel
import com.roadforge.launcher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel(),
) {
    val settings by vm.settings.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = RFBackground,
        topBar = {
            TopAppBar(
                title = { Text("การตั้งค่า", color = RFTextPrimary, fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ──────────────────────── DISPLAY ────────────────────────
            SectionHeader("หน้าจอ")
            SwitchRow(
                icon = Icons.Default.DarkMode,
                label = "ธีมมืด",
                checked = settings.darkTheme,
                onToggle = { vm.setDarkTheme(it) },
            )
            SwitchRow(
                icon = Icons.Default.Speed,
                label = "แสดง FPS Counter",
                checked = settings.showFps,
                onToggle = { vm.setShowFps(it) },
            )
            SwitchRow(
                icon = Icons.Default.Memory,
                label = "แสดง Performance Overlay",
                checked = settings.perfOverlay,
                onToggle = { vm.setPerfOverlay(it) },
            )

            Spacer(Modifier.height(4.dp))

            // ──────────────────────── TOUCH OVERLAY ─────────────────
            SectionHeader("Touch Overlay")
            SwitchRow(
                icon = Icons.Default.TouchApp,
                label = "เปิดใช้ Touch Overlay",
                sub = "แสดงปุ่มเกมทับหน้าจอ",
                checked = settings.touchOverlayEnabled,
                onToggle = { vm.setTouchOverlayEnabled(it) },
            )

            Spacer(Modifier.height(4.dp))

            // ──────────────────────── GENERAL ────────────────────────
            SectionHeader("ทั่วไป")
            SwitchRow(
                icon = Icons.Default.ExitToApp,
                label = "ยืนยันก่อนออก",
                checked = settings.confirmExit,
                onToggle = { vm.setConfirmExit(it) },
            )

            Spacer(Modifier.height(4.dp))

            // ──────────────────────── RUNTIME ────────────────────────
            SectionHeader("Runtime")
            InfoRow(
                icon = Icons.Default.Folder,
                label = "ไดเรกทอรี Runtime",
                value = settings.runtimeBaseDir.ifEmpty { "ค่าเริ่มต้น" },
            )

            Spacer(Modifier.height(4.dp))

            // ──────────────────────── ABOUT ──────────────────────────
            SectionHeader("เกี่ยวกับ")
            InfoRow(
                icon = Icons.Default.Info,
                label = "เวอร์ชัน",
                value = "0.1.0-alpha",
            )
            InfoRow(
                icon = Icons.Default.Gavel,
                label = "สิทธิ์การใช้งาน",
                value = "Wine: LGPL-2.1 · Box64: LGPL-2.1 · DXVK: zlib",
            )
            InfoRow(
                icon = Icons.Default.Warning,
                label = "คำเตือน",
                value = "ต้องใช้ไฟล์เกมที่ซื้อมาถูกต้องตามกฎหมายเท่านั้น",
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        color = RFAccent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    label: String,
    sub: String? = null,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RFSurface, RoundedCornerShape(6.dp))
            .border(1.dp, RFBorder, RoundedCornerShape(6.dp))
            .clickable { onToggle(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = RFTextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = RFTextPrimary, fontSize = 14.sp)
            sub?.let { Text(it, color = RFTextSecondary, fontSize = 11.sp) }
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = RFBackground,
                checkedTrackColor = RFAccent,
                uncheckedTrackColor = RFBorder,
            ),
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RFSurface, RoundedCornerShape(6.dp))
            .border(1.dp, RFBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = RFTextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = RFTextPrimary, fontSize = 14.sp)
            Text(value, color = RFTextSecondary, fontSize = 11.sp)
        }
    }
}
