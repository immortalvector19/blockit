package com.blockit.ui.screens.focushome

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blockit.domain.model.BreakValidator
import com.blockit.domain.model.FocusSession
import com.blockit.domain.model.SessionStrictness
import com.blockit.ui.components.BreakWarningCard
import com.blockit.ui.components.FocusButton
import com.blockit.ui.components.FocusDestructiveButton
import com.blockit.ui.components.FocusMonolithMark
import com.blockit.ui.components.FocusOutlinedButton
import com.blockit.ui.components.StopSessionFrictionDialog
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography
import kotlinx.coroutines.delay

data class WhitelistAppInfo(
    val packageName: String,
    val appName: String,
    val iconBitmap: ImageBitmap? = null
)

@Composable
fun FocusHomeScreen(
    session: FocusSession?,
    profileName: String,
    permittedPackages: Set<String>,
    strictness: SessionStrictness,
    scheduleEndMs: Long? = null,
    isBreakActive: Boolean = false,
    breakExpiresAtMs: Long = 0L,
    onGrantBreak: ((durationMinutes: Int) -> Unit)? = null,
    onEndBreakEarly: (() -> Unit)? = null,
    onSessionExpired: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = FocusTheme.colors

    // Intercept back button completely to hold kiosk lock
    BackHandler(enabled = true) {
        // Trapped inside Focus Home during Allowlist lockdown
    }

    var hasEverObservedSession by remember { mutableStateOf(false) }

    LaunchedEffect(session) {
        if (session != null) {
            hasEverObservedSession = true
        } else if (hasEverObservedSession) {
            // Trigger expiry only when an active session was previously present and has now ended
            onSessionExpired()
        }
    }

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
            if (frozenRemainingMsOnBreak == null && session != null) {
                val candidate = if (breakExpiresAtMs > 0L && session.endTimestamp > breakExpiresAtMs) {
                    session.endTimestamp - breakExpiresAtMs
                } else {
                    (session.endTimestamp - currentTimeMs).coerceAtLeast(0L)
                }
                frozenRemainingMsOnBreak = candidate
            }
        } else {
            frozenRemainingMsOnBreak = null
        }
    }

    val remainingMs = remember(session, currentTimeMs, isBreakActive, frozenRemainingMsOnBreak) {
        if (session != null) {
            if (isBreakActive && frozenRemainingMsOnBreak != null) {
                frozenRemainingMsOnBreak!!
            } else {
                (session.endTimestamp - currentTimeMs).coerceAtLeast(0L)
            }
        } else 0L
    }

    LaunchedEffect(hasEverObservedSession, session, remainingMs, isBreakActive) {
        if (hasEverObservedSession && session != null && remainingMs <= 0L && !isBreakActive) {
            onSessionExpired()
        }
    }

    val totalDurationMs = remember(session) {
        if (session != null) {
            (session.endTimestamp - session.startTimestamp).coerceAtLeast(1000L)
        } else 1000L
    }

    val progress = (remainingMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)

    val remainingSeconds = (remainingMs / 1000) % 60
    val remainingMinutes = (remainingMs / (1000 * 60)) % 60
    val remainingHours = remainingMs / (1000 * 60 * 60)

    val timerText = if (remainingHours > 0) {
        String.format("%d:%02d:%02d", remainingHours, remainingMinutes, remainingSeconds)
    } else {
        String.format("%02d:%02d", remainingMinutes, remainingSeconds)
    }

    val breakRemainingMs = if (isBreakActive && breakExpiresAtMs > 0L) {
        (breakExpiresAtMs - currentTimeMs).coerceAtLeast(0L)
    } else 0L
    val breakMin = (breakRemainingMs / (1000 * 60)) % 60
    val breakSec = (breakRemainingMs / 1000) % 60
    val breakTimerStr = String.format("%02d:%02d", breakMin, breakSec)

    var showBreakDialog by remember { mutableStateOf(false) }

    // Load permitted app metadata asynchronously on IO dispatcher
    val permittedApps by androidx.compose.runtime.produceState<List<WhitelistAppInfo>>(
        initialValue = emptyList(),
        key1 = permittedPackages
    ) {
        val list = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            permittedPackages.map { pkg ->
                val label = com.blockit.core.cache.AppIconCache.getLabel(context, pkg)
                val icon = com.blockit.core.cache.AppIconCache.getIcon(context, pkg)
                WhitelistAppInfo(pkg, label.ifBlank { pkg }, icon)
            }.sortedBy { it.appName.lowercase() }
        }
        value = list
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(FocusSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Status Bar: Timer & Monolith Badge
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                    ) {
                        FocusMonolithMark(
                            size = 18.dp,
                            tint = colors.accent
                        )
                        Text(
                            text = profileName.ifBlank { "Allowlist focus" },
                            style = FocusTypography.title,
                            color = colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(FocusSpacing.md))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(130.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 6.dp,
                            color = colors.surfaceVariant,
                            trackColor = Color.Transparent
                        )
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 6.dp,
                            color = if (isBreakActive) colors.warning else colors.accent,
                            trackColor = Color.Transparent
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timerText,
                                style = FocusTypography.headline,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (isBreakActive) "PAUSED" else "remaining",
                                style = FocusTypography.caption.copy(
                                    fontWeight = if (isBreakActive) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isBreakActive) colors.warning else colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(FocusSpacing.sm))

                    if (isBreakActive) {
                        Surface(
                            shape = FocusShapes.small,
                            color = colors.accentContainer,
                            modifier = Modifier.padding(top = FocusSpacing.xxs)
                        ) {
                            Text(
                                text = "Break in progress: $breakTimerStr • Apps temporarily unlocked",
                                style = FocusTypography.caption.copy(fontWeight = FontWeight.Medium),
                                color = colors.accent,
                                modifier = Modifier.padding(horizontal = FocusSpacing.sm, vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = "Allowlist lockdown active: only permitted apps are launchable.",
                            style = FocusTypography.caption,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(FocusSpacing.lg))

            // Permitted Apps Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Permitted apps (${permittedApps.size})",
                    style = FocusTypography.label,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(FocusSpacing.xs))

            if (permittedApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(FocusShapes.medium)
                        .background(colors.surface)
                        .border(1.dp, colors.border, FocusShapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No additional apps permitted in this profile.\nUse Phone below if required.",
                        style = FocusTypography.body,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(FocusSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm),
                    contentPadding = PaddingValues(vertical = FocusSpacing.xxs)
                ) {
                    items(permittedApps, key = { it.packageName }) { app ->
                        PermittedAppTile(
                            app = app,
                            onClick = {
                                com.blockit.core.engine.EngineLifecycleMonitor.markPermittedAppLaunched(app.packageName)
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                                    context.startActivity(launchIntent)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(FocusSpacing.md))

            // Bottom Actions: Emergency Phone Call + Circular Break Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = FocusSpacing.sm),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emergency Phone Call Button (round 52dp button with phone icon)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        onClick = {
                            val dialerPkg = com.blockit.core.system.AndroidSystemAppResolver(context).getDefaultDialerPackage() ?: "com.android.dialer"
                            com.blockit.core.engine.EngineLifecycleMonitor.markPermittedAppLaunched(dialerPkg)
                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(dialIntent)
                        },
                        shape = CircleShape,
                        color = colors.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Phone,
                                contentDescription = "Emergency Call",
                                tint = colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Phone",
                        style = FocusTypography.caption,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(36.dp))

                if (isBreakActive) {
                    // Resume button when on break (accent-filled 52dp circular button with play icon)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            onClick = {
                                onEndBreakEarly?.invoke()
                            },
                            shape = CircleShape,
                            color = colors.accent,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Resume Focus",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Resume",
                            style = FocusTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.accent
                        )
                    }
                } else {
                    // Break button with the pause sign in a circle (round 52dp button with pause icon)
                    val isBreakAllowed = strictness != SessionStrictness.LOCKED
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            onClick = {
                                if (isBreakAllowed) {
                                    showBreakDialog = true
                                }
                            },
                            enabled = isBreakAllowed,
                            shape = CircleShape,
                            color = colors.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isBreakAllowed) colors.accent else colors.border
                            ),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isBreakAllowed) Icons.Rounded.Pause else Icons.Rounded.Lock,
                                    contentDescription = "Break",
                                    tint = if (isBreakAllowed) colors.accent else colors.textSecondary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Break",
                            style = FocusTypography.caption,
                            color = if (isBreakAllowed) colors.textSecondary else colors.border
                        )
                    }
                }
            }
        }
    }

    if (showBreakDialog) {
        BreakPickerDialog(
            session = session,
            remainingSessionMs = remainingMs,
            scheduleEndMs = scheduleEndMs,
            onDismiss = { showBreakDialog = false },
            onConfirmBreak = { mins ->
                showBreakDialog = false
                onGrantBreak?.invoke(mins)
            }
        )
    }
}

