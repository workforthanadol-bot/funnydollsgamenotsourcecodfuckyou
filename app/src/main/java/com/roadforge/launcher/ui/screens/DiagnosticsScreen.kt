package com.roadforge.launcher.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadforge.launcher.diagnostics.DiagnosticsViewModel
import com.roadforge.launcher.runtime.ComponentStatus
import com.roadforge.launcher.runtime.RuntimeComponent
import com.roadforge.launcher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    gameId: String,
    onBack: () -> Unit,
    vm: DiagnosticsViewModel = viewModel(),
) {
    val device by vm.deviceInfo.collectAsStateWithLifecycle()
    val runtime by vm.runtimeStatus.collectAsStateWithLifecycle()
    val isChecking by vm.isChecking.collectAsStateWithLifecycle()

    LaunchedEffect(gameId) { vm.init(gameId) }

    Scaffold(
        containerColor = RFBackground,
        topBar = {
            TopAppBar(
                title = { Text("วิเคราะห์ระบบ", color = RFTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "กลับ", tint = RFTextSecondary)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.recheck() }, enabled = !isChecking) {
                        Icon(Icons.Default.Refresh, "ตรวจสอบใหม่", tint = RFAccent)
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isChecking) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = RFAccent,
                    trackColor = RFBorder,
                )
            }

            // ─── Device Info ───────────────────────────────────────
            SectionCard(title = "อุปกรณ์") {
                device?.let { d ->
                    DiagRow("CPU", "${d.cpuAbi} · ${d.cpuCores} cores")
                    DiagRow("RAM", "${d.totalRamMb} MB (ว่าง ${d.availableRamMb} MB)")
                    DiagRow("GPU", "${d.gpuVendor} ${d.gpuRenderer}")
                    DiagRow("Vulkan", if (d.supportsVulkan) "รองรับ (API ${d.vulkanApiVersion})" else "ไม่รองรับ")
                    DiagRow("Android SDK", "API ${d.androidSdk}")
                    DiagRow("พื้นที่ว่าง", "${d.availableStorageMb} MB")
                    if (d.isAdreno) DiagRow("Turnip", "✓ รองรับ (Adreno ${d.adrenoModel})")
                    if (d.isXclipse) DiagRow("GPU", "Samsung Xclipse — ใช้ System Vulkan เท่านั้น")
                } ?: Text("กำลังตรวจสอบ…", color = RFTextSecondary, fontSize = 13.sp)
            }

            // ─── Runtime Components ────────────────────────────────
            SectionCard(title = "Runtime Components") {
                runtime?.components?.forEach { comp ->
                    RuntimeComponentRow(comp)
                } ?: Text("กำลังตรวจสอบ…", color = RFTextSecondary, fontSize = 13.sp)
            }

            // ─── Overall Status ────────────────────────────────────
            runtime?.let { rt ->
                val allGood = rt.components.all {
                    it.status in listOf(ComponentStatus.AVAILABLE, ComponentStatus.INSTALLED,
                        ComponentStatus.COMPATIBLE, ComponentStatus.CHECKING)
                }
                val hasMissing = rt.components.any { it.status == ComponentStatus.MISSING }
                val (msg, color) = when {
                    allGood   -> "ระบบพร้อมใช้งาน" to RFStatusReady
                    hasMissing -> "Runtime ขาดหาย — ไม่สามารถรันเกมได้" to RFStatusError
                    else       -> "บางส่วนต้องการการติดตั้ง" to RFStatusWarn
                }
                SummaryBanner(msg, color)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RFSurface, RoundedCornerShape(6.dp))
            .border(1.dp, RFBorder, RoundedCornerShape(6.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title.uppercase(), color = RFAccent, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
        Divider(color = RFBorder, thickness = 0.5.dp)
        content()
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = RFTextSecondary, fontSize = 12.sp, modifier = Modifier.width(110.dp))
        Text(value, color = RFTextPrimary, fontSize = 12.sp)
    }
}

@Composable
private fun RuntimeComponentRow(comp: RuntimeComponent) {
    val (statusText, color) = when (comp.status) {
        ComponentStatus.AVAILABLE              -> "พร้อม" to RFStatusReady
        ComponentStatus.INSTALLED              -> "ติดตั้งแล้ว" to RFStatusReady
        ComponentStatus.COMPATIBLE             -> "รองรับ" to RFStatusReady
        ComponentStatus.MISSING                -> "ขาดหาย" to RFStatusError
        ComponentStatus.UNSUPPORTED            -> "ไม่รองรับ" to RFTextDisabled
        ComponentStatus.REQUIRES_INSTALLATION  -> "ต้องติดตั้ง" to RFStatusWarn
        ComponentStatus.CHECKING               -> "กำลังตรวจ…" to RFAccent
        ComponentStatus.ERROR                  -> "ข้อผิดพลาด" to RFStatusError
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(comp.name, color = RFTextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(statusText, color = color, fontSize = 11.sp)
    }
    comp.detail?.let {
        Text(it, color = RFTextDisabled, fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SummaryBanner(message: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Info, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(message, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
