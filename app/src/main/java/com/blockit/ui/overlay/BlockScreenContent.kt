package com.blockit.ui.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockit.domain.model.SessionStrictness
import kotlinx.coroutines.delay

@Composable
fun BlockScreenContent(
    targetIdentifier: String,
    profileName: String,
    reason: String,
    strictness: SessionStrictness = SessionStrictness.NORMAL,
    canRequestPause: Boolean = true,
    remainingPauses: Int = 3,
    maxPauses: Int = 3,
    onHomeClicked: () -> Unit,
    onGrantPause: ((durationMinutes: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val targetAppName = remember(targetIdentifier) { getAppName(context, targetIdentifier) }
    val targetAppIcon = remember(targetIdentifier) { getAppIcon(context, targetIdentifier) }

    // Pause Dialog / Sheet state
    var isPausePickerOpen by remember { mutableStateOf(false) }
    var selectedMinutes by remember { mutableIntStateOf(5) }
    val minuteChoices = listOf(1, 5, 10, 15)

    // Strict Challenge State for Pause
    val isStrict = strictness == SessionStrictness.STRICT
    val isLocked = strictness == SessionStrictness.LOCKED

    var strictCooldownSeconds by remember { mutableIntStateOf(15) }
    var typedPhrase by remember { mutableStateOf("") }
    val requiredPhrase = "I want to pause"

    LaunchedEffect(isPausePickerOpen) {
        if (isPausePickerOpen && isStrict) {
            strictCooldownSeconds = 15
            typedPhrase = ""
            while (strictCooldownSeconds > 0) {
                delay(1000)
                strictCooldownSeconds--
            }
        }
    }

    val canConfirmPause = canRequestPause && !isLocked && remainingPauses > 0 && (
        !isStrict || (strictCooldownSeconds == 0 && typedPhrase.trim().equals(requiredPhrase, ignoreCase = true))
    )

    // Palette: ink dark background with high-contrast text and warm signal green accent
    val bgInk = Color(0xFF0F172A)
    val surfaceElevated = Color(0xFF1E293B)
    val borderDim = Color(0xFF334155)
    val signalGreen = Color(0xFF10B981)
    val textPrimary = Color(0xFFF8FAFC)
    val textMuted = Color(0xFF94A3B8)
    val alertAmber = Color(0xFFF59E0B)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = bgInk
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Shield Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(surfaceElevated)
                        .border(1.dp, borderDim, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = signalGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FocusShield: $profileName",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // App Icon Box
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(surfaceElevated)
                        .border(1.dp, borderDim, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (targetAppIcon != null) {
                        Image(
                            bitmap = targetAppIcon,
                            contentDescription = targetAppName,
                            modifier = Modifier.size(48.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = alertAmber,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Target App Name
                Text(
                    text = targetAppName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = reason.ifBlank { "App is blocked by $profileName" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = textMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // If user opened Pause picker
                AnimatedVisibility(
                    visible = isPausePickerOpen,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderDim, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = surfaceElevated),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Take a Temporary Pause",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "$remainingPauses of $maxPauses pauses remaining in this session",
                                style = MaterialTheme.typography.bodySmall,
                                color = textMuted
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Minute Choices: 1, 5, 10, 15
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                minuteChoices.forEach { minutes ->
                                    val isSelected = selectedMinutes == minutes
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedMinutes = minutes },
                                        label = {
                                            Text(
                                                text = "$minutes min",
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = signalGreen,
                                            selectedLabelColor = Color(0xFF0F172A),
                                            containerColor = Color(0xFF0F172A),
                                            labelColor = textPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }

                            if (isStrict) {
                                Spacer(modifier = Modifier.height(14.dp))

                                if (strictCooldownSeconds > 0) {
                                    Text(
                                        text = "Strict reflection cooldown: wait $strictCooldownSeconds seconds.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = alertAmber
                                    )
                                } else {
                                    Text(
                                        text = "Type phrase below to confirm pause:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = requiredPhrase,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = signalGreen
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
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

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { isPausePickerOpen = false },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Cancel", color = textMuted)
                                }

                                Button(
                                    onClick = {
                                        if (canConfirmPause && onGrantPause != null) {
                                            onGrantPause(selectedMinutes)
                                        }
                                    },
                                    enabled = canConfirmPause,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = signalGreen,
                                        contentColor = Color(0xFF0F172A),
                                        disabledContainerColor = borderDim,
                                        disabledContentColor = textMuted
                                    )
                                ) {
                                    Text(
                                        text = if (isStrict && strictCooldownSeconds > 0) {
                                            "${strictCooldownSeconds}s"
                                        } else {
                                            "Confirm ($selectedMinutes min)"
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Actions: "Go Home" and "Pause"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary Action 1: "Go home"
                Button(
                    onClick = onHomeClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = signalGreen,
                        contentColor = Color(0xFF0F172A)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Go Home",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Primary Action 2: "Pause" button
                if (!isPausePickerOpen) {
                    val pauseButtonText = when {
                        isLocked -> "Pauses Disabled (Locked Session)"
                        remainingPauses <= 0 -> "No Pauses Remaining"
                        else -> "Pause App"
                    }

                    OutlinedButton(
                        onClick = {
                            if (!isLocked && remainingPauses > 0) {
                                isPausePickerOpen = true
                            }
                        },
                        enabled = !isLocked && remainingPauses > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textPrimary,
                            disabledContentColor = textMuted
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!isLocked && remainingPauses > 0) borderDim else borderDim.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pauseButtonText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

private fun getAppName(context: Context, packageName: String): String {
    return try {
        val pm = context.packageManager
        val info = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(info).toString()
    } catch (_: Exception) {
        packageName.substringAfterLast('.')
    }
}

private fun getAppIcon(context: Context, packageName: String): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