@Composable
fun BreakPickerDialog(
    session: FocusSession?,
    remainingSessionMs: Long,
    scheduleEndMs: Long? = null,
    onDismiss: () -> Unit,
    onConfirmBreak: (Int) -> Unit
) {
    val colors = FocusTheme.colors
    var selectedMinutes by remember { mutableIntStateOf(5) }
    val choices = listOf(5, 10, 20)

    val warningInfo = remember(selectedMinutes, remainingSessionMs, session, scheduleEndMs) {
        BreakValidator.evaluate(
            requestedMinutes = selectedMinutes,
            remainingSessionMs = remainingSessionMs,
            sessionEndMs = session?.endTimestamp ?: 0L,
            scheduleEndMs = scheduleEndMs
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Take a Break",
                style = FocusTypography.title,
                color = colors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.md)) {
                Text(
                    text = "Temporarily pause allowlist lockdown to use your phone. Choose break duration:",
                    style = FocusTypography.body,
                    color = colors.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
                ) {
                    choices.forEach { mins ->
                        val isSelected = selectedMinutes == mins
                        Surface(
                            onClick = { selectedMinutes = mins },
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
                                    text = "${mins} min",
                                    style = FocusTypography.label,
                                    color = if (isSelected) Color.White else colors.textPrimary
                                )
                            }
                        }
                    }
                }

                if (warningInfo.exceedsRemainingSession) {
                    BreakWarningCard(
                        warningInfo = warningInfo,
                        selectedMinutes = selectedMinutes
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
            ) {
                if (warningInfo.exceedsRemainingSession && warningInfo.cappedBreakMinutes in 1 until selectedMinutes) {
                    FocusOutlinedButton(
                        text = "Cap break to remaining (${warningInfo.cappedBreakMinutes} min)",
                        onClick = {
                            onConfirmBreak(warningInfo.cappedBreakMinutes)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                FocusButton(
                    text = if (warningInfo.exceedsRemainingSession) {
                        "Extend & start break ($selectedMinutes min)"
                    } else {
                        "Start break ($selectedMinutes min)"
                    },
                    onClick = {
                        onConfirmBreak(selectedMinutes)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = {
            FocusOutlinedButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = colors.surface,
        shape = FocusShapes.large
    )
}

@Composable
private fun PermittedAppTile(
    app: WhitelistAppInfo,
    onClick: () -> Unit
) {
    val colors = FocusTheme.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FocusShapes.medium)
            .border(1.dp, colors.border, FocusShapes.medium)
            .clickable(onClick = onClick),
        color = colors.surface,
        shape = FocusShapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FocusSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(FocusShapes.small)
                    .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (app.iconBitmap != null) {
                    Image(
                        bitmap = app.iconBitmap,
                        contentDescription = app.appName,
                        modifier = Modifier.size(38.dp)
                    )
                } else {
                    FocusMonolithMark(
                        size = 20.dp,
                        tint = colors.textTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(FocusSpacing.xs))

            Text(
                text = app.appName,
                style = FocusTypography.caption.copy(fontWeight = FontWeight.Medium),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
