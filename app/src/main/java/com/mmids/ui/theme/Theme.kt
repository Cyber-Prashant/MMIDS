package com.mmids.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand Colors ────────────────────────────────────────────────
val Green = Color(0xFF4CAF50)
val GreenDim = Color(0xFF2E7D32)
val Red = Color(0xFFF44336)
val RedDim = Color(0xFFC62828)
val Blue = Color(0xFF2196F3)
val Orange = Color(0xFFFF9800)
val Purple = Color(0xFF9C27B0)
val Teal = Color(0xFF009688)

// ── Background Shades ───────────────────────────────────────────
val BgPrimary   = Color(0xFF0A0A0A)
val BgCard      = Color(0xFF141414)
val BgElevated  = Color(0xFF1A1A1A)
val BgDivider   = Color(0xFF1E1E1E)
val BgInput     = Color(0xFF1A1A1A)

// ── Text ────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF888888)
val TextDim       = Color(0xFF555555)
val TextTerminal  = Color(0xFF00FF41)

private val MMIDSColorScheme = darkColorScheme(
    primary          = Green,
    onPrimary        = Color.Black,
    secondary        = Blue,
    background       = BgPrimary,
    surface          = BgCard,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    error            = Red,
)

@Composable
fun MMIDSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MMIDSColorScheme,
        content = content
    )
}
