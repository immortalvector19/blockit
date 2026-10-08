package com.blockit.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography

/**
 * Material 3 TimePicker wrapped in an AlertDialog.
 * Features:
 * - Dial mode (TimePicker with AM/PM)
 * - Keyboard text entry mode (TimeInput)
 * - Toggle icon button switching between dial and keyboard entry
 * - Full configuration-change preservation (state surviving screen rotation)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimePickerDialog(
    initialMinuteOfDay: Int,
    title: String = "Select time",
    onDismiss: () -> Unit,
    onTimeSelected: (minuteOfDay: Int) -> Unit
) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val colors = FocusTheme.colors

    val initialH24 = ((initialMinuteOfDay % 1440 + 1440) % 1440) / 60
    val initialMin = ((initialMinuteOfDay % 1440 + 1440) % 1440) % 60

    val timePickerState = rememberTimePickerState(
        initialHour = initialH24,
        initialMinute = initialMin,
        is24Hour = is24Hour
    )

    var showDial by rememberSaveable { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = FocusShapes.large,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        title = {
            Text(
                text = title,
                style = FocusTypography.title.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showDial) {
                    TimePicker(state = timePickerState)
                } else {
                    TimeInput(state = timePickerState)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val totalMinutes = ((timePickerState.hour * 60) + timePickerState.minute) % 1440
                    onTimeSelected(totalMinutes)
                }
            ) {
                Text(
                    text = "OK",
                    style = FocusTypography.label.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primary
                )
            }
        },
        dismissButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Keyboard / Dial mode toggle button on bottom-left
                IconButton(onClick = { showDial = !showDial }) {
                    Icon(
                        imageVector = if (showDial) Icons.Default.Keyboard else Icons.Default.Schedule,
                        contentDescription = if (showDial) "Switch to text input" else "Switch to clock dial",
                        tint = colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(FocusSpacing.sm))
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        style = FocusTypography.label,
                        color = colors.textSecondary
                    )
                }
            }
        }
    )
}
