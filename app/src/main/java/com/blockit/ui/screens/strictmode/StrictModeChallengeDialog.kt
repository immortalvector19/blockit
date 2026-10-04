package com.blockit.ui.screens.strictmode

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blockit.domain.model.ChallengeType
import com.blockit.domain.model.StrictModeConfig
import kotlinx.coroutines.delay

@Composable
fun StrictModeChallengeDialog(
    config: StrictModeConfig,
    actionDescription: String,
    onDismiss: () -> Unit,
    onChallengeCompleted: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Strict Mode Friction")
            }
        },
        text = {
            Column {
                Text(
                    text = "Before you can $actionDescription, complete this deliberate pause challenge:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                when (config.challengeType) {
                    ChallengeType.PIN_PASSWORD -> {
                        PinPasswordChallengeContent(
                            configuredPin = config.unlockPin,
                            cooldownFallbackSeconds = config.cooldownSeconds,
                            onCorrectlyEntered = onChallengeCompleted,
                            onCancel = onDismiss
                        )
                    }
                    ChallengeType.COOLDOWN -> {
                        CooldownChallengeContent(
                            totalSeconds = config.cooldownSeconds,
                            onTimeElapsed = onChallengeCompleted,
                            onCancel = onDismiss
                        )
                    }
                    ChallengeType.TYPED_PASSAGE -> {
                        TypedPassageChallengeContent(
                            targetPassage = config.targetPassage,
                            onCorrectlyTyped = onChallengeCompleted,
                            onCancel = onDismiss
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun PinPasswordChallengeContent(
    configuredPin: String,
    cooldownFallbackSeconds: Int,
    onCorrectlyEntered: () -> Unit,
    onCancel: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showEmergencyCooldown by remember { mutableStateOf(false) }

    if (showEmergencyCooldown) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Emergency Unlock via Cooldown",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Take a mindful pause while the timer elapses to unlock without a PIN.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            CooldownChallengeContent(
                totalSeconds = cooldownFallbackSeconds.coerceAtLeast(30),
                onTimeElapsed = onCorrectlyEntered,
                onCancel = { showEmergencyCooldown = false }
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (configuredPin.isBlank()) "Enter 4 digits to confirm unlock:" else "Enter your 4-digit PIN to unlock:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Visual PIN Dots Indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            repeat(4) { index ->
                val isFilled = index < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            if (isFilled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hidden input / Numpad interface
        OutlinedTextField(
            value = enteredPin,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }.take(4)
                enteredPin = digits
                errorMessage = null
                if (digits.length == 4) {
                    if (configuredPin.isBlank() || digits == configuredPin) {
                        onCorrectlyEntered()
                    } else {
                        errorMessage = "Incorrect PIN. Try again."
                        enteredPin = ""
                    }
                }
            },
            label = { Text("4-Digit PIN") },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }

            if (configuredPin.isNotBlank()) {
                TextButton(onClick = { showEmergencyCooldown = true }) {
                    Text("Forgot PIN?", style = MaterialTheme.typography.bodySmall)
                }
            }

            Button(
                onClick = {
                    if (configuredPin.isBlank() || enteredPin == configuredPin) {
                        onCorrectlyEntered()
                    } else {
                        errorMessage = "Incorrect PIN. Try again."
                        enteredPin = ""
                    }
                },
                enabled = enteredPin.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Unlock")
            }
        }
    }
}

@Composable
private fun CooldownChallengeContent(
    totalSeconds: Int,
    onTimeElapsed: () -> Unit,
    onCancel: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(totalSeconds) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }

    val progress = 1f - (secondsLeft.toFloat() / totalSeconds.coerceAtLeast(1))

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.HourglassBottom,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (secondsLeft > 0) "Cooldown: $secondsLeft seconds remaining" else "Cooldown complete",
                style = MaterialTheme.typography.titleSmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Stay Focused")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onTimeElapsed,
                enabled = secondsLeft == 0,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Confirm")
            }
        }
    }
}

@Composable
private fun TypedPassageChallengeContent(
    targetPassage: String,
    onCorrectlyTyped: () -> Unit,
    onCancel: () -> Unit
) {
    var typedText by remember { mutableStateOf("") }
    val isMatch = typedText.trim() == targetPassage.trim()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Type the passage below exactly to continue:", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            Text(
                text = targetPassage,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = typedText,
            onValueChange = { typedText = it },
            label = { Text("Type passage here") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onCorrectlyTyped,
                enabled = isMatch,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Unlock")
            }
        }
    }
}
