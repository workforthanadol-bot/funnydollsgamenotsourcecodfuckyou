package com.roadforge.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadforge.launcher.data.TouchButton
import com.roadforge.launcher.data.TouchLayout
import com.roadforge.launcher.data.defaultBeamNGLayout
import com.roadforge.launcher.editor.OverlayEditorViewModel
import com.roadforge.launcher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayEditorScreen(
    layoutId: String,
    onDone: () -> Unit,
    vm: OverlayEditorViewModel = viewModel(),
) {
    val layout by vm.layout.collectAsStateWithLifecycle()
    val selected by vm.selectedButtonId.collectAsStateWithLifecycle()
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(layoutId) { vm.init(layoutId) }
    LaunchedEffect(saved) { if (saved) onDone() }

    Scaffold(
        containerColor = RFBackground,
        topBar = {
            TopAppBar(
                title = { Text("แก้ไข Touch Overlay", color = RFTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.Close, "ปิด", tint = RFTextSecondary)
                    }
                },
                actions = {
                    TextButton(onClick = { vm.reset(); }) {
                        Text("รีเซ็ต", color = RFStatusWarn)
                    }
                    TextButton(onClick = { vm.save(); saved = true }) {
                        Text("บันทึก", color = RFAccent, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RFBackground),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ─── Preview canvas ────────────────────────────────────
            layout?.let { l ->
                OverlayPreviewCanvas(
                    layout = l,
                    selectedId = selected,
                    onSelect = { vm.selectButton(it) },
                    onMove = { id, dx, dy, cw, ch ->
                        vm.moveButton(id, dx / cw, dy / ch)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }

            // ─── Selected button controls ──────────────────────────
            selected?.let { id ->
                layout?.buttons?.find { it.id == id }?.let { btn ->
                    ButtonPropertyPanel(
                        button = btn,
                        onToggleVisible = { vm.toggleVisible(id) },
                        onAlphaChange = { vm.setAlpha(id, it) },
                    )
                }
            } ?: Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun OverlayPreviewCanvas(
    layout: TouchLayout,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onMove: (id: String, dx: Float, dy: Float, cw: Float, ch: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var canvasWidth  by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .background(Color(0xFF111111))
            .onSizeChanged {
                canvasWidth  = it.width.toFloat()
                canvasHeight = it.height.toFloat()
            }
    ) {
        // grid lines for orientation
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val gridColor = Color(0xFF1E1E1E)
            for (i in 1..3) {
                drawLine(gridColor, Offset(size.width * i / 4f, 0f), Offset(size.width * i / 4f, size.height), 1f)
                drawLine(gridColor, Offset(0f, size.height * i / 4f), Offset(size.width, size.height * i / 4f), 1f)
            }
        }

        layout.buttons.forEach { btn ->
            if (!btn.isVisible) return@forEach
            if (canvasWidth == 0f || canvasHeight == 0f) return@forEach

            val xPx = with(density) { (btn.xFraction * canvasWidth).toDp() }
            val yPx = with(density) { (btn.yFraction * canvasHeight).toDp() }
            val wPx = with(density) { (btn.widthFraction * canvasWidth).toDp() }
            val hPx = with(density) { (btn.heightFraction * canvasHeight).toDp() }

            val isSelected = btn.id == selectedId

            Box(
                modifier = Modifier
                    .offset(xPx, yPx)
                    .size(wPx, hPx)
                    .background(
                        if (isSelected) RFAccent.copy(alpha = 0.30f) else Color(0x441A1A1A),
                        RoundedCornerShape(6.dp),
                    )
                    .border(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) RFAccent else Color(0x66FFFFFF),
                        RoundedCornerShape(6.dp),
                    )
                    .pointerInput(btn.id) {
                        detectDragGestures(
                            onDragStart = { onSelect(btn.id) },
                            onDrag = { _, drag ->
                                onMove(btn.id, drag.x, drag.y, canvasWidth, canvasHeight)
                            }
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    btn.label.take(4),
                    color = if (isSelected) RFAccent else Color.White,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun ButtonPropertyPanel(
    button: TouchButton,
    onToggleVisible: () -> Unit,
    onAlphaChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RFSurface)
            .border(1.dp, RFBorder)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("ปุ่ม: ${button.label}", color = RFTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("มองเห็น", color = RFTextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = button.isVisible,
                onCheckedChange = { onToggleVisible() },
                colors = SwitchDefaults.colors(checkedTrackColor = RFAccent, uncheckedTrackColor = RFBorder),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ความโปร่งใส", color = RFTextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("${(button.alpha * 100).toInt()}%", color = RFTextSecondary, fontSize = 12.sp, modifier = Modifier.width(40.dp))
        }
        Slider(
            value = button.alpha,
            onValueChange = onAlphaChange,
            valueRange = 0.1f..1.0f,
            colors = SliderDefaults.colors(thumbColor = RFAccent, activeTrackColor = RFAccent, inactiveTrackColor = RFBorder),
        )

        Text(
            "ลาก ปุ่มบน Canvas ด้านบนเพื่อย้ายตำแหน่ง",
            color = RFTextDisabled,
            fontSize = 11.sp,
        )
    }
}

