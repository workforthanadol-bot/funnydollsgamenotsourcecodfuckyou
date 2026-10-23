package com.roadforge.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadforge.launcher.data.GameProfile
import com.roadforge.launcher.data.GameStatus
import com.roadforge.launcher.library.LibraryViewModel
import com.roadforge.launcher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onAddGame: () -> Unit,
    onConfigure: (String) -> Unit,
    onDiagnostics: (String) -> Unit,
    onSettings: () -> Unit,
    onEditOverlay: (String) -> Unit,
    vm: LibraryViewModel = viewModel(),
) {
    val games by vm.games.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = RFBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "RoadForge",
                        color = RFAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = 1.5.sp,
                    )
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "การตั้งค่า", tint = RFTextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RFBackground),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = RFAccent,
                contentColor = Color(0xFF001F24),
            ) {
                Icon(Icons.Default.Add, "เพิ่มเกม")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (games.isEmpty()) {
                EmptyLibraryState(
                    modifier = Modifier.align(Alignment.Center),
                    onAddGame = { showAddDialog = true },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(games, key = { it.id }) { game ->
                        GameCard(
                            game = game,
                            onConfigure = { onConfigure(game.id) },
                            onPlay = { vm.launchGame(game) },
                            onRemove = { vm.removeGame(game) },
                            onDiagnostics = { onDiagnostics(game.id) },
                            onEditOverlay = { onEditOverlay(game.id) },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddGameDialog(
            onSelectFolder = {
                showAddDialog = false
                vm.requestFolderPicker()
            },
            onSelectExe = {
                showAddDialog = false
                vm.requestExePicker()
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun EmptyLibraryState(modifier: Modifier = Modifier, onAddGame: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            Icons.Default.SportsEsports,
            contentDescription = null,
            tint = RFTextDisabled,
            modifier = Modifier.size(64.dp),
        )
        Text(
            "ยังไม่มีเกม",
            color = RFTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            "เพิ่มเกม Windows ของคุณเพื่อเริ่มต้น",
            color = RFTextSecondary,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onAddGame,
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(RFAccent)
            ),
        ) {
            Icon(Icons.Default.Add, null, tint = RFAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("เพิ่มเกมแรกของคุณ", color = RFAccent, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
private fun GameCard(
    game: GameProfile,
    onConfigure: () -> Unit,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onDiagnostics: () -> Unit,
    onEditOverlay: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RFSurface, RoundedCornerShape(6.dp))
            .border(1.dp, RFBorder, RoundedCornerShape(6.dp))
            .padding(12.dp),
    ) {
        // header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = RFAccent,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    game.name,
                    color = RFTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    game.exePath,
                    color = RFTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "เมนู", tint = RFTextSecondary)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(RFSurfaceVariant),
                ) {
                    DropdownMenuItem(
                        text = { Text("ตั้งค่า", color = RFTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Settings, null, tint = RFTextSecondary) },
                        onClick = { showMenu = false; onConfigure() },
                    )
                    DropdownMenuItem(
                        text = { Text("แก้ไข Overlay", color = RFTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.TouchApp, null, tint = RFTextSecondary) },
                        onClick = { showMenu = false; onEditOverlay() },
                    )
                    DropdownMenuItem(
                        text = { Text("วิเคราะห์ระบบ", color = RFTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.BugReport, null, tint = RFTextSecondary) },
                        onClick = { showMenu = false; onDiagnostics() },
                    )
                    Divider(color = RFBorder)
                    DropdownMenuItem(
                        text = { Text("ลบเกมออก", color = RFStatusError) },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = RFStatusError) },
                        onClick = { showMenu = false; onRemove() },
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // status + action row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GameStatusChip(game.status, game.errorMessage)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (game.status != GameStatus.READY) {
                    OutlinedButton(
                        onClick = onConfigure,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(RFBorder)
                        ),
                    ) {
                        Text("ตั้งค่า", color = RFTextSecondary, fontSize = 12.sp)
                    }
                }
                Button(
                    onClick = onPlay,
                    enabled = game.status == GameStatus.READY,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RFAccent,
                        disabledContainerColor = RFBorder,
                        contentColor = Color(0xFF001F24),
                        disabledContentColor = RFTextDisabled,
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                ) {
                    Text("PLAY", fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
                }
            }
        }

        // error message
        game.errorMessage?.let { msg ->
            Spacer(Modifier.height(6.dp))
            Text(
                msg,
                color = RFStatusError,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x22F44336), RoundedCornerShape(4.dp))
                    .padding(6.dp),
            )
        }
    }
}

@Composable
private fun GameStatusChip(status: GameStatus, error: String?) {
    val (label, color) = when (status) {
        GameStatus.READY           -> "พร้อมแล้ว" to RFStatusReady
        GameStatus.PREPARING       -> "กำลังเตรียม" to RFAccent
        GameStatus.NOT_CONFIGURED  -> "ยังไม่ได้ตั้งค่า" to RFTextSecondary
        GameStatus.INCOMPLETE      -> "ไฟล์ไม่ครบ" to RFStatusWarn
        GameStatus.RUNTIME_MISSING -> "Runtime ขาดหาย" to RFStatusWarn
        GameStatus.LAUNCH_FAILED   -> "Launch ล้มเหลว" to RFStatusError
        GameStatus.ERROR           -> "ข้อผิดพลาด" to RFStatusError
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
    ) {
        Text(
            label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun AddGameDialog(
    onSelectFolder: () -> Unit,
    onSelectExe: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RFSurface,
        title = { Text("เพิ่มเกม", color = RFTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("เลือกวิธีเพิ่มเกม:", color = RFTextSecondary, fontSize = 13.sp)
                Button(
                    onClick = onSelectFolder,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RFSurfaceVariant),
                ) {
                    Icon(Icons.Default.Folder, null, tint = RFAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("เลือกโฟลเดอร์เกม", color = RFTextPrimary)
                }
                Button(
                    onClick = onSelectExe,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RFSurfaceVariant),
                ) {
                    Icon(Icons.Default.InsertDriveFile, null, tint = RFAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("เลือกไฟล์ EXE", color = RFTextPrimary)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก", color = RFTextSecondary)
            }
        },
    )
}
