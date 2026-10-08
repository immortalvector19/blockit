package com.blockit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockit.ui.theme.FocusTheme

/**
 * Direction 3: The Chrono Prism Mark
 * A modern geometric hourglass horizon: two opposing balanced triangles
 * meeting at a single tangent node, split by a horizontal calm horizon line.
 * Represents conscious, preserved time and boundary discipline.
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

        // Normalization scale factor from original 108dp grid
        val sx = w / 108f
        val sy = h / 108f

        // Top Inverted Triangle Prism
        val topPath = Path().apply {
            moveTo(37f * sx, 34f * sy)
            lineTo(71f * sx, 34f * sy)
            lineTo(54f * sx, 51.5f * sy)
            close()
        }

        // Bottom Upright Triangle Prism
        val bottomPath = Path().apply {
            moveTo(54f * sx, 56.5f * sy)
            lineTo(71f * sx, 74f * sy)
            lineTo(37f * sx, 74f * sy)
            close()
        }

        // Horizontal Focal Horizon Bar
        val barLeft = 30f * sx
        val barTop = 52.5f * sy
        val barRight = 78f * sx
        val barBottom = 55.5f * sy
        val barRadius = 1.5f * sy

        drawPath(topPath, color = tint)
        drawPath(bottomPath, color = tint)
        drawRoundRect(
            color = tint,
            topLeft = Offset(barLeft, barTop),
            size = Size(barRight - barLeft, barBottom - barTop),
            cornerRadius = CornerRadius(barRadius, barRadius)
        )
    }
}
