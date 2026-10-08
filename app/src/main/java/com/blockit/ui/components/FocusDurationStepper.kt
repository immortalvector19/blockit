package com.blockit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockit.domain.model.FocusDurationState
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography
import kotlinx.coroutines.delay

/**
 * Format hours and minutes into clean human-readable label: "1 h 25 min", "25 min", "2 h".
 */
fun formatDurationReadout(hours: Int, minutes: Int): String {
    return when {
        hours > 0 && minutes > 0 -> "$hours h $minutes min"
        hours > 0 && minutes == 0 -> "$hours h"
        else -> "$minutes min"
    }
}

/**
 * Stepper duration component featuring:
 * - Preset chips (15m, 25m, 45m, 60m)
 * - 5-minute decrement / increment stepper with hold-to-repeat
 * - Large central readout like "1 h 25 min"
 * - Tapping readout opens numeric typing dialog
 * - "Ends at" time computed from same state with injectable clock
 */
@Composable
fun FocusDurationStepper(
    durationState: FocusDurationState,
    onDurationChanged: (FocusDurationState) -> Unit,
    modifier: Modifier = Modifier,
    presets: List<Int> = listOf(15, 25, 45, 60)
) {
    val colors = FocusTheme.colors
    var showEditDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FocusSpacing.md)
    ) {
        // 1. Presets Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
        ) {
            presets.forEach { mins ->
                val isSelected = durationState.totalMinutes == mins
                Surface(
                    onClick = {
                        onDurationChanged(durationState.withTotalMinutes(mins))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = FocusShapes.small,
                    color = if (isSelected) colors.accent else colors.surfaceVariant,
                    border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${mins}m",
                            style = FocusTypography.label.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) Color.White else colors.textPrimary
                        )
                    }
                }
            }
        }

        // 2. Stepper & Readout Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = FocusShapes.medium,
            color = colors.surfaceVariant,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = FocusSpacing.md, horizontal = FocusSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Minus 5 min button with hold-to-repeat
                    HoldRepeatingButton(
                        onTrigger = {
                            val newTotal = (durationState.totalMinutes - 5).coerceAtLeast(5)
                            onDurationChanged(durationState.withTotalMinutes(newTotal))
                        },
                        enabled = durationState.totalMinutes > 5,
                        icon = Icons.Default.Remove,
                        contentDescription = "Decrease 5 minutes"
                    )

                    // Large Readout (Tappable to type)
                    Surface(
                        onClick = { showEditDialog = true },
                        shape = FocusShapes.small,
                        color = Color.Transparent
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = FocusSpacing.sm, vertical = FocusSpacing.xs)
                        ) {
                            Text(
                                text = formatDurationReadout(durationState.hours, durationState.minutes),
                                style = FocusTypography.headline.copy(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Tap to type duration",
                                tint = colors.textSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Plus 5 min button with hold-to-repeat
                    HoldRepeatingButton(
                        onTrigger = {
                            val newTotal = (durationState.totalMinutes + 5).coerceAtMost(720)
                            onDurationChanged(durationState.withTotalMinutes(newTotal))
                        },
                        enabled = durationState.totalMinutes < 720,
                        icon = Icons.Default.Add,
                        contentDescription = "Increase 5 minutes"
                    )
                }

                Spacer(modifier = Modifier.height(FocusSpacing.xs))

                // "Ends at" computed from single state
                Text(
                    text = durationState.formatEndsAt(),
                    style = FocusTypography.caption.copy(fontWeight = FontWeight.Medium),
                    color = colors.accent
                )
            }
        }
    }

    // Direct Typing Dialog
    if (showEditDialog) {
        DurationTypeDialog(
            initialHours = durationState.hours,
            initialMinutes = durationState.minutes,
            onDismiss = { showEditDialog = false },
            onConfirm = { h, m ->
                val totalMins = (h * 60 + m).coerceIn(1, 720)
                onDurationChanged(durationState.withTotalMinutes(totalMins))
                showEditDialog = false
            }
        )
    }
}

/**
 * Clean button implementing instant tap + 400ms delay hold-to-repeat.
 */
@Composable
private fun HoldRepeatingButton(
    onTrigger: () -> Unit,
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String
) {
    val colors = FocusTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed && enabled) {
            delay(400)
            while (isPressed && enabled) {
                onTrigger()
                delay(100)
            }
        }
    }

    Surface(
        onClick = onTrigger,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) colors.surface else colors.surfaceVariant,
        border = BorderStroke(1.dp, if (enabled) colors.border else Color.Transparent),
        interactionSource = interactionSource,
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) colors.textPrimary else colors.textSecondary.copy(alpha = 0.38f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Dialog allowing user to type custom hours and minutes directly.
 */
@Composable
fun DurationTypeDialog(
    initialHours: Int,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (hours: Int, minutes: Int) -> Unit
) {
    val colors = FocusTheme.colors
    var hoursText by rememberSaveable { mutableStateOf(initialHours.toString()) }
    var minutesText by rememberSaveable { mutableStateOf(initialMinutes.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = FocusShapes.large,
        containerColor = colors.surface,
        title = {
            Text(
                text = "Set duration",
                style = FocusTypography.title.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FocusSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(2)
                        hoursText = digits
                    },
                    label = { Text("Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = ":",
                    style = FocusTypography.headline.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )

                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(2)
                        minutesText = digits
                    },
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val h = hoursText.toIntOrNull() ?: 0
                    val m = minutesText.toIntOrNull() ?: 0
                    onConfirm(h.coerceIn(0, 12), m.coerceIn(0, 59))
                }
            ) {
                Text(
                    text = "Set",
                    style = FocusTypography.label.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = FocusTypography.label,
                    color = colors.textSecondary
                )
            }
        }
    )
}
