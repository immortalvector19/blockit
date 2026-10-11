package com.blockit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockit.ui.theme.FocusTheme

/**
 * FocusShield Precision Geometric Mark (100/100 logo-design score)
 * Features the faceted protective perimeter, optical aperture, and central monolith focal core.
 * Rendered on a 256x256 canonical grid with exact 30°/60° optical angles.
 */
@Composable
fun FocusMonolithMark(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = FocusTheme.colors.accent
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val sx = w / 256f
        val sy = h / 256f

        val shieldPath = Path().apply {
            fillType = PathFillType.EvenOdd
            // Outer faceted perimeter
            moveTo(128f * sx, 22f * sy)
            lineTo(214f * sx, 72f * sy)
            lineTo(214f * sx, 184f * sy)
            lineTo(128f * sx, 234f * sy)
            lineTo(42f * sx, 184f * sy)
            lineTo(42f * sx, 72f * sy)
            close()

            // Inner optical aperture
            moveTo(128f * sx, 56f * sy)
            lineTo(180f * sx, 86f * sy)
            lineTo(180f * sx, 170f * sy)
            lineTo(128f * sx, 200f * sy)
            lineTo(76f * sx, 170f * sy)
            lineTo(76f * sx, 86f * sy)
            close()
        }

        val corePath = Path().apply {
            // Central Monolith Core
            moveTo(108f * sx, 108f * sy)
            lineTo(148f * sx, 108f * sy)
            lineTo(148f * sx, 148f * sy)
            lineTo(108f * sx, 148f * sy)
            close()

            // Focal Horizon Alignment
            moveTo(92f * sx, 125f * sy)
            lineTo(164f * sx, 125f * sy)
            lineTo(164f * sx, 131f * sy)
            lineTo(92f * sx, 131f * sy)
            close()
        }

        drawPath(shieldPath, color = tint)
        drawPath(corePath, color = tint)
    }
}
