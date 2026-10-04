package com.blockit.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockit.ui.theme.BlockitOrange
import com.blockit.ui.theme.BlockitTheme
import com.blockit.ui.theme.CharcoalDark
import com.blockit.ui.theme.CreamSurface
import kotlin.math.roundToInt

/**
 * Geometric Block Digit representing the authentic blockit pixel-font numbers.
 */
@Composable
fun BlockNumberDisplay(
    number: Int,
    modifier: Modifier = Modifier,
    digitCount: Int = 2,
    color: Color = CharcoalDark
) {
    val formatted = String.format("%0${digitCount}d", number)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        formatted.forEach { char ->
            Text(
                text = char.toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 90.sp,
                    letterSpacing = (-2).sp
                ),
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Authentic blockit digital clock display for Timer tab (e.g. 00:15).
 */
@Composable
fun BlockDigitalClock(
    firstValue: Int,
    secondValue: Int,
    modifier: Modifier = Modifier,
    color: Color = CharcoalDark,
    separator: String = ":"
) {
    val firstStr = String.format("%02d", firstValue)
    val secondStr = String.format("%02d", secondValue)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = firstStr,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 72.sp,
                letterSpacing = (-2).sp
            ),
            color = color
        )
        Text(
            text = " $separator ",
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 64.sp
            ),
            color = color
        )
        Text(
            text = secondStr,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 72.sp,
                letterSpacing = (-2).sp
            ),
            color = color
        )
    }
}

/**
 * 4-step progress indicator squares matching the video: [■] [□] [□] [□]
 */
@Composable
fun BlockitProgressSquares(
    currentStep: Int,
    totalSteps: Int = 4,
    modifier: Modifier = Modifier,
    activeColor: Color = BlockitOrange,
    inactiveColor: Color = Color(0xFFD6D4CC)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSteps) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (i < currentStep) activeColor else inactiveColor)
            )
        }
    }
}

/**
 * Tactile physical lever matching the video's "ANALOG" onboarding lever:
 * User drags the slider handle down until it triggers/clicks!
 */
@Composable
fun AnalogLever(
    onTrigger: () -> Unit,
    modifier: Modifier = Modifier,
    leverHeight: Dp = 160.dp,
    leverWidth: Dp = 64.dp
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val colors = BlockitTheme.colors
    var offsetY by remember { mutableFloatStateOf(0f) }
    val maxDragPx = with(density) { (leverHeight - 56.dp).toPx() }

    Box(
        modifier = modifier
            .width(leverWidth)
            .height(leverHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.TopCenter
    ) {
        // Vertical dashed tick marks inside the track
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(8) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(colors.textMuted.copy(alpha = 0.5f))
                )
            }
        }

        // Draggable Lever Knob
        Box(
            modifier = Modifier
                .offset { IntOffset(0, offsetY.roundToInt()) }
                .size(width = 54.dp, height = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.textPrimary)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        val newOffset = (offsetY + delta).coerceIn(0f, maxDragPx)
                        offsetY = newOffset
                    },
                    onDragStopped = {
                        if (offsetY >= maxDragPx * 0.75f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTrigger()
                        }
                        offsetY = 0f
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Grooves on lever
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color(0xFF8C8881))
                    )
                }
            }
        }
    }
}

/**
 * The iconic "SLIDE TO START / SLIDE TO STOP" tactile slider from the reference video.
 */
@Composable
fun SlideToActButton(
    text: String,
    isActive: Boolean,
    onTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val colors = BlockitTheme.colors
    var offsetX by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isActive) Color(0xFF291B24) else colors.surface)
            .border(
                1.dp,
                if (isActive) Color(0xFFDC2626).copy(alpha = 0.5f) else colors.border,
                RoundedCornerShape(18.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        val maxDragPx = with(LocalDensity.current) { (maxWidth - 60.dp).toPx() }

        // Centered helper text
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = if (isActive) Color(0xFFF87171) else colors.textMuted
            )
        }

        // Draggable Thumb
        val animatedOffsetX by animateFloatAsState(
            targetValue = offsetX,
            animationSpec = tween(durationMillis = 100),
            label = "slide_offset"
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .size(60.dp)
                .padding(4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isActive) Color(0xFFDC2626) else colors.primary)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(0f, maxDragPx)
                    },
                    onDragStopped = {
                        if (offsetX >= maxDragPx * 0.75f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTrigger()
                        }
                        offsetX = 0f
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
