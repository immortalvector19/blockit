package com.blockit.ui.screens.dashboard

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockit.domain.model.BlockMode
import com.blockit.domain.model.BlockedApp
import com.blockit.domain.model.BlockedDomain
import com.blockit.domain.model.FocusSession
import com.blockit.domain.model.Profile
import com.blockit.domain.model.Schedule
import com.blockit.domain.model.TimeSavedCalculator
import com.blockit.domain.model.UsageInsightsData
import com.blockit.ui.components.BlockDigitalClock
import com.blockit.ui.components.SlideToActButton
import com.blockit.ui.screens.strictmode.StrictModeChallengeDialog
import com.blockit.ui.theme.BlockitOrange
import com.blockit.ui.theme.BlockitTheme
import com.blockit.ui.theme.CharcoalDark
import com.blockit.ui.theme.CharcoalSurface
import com.blockit.ui.theme.CreamBackground
import com.blockit.ui.theme.CreamSurface
import com.blockit.ui.theme.CreamSurfaceVariant
import com.blockit.ui.theme.CreamText
import com.blockit.ui.theme.CreamTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun MainDashboardScreen(
    viewModel: MainDashboardViewModel,
    onAddAppsClick: (Long) -> Unit,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Timer, 1: Bags, 2: Routines, 3: Statistics

    val profiles by viewModel.profiles.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val strictConfig by viewModel.strictModeConfig.collectAsState()
    val insightsData by viewModel.insightsData.collectAsState()
    val selectedProfileId by viewModel.selectedProfileId.collectAsState()
    val packedApps by viewModel.packedApps.collectAsState()
    val packedDomains by viewModel.packedDomains.collectAsState()
    val schedules by viewModel.schedules.collectAsState()

    // Strict Mode friction state
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingDescription by remember { mutableStateOf("") }
    var showStopConfirmDialog by remember { mutableStateOf(false) }
    var pendingStopSessionId by remember { mutableStateOf<Long?>(null) }

    // Live permission check
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var isOverlayEnabled by remember { mutableStateOf(false) }
    var isBatteryIgnored by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        isAccessibilityEnabled = com.blockit.core.permission.PermissionIntents.isAccessibilityServiceEnabled(context)
        isOverlayEnabled = com.blockit.core.permission.PermissionIntents.canDrawOverlays(context)
        isBatteryIgnored = com.blockit.core.permission.PermissionIntents.isIgnoringBatteryOptimizations(context)
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

    val isProtected = isAccessibilityEnabled && isOverlayEnabled && isBatteryIgnored
    val activeProfile = profiles.find { it.id == activeSession?.profileId }
    val colors = BlockitTheme.colors

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            DashboardTopBar(
                activeSession = activeSession,
                activeProfileName = activeProfile?.name,
                isProtected = isProtected,
                isDark = colors.isDark,
                onToggleDarkMode = { viewModel.toggleDarkMode() },
                onStatusClick = {
                    if (!isAccessibilityEnabled) {
                        com.blockit.core.permission.PermissionIntents.openAccessibilitySettings(context)
                    } else if (!isOverlayEnabled) {
                        com.blockit.core.permission.PermissionIntents.openOverlaySettings(context)
                    } else if (!isBatteryIgnored) {
                        com.blockit.core.permission.PermissionIntents.requestBatteryExemption(context)
                    } else {
                        onOpenSettingsClick()
                    }
                },
                onSettingsClick = onOpenSettingsClick
            )
        },
        bottomBar = {
            DashboardBottomBar(
                selectedTab = selectedTab,
                onTabSelect = { selectedTab = it }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "dashboard_tab"
            ) { tab ->
                when (tab) {
                    0 -> TimerTabContent(
                        profiles = profiles,
                        activeSession = activeSession,
                        selectedProfileId = selectedProfileId,
                        isProtected = isProtected,
                        onFixPermissions = {
                            if (!isAccessibilityEnabled) {
                                com.blockit.core.permission.PermissionIntents.openAccessibilitySettings(context)
                            } else if (!isOverlayEnabled) {
                                com.blockit.core.permission.PermissionIntents.openOverlaySettings(context)
                            } else if (!isBatteryIgnored) {
                                com.blockit.core.permission.PermissionIntents.requestBatteryExemption(context)
                            }
                        },
                        onSelectProfile = { viewModel.selectProfile(it) },
                        onStartSession = { profId, mins -> viewModel.startSession(profId, mins) },
                        onEndSession = { sessId ->
                            val hasPinLock = strictConfig.unlockPin.isNotBlank() && strictConfig.requirePinToStopSession
                            if (strictConfig.isEnabled || hasPinLock) {
                                pendingDescription = "stop the ongoing focus session early"
                                pendingAction = { viewModel.endSession(sessId) }
                            } else {
                                showStopConfirmDialog = true
                                pendingStopSessionId = sessId
                            }
                        },
                        onNavigateToProfiles = { selectedTab = 1 }
                    )
                    1 -> ProfilesTabContent(
                        profiles = profiles,
                        selectedProfileId = selectedProfileId,
                        packedApps = packedApps,
                        packedDomains = packedDomains,
                        activeSession = activeSession,
                        isProtected = isProtected,
                        onFixPermissions = {
                            if (!isAccessibilityEnabled) {
                                com.blockit.core.permission.PermissionIntents.openAccessibilitySettings(context)
                            } else if (!isOverlayEnabled) {
                                com.blockit.core.permission.PermissionIntents.openOverlaySettings(context)
                            } else if (!isBatteryIgnored) {
                                com.blockit.core.permission.PermissionIntents.requestBatteryExemption(context)
                            }
                        },
                        onSelectProfile = { viewModel.selectProfile(it) },
                        onSetProfileMode = { id, mode -> viewModel.setProfileMode(id, mode) },
                        onToggleProfile = { id, enabled ->
                            if (!enabled && strictConfig.isEnabled) {
                                pendingDescription = "turn off this profile"
                                pendingAction = { viewModel.toggleProfileEnabled(id, false) }
                            } else {
                                viewModel.toggleProfileEnabled(id, enabled)
                            }
                        },
                        onAddApps = { id -> onAddAppsClick(id) },
                        onRemoveApp = { id, pkg -> viewModel.removeApp(id, pkg) },
                        onAddDomain = { id, pattern, wildcard -> viewModel.addDomain(id, pattern, wildcard) },
                        onRemoveDomain = { domainId -> viewModel.removeDomain(domainId) },
                        onCreateProfile = { name, mode -> viewModel.createProfile(name, mode) },
                        onDeleteProfile = { id ->
                            if (strictConfig.isEnabled) {
                                pendingDescription = "delete this profile"
                                pendingAction = { viewModel.deleteProfile(id) }
                            } else {
                                viewModel.deleteProfile(id)
                            }
                        }
                    )
                    2 -> RoutinesTabContent(
                        profiles = profiles,
                        selectedProfileId = selectedProfileId,
                        schedules = schedules,
                        onSelectProfile = { viewModel.selectProfile(it) },
                        onSaveSchedule = { viewModel.saveSchedule(it) },
                        onDeleteSchedule = { viewModel.deleteSchedule(it) },
                        onNavigateToProfiles = { selectedTab = 1 }
                    )
                    3 -> StatisticsTabContent(
                        insightsData = insightsData
                    )
                }
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

            if (showStopConfirmDialog && pendingStopSessionId != null) {
                StopSessionFrictionDialog(
                    onDismiss = {
                        showStopConfirmDialog = false
                        pendingStopSessionId = null
                    },
                    onConfirmStop = {
                        val id = pendingStopSessionId
                        showStopConfirmDialog = false
                        pendingStopSessionId = null
                        if (id != null) viewModel.endSession(id)
                    }
                )
            }
        }
    }
}

