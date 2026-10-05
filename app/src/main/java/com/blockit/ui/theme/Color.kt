package com.blockit.ui.theme

import androidx.compose.ui.graphics.Color

// FocusShield Design Tokens (tested for > 4.5:1 text contrast)

// Light Paper Palette
val PaperLightBackground = Color(0xFFF7F5F0) // Warm paper background
val PaperLightSurface = Color(0xFFFFFFFF)    // Crisp white elevated card surface
val PaperLightSurfaceVariant = Color(0xFFEFECE6)
val PaperLightBorder = Color(0xFFE2DDD5)     // Visible 1dp card border
val PaperLightText = Color(0xFF1A1C1E)       // Deep charcoal primary text (contrast > 14:1)
val PaperLightTextMuted = Color(0xFF5F6368)  // Accessible secondary text (contrast > 4.7:1)

// Dark Ink Palette
val InkDarkBackground = Color(0xFF0E1621)    // Deep ink background
val InkDarkSurface = Color(0xFF172233)       // Elevated panel surface
val InkDarkSurfaceVariant = Color(0xFF1E2D42)
val InkDarkBorder = Color(0xFF283952)        // Visible 1dp panel border
val InkDarkText = Color(0xFFF1F5F9)          // Crisp light primary text (contrast > 14:1)
val InkDarkTextMuted = Color(0xFF94A3B8)     // Accessible muted slate text (contrast > 4.8:1)

// Functional Signal Accents
val SignalGreen = Color(0xFF3DDC97)          // Crisp signal green for active states
val SignalGreenDark = Color(0xFF059669)      // Signal green for light mode contrast
val WarningAmber = Color(0xFFF59E0B)         // Amber for warnings / limits
val WarningAmberDark = Color(0xFFD97706)
val DestructiveRed = Color(0xFFEF4444)       // Red only for destructive actions
val DestructiveRedDark = Color(0xFFDC2626)

// Brand Primary Accent
val ShieldEmerald = Color(0xFF10B981)
val ShieldEmeraldDark = Color(0xFF059669)

// Backward-compatible aliases
val BlockitOrange = ShieldEmerald
val BlockitOrangeDark = ShieldEmeraldDark
val CharcoalDark = InkDarkBackground
val CharcoalLight = InkDarkSurface
val CharcoalSurface = InkDarkSurface
val CharcoalText = InkDarkText
val CreamBackground = PaperLightBackground
val CreamSurface = PaperLightSurface
val CreamSurfaceVariant = PaperLightSurfaceVariant
val CreamText = PaperLightText
val CreamTextMuted = PaperLightTextMuted
val MachineGreen = SignalGreen
val MachineRed = DestructiveRed

val MachineDarkBackground = InkDarkBackground
val MachineDarkSurface = InkDarkSurface
val MachineDarkSurfaceVariant = InkDarkSurfaceVariant
val MachineDarkBorder = InkDarkBorder
val MachineDarkText = InkDarkText
val MachineDarkTextMuted = InkDarkTextMuted

val MachineLightBackground = PaperLightBackground
val MachineLightSurface = PaperLightSurface
val MachineLightSurfaceVariant = PaperLightSurfaceVariant
val MachineLightBorder = PaperLightBorder
val MachineLightText = PaperLightText
val MachineLightTextMuted = PaperLightTextMuted
