package com.roadforge.launcher.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// =========================================================
// RoadForge Color System
// Dark-first. Accent = Cyan. Status colors semánticos.
// =========================================================

val RFBackground     = Color(0xFF0D0D0D)
val RFSurface        = Color(0xFF1A1A1A)
val RFSurfaceVariant = Color(0xFF242424)
val RFBorder         = Color(0xFF2E2E2E)

val RFAccent         = Color(0xFF00BCD4)   // Cyan — interactive elements
val RFAccentDim      = Color(0xFF007A8A)

val RFTextPrimary    = Color(0xFFE8E8E8)
val RFTextSecondary  = Color(0xFF8A8A8A)
val RFTextDisabled   = Color(0xFF444444)

val RFStatusReady    = Color(0xFF4CAF50)   // เขียว — Ready
val RFStatusWarn     = Color(0xFFFF9800)   // ส้ม — Warning
val RFStatusError    = Color(0xFFF44336)   // แดง — Error
val RFStatusInfo     = Color(0xFF2196F3)   // น้ำเงิน — Info

private val DarkColorScheme = darkColorScheme(
    primary            = RFAccent,
    onPrimary          = Color(0xFF001F24),
    primaryContainer   = RFAccentDim,
    onPrimaryContainer = Color(0xFFB2EBF2),
    secondary          = Color(0xFF607D8B),
    onSecondary        = Color(0xFFFFFFFF),
    background         = RFBackground,
    onBackground       = RFTextPrimary,
    surface            = RFSurface,
    onSurface          = RFTextPrimary,
    surfaceVariant     = RFSurfaceVariant,
    onSurfaceVariant   = RFTextSecondary,
    outline            = RFBorder,
    error              = RFStatusError,
    onError            = Color.White,
)

@Composable
fun RoadForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content,
    )
}
