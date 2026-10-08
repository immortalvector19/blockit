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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blockit.domain.model.SessionStrictness
import com.blockit.ui.components.FocusButton
import com.blockit.ui.components.FocusMonolithMark
import com.blockit.ui.components.FocusOutlinedButton
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography
import com.blockit.ui.theme.WarningAmber
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

    val colors = FocusTheme.colors

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FocusSpacing.xl, vertical = FocusSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Status Tag
                Surface(
                    shape = FocusShapes.small,
                    color = colors.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = FocusSpacing.sm, vertical = FocusSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                    ) {
                        FocusMonolithMark(
                            size = 16.dp,
                            tint = colors.accent
                        )
                        Text(
                            text = if (profileName.isNotBlank()) "$profileName is active" else "Focus session active",
                            style = FocusTypography.label,
                            color = colors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FocusSpacing.xxl))

                // App Icon Box
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(FocusShapes.medium)
                        .background(colors.surface)
                        .border(1.dp, colors.border, FocusShapes.medium),
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
                            imageVector = Icons.Rounded.HourglassTop,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FocusSpacing.lg))

                // Target App Name
                Text(
                    text = targetAppName,
                    style = FocusTypography.headline,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(FocusSpacing.xs))

                Text(
                    text = reason.ifBlank { "This app is restricted by $profileName" },
                    style = FocusTypography.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(FocusSpacing.xl))

                // If user opened Pause picker
                AnimatedVisibility(
                    visible = isPausePickerOpen,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(FocusShapes.medium)
                            .border(1.dp, colors.border, FocusShapes.medium),
                        color = colors.surface,
                        shape = FocusShapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(FocusSpacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Take a temporary pause",
                                style = FocusTypography.title,
                                color = colors.textPrimary
                            )

                            Spacer(modifier = Modifier.height(FocusSpacing.xxs))

                            Text(
                                text = "$remainingPauses of $maxPauses pauses remaining in this session",
                                style = FocusTypography.caption,
                                color = colors.textSecondary
                            )

                            Spacer(modifier = Modifier.height(FocusSpacing.md))

                            // Minute Choices: 1, 5, 10, 15
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                            ) {
                                minuteChoices.forEach { minutes ->
                                    val isSelected = selectedMinutes == minutes
                                    Surface(
                                        onClick = { selectedMinutes = minutes },
                                        shape = FocusShapes.small,
                                        color = if (isSelected) colors.accent else colors.surfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) colors.accent else colors.border
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = FocusSpacing.sm),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${minutes}m",
                                                style = FocusTypography.label,
                                                color = if (isSelected) Color.White else colors.textPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            if (isStrict) {
                                Spacer(modifier = Modifier.height(FocusSpacing.md))

                                if (strictCooldownSeconds > 0) {
                                    Text(
                                        text = "Strict cooldown: wait $strictCooldownSeconds seconds.",
                                        style = FocusTypography.caption.copy(fontWeight = FontWeight.Medium),
                                        color = WarningAmber
                                    )
                                } else {
                                    Text(
                                        text = "Type phrase below to confirm:",
                                        style = FocusTypography.caption,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = requiredPhrase,
                                        style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.accent
                                    )
                                    Spacer(modifier = Modifier.height(FocusSpacing.xs))
                                    OutlinedTextField(
                                        value = typedPhrase,
                                        onValueChange = { typedPhrase = it },
                                        placeholder = { Text(requiredPhrase) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = FocusShapes.small
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(FocusSpacing.lg))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
                            ) {
                                FocusOutlinedButton(
                                    text = "Cancel",
                                    onClick = { isPausePickerOpen = false },
                                    modifier = Modifier.weight(1f)
                                )

                                FocusButton(
                                    text = if (isStrict && strictCooldownSeconds > 0) {
                                        "${strictCooldownSeconds}s"
                                    } else {
                                        "Confirm ($selectedMinutes min)"
                                    },
                                    onClick = {
                                        if (canConfirmPause && onGrantPause != null) {
                                            onGrantPause(selectedMinutes)
                                        }
                                    },
                                    enabled = canConfirmPause,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Actions: "Go home" and "Pause"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = FocusSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(FocusSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary Action 1: "Go home"
                FocusButton(
                    text = "Go home",
                    leadingIcon = Icons.Rounded.Home,
                    onClick = onHomeClicked,
                    modifier = Modifier.fillMaxWidth()
                )

                // Primary Action 2: "Pause" button
                if (!isPausePickerOpen) {
                    val pauseButtonText = when {
                        isLocked -> "Pauses disabled in locked mode"
                        remainingPauses <= 0 -> "No pauses remaining"
                        else -> "Pause"
                    }

                    FocusOutlinedButton(
                        text = pauseButtonText,
                        leadingIcon = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.Pause,
                        onClick = {
                            if (!isLocked && remainingPauses > 0) {
                                isPausePickerOpen = true
                            }
                        },
                        enabled = !isLocked && remainingPauses > 0,
                        modifier = Modifier.fillMaxWidth()
                    )
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
