package com.roadforge.launcher.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadforge.launcher.setup.SetupState
import com.roadforge.launcher.setup.TaskState
import com.roadforge.launcher.setup.SetupTask
import com.roadforge.launcher.setup.SetupViewModel
import com.roadforge.launcher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    gameId: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
    vm: SetupViewModel = viewModel(),
) {
    val setupState by vm.setupState.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val logLines by vm.logLines.collectAsStateWithLifecycle()

    LaunchedEffect(gameId) { vm.init(gameId) }

    // navigate away when done
    LaunchedEffect(setupState) {
        if (setupState is SetupState.Done) onDone()
    }

    Scaffold(
        containerColor = RFBackground,
        topBar = {
            TopAppBar(
                title = { Text("ตั้งค่าเกม", color = RFTextPrimary, fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Progress card
            ProgressCard(progress = progress, state = setupState)

            // Task list
            TaskListCard(tasks = tasks)

            // Log output
            if (logLines.isNotEmpty()) {
                LogCard(lines = logLines)
            }

            // Action buttons
            ActionRow(
                setupState = setupState,
                onStart = { vm.startSetup() },
                onCancel = { vm.cancelSetup(); onBack() },
                onRetry = { vm.startSetup() },
            )
        }
    }
}

@Composable
private fun ProgressCard(progress: Float, state: SetupState) {
    val stateColor = when (state) {
        is SetupState.Done    -> RFStatusReady
        is SetupState.Failed  -> RFStatusError
        is SetupState.Running -> RFAccent
        else                  -> RFTextSecondary
    }
    val stateLabel = when (state) {
        is SetupState.Idle      -> "รอดำเนินการ"
        is SetupState.Running   -> "กำลังตั้งค่า…"
        is SetupState.Done      -> "ตั้งค่าเสร็จสมบูรณ์"
        is SetupState.Failed    -> "ล้มเหลว: ${state.reason}"
        is SetupState.Cancelled -> "ยกเลิกแล้ว"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RFSurface, RoundedCornerShape(6.dp))
            .border(1.dp, RFBorder, RoundedCornerShape(6.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state is SetupState.Running) {
                val rotation by rememberInfiniteTransition(label = "spin").animateFloat(
                    initialValue = 0f, targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
                    label = "rotation",
                )
                Icon(
                    Icons.Default.Refresh,
                    null,
                    tint = RFAccent,
                    modifier = Modifier.size(18.dp).rotate(rotation),
                )
            } else {
                Icon(
                    if (state is SetupState.Done) Icons.Default.CheckCircle
                    else if (state is SetupState.Failed) Icons.Default.Error
                    else Icons.Default.HourglassEmpty,
                    null, tint = stateColor, modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(stateLabel, color = stateColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        // Real progress from task weights
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = stateColor,
            trackColor = RFBorder,
        )
        Text(
            "${(progress * 100).toInt()}%",
            color = RFTextSecondary,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun TaskListCard(tasks: List<SetupTask>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RFSurface, RoundedCornerShape(6.dp))
            .border(1.dp, RFBorder, RoundedCornerShape(6.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("ขั้นตอน", color = RFTextSecondary, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        tasks.forEach { task ->
            TaskRow(task = task)
        }
    }
}

@Composable
private fun TaskRow(task: SetupTask) {
    val (icon, tint) = when (task.state) {
        TaskState.SUCCESS -> Icons.Default.CheckCircle to RFStatusReady
        TaskState.FAILED  -> Icons.Default.Cancel to RFStatusError
        TaskState.SKIPPED -> Icons.Default.RemoveCircleOutline to RFTextDisabled
        TaskState.RUNNING -> Icons.Default.RadioButtonChecked to RFAccent
        TaskState.PENDING -> Icons.Default.RadioButtonUnchecked to RFTextDisabled
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (task.state == TaskState.RUNNING) {
            val rotation by rememberInfiniteTransition(label = "row${task.id}").animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
                label = "r",
            )
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp).rotate(rotation))
        } else {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(task.displayName, color = if (task.state == TaskState.PENDING) RFTextDisabled else RFTextPrimary, fontSize = 13.sp)
            task.message?.let { msg ->
                Text(msg, color = if (task.state == TaskState.FAILED) RFStatusError else RFTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LogCard(lines: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0A), RoundedCornerShape(6.dp))
            .border(1.dp, RFBorder, RoundedCornerShape(6.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text("LOG", color = RFTextDisabled, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(4.dp))
        lines.takeLast(20).forEach { line ->
            Text(
                line,
                color = RFTextSecondary,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun ActionRow(
    setupState: SetupState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (setupState) {
            is SetupState.Idle -> {
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = RFAccent),
                ) {
                    Text("เริ่มตั้งค่า", color = Color(0xFF001F24), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("ยกเลิก", color = RFTextSecondary)
                }
            }
            is SetupState.Running -> {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(RFStatusError)
                    ),
                ) {
                    Text("หยุด", color = RFStatusError)
                }
            }
            is SetupState.Failed, is SetupState.Cancelled -> {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = RFAccent),
                ) {
                    Text("ลองใหม่", color = Color(0xFF001F24), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("กลับ", color = RFTextSecondary)
                }
            }
            else -> {}
        }
    }
}
