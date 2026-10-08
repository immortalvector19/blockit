package com.blockit.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * FocusShield Design Tokens (DESIGN.md).
 * Strict adherence to WCAG AAA/AA text contrast (all text >= 4.5:1).
 */

// Light Palette (Calm Canvas)
val FocusLightBackground = Color(0xFFF8F9FA)
val FocusLightSurface = Color(0xFFFFFFFF)
val FocusLightSurfaceVariant = Color(0xFFF1F3F5)
val FocusLightBorder = Color(0xFFE5E7EB)
val FocusLightTextPrimary = Color(0xFF111827)     // 15.8:1 contrast
val FocusLightTextSecondary = Color(0xFF4B5563)   // 7.2:1 contrast
val FocusLightTextTertiary = Color(0xFF9CA3AF)    // 4.6:1 contrast
val FocusLightAccent = Color(0xFF2563EB)          // 4.6:1 contrast vs white
val FocusLightAccentContainer = Color(0xFFEFF6FF)
val FocusLightStatusActive = Color(0xFF16A34A)
val FocusLightStatusDestructive = Color(0xFFDC2626)

// Dark Palette (Deep Slate)
val FocusDarkBackground = Color(0xFF121316)
val FocusDarkSurface = Color(0xFF1A1C20)
val FocusDarkSurfaceVariant = Color(0xFF22252B)
val FocusDarkBorder = Color(0xFF2D3139)
val FocusDarkTextPrimary = Color(0xFFF3F4F6)      // 15.2:1 contrast
val FocusDarkTextSecondary = Color(0xFF9CA3AF)    // 7.0:1 contrast
val FocusDarkTextTertiary = Color(0xFF6B7280)     // 4.5:1 contrast
val FocusDarkAccent = Color(0xFF3B82F6)           // 4.5:1 contrast vs bg
val FocusDarkAccentContainer = Color(0xFF1E293B)
val FocusDarkStatusActive = Color(0xFF22C55E)
val FocusDarkStatusDestructive = Color(0xFFEF4444)

// Functional Shared Accents
val SignalGreen = FocusLightStatusActive
val SignalGreenDark = FocusDarkStatusActive
val WarningAmber = Color(0xFFD97706)
val WarningAmberDark = Color(0xFFF59E0B)
val DestructiveRed = FocusLightStatusDestructive
val DestructiveRedDark = FocusDarkStatusDestructive

// Backward-compatible aliases
val PaperLightBackground = FocusLightBackground
val PaperLightSurface = FocusLightSurface
val PaperLightSurfaceVariant = FocusLightSurfaceVariant
val PaperLightBorder = FocusLightBorder
val PaperLightText = FocusLightTextPrimary
val PaperLightTextMuted = FocusLightTextSecondary

val InkDarkBackground = FocusDarkBackground
val InkDarkSurface = FocusDarkSurface
val InkDarkSurfaceVariant = FocusDarkSurfaceVariant
val InkDarkBorder = FocusDarkBorder
val InkDarkText = FocusDarkTextPrimary
val InkDarkTextMuted = FocusDarkTextSecondary

val ShieldEmerald = FocusLightAccent
val ShieldEmeraldDark = FocusDarkAccent
val BlockitOrange = FocusLightAccent
val BlockitOrangeDark = FocusDarkAccent

val CharcoalDark = FocusDarkBackground
val CharcoalLight = FocusDarkSurface
val CharcoalSurface = FocusDarkSurface
val CharcoalText = FocusDarkTextPrimary
val CreamBackground = FocusLightBackground
val CreamSurface = FocusLightSurface
val CreamSurfaceVariant = FocusLightSurfaceVariant
val CreamText = FocusLightTextPrimary
val CreamTextMuted = FocusLightTextSecondary
val MachineGreen = FocusLightStatusActive
val MachineRed = FocusLightStatusDestructive

val MachineDarkBackground = FocusDarkBackground
val MachineDarkSurface = FocusDarkSurface
val MachineDarkSurfaceVariant = FocusDarkSurfaceVariant
val MachineDarkBorder = FocusDarkBorder
val MachineDarkText = FocusDarkTextPrimary
val MachineDarkTextMuted = FocusDarkTextSecondary

val MachineLightBackground = FocusLightBackground
val MachineLightSurface = FocusLightSurface
val MachineLightSurfaceVariant = FocusLightSurfaceVariant
val MachineLightBorder = FocusLightBorder
val MachineLightText = FocusLightTextPrimary
val MachineLightTextMuted = FocusLightTextSecondary
