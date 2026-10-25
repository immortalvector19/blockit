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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blockit.core.overlay.OverlayImageManager
import com.blockit.domain.model.BreakValidator
import com.blockit.domain.model.FocusGoal
import com.blockit.domain.model.OverlayBackgroundType
import com.blockit.domain.model.SessionStrictness
import com.blockit.ui.components.BreakWarningCard
import com.blockit.ui.components.FocusButton
import com.blockit.ui.components.FocusMonolithMark
import com.blockit.ui.components.FocusOutlinedButton
import com.blockit.ui.components.OverlayBackgroundView
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
    sessionEndMs: Long = 0L,
    scheduleEndMs: Long? = null,
    isBreakActive: Boolean = false,
    breakExpiresAtMs: Long = 0L,
    overlayBackgroundType: OverlayBackgroundType = OverlayBackgroundType.DEFAULT,
    overlayImageUris: List<String> = emptyList(),
    overlayCustomText: String? = null,
    goals: List<FocusGoal> = emptyList(),
    onToggleGoal: ((Long, Boolean) -> Unit)? = null,
    onAddGoal: ((String) -> Unit)? = null,
    onHomeClicked: () -> Unit,
    onGrantPause: ((durationMinutes: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val targetAppName = remember(targetIdentifier) { getAppName(context, targetIdentifier) }
    val targetAppIcon = remember(targetIdentifier) { getAppIcon(context, targetIdentifier) }

    val overlayBitmaps by androidx.compose.runtime.produceState<List<androidx.compose.ui.graphics.ImageBitmap?>>(
        initialValue = emptyList(),
        key1 = overlayImageUris
    ) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            overlayImageUris.map { path ->
                if (path.isNotBlank()) OverlayImageManager.loadBitmap(path) else null
            }
        }
    }

    // Pause / Break Dialog state
    var isPausePickerOpen by remember { mutableStateOf(false) }
    var selectedMinutes by remember { mutableIntStateOf(5) }
    val minuteChoices = listOf(5, 10, 20)

    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    var frozenRemainingMsOnBreak by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(isBreakActive) {
        if (isBreakActive) {
            if (frozenRemainingMsOnBreak == null && sessionEndMs > 0L) {
                val candidate = if (breakExpiresAtMs > 0L && sessionEndMs > breakExpiresAtMs) {
                    sessionEndMs - breakExpiresAtMs
                } else {
                    (sessionEndMs - currentTimeMs).coerceAtLeast(0L)
                }
                frozenRemainingMsOnBreak = candidate
            }
        } else {
            frozenRemainingMsOnBreak = null
        }
    }

    val remainingSessionMs = if (sessionEndMs > 0L) {
        if (isBreakActive && frozenRemainingMsOnBreak != null) {
            frozenRemainingMsOnBreak!!
        } else {
            (sessionEndMs - currentTimeMs).coerceAtLeast(0L)
        }
    } else 0L

    val sessionRemainingSeconds = (remainingSessionMs / 1000) % 60
    val sessionRemainingMinutes = (remainingSessionMs / (1000 * 60)) % 60
    val sessionRemainingHours = remainingSessionMs / (1000 * 60 * 60)

    val sessionTimerText = if (sessionRemainingHours > 0) {
        String.format("%d:%02d:%02d", sessionRemainingHours, sessionRemainingMinutes, sessionRemainingSeconds)
    } else {
        String.format("%02d:%02d", sessionRemainingMinutes, sessionRemainingSeconds)
    }

    val remainingBreakMs = if (isBreakActive && breakExpiresAtMs > 0L) {
        (breakExpiresAtMs - currentTimeMs).coerceAtLeast(0L)
    } else 0L
    val breakRemainingSeconds = (remainingBreakMs / 1000) % 60
    val breakRemainingMinutes = (remainingBreakMs / (1000 * 60)) % 60
    val breakTimerText = String.format("%02d:%02d", breakRemainingMinutes, breakRemainingSeconds)

    var hasObservedPositiveRemaining by remember(sessionEndMs) { mutableStateOf(false) }
    LaunchedEffect(remainingSessionMs) {
        if (remainingSessionMs > 0L) {
            hasObservedPositiveRemaining = true
        }
    }

    LaunchedEffect(sessionEndMs, remainingSessionMs, isBreakActive, hasObservedPositiveRemaining) {
        if (sessionEndMs > 0L && hasObservedPositiveRemaining && remainingSessionMs <= 0L && !isBreakActive) {
            onHomeClicked()
        }
    }

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

    Box(modifier = modifier.fillMaxSize()) {
        // Dynamic Overlay Background (Single photo, 4-photo collage, or default theme)
        OverlayBackgroundView(
            backgroundType = overlayBackgroundType,
            bitmaps = overlayBitmaps,
            modifier = Modifier.fillMaxSize()
        )

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
                    color = colors.surfaceVariant.copy(alpha = 0.92f),
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

                if (sessionEndMs > 0L) {
                    Spacer(modifier = Modifier.height(FocusSpacing.xs))
                    Surface(
                        shape = FocusShapes.small,
                        color = colors.surface.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = FocusSpacing.sm, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HourglassTop,
                                contentDescription = null,
                                tint = if (isBreakActive) colors.warning else colors.accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isBreakActive) "Break: $breakTimerText (Focus paused at $sessionTimerText)" else "$sessionTimerText remaining",
                                style = FocusTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isBreakActive) colors.warning else colors.accent
                            )
                        }
                    }
                }

                // Motivational Mantra / Quote Card ("Text over the text")
                if (!overlayCustomText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(FocusSpacing.md))
                    Surface(
                        shape = FocusShapes.medium,
                        color = Color.Black.copy(alpha = 0.60f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = FocusSpacing.sm)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = FocusSpacing.md, vertical = FocusSpacing.sm),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "“$overlayCustomText”",
                                style = FocusTypography.bodyMedium.copy(
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Focus Goals & Achievements Interactive Card
                Spacer(modifier = Modifier.height(FocusSpacing.md))
                OverlayGoalsCard(
                    goals = goals,
                    onToggleGoal = onToggleGoal,
                    onAddGoal = onAddGoal,
                    isCustomBackground = overlayBackgroundType != OverlayBackgroundType.DEFAULT,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = FocusSpacing.sm)
                )

                Spacer(modifier = Modifier.height(FocusSpacing.xl))

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
                                text = "Take a temporary break",
                                style = FocusTypography.title,
                                color = colors.textPrimary
                            )

                            Spacer(modifier = Modifier.height(FocusSpacing.xxs))

                            Text(
                                text = "$remainingPauses of $maxPauses breaks remaining in this session",
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

                            val warningInfo = remember(selectedMinutes, remainingSessionMs, sessionEndMs, scheduleEndMs) {
                                BreakValidator.evaluate(
                                    requestedMinutes = selectedMinutes,
                                    remainingSessionMs = remainingSessionMs,
                                    sessionEndMs = sessionEndMs,
                                    scheduleEndMs = scheduleEndMs
                                )
                            }

                            if (warningInfo.exceedsRemainingSession) {
                                Spacer(modifier = Modifier.height(FocusSpacing.sm))
                                BreakWarningCard(
                                    warningInfo = warningInfo,
                                    selectedMinutes = selectedMinutes
                                )
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

                            if (warningInfo.exceedsRemainingSession && warningInfo.cappedBreakMinutes in 1 until selectedMinutes) {
                                FocusOutlinedButton(
                                    text = "Cap break to remaining (${warningInfo.cappedBreakMinutes} min)",
                                    onClick = {
                                        if (canConfirmPause && onGrantPause != null) {
                                            onGrantPause(warningInfo.cappedBreakMinutes)
                                        }
                                    },
                                    enabled = canConfirmPause,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(FocusSpacing.xs))
                            }

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
                                    } else if (warningInfo.exceedsRemainingSession) {
                                        "Extend & start break ($selectedMinutes min)"
                                    } else {
                                        "Start break ($selectedMinutes min)"
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

            // Bottom Actions: "Go home" and circular Break button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = FocusSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isPausePickerOpen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FocusSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FocusButton(
                            text = "Go home",
                            leadingIcon = Icons.Rounded.Home,
                            onClick = onHomeClicked,
                            modifier = Modifier.weight(1f)
                        )

                        // Circular Break button with pause sign in a circle
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val isBreakEnabled = !isLocked && remainingPauses > 0
                            Surface(
                                onClick = {
                                    if (isBreakEnabled) {
                                        isPausePickerOpen = true
                                    }
                                },
                                enabled = isBreakEnabled,
                                shape = CircleShape,
                                color = colors.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (isBreakEnabled) colors.accent else colors.border
                                ),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.Pause,
                                        contentDescription = "Break",
                                        tint = if (isBreakEnabled) colors.accent else colors.textSecondary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Break",
                                style = FocusTypography.caption,
                                color = if (isBreakEnabled) colors.textSecondary else colors.border
                            )
                        }
                    }
                } else {
                    FocusButton(
                        text = "Go home",
                        leadingIcon = Icons.Rounded.Home,
                        onClick = onHomeClicked,
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

@Composable
private fun OverlayGoalsCard(
    goals: List<FocusGoal>,
    onToggleGoal: ((Long, Boolean) -> Unit)?,
    onAddGoal: ((String) -> Unit)?,
    isCustomBackground: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = FocusTheme.colors
    var isAddingGoal by remember { mutableStateOf(false) }
    var newGoalText by remember { mutableStateOf("") }

    val completedCount = goals.count { it.isCompleted }
    val totalCount = goals.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val allCompleted = totalCount > 0 && completedCount == totalCount

    Surface(
        shape = FocusShapes.medium,
        color = if (isCustomBackground) Color.Black.copy(alpha = 0.65f) else colors.surface.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCustomBackground) Color.White.copy(alpha = 0.22f) else colors.border
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FocusSpacing.md)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = if (allCompleted) WarningAmber else colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Focus Goals & Achievements",
                        style = FocusTypography.label.copy(fontWeight = FontWeight.Bold),
                        color = if (isCustomBackground) Color.White else colors.textPrimary
                    )
                }

                if (totalCount > 0) {
                    Surface(
                        shape = FocusShapes.small,
                        color = if (allCompleted) WarningAmber.copy(alpha = 0.2f) else colors.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (allCompleted) WarningAmber else colors.border
                        )
                    ) {
                        Text(
                            text = if (allCompleted) "All goals completed" else "$completedCount/$totalCount",
                            style = FocusTypography.caption.copy(fontWeight = FontWeight.Bold),
                            color = if (allCompleted) WarningAmber else (if (isCustomBackground) Color.White else colors.textPrimary),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Progress bar if goals exist
            if (totalCount > 0) {
                Spacer(modifier = Modifier.height(FocusSpacing.xs))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = if (allCompleted) WarningAmber else colors.accent,
                    trackColor = if (isCustomBackground) Color.White.copy(alpha = 0.2f) else colors.border
                )
            }

            Spacer(modifier = Modifier.height(FocusSpacing.sm))

            if (goals.isEmpty()) {
                Text(
                    text = "No focus goals set for today yet.",
                    style = FocusTypography.caption,
                    color = if (isCustomBackground) Color.White.copy(alpha = 0.7f) else colors.textSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    goals.forEach { goal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(FocusShapes.small)
                                .clickable {
                                    onToggleGoal?.invoke(goal.id, !goal.isCompleted)
                                }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (goal.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = if (goal.isCompleted) "Completed" else "Incomplete",
                                tint = if (goal.isCompleted) {
                                    colors.accent
                                } else {
                                    if (isCustomBackground) Color.White.copy(alpha = 0.6f) else colors.textTertiary
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = goal.title,
                                style = FocusTypography.bodySmall.copy(
                                    textDecoration = if (goal.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    fontWeight = if (goal.isCompleted) FontWeight.Normal else FontWeight.Medium
                                ),
                                color = if (goal.isCompleted) {
                                    if (isCustomBackground) Color.White.copy(alpha = 0.5f) else colors.textTertiary
                                } else {
                                    if (isCustomBackground) Color.White else colors.textPrimary
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Quick Add Goal Input
            if (onAddGoal != null) {
                Spacer(modifier = Modifier.height(FocusSpacing.xs))
                if (isAddingGoal) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                    ) {
                        OutlinedTextField(
                            value = newGoalText,
                            onValueChange = { newGoalText = it },
                            placeholder = {
                                Text(
                                    "New goal...",
                                    style = FocusTypography.caption,
                                    color = if (isCustomBackground) Color.White.copy(alpha = 0.6f) else colors.textTertiary
                                )
                            },
                            textStyle = FocusTypography.bodySmall.copy(
                                color = if (isCustomBackground) Color.White else colors.textPrimary
                            ),
                            singleLine = true,
                            shape = FocusShapes.small,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (newGoalText.isNotBlank()) {
                                    onAddGoal(newGoalText.trim())
                                    newGoalText = ""
                                    isAddingGoal = false
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(FocusShapes.small)
                                .background(colors.accent)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Save Goal",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                isAddingGoal = false
                                newGoalText = ""
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cancel",
                                tint = if (isCustomBackground) Color.White.copy(alpha = 0.7f) else colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(FocusShapes.small)
                            .clickable { isAddingGoal = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Add goal",
                            style = FocusTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.accent
                        )
                    }
                }
            }
        }
    }
}
