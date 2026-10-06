package com.blockit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockit.domain.model.SessionStrictness
import com.blockit.ui.theme.BlockitTheme
import com.blockit.ui.theme.DestructiveRed
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StopSessionFrictionDialog(
    strictness: SessionStrictness,
    endTimeMs: Long?,
    onDismiss: () -> Unit,
    onConfirmStop: () -> Unit
) {
    val colors = BlockitTheme.colors

    if (strictness == SessionStrictness.LOCKED) {
        val formattedEndTime = if (endTimeMs != null) {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(endTimeMs))
        } else "scheduled time"

        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Focus Session Locked",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "This focus session was started in Locked mode to prevent self-interruption.",
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Early exit is disabled. Your focus block will unlock automatically at $formattedEndTime.",
                        color = colors.textMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Stay Focused", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(18.dp)
        )
        return
    }

    val isStrict = strictness == SessionStrictness.STRICT
    var secondsRemaining by remember { mutableIntStateOf(if (isStrict) 15 else 10) }
    var typedPhrase by remember { mutableStateOf("") }
    val requiredPhrase = "I want to break my focus"

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000)
            secondsRemaining--
        }
    }

    val canConfirm = secondsRemaining == 0 && (!isStrict || typedPhrase.trim() == requiredPhrase)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isStrict) "Strict Focus Challenge" else "Stop Focus Session Early?",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Stopping early interrupts your momentum and discipline. Take a breath before breaking your focus.",
                    color = colors.textMuted,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (secondsRemaining > 0) {
                    Text(
                        text = "Friction cooldown active: please reflect for $secondsRemaining seconds.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primary
                    )
                } else {
                    Text(
                        text = "Cooldown complete.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }

                if (isStrict) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "To confirm exit, type the exact phrase below:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Surface(
                        color = if (colors.isDark) colors.surfaceVariant else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = requiredPhrase,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.primary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = typedPhrase,
                        onValueChange = { typedPhrase = it },
                        placeholder = { Text(requiredPhrase, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmStop,
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DestructiveRed,
                    disabledContainerColor = DestructiveRed.copy(alpha = 0.25f),
                    disabledContentColor = Color.White.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = when {
                        secondsRemaining > 0 -> "Wait (${secondsRemaining}s)"
                        isStrict && !canConfirm -> "Type phrase"
                        else -> "Confirm Stop"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Focusing", color = colors.primary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(18.dp)
    )
}
