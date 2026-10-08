package com.blockit.ui.screens.profiles

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockit.ui.components.formatDaysSummary
import com.blockit.ui.components.formatMinuteOfDayAmPm
import com.blockit.ui.components.formatNextScheduleRun
import com.blockit.domain.model.BlockMode
import com.blockit.domain.model.FocusSession
import com.blockit.domain.model.ListType
import com.blockit.domain.model.Profile
import com.blockit.domain.model.ProfileType
import com.blockit.ui.components.CreateProfileDialog
import com.blockit.ui.components.FocusAlertDialog
import com.blockit.ui.components.FocusButton
import com.blockit.ui.components.FocusDestructiveButton
import com.blockit.ui.components.FocusEmptyState
import com.blockit.ui.components.FocusOutlinedButton
import com.blockit.ui.components.FocusSectionHeader
import com.blockit.ui.components.FocusTopBar
import com.blockit.ui.screens.strictmode.StrictModeChallengeDialog
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography
import kotlinx.coroutines.delay

@Composable
fun ProfilesScreen(
    viewModel: ProfilesViewModel,
    onProfileClick: (Long) -> Unit,
    onOpenSettingsClick: () -> Unit,
    onOpenInsightsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = FocusTheme.colors
    val profiles by viewModel.profiles.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val strictConfig by viewModel.strictModeConfig.collectAsState()

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    // Strict Mode friction interception state
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingDescription by remember { mutableStateOf("") }

    // Live permission detection
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var isOverlayEnabled by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val expected = "${context.packageName}/com.blockit.service.accessibility.BlockAccessibilityService"
        isAccessibilityEnabled = enabledServices.contains(expected)
        isOverlayEnabled = Settings.canDrawOverlays(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        refreshPermissions()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            FocusTopBar(
                title = "Profiles",
                showLogo = true,
                actions = {
                    IconButton(onClick = onOpenInsightsClick) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = "Stats",
                            tint = colors.textPrimary
                        )
                    }
                    IconButton(onClick = onOpenSettingsClick) {
                        Icon(
                            imageVector = if (strictConfig.isEnabled) Icons.Rounded.Lock else Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = if (strictConfig.isEnabled) colors.statusDestructive else colors.textPrimary
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(FocusSpacing.md),
            verticalArrangement = Arrangement.spacedBy(FocusSpacing.md)
        ) {
            // Live Status Card
            item {
                ProtectionStatusCard(
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    isOverlayEnabled = isOverlayEnabled,
                    profiles = profiles,
                    activeSession = activeSession,
                    onEnableAccessibility = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onEnableOverlay = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )
            }

            // Quick Focus Session Banner
            item {
                QuickFocusSessionCard(
                    activeSession = activeSession,
                    profiles = profiles,
                    onStartSession = { profileId, mins -> viewModel.startQuickSession(profileId, mins) },
                    onEndSession = { sessionId ->
                        if (strictConfig.isEnabled) {
                            pendingDescription = "end the active focus session early"
                            pendingAction = { viewModel.endSession(sessionId) }
                        } else {
                            viewModel.endSession(sessionId)
                        }
                    }
                )
            }

            if (profiles.isEmpty()) {
                item {
                    FocusEmptyState(
                        title = "No focus profiles yet",
                        message = "Create a profile to start blocking distractions.",
                        icon = Icons.Rounded.Layers,
                        actionButton = {
                            FocusButton(
                                text = "Create profile",
                                leadingIcon = Icons.Rounded.Add,
                                onClick = { showCreateDialog = true }
                            )
                        }
                    )
                }
            } else {
                item {
                    FocusSectionHeader(
                        title = "Profiles",
                        subtitle = "Select a profile to configure rules",
                        action = {
                            FocusOutlinedButton(
                                text = "Add",
                                leadingIcon = Icons.Rounded.Add,
                                onClick = { showCreateDialog = true }
                            )
                        }
                    )
                }

                items(profiles, key = { it.id }) { profile ->
                    ProfileCard(
                        profile = profile,
                        onToggle = { isEnabled ->
                            if (!isEnabled && strictConfig.isEnabled) {
                                pendingDescription = "turn off profile '${profile.name}'"
                                pendingAction = { viewModel.toggleProfileEnabled(profile.id, false) }
                            } else {
                                viewModel.toggleProfileEnabled(profile.id, isEnabled)
                            }
                        },
                        onClick = { onProfileClick(profile.id) },
                        onDelete = {
                            if (strictConfig.isEnabled) {
                                pendingDescription = "delete profile '${profile.name}'"
                                pendingAction = { viewModel.deleteProfile(profile.id) }
                            } else {
                                viewModel.deleteProfile(profile.id)
                            }
                        }
                    )
                }
            }
        }

        if (showCreateDialog) {
            CreateProfileDialog(
                onDismiss = { showCreateDialog = false },
                onConfirmWithDomains = { name, profileType, listType, daysMask, startMinute, endMinute, domains ->
                    viewModel.createProfile(
                        name = name,
                        profileType = profileType,
                        listType = listType,
                        daysMask = daysMask,
                        startMinute = startMinute,
                        endMinute = endMinute,
                        domains = domains
                    )
                    showCreateDialog = false
                }
            )
        }

        if (pendingAction != null) {
            StrictModeChallengeDialog(
                config = strictConfig,
                actionDescription = pendingDescription,
                onDismiss = { pendingAction = null },
                onChallengeCompleted = {
                    pendingAction?.invoke()
                    pendingAction = null
                }
            )
        }
    }
}

@Composable
private fun ProtectionStatusCard(
    isAccessibilityEnabled: Boolean,
    isOverlayEnabled: Boolean,
    profiles: List<Profile>,
    activeSession: FocusSession?,
    onEnableAccessibility: () -> Unit,
    onEnableOverlay: () -> Unit
) {
    val colors = FocusTheme.colors
    if (!isAccessibilityEnabled || !isOverlayEnabled) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.statusWarning.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, colors.statusWarning),
            shape = FocusShapes.medium
        ) {
            Column(modifier = Modifier.padding(FocusSpacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = colors.statusWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(FocusSpacing.xs))
                    Text(
                        text = "Needs setup",
                        style = FocusTypography.title,
                        color = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(FocusSpacing.xs))
                Text(
                    text = if (!isAccessibilityEnabled && !isOverlayEnabled)
                        "Accessibility service and overlay permission are required to block apps."
                    else if (!isAccessibilityEnabled)
                        "Accessibility service is required to detect open apps."
                    else
                        "Display over other apps is required to show the block screen.",
                    style = FocusTypography.caption,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(FocusSpacing.sm))
                FocusButton(
                    text = if (!isAccessibilityEnabled) "Enable accessibility" else "Enable overlay",
                    onClick = {
                        if (!isAccessibilityEnabled) onEnableAccessibility() else onEnableOverlay()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    } else {
        val anyWhitelistActive = profiles.any { it.isEnabled && it.blockMode == BlockMode.WHITELIST }
        val anyBlacklistActive = profiles.any { it.isEnabled && it.blockMode == BlockMode.BLACKLIST }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            shape = FocusShapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FocusSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = colors.statusActive,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(FocusSpacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Protection ready",
                        style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = when {
                            anyWhitelistActive -> "Allowlist kiosk active"
                            activeSession != null -> "Focus session active"
                            anyBlacklistActive -> "Blocklist guarding (${profiles.count { it.isEnabled }} active)"
                            else -> "Standby • Choose a profile to start"
                        },
                        style = FocusTypography.caption,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickFocusSessionCard(
    activeSession: FocusSession?,
    profiles: List<Profile>,
    onStartSession: (Long, Int) -> Unit,
    onEndSession: (Long) -> Unit
) {
    val colors = FocusTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        shape = FocusShapes.medium
    ) {
        Column(modifier = Modifier.padding(FocusSpacing.md)) {
            if (activeSession != null) {
                var remainingMs by remember(activeSession.endTimestamp) {
                    mutableLongStateOf((activeSession.endTimestamp - System.currentTimeMillis()).coerceAtLeast(0))
                }

                LaunchedEffect(activeSession.endTimestamp) {
                    while (remainingMs > 0) {
                        delay(1000)
                        remainingMs = (activeSession.endTimestamp - System.currentTimeMillis()).coerceAtLeast(0)
                    }
                }

                val mins = (remainingMs / 1000) / 60
                val secs = (remainingMs / 1000) % 60

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.HourglassTop,
                        contentDescription = null,
                        tint = colors.accent
                    )
                    Spacer(modifier = Modifier.width(FocusSpacing.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Focus session active",
                            style = FocusTypography.title,
                            color = colors.textPrimary
                        )
                        Text(
                            text = String.format("%02d:%02d remaining", mins, secs),
                            style = FocusTypography.caption,
                            color = colors.textSecondary
                        )
                    }
                    FocusDestructiveButton(
                        text = "End",
                        onClick = { onEndSession(activeSession.id) }
                    )
                }
            } else {
                Text(
                    text = "Quick focus",
                    style = FocusTypography.title,
                    color = colors.textPrimary
                )
                Text(
                    text = "Start a timer with the active profile:",
                    style = FocusTypography.caption,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(FocusSpacing.sm))

                val targetProfileId = profiles.firstOrNull()?.id ?: 1L
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                ) {
                    listOf(15, 25, 45, 60).forEach { mins ->
                        Surface(
                            onClick = { onStartSession(targetProfileId, mins) },
                            modifier = Modifier.weight(1f),
                            shape = FocusShapes.small,
                            color = colors.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = FocusSpacing.sm),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$mins",
                                    style = FocusTypography.title,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "min",
                                    style = FocusTypography.caption,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: Profile,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = FocusTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = FocusShapes.medium,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FocusSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(FocusSpacing.xs))
                    Surface(
                        shape = FocusShapes.small,
                        color = colors.surfaceVariant
                    ) {
                        Text(
                            text = if (profile.profileType == ProfileType.SCHEDULE) "Schedule" else "Focus",
                            style = FocusTypography.caption,
                            color = colors.textSecondary,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = FocusSpacing.xs, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = FocusShapes.small,
                        color = colors.surfaceVariant
                    ) {
                        Text(
                            text = if (profile.listType == ListType.ALLOWLIST || profile.blockMode == BlockMode.WHITELIST) "Allowlist" else "Blocklist",
                            style = FocusTypography.caption,
                            color = if (profile.listType == ListType.ALLOWLIST || profile.blockMode == BlockMode.WHITELIST) colors.accent else colors.textSecondary,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = FocusSpacing.xs, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${profile.appCount} apps, ${profile.domainCount} sites",
                    style = FocusTypography.caption,
                    color = colors.textSecondary
                )
                if (profile.profileType == ProfileType.SCHEDULE) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${formatDaysSummary(profile.scheduleDaysMask)} • ${formatMinuteOfDayAmPm(profile.scheduleStartMinute)}–${formatMinuteOfDayAmPm(profile.scheduleEndMinute)}",
                            style = FocusTypography.caption.copy(fontWeight = FontWeight.Medium),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Next: ${formatNextScheduleRun(profile.scheduleDaysMask, profile.scheduleStartMinute)}",
                        style = FocusTypography.caption,
                        color = colors.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(FocusSpacing.xs))

            Switch(
                checked = profile.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = colors.accent,
                    uncheckedThumbColor = colors.textTertiary,
                    uncheckedTrackColor = colors.surfaceVariant,
                    uncheckedBorderColor = colors.border
                )
            )
        }
    }
}