@Composable
private fun StopSessionFrictionDialog(
    onDismiss: () -> Unit,
    onConfirmStop: () -> Unit
) {
    val colors = BlockitTheme.colors
    var secondsRemaining by remember { mutableIntStateOf(10) }

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000)
            secondsRemaining--
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Stop Focus Session Early?",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Stopping early interrupts your momentum and discipline. Take a breath before deciding to abandon your focus session.",
                    color = colors.textMuted
                )
                Spacer(modifier = Modifier.height(14.dp))
                if (secondsRemaining > 0) {
                    Text(
                        text = "Friction cooldown active: please reflect for $secondsRemaining seconds.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primary
                    )
                } else {
                    Text(
                        text = "Cooldown complete. You may confirm to end your session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmStop,
                enabled = secondsRemaining == 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                    disabledContentColor = Color.White.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (secondsRemaining > 0) "Wait (${secondsRemaining}s)" else "Confirm Stop")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Focusing", color = colors.primary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun DashboardTopBar(
    activeSession: FocusSession?,
    activeProfileName: String?,
    isProtected: Boolean,
    isDark: Boolean,
    onToggleDarkMode: () -> Unit,
    onStatusClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val colors = BlockitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Status Pill
        val pillBg = when {
            activeSession != null -> if (colors.isDark) Color(0xFF14532D).copy(alpha = 0.4f) else Color(0xFFDCFCE7)
            isProtected -> if (colors.isDark) colors.surfaceVariant else Color(0xFFF1F5F9)
            else -> if (colors.isDark) Color(0xFF451A1A) else Color(0xFFFFE4E6)
        }
        val pillBorder = when {
            activeSession != null -> if (colors.isDark) Color(0xFF16A34A) else Color(0xFF86EFAC)
            isProtected -> colors.border
            else -> if (colors.isDark) Color(0xFF7F1D1D) else Color(0xFFFDA4AF)
        }
        val dotColor = when {
            activeSession != null -> Color(0xFF22C55E) // Signal Green
            isProtected -> Color(0xFF10B981) // Ready Green
            else -> Color(0xFFEF4444) // Warning Red
        }
        val pillText = when {
            activeSession != null -> if (!activeProfileName.isNullOrBlank()) "Active: $activeProfileName" else "Active"
            isProtected -> "Ready"
            else -> "Action Req."
        }

        Surface(
            onClick = onStatusClick,
            shape = RoundedCornerShape(20.dp),
            color = pillBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, pillBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = pillText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Center Brand Identity
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "FocusShield",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = colors.textPrimary
            )
        }

        // Right Actions: Dark Mode Quick Toggle + Settings Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onToggleDarkMode,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDark) "Switch to Light Mode" else "Switch to Dark Mode",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardBottomBar(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit
) {
    val colors = BlockitTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        shape = RoundedCornerShape(28.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Icons.Default.Timer to "Timer",
                Icons.Default.Shield to "Profiles",
                Icons.Default.Schedule to "Routines",
                Icons.Default.BarChart to "Insights"
            )

            tabs.forEachIndexed { index, (icon, label) ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) colors.primary.copy(alpha = 0.15f) else Color.Transparent
                        )
                        .clickable { onTabSelect(index) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) colors.primary else colors.textMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: TIMER TAB (Matching video frame 00:16)
