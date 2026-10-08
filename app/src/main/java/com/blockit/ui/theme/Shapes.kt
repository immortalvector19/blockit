package com.blockit.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * FocusShield corner radius tokens.
 * Exactly three radii used consistently across the entire application.
 */
object FocusShapes {
    val small = RoundedCornerShape(8.dp)    // Buttons, filter chips, compact inputs
    val medium = RoundedCornerShape(12.dp)  // Cards, list surfaces, item panels
    val large = RoundedCornerShape(20.dp)   // Bottom sheets, alert dialogs
}

val MaterialFocusShapes = Shapes(
    extraSmall = FocusShapes.small,
    small = FocusShapes.small,
    medium = FocusShapes.medium,
    large = FocusShapes.large,
    extraLarge = FocusShapes.large
)