// -------------------------------------------------------------
@Composable
private fun TimerTabContent(
    profiles: List<Profile>,
    activeSession: FocusSession?,
    selectedProfileId: Long?,
    isProtected: Boolean,
    onFixPermissions: () -> Unit,
    onSelectProfile: (Long) -> Unit,
    onStartSession: (Long, Int) -> Unit,
    onEndSession: (Long) -> Unit,
    onNavigateToProfiles: () -> Unit
) {
    val colors = BlockitTheme.colors
    var selectedDurationMinutes by remember { mutableIntStateOf(15) }
    var showCustomDurationDialog by remember { mutableStateOf(false) }
    var showProfilePickerDialog by remember { mutableStateOf(false) }

    // Live remaining time countdown
    val now = System.currentTimeMillis()
    var remainingMs by remember(activeSession?.endTimestamp) {
        mutableLongStateOf(
            if (activeSession != null) (activeSession.endTimestamp - now).coerceAtLeast(0) else 0L
        )
    }

    LaunchedEffect(activeSession?.endTimestamp) {
        while (activeSession != null && remainingMs > 0) {
            delay(1000)
            remainingMs = (activeSession.endTimestamp - System.currentTimeMillis()).coerceAtLeast(0)
        }
    }

    val displayHours = if (activeSession != null) (remainingMs / 1000 / 3600).toInt() else selectedDurationMinutes / 60
    val displayMins = if (activeSession != null) ((remainingMs / 1000 % 3600) / 60).toInt() else selectedDurationMinutes % 60
    val displaySecs = if (activeSession != null) (remainingMs / 1000 % 60).toInt() else 0

    val targetProfile = profiles.find { it.id == selectedProfileId } ?: profiles.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!isProtected) {
                Card(
                    onClick = onFixPermissions,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (colors.isDark) Color(0xFF451A1A) else Color(0xFFFFE4E6)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (colors.isDark) Color(0xFF7F1D1D) else Color(0xFFFDA4AF))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Blocking Engine Needs Setup",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (colors.isDark) Color(0xFFFCA5A5) else Color(0xFF9F1239)
                            )
                            Text(
                                text = "Tap here to grant Accessibility & Display Over Apps so FocusShield can block distractions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (colors.isDark) Color(0xFFF87171) else Color(0xFFBE123C)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeSession != null) {
                // -------------------------------------------------------------
                // ACTIVE FOCUS SESSION DISPLAY
                // -------------------------------------------------------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(colors.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "FOCUS SESSION RUNNING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            ),
                            color = colors.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val countdownText = if (displayHours > 0) {
                            String.format("%02d:%02d:%02d", displayHours, displayMins, displaySecs)
                        } else {
                            String.format("%02d:%02d", displayMins, displaySecs)
                        }

                        Text(
                            text = countdownText,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 56.sp,
                                letterSpacing = (-1).sp
                            ),
                            color = colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val endTime = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                            .format(java.util.Date(activeSession.endTimestamp))

                        Text(
                            text = "Ends at $endTime • Protected by '${targetProfile?.name ?: "Focus"}'",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }
            } else {
                // -------------------------------------------------------------
                // FOCUS CONTROLLER SETUP (ORIGINAL FOCUSSHIELD DESIGN)
                // -------------------------------------------------------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TARGET DURATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = colors.textMuted
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$selectedDurationMinutes",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 58.sp,
                                    letterSpacing = (-1).sp
                                ),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MIN",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = colors.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        val targetEndCal = java.util.Calendar.getInstance().apply {
                            add(java.util.Calendar.MINUTE, selectedDurationMinutes)
                        }
                        val targetEndTimeStr = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(targetEndCal.time)

                        Text(
                            text = "Focus until $targetEndTimeStr",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = colors.textMuted
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        val presets = listOf(15, 25, 45, 60, 90)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEach { mins ->
                                val isSelected = selectedDurationMinutes == mins
                                Surface(
                                    onClick = { selectedDurationMinutes = mins },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) (if (colors.isDark) colors.surfaceVariant else Color(0xFF1E293B)) else Color.Transparent,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) colors.primary else colors.border
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${mins}m",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) (if (colors.isDark) colors.primary else Color.White) else colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(onClick = { showCustomDurationDialog = true }) {
                            Text(
                                text = if (selectedDurationMinutes !in presets) "Custom: ${selectedDurationMinutes}m (Change)" else "+ Custom Duration",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.primary
                            )
                        }
                    }
                }
            }
        }

        // Clean Session Profile Card
        Card(
            onClick = {
                if (activeSession == null) {
                    showProfilePickerDialog = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (activeSession != null) colors.primary.copy(alpha = 0.5f) else colors.border
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (activeSession != null) colors.primary.copy(alpha = 0.15f)
                            else if (colors.isDark) colors.surfaceVariant else Color(0xFFE2E8F0)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (activeSession != null) Icons.Default.Lock else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (activeSession != null) colors.primary else colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (activeSession != null) "Locking: ${targetProfile?.name ?: "Focus"}" else (targetProfile?.name ?: "No Profile Selected"),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        if (targetProfile != null) {
                            val isWhitelist = targetProfile.blockMode == BlockMode.WHITELIST
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isWhitelist) colors.primary.copy(alpha = 0.15f) else if (colors.isDark) Color(0xFF1E293B) else Color(0xFFE0E7FF),
                            ) {
                                Text(
                                    text = if (isWhitelist) "Whitelist" else "Blacklist",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                    color = if (isWhitelist) colors.primary else if (colors.isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            targetProfile == null -> "Tap to create or choose a profile"
                            targetProfile.blockMode == BlockMode.WHITELIST -> "${targetProfile.appCount} permitted apps • All other apps locked"
                            else -> "${targetProfile.appCount} apps & ${targetProfile.domainCount} sites blocked"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }

                if (activeSession == null) {
                    Surface(
                        onClick = { showProfilePickerDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = if (colors.isDark) colors.surfaceVariant else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Slide to Start / Slide to Stop
        val targetId = targetProfile?.id
        SlideToActButton(
            text = when {
                activeSession != null -> "Slide to Stop"
                targetId == null -> "Create Profile to Start"
                else -> "Slide to Start"
            },
            isActive = activeSession != null,
            onTrigger = {
                if (activeSession != null) {
                    onEndSession(activeSession.id)
                } else if (targetId != null) {
                    onStartSession(targetId, selectedDurationMinutes)
                } else {
                    onNavigateToProfiles()
                }
            }
        )
    }

    if (showProfilePickerDialog) {
        AlertDialog(
            onDismissRequest = { showProfilePickerDialog = false },
            title = { Text("Select Focus Profile", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (profiles.isEmpty()) {
                        Text(
                            "No profiles exist yet. Create a profile to choose which apps and websites to block.",
                            color = colors.textMuted
                        )
                    } else {
                        profiles.forEach { prof ->
                            val isSelected = prof.id == targetProfile?.id
                            Surface(
                                onClick = {
                                    onSelectProfile(prof.id)
                                    showProfilePickerDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) colors.primary else colors.border
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prof.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) colors.primary else colors.textPrimary
                                        )
                                        Text(
                                            text = if (prof.blockMode == BlockMode.WHITELIST)
                                                "Whitelist • ${prof.appCount} allowed apps"
                                            else
                                                "Blacklist • ${prof.appCount} apps, ${prof.domainCount} sites",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textMuted
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showProfilePickerDialog = false
                    onNavigateToProfiles()
                }) {
                    Text("+ Manage Profiles", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfilePickerDialog = false }) {
                    Text("Close", color = colors.textMuted)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showCustomDurationDialog) {
        var inputMinutes by remember { mutableStateOf("25") }
        AlertDialog(
            onDismissRequest = { showCustomDurationDialog = false },
            title = { Text("Set Custom Duration", color = colors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = inputMinutes,
                    onValueChange = { inputMinutes = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Minutes (1 - 720)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = inputMinutes.toIntOrNull()?.coerceIn(1, 720) ?: 25
                    selectedDurationMinutes = parsed
                    showCustomDurationDialog = false
                }) {
                    Text("Set", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDurationDialog = false }) {
                    Text("Cancel", color = colors.textMuted)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun DurationTile(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BlockitTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) (if (colors.isDark) colors.surfaceVariant else CharcoalDark) else colors.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) colors.primary else colors.border
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 18.sp
                ),
                color = if (isSelected) (if (colors.isDark) colors.primary else Color.White) else colors.textPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// -------------------------------------------------------------
// TAB 2: PROFILES (PROFILES & WHITELIST / BLACKLIST)
// -------------------------------------------------------------
@Composable
private fun ProfilesTabContent(
    profiles: List<Profile>,
    selectedProfileId: Long?,
    packedApps: List<BlockedApp>,
    packedDomains: List<BlockedDomain>,
    activeSession: FocusSession?,
    isProtected: Boolean,
    onFixPermissions: () -> Unit,
    onSelectProfile: (Long) -> Unit,
    onSetProfileMode: (Long, BlockMode) -> Unit,
    onToggleProfile: (Long, Boolean) -> Unit,
    onAddApps: (Long) -> Unit,
    onRemoveApp: (Long, String) -> Unit,
    onAddDomain: (Long, String, Boolean) -> Unit,
    onRemoveDomain: (Long) -> Unit,
    onCreateProfile: (String, BlockMode) -> Unit,
    onDeleteProfile: (Long) -> Unit
) {
    val colors = BlockitTheme.colors
    val currentProfile = profiles.find { it.id == selectedProfileId } ?: profiles.firstOrNull()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddDomainDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        if (!isProtected) {
            Card(
                onClick = onFixPermissions,
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (colors.isDark) Color(0xFF451A1A) else Color(0xFFFFE4E6)),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (colors.isDark) Color(0xFF7F1D1D) else Color(0xFFFDA4AF))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Blocking Engine Needs Setup",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (colors.isDark) Color(0xFFFCA5A5) else Color(0xFF9F1239)
                        )
                        Text(
                            text = "Tap here to grant Accessibility, Display Over Apps, and Battery Exemption.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (colors.isDark) Color(0xFFF87171) else Color(0xFFBE123C)
                        )
                    }
                }
            }
        }

        if (activeSession != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (colors.isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (colors.isDark) Color(0xFF3B82F6) else Color(0xFF93C5FD))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Session active: Profiles and blocklists are locked read-only.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (colors.isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                    )
                }
            }
        }

        // Distinct FocusShield Profiles Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Focus Profiles",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Text(
                    text = "Blacklists block distractions; Whitelists lock everything except essential apps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (profiles.isEmpty()) {
            // Empty state when no profiles exist
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Focus Profiles Yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Create your first profile to configure which apps and websites to block or allow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (colors.isDark) colors.surfaceVariant else CharcoalDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+ Create First Profile", color = if (colors.isDark) colors.primary else Color.White)
                    }
                }
            }
        } else {
            // Profile selector pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(profiles) { profile ->
                        val isSelected = profile.id == currentProfile?.id
                        Surface(
                            onClick = { onSelectProfile(profile.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) (if (colors.isDark) colors.surfaceVariant else CharcoalDark) else colors.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) colors.primary else colors.border)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) (if (colors.isDark) colors.primary else Color.White) else colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                // Add Profile button (disabled during active session)
                if (activeSession == null) {
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(colors.surface)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Profile", tint = colors.textPrimary)
                    }
                }
            }

            if (currentProfile != null) {
                Spacer(modifier = Modifier.height(16.dp))

                // Profile configuration card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentProfile.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                if (activeSession == null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { showDeleteConfirmDialog = true },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Profile",
                                            tint = colors.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Switch(
                                checked = currentProfile.isEnabled,
                                onCheckedChange = { onToggleProfile(currentProfile.id, it) },
                                enabled = activeSession == null,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.primary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (currentProfile.blockMode == BlockMode.WHITELIST)
                                "Whitelist Mode (Kiosk) — Only apps and domains listed below can run. All other apps on your phone are locked out."
                            else
                                "Blacklist Mode — Apps and domains listed below will be blocked. The rest of your phone stays completely accessible.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mode Switcher buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { if (activeSession == null) onSetProfileMode(currentProfile.id, BlockMode.WHITELIST) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = if (currentProfile.blockMode == BlockMode.WHITELIST) (if (colors.isDark) colors.surfaceVariant else CharcoalDark) else Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (currentProfile.blockMode == BlockMode.WHITELIST) colors.primary else colors.border)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "🔒 Whitelist",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (currentProfile.blockMode == BlockMode.WHITELIST) (if (colors.isDark) colors.primary else Color.White) else colors.textPrimary
                                    )
                                }
                            }
                            Surface(
                                onClick = { if (activeSession == null) onSetProfileMode(currentProfile.id, BlockMode.BLACKLIST) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = if (currentProfile.blockMode == BlockMode.BLACKLIST) (if (colors.isDark) colors.surfaceVariant else CharcoalDark) else Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (currentProfile.blockMode == BlockMode.BLACKLIST) colors.primary else colors.border)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "🛡️ Blacklist",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (currentProfile.blockMode == BlockMode.BLACKLIST) (if (colors.isDark) colors.primary else Color.White) else colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Apps Grid with "+" buttons
                Text(
                    text = if (currentProfile.blockMode == BlockMode.WHITELIST) "Permitted Apps (Stay Usable)" else "Blocked Apps (${packedApps.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Grid of app slots (3 columns)
                val slots = if (activeSession == null) packedApps + listOf(null) else packedApps
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    slots.chunked(3).forEach { rowApps ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowApps.forEach { app ->
                                if (app != null) {
                                    Surface(
                                        modifier = Modifier.weight(1f).height(92.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        color = colors.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                                            if (activeSession == null) {
                                                IconButton(
                                                    onClick = { onRemoveApp(currentProfile.id, app.packageName) },
                                                    modifier = Modifier.size(18.dp).align(Alignment.TopEnd)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = colors.textMuted, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                AppIconView(packageName = app.packageName, modifier = Modifier.size(34.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = app.appName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Add App Slot
                                    Surface(
                                        onClick = { onAddApps(currentProfile.id) },
                                        modifier = Modifier.weight(1f).height(92.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.Transparent,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add", tint = colors.primary, modifier = Modifier.size(28.dp))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (currentProfile.blockMode == BlockMode.WHITELIST) "Permit App" else "Block App",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.textMuted
                                            )
                                        }
                                    }
                                }
                            }
                            // Fill remaining spaces if row is incomplete
                            repeat(3 - rowApps.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Websites Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Websites (${packedDomains.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    if (activeSession == null) {
                        TextButton(onClick = { showAddDomainDialog = true }) {
                            Text("+ Add Domain", color = colors.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (packedDomains.isEmpty()) {
                    Text(
                        text = "No websites configured yet. Add domains like 'instagram.com' or '*.reddit.com'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        packedDomains.forEach { domain ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = colors.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(domain.pattern, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                                    if (activeSession == null) {
                                        IconButton(
                                            onClick = { onRemoveDomain(domain.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = colors.textMuted, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var profileName by remember { mutableStateOf("") }
        var profileMode by remember { mutableStateOf(BlockMode.WHITELIST) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Focus Profile", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Profile Name (e.g. Deep Work, Study)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Blocking Mode:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { profileMode = BlockMode.WHITELIST },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (profileMode == BlockMode.WHITELIST) colors.primary else colors.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (profileMode == BlockMode.WHITELIST) colors.primary else colors.border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🔒 Whitelist", color = if (profileMode == BlockMode.WHITELIST) Color.White else colors.textPrimary)
                        }
                        Button(
                            onClick = { profileMode = BlockMode.BLACKLIST },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (profileMode == BlockMode.BLACKLIST) colors.primary else colors.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (profileMode == BlockMode.BLACKLIST) colors.primary else colors.border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🛡️ Blacklist", color = if (profileMode == BlockMode.BLACKLIST) Color.White else colors.textPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (profileMode == BlockMode.WHITELIST)
                            "Whitelist: Locks entire phone except chosen essential apps."
                        else
                            "Blacklist: Phone stays normal, but chosen distracting apps/sites are blocked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (profileName.isNotBlank()) {
                        onCreateProfile(profileName.trim(), profileMode)
                        showCreateDialog = false
                    }
                }) {
                    Text("Create", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel", color = colors.textMuted) }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showDeleteConfirmDialog && currentProfile != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Profile", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = { Text("Are you sure you want to delete '${currentProfile.name}'? All configured apps and domains for this profile will be removed.", color = colors.textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteProfile(currentProfile.id)
                    showDeleteConfirmDialog = false
                }) {
                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = colors.textMuted)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showAddDomainDialog && currentProfile != null) {
        var domainPattern by remember { mutableStateOf("") }
        var isWildcard by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDomainDialog = false },
            title = { Text("Block Domain", color = colors.textPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = domainPattern,
                        onValueChange = { domainPattern = it },
                        label = { Text("Domain (e.g. twitter.com or *.reddit.com)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (domainPattern.isNotBlank()) {
                        onAddDomain(currentProfile.id, domainPattern.trim(), isWildcard)
                        showAddDomainDialog = false
                    }
                }) {
                    Text("Add", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDomainDialog = false }) { Text("Cancel", color = colors.textMuted) }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// -------------------------------------------------------------
// TAB 3: ROUTINES (CONTROL CENTER) (Matching video frame 00:26)
// -------------------------------------------------------------
@Composable
private fun RoutinesTabContent(
    profiles: List<Profile>,
    selectedProfileId: Long?,
    schedules: List<Schedule>,
    onSelectProfile: (Long) -> Unit,
    onSaveSchedule: (Schedule) -> Unit,
    onDeleteSchedule: (Long) -> Unit,
    onNavigateToProfiles: () -> Unit
) {
    val colors = BlockitTheme.colors
    val currentBag = profiles.find { it.id == selectedProfileId } ?: profiles.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        // Timeline Header: 00:00 - 24:00 with pixel dots matching video frame 00:26
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "00:00",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                ),
                color = colors.textPrimary
            )
            Text(
                "24:00",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                ),
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Pixel Timeline Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (colors.isDark) colors.surfaceVariant else Color(0xFFD6D4CC))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .fillMaxSize()
                    .background(colors.primary)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Target Profile Selector for Routines
        if (profiles.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(profiles) { profile ->
                    val isSelected = profile.id == currentBag?.id
                    Surface(
                        onClick = { onSelectProfile(profile.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) colors.primary else colors.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else colors.textPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = "Control center",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
        Text(
            text = "Routines start a block on their own, at the time you set, on the days you pick.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (profiles.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No Profiles Created Yet",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Create a focus profile first before creating automated routines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onNavigateToProfiles,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (colors.isDark) colors.surfaceVariant else CharcoalDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "+ Go to Profiles",
                            color = if (colors.isDark) colors.primary else Color.White
                        )
                    }
                }
            }
        } else if (schedules.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No automated routines active.",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Add a schedule to lock distractions automatically (e.g. 09:00 - 17:00 Mon-Fri).",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            if (currentBag != null) {
                                onSaveSchedule(
                                    Schedule(
                                        profileId = currentBag.id,
                                        daysOfWeekMask = 31, // Mon-Fri
                                        startMinuteOfDay = 540, // 09:00
                                        endMinuteOfDay = 1020, // 17:00
                                        isEnabled = true
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (colors.isDark) colors.surfaceVariant else CharcoalDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Add Workday Routine (9am - 5pm)",
                            color = if (colors.isDark) colors.primary else Color.White
                        )
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                schedules.forEachIndexed { index, schedule ->
                    RoutineCard(
                        index = index + 1,
                        schedule = schedule,
                        onToggle = { isEnabled ->
                            onSaveSchedule(schedule.copy(isEnabled = isEnabled))
                        },
                        onDelete = { onDeleteSchedule(schedule.id) },
                        onUpdateDays = { mask ->
                            onSaveSchedule(schedule.copy(daysOfWeekMask = mask))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineCard(
    index: Int,
    schedule: Schedule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onUpdateDays: (Int) -> Unit
) {
    val colors = BlockitTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ROUTINE $index",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.textPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = schedule.isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = colors.textMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Time range display: e.g. 09:00 — 17:00
            val startH = schedule.startMinuteOfDay / 60
            val startM = schedule.startMinuteOfDay % 60
            val endH = schedule.endMinuteOfDay / 60
            val endM = schedule.endMinuteOfDay % 60

            Text(
                text = String.format("%02d:%02d — %02d:%02d", startH, startM, endH, endM),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                ),
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Days of Week Chips matching video frame 00:26
            val days = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEachIndexed { dIndex, letter ->
                    val bit = 1 shl dIndex
                    val isDayActive = (schedule.daysOfWeekMask and bit) != 0
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDayActive) colors.primary else (if (colors.isDark) colors.surfaceVariant else Color(0xFFD6D4CC)))
                            .clickable {
                                val newMask = schedule.daysOfWeekMask xor bit
                                onUpdateDays(newMask)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDayActive) Color.White else colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: STATISTICS (Matching video frame 00:31)
// -------------------------------------------------------------
@Composable
private fun StatisticsTabContent(
    insightsData: UsageInsightsData
) {
    val colors = BlockitTheme.colors
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: Week, 1: Month, 2: Year
    val currentMonthYear = remember {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()).uppercase()
    }
    val hasData = insightsData.totalBlocked > 0 || insightsData.totalFocusMinutes > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        // Date Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentMonthYear,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = colors.textPrimary
            )
            Text(
                text = "${insightsData.formattedFocusTime} FOCUSED",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                ),
                color = colors.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Period filter buttons: Week, Month, Year
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Week", "Month", "Year").forEachIndexed { index, label ->
                val isSelected = selectedFilter == index
                Surface(
                    onClick = { selectedFilter = index },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) (if (colors.isDark) colors.surfaceVariant else CharcoalDark) else colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) colors.primary else colors.border),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) (if (colors.isDark) colors.primary else Color.White) else colors.textPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Real Histogram / Bar Chart with Labeled Axes
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Intercept Activity",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    val maxVal = insightsData.dailyStats.maxOfOrNull { it.count } ?: 0
                    if (maxVal > 0) {
                        Text(
                            text = "Peak: $maxVal/day",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Real logged intercepts across the past 7 days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                val stats = insightsData.dailyStats
                val maxCount = remember(stats) {
                    stats.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val todayStart = remember {
                        val cal = java.util.Calendar.getInstance()
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                        cal.set(java.util.Calendar.MINUTE, 0)
                        cal.set(java.util.Calendar.SECOND, 0)
                        cal.set(java.util.Calendar.MILLISECOND, 0)
                        cal.timeInMillis
                    }

                    if (stats.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No activity recorded yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    } else {
                        stats.forEach { stat ->
                            val isToday: Boolean = stat.dateMs >= todayStart
                            val heightFraction = if (stat.count == 0) 0.05f else (stat.count.toFloat() / maxCount).coerceIn(0.12f, 1f)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                if (stat.count > 0) {
                                    Text(
                                        text = "${stat.count}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = colors.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .fillMaxHeight(heightFraction)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            if (stat.count > 0) colors.primary
                                            else if (isToday) colors.primary.copy(alpha = 0.35f)
                                            else if (colors.isDark) colors.surfaceVariant else CharcoalDark.copy(alpha = 0.15f)
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stat.dayLabel.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isToday) colors.primary else colors.textMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!hasData) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Clean slate. Start a focus session to block distracting apps and track your stats here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Summary Metric Tiles (Real data only)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Total Intercepts",
                value = "${insightsData.totalBlocked}",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Focus Time",
                value = insightsData.formattedFocusTime,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Blocked Today",
                value = "${insightsData.blockedToday}",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Apps Protected",
                value = "${insightsData.topTargets.size}",
                modifier = Modifier.weight(1f)
            )
        }

        // Top Intercepted Apps (Real list from DB)
        if (insightsData.topTargets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Most Intercepted",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    insightsData.topTargets.take(5).forEach { target ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (target.targetType == "APP") {
                                    AppIconView(packageName = target.identifier, modifier = Modifier.size(28.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = target.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${target.count} blocked",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = BlockitTheme.colors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                ),
                color = colors.textPrimary
            )
        }
    }
}

@Composable
private fun AppIconView(packageName: String, modifier: Modifier = Modifier) {
    val colors = BlockitTheme.colors
    val context = LocalContext.current
    val iconBitmap = remember(packageName) {
        try {
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

    if (iconBitmap != null) {
        Image(bitmap = iconBitmap, contentDescription = null, modifier = modifier)
    } else {
        Box(
            modifier = modifier.clip(RoundedCornerShape(8.dp)).background(colors.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = packageName.takeLast(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
