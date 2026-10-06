package com.blockit.ui.screens.dashboard

import com.blockit.ui.components.StopSessionFrictionDialog

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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import com.blockit.domain.model.SessionStrictness
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
import com.blockit.ui.theme.SignalGreen
import com.blockit.ui.theme.DestructiveRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun MainDashboardScreen(
    viewModel: MainDashboardViewModel,
    onAddAppsClick: (Long) -> Unit,
    onOpenSettingsClick: () -> Unit,
    onOpenStrictModeClick: () -> Unit = onOpenSettingsClick,
    onOpenHealthClick: () -> Unit = onOpenSettingsClick,
    onOpenSetupGuideClick: () -> Unit = onOpenSettingsClick,
    onOpenBackupRestoreClick: () -> Unit = onOpenSettingsClick,
    onOpenHowItWorksClick: () -> Unit = onOpenSettingsClick,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Timer, 1: Profiles, 2: Routines, 3: Statistics

    var showHelpDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

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
    var activeSessionStrictness by rememberSaveable { mutableStateOf(SessionStrictness.NORMAL) }

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.surface,
                drawerContentColor = colors.textPrimary,
                modifier = Modifier.width(310.dp)
            ) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 22.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "FocusShield",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = colors.textPrimary
                        )
                    }
                }

                HorizontalDivider(color = colors.border, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = colors.textPrimary) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenSettingsClick()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    label = { Text("Strict Mode", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = colors.textPrimary) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenStrictModeClick()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    label = { Text("Engine Health", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.textPrimary) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenHealthClick()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    label = { Text("Permissions Guide", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = colors.textPrimary) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenSetupGuideClick()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    label = { Text("Backup & Restore", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = colors.textPrimary) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenBackupRestoreClick()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    label = { Text("How Blocking Works", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Info, contentDescription = null, tint = colors.textPrimary) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenHowItWorksClick()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = colors.border, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Dark Theme Quick Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (colors.isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Dark Mode",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary
                        )
                    }
                    Switch(
                        checked = colors.isDark,
                        onCheckedChange = { viewModel.toggleDarkMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.primary
                        )
                    )
                }

                NavigationDrawerItem(
                    label = { Text("Help & Support", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null, tint = colors.textPrimary) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showHelpDialog = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    label = { Text("About FocusShield", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = colors.textPrimary) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showAboutDialog = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = colors.background,
            topBar = {
                DashboardTopBar(
                    activeSession = activeSession,
                    activeProfileName = activeProfile?.name,
                    onMenuClick = { scope.launch { drawerState.open() } }
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
                    0 -> DashboardHomeContent(
                        profiles = profiles,
                        activeSession = activeSession,
                        selectedProfileId = selectedProfileId,
                        isProtected = isProtected,
                        insightsData = insightsData,
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
                        onStartSession = { profId, mins, strictness ->
                            activeSessionStrictness = strictness
                            viewModel.startSession(profId, mins)
                        },
                        onEndSession = { sessId ->
                            pendingStopSessionId = sessId
                            showStopConfirmDialog = true
                        },
                        onDuplicateProfile = { profId -> viewModel.duplicateProfile(profId) },
                        onCreateTemplateProfile = { type -> viewModel.createTemplateProfile(type) },
                        onNavigateToProfiles = { selectedTab = 1 },
                        onNavigateToStats = { selectedTab = 3 },
                        onCreateProfileClick = { selectedTab = 1 }
                    )
                    1 -> ProfilesTabContent(
                        profiles = profiles,
                        selectedProfileId = selectedProfileId,
                        packedApps = packedApps,
                        packedDomains = packedDomains,
                        activeSession = activeSession,
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
                    strictness = activeSessionStrictness,
                    endTimeMs = activeSession?.endTimestamp,
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

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "FocusShield FAQs",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("1. How does blocking work?", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("FocusShield detects configured apps locally via Accessibility Service and displays a blocking screen.", color = colors.textMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("2. Whitelist vs Blacklist?", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("Blacklists block selected apps. Whitelists lock down your phone except permitted apps.", color = colors.textMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("3. Privacy Guarantee", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("100% on-device. Zero network traffic, zero analytics, zero account required.", color = colors.textMuted, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Got it")
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "About FocusShield",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("FocusShield v2.0", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text(
                        "Personal, on-device app and website blocker.\n\n" +
                                "- 100% On-Device execution\n" +
                                "- No network requests or telemetry\n" +
                                "- No accounts or advertising",
                        color = colors.textMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Close")
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(18.dp)
        )
    }
}


@Composable
private fun DashboardTopBar(
    activeSession: FocusSession?,
    activeProfileName: String?,
    onMenuClick: () -> Unit
) {
    val colors = BlockitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Hamburger Menu Button + Logo & Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Navigation Drawer",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FocusShield",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = colors.textPrimary
                )
            }
        }

        // Right: Active session pulse indicator
        if (activeSession != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (colors.isDark) Color(0xFF14532D).copy(alpha = 0.4f) else Color(0xFFDCFCE7),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (colors.isDark) Color(0xFF16A34A) else Color(0xFF86EFAC))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SignalGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (!activeProfileName.isNullOrBlank()) "Active: $activeProfileName" else "Active",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
    NavigationBar(
        containerColor = colors.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        val tabs = listOf(
            Icons.Default.Timer to "Home",
            Icons.Default.Shield to "Profiles",
            Icons.Default.Schedule to "Schedules",
            Icons.Default.BarChart to "Stats"
        )

        tabs.forEachIndexed { index, (icon, label) ->
            val isSelected = selectedTab == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelect(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    selectedTextColor = colors.primary,
                    indicatorColor = colors.primary.copy(alpha = 0.12f),
                    unselectedIconColor = colors.textMuted,
                    unselectedTextColor = colors.textMuted
                )
            )
        }
    }
}

// -------------------------------------------------------------
// TAB 1: DASHBOARD HOME (Phase 2 New Structure)
// -------------------------------------------------------------
@Composable
private fun DashboardHomeContent(
    profiles: List<Profile>,
    activeSession: FocusSession?,
    selectedProfileId: Long?,
    isProtected: Boolean,
    insightsData: UsageInsightsData,
    onFixPermissions: () -> Unit,
    onSelectProfile: (Long) -> Unit,
    onStartSession: (Long, Int, SessionStrictness) -> Unit,
    onEndSession: (Long) -> Unit,
    onDuplicateProfile: (Long) -> Unit,
    onCreateTemplateProfile: (String) -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToStats: () -> Unit,
    onCreateProfileClick: () -> Unit
) {
    val colors = BlockitTheme.colors
    var showDurationSheet by remember { mutableStateOf(false) }
    var showProfilePickerDialog by remember { mutableStateOf(false) }

    // Live remaining time countdown for active session
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

    val totalSessionMs = if (activeSession != null) {
        (activeSession.endTimestamp - activeSession.startTimestamp).coerceAtLeast(1000L)
    } else 1L
    val sessionProgress = if (activeSession != null) {
        1f - (remainingMs.toFloat() / totalSessionMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displayHours = if (activeSession != null) (remainingMs / 1000 / 3600).toInt() else 0
    val displayMins = if (activeSession != null) ((remainingMs / 1000 % 3600) / 60).toInt() else 0
    val displaySecs = if (activeSession != null) (remainingMs / 1000 % 60).toInt() else 0

    val targetProfile = profiles.find { it.id == (activeSession?.profileId ?: selectedProfileId) } ?: profiles.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // -------------------------------------------------------------
        // SECTION 1: ENGINE STATUS CARD (Only shown when !isProtected)
        // -------------------------------------------------------------
        if (!isProtected) {
            Card(
                onClick = onFixPermissions,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (colors.isDark) Color(0xFF3B1515) else Color(0xFFFFF1F2)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (colors.isDark) Color(0xFF7F1D1D) else Color(0xFFFECDD3)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (colors.isDark) Color(0xFF7F1D1D) else Color(0xFFFFE4E6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Blocking Engine Needs Setup",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (colors.isDark) Color(0xFFFCA5A5) else Color(0xFF9F1239)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Grant Accessibility, Overlay, and Battery Exemption to enable background protection.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (colors.isDark) Color(0xFFF87171) else Color(0xFFBE123C)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onFixPermissions,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE11D48),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Fix", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // SECTION 2: ACTIVE SESSION CARD or START FOCUS CARD
        // -------------------------------------------------------------
        if (activeSession != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ACTIVE FOCUS SESSION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp
                                ),
                                color = colors.primary
                            )
                        }

                        if (targetProfile != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (targetProfile.blockMode == BlockMode.WHITELIST)
                                    colors.primary.copy(alpha = 0.15f)
                                else if (colors.isDark) Color(0xFF1E293B) else Color(0xFFE0E7FF)
                            ) {
                                Text(
                                    text = if (targetProfile.blockMode == BlockMode.WHITELIST) "Whitelist" else "Blacklist",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                    color = if (targetProfile.blockMode == BlockMode.WHITELIST) colors.primary else if (colors.isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Circular countdown ring
                    Box(
                        modifier = Modifier.size(170.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = colors.border,
                            strokeWidth = 8.dp,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        CircularProgressIndicator(
                            progress = { sessionProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = colors.primary,
                            strokeWidth = 8.dp,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val countdownText = if (displayHours > 0) {
                                String.format("%02d:%02d:%02d", displayHours, displayMins, displaySecs)
                            } else {
                                String.format("%02d:%02d", displayMins, displaySecs)
                            }
                            Text(
                                text = countdownText,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 32.sp,
                                    letterSpacing = (-1).sp
                                ),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "remaining",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    val endTimeStr = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                        .format(java.util.Date(activeSession.endTimestamp))

                    Text(
                        text = "Protecting with '${targetProfile?.name ?: "Focus"}' until $endTimeStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Stop button with friction trigger
                    OutlinedButton(
                        onClick = { onEndSession(activeSession.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Stop Session",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Quick Focus",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select a profile to start blocking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Selected Profile Pill
                    Surface(
                        onClick = { showProfilePickerDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (colors.isDark) colors.surfaceVariant else Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = targetProfile?.name ?: "No Profile Selected",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary
                                    )
                                    if (targetProfile != null) {
                                        val isWhitelist = targetProfile.blockMode == BlockMode.WHITELIST
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isWhitelist) colors.primary.copy(alpha = 0.15f) else if (colors.isDark) Color(0xFF1E293B) else Color(0xFFE0E7FF)
                                        ) {
                                            Text(
                                                text = if (isWhitelist) "Whitelist" else "Blacklist",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                color = if (isWhitelist) colors.primary else if (colors.isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = when {
                                        targetProfile == null -> "Tap to choose a profile"
                                        targetProfile.blockMode == BlockMode.WHITELIST -> "${targetProfile.appCount} permitted apps"
                                        else -> "${targetProfile.appCount} apps, ${targetProfile.domainCount} sites"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Start Focus Button
                    Button(
                        onClick = {
                            if (!isProtected) {
                                onFixPermissions()
                            } else if (targetProfile != null) {
                                showDurationSheet = true
                            } else {
                                onCreateProfileClick()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isProtected) Color(0xFFE11D48) else colors.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (!isProtected) Icons.Default.Warning else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (!isProtected) "Setup Engine to Start" else if (targetProfile == null) "Create Profile to Start" else "Choose Duration & Start",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // SECTION 3: TODAY'S STATS SUMMARY (Real Room DB data only)
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Activity",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    TextButton(
                        onClick = onNavigateToStats,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Full Stats",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val hasData = insightsData.blockedToday > 0 || insightsData.totalFocusMinutes > 0

                if (!hasData) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No distraction attempts today. Start a focus session to protect your flow.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Focus Time
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (colors.isDark) colors.surfaceVariant else Color(0xFFF8FAFC)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Focus Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = insightsData.formattedFocusTime,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = colors.textPrimary
                                )
                            }
                        }

                        // Blocked attempts
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (colors.isDark) colors.surfaceVariant else Color(0xFFF8FAFC)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Intercepted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${insightsData.blockedToday}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = colors.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // SECTION 4: PROFILES OVERVIEW & TEMPLATES
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Focus Profiles",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    TextButton(
                        onClick = onNavigateToProfiles,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Manage",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Template quick-add buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onCreateTemplateProfile("WORK") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+ Work Hours",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.primary
                        )
                    }
                    OutlinedButton(
                        onClick = { onCreateTemplateProfile("SLEEP") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+ Sleep & Rest",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Profile items list
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    profiles.forEach { prof ->
                        val isSelected = prof.id == targetProfile?.id
                        Surface(
                            onClick = {
                                if (activeSession == null) {
                                    onSelectProfile(prof.id)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) colors.primary.copy(alpha = 0.08f) else if (colors.isDark) colors.surfaceVariant else Color(0xFFF8FAFC),
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = prof.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colors.textPrimary
                                        )
                                        val isWhitelist = prof.blockMode == BlockMode.WHITELIST
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isWhitelist) colors.primary.copy(alpha = 0.15f) else if (colors.isDark) Color(0xFF1E293B) else Color(0xFFE0E7FF)
                                        ) {
                                            Text(
                                                text = if (isWhitelist) "Whitelist" else "Blacklist",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                color = if (isWhitelist) colors.primary else if (colors.isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (activeSession != null) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (colors.isDark) Color(0xFF332A15) else Color(0xFFFEF3C7)
                                            ) {
                                                Text(
                                                    text = "Locked",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                    color = if (colors.isDark) Color(0xFFFCD34D) else Color(0xFFB45309),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (prof.blockMode == BlockMode.WHITELIST)
                                            "${prof.appCount} permitted apps"
                                        else
                                            "${prof.appCount} apps, ${prof.domainCount} sites",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onDuplicateProfile(prof.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Duplicate Profile",
                                            tint = colors.textMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Profile Picker Dialog
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
                                                "Whitelist: ${prof.appCount} allowed apps"
                                            else
                                                "Blacklist: ${prof.appCount} apps, ${prof.domainCount} sites",
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

    // Duration Bottom Sheet
    if (showDurationSheet) {
        DurationBottomSheet(
            targetProfile = targetProfile,
            onDismiss = { showDurationSheet = false },
            onStart = { profId, mins, strictness ->
                showDurationSheet = false
                onStartSession(profId, mins, strictness)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationBottomSheet(
    targetProfile: Profile?,
    onDismiss: () -> Unit,
    onStart: (Long, Int, SessionStrictness) -> Unit
) {
    val colors = BlockitTheme.colors
    var selectedMinutes by remember { mutableIntStateOf(25) }
    var selectedStrictness by remember { mutableStateOf(SessionStrictness.NORMAL) }
    var customHoursText by remember { mutableStateOf("0") }
    var customMinsText by remember { mutableStateOf("25") }
    var isCustomMode by remember { mutableStateOf(false) }

    val presets = listOf(15, 25, 45, 60, 90)

    val targetEndCal = Calendar.getInstance().apply {
        add(Calendar.MINUTE, selectedMinutes)
    }
    val targetEndTimeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(targetEndCal.time)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.border)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Start Focus Session",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )

            // Target Profile Pill
            if (targetProfile != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (colors.isDark) colors.surfaceVariant else Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Profile: ${targetProfile.name}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (targetProfile.blockMode == BlockMode.WHITELIST) "Whitelist Mode (${targetProfile.appCount} permitted)" else "Blacklist Mode (${targetProfile.appCount} blocked)",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }

            // Duration Presets
            Text(
                text = "Duration",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { mins ->
                    val isSelected = !isCustomMode && selectedMinutes == mins
                    Surface(
                        onClick = {
                            isCustomMode = false
                            selectedMinutes = mins
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) colors.primary else if (colors.isDark) colors.surfaceVariant else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) colors.primary else colors.border)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mins}m",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else colors.textPrimary
                            )
                        }
                    }
                }
            }

            // Custom Time Toggle / Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { isCustomMode = !isCustomMode }) {
                    Text(
                        text = if (isCustomMode) "Use Presets" else "+ Custom Duration",
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "Ends at $targetEndTimeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }

            if (isCustomMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customHoursText,
                        onValueChange = {
                            if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                customHoursText = it
                                val h = it.toIntOrNull() ?: 0
                                val m = customMinsText.toIntOrNull() ?: 0
                                selectedMinutes = (h * 60 + m).coerceIn(1, 720)
                            }
                        },
                        label = { Text("Hours") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customMinsText,
                        onValueChange = {
                            if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                customMinsText = it
                                val h = customHoursText.toIntOrNull() ?: 0
                                val m = it.toIntOrNull() ?: 0
                                selectedMinutes = (h * 60 + m).coerceIn(1, 720)
                            }
                        },
                        label = { Text("Mins") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            // Strictness Selection
            Text(
                text = "Session Strictness",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple(SessionStrictness.NORMAL, "Normal", "Stop anytime with brief 10s reflection"),
                    Triple(SessionStrictness.STRICT, "Strict", "15s cooldown + type phrase to unlock"),
                    Triple(SessionStrictness.LOCKED, "Locked", "Cannot stop session early until timer expires")
                ).forEach { (level, title, desc) ->
                    val isSelected = selectedStrictness == level
                    Surface(
                        onClick = { selectedStrictness = level },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) (if (colors.isDark) colors.surfaceVariant else Color(0xFFF8FAFC)) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) colors.primary else colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Big Start Button
            Button(
                onClick = {
                    if (targetProfile != null) {
                        onStart(targetProfile.id, selectedMinutes, selectedStrictness)
                    }
                },
                enabled = targetProfile != null,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Start ${selectedMinutes}-Minute Focus",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
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
                                "Whitelist Mode (Kiosk) - Only apps and domains listed below can run. All other apps on your phone are locked out."
                            else
                                "Blacklist Mode - Apps and domains listed below will be blocked. The rest of your phone stays completely accessible.",
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
                                        text = "Whitelist",
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
                                        text = "Blacklist",
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
                            Text("Whitelist", color = if (profileMode == BlockMode.WHITELIST) Color.White else colors.textPrimary)
                        }
                        Button(
                            onClick = { profileMode = BlockMode.BLACKLIST },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (profileMode == BlockMode.BLACKLIST) colors.primary else colors.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (profileMode == BlockMode.BLACKLIST) colors.primary else colors.border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Blacklist", color = if (profileMode == BlockMode.BLACKLIST) Color.White else colors.textPrimary)
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
    val currentProfile = profiles.find { it.id == selectedProfileId } ?: profiles.firstOrNull()
    var showCreateDialog by remember { mutableStateOf(false) }

    val presets = remember {
        listOf(
            RoutinePresetItem("Workday", "09:00 - 17:00 (M-F)", 31, 540, 1020),
            RoutinePresetItem("Deep Work", "08:00 - 12:00 (M-F)", 31, 480, 720),
            RoutinePresetItem("Evening", "20:30 - 23:30 (Daily)", 127, 1230, 1410),
            RoutinePresetItem("Weekend", "10:00 - 18:00 (S-S)", 96, 600, 1080)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        // Timeline Header: 00:00 - 24:00
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
                    val isSelected = profile.id == currentProfile?.id
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Control Center",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Text(
                    text = "Automated schedules engage focus locks without manual intervention.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
            if (profiles.isNotEmpty()) {
                IconButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (colors.isDark) colors.surfaceVariant else CharcoalDark)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Routine",
                        tint = if (colors.isDark) colors.primary else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Smart Presets
        if (profiles.isNotEmpty()) {
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { preset ->
                    Surface(
                        onClick = {
                            if (currentProfile != null) {
                                onSaveSchedule(
                                    Schedule(
                                        profileId = currentProfile.id,
                                        daysOfWeekMask = preset.daysMask,
                                        startMinuteOfDay = preset.startMinute,
                                        endMinuteOfDay = preset.endMinute,
                                        isEnabled = true
                                    )
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = "+ ${preset.name}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.primary
                            )
                            Text(
                                text = preset.timeRangeText,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

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
                        "No Focus Profiles Available",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Create a focus profile first before setting up automated routines.",
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
                        "No automated routines active",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Add a schedule to lock distractions automatically during your work or study hours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+ Custom Routine", color = Color.White)
                        }
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

                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (colors.isDark) colors.surfaceVariant else CharcoalDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+ Add Another Routine", color = if (colors.isDark) colors.primary else Color.White)
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateRoutineDialog(
            profiles = profiles,
            initialProfileId = currentProfile?.id,
            onDismiss = { showCreateDialog = false },
            onSaveSchedule = { newSchedule ->
                onSaveSchedule(newSchedule)
                showCreateDialog = false
            }
        )
    }
}

private data class RoutinePresetItem(
    val name: String,
    val timeRangeText: String,
    val daysMask: Int,
    val startMinute: Int,
    val endMinute: Int
)

@Composable
private fun CreateRoutineDialog(
    profiles: List<Profile>,
    initialProfileId: Long?,
    onDismiss: () -> Unit,
    onSaveSchedule: (Schedule) -> Unit
) {
    val colors = BlockitTheme.colors
    var selectedProfileId by remember { mutableStateOf(initialProfileId ?: profiles.firstOrNull()?.id ?: 0L) }
    var startHourText by remember { mutableStateOf("09") }
    var startMinText by remember { mutableStateOf("00") }
    var endHourText by remember { mutableStateOf("17") }
    var endMinText by remember { mutableStateOf("00") }
    var daysMask by remember { mutableIntStateOf(31) } // Mon-Fri default (bits 0..4)

    val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Create Automated Routine",
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Profile selection
                if (profiles.size > 1) {
                    Text(
                        "Target Profile:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(profiles) { profile ->
                            val isSelected = profile.id == selectedProfileId
                            Surface(
                                onClick = { selectedProfileId = profile.id },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) colors.primary else colors.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                            ) {
                                Text(
                                    profile.name,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else colors.textPrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Time Pickers
                Text(
                    "Hours (24-Hour Format):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start Time", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = startHourText,
                                onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) startHourText = it },
                                modifier = Modifier.width(56.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Monospace
                                ),
                                singleLine = true
                            )
                            Text(" : ", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            OutlinedTextField(
                                value = startMinText,
                                onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) startMinText = it },
                                modifier = Modifier.width(56.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Monospace
                                ),
                                singleLine = true
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End Time", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = endHourText,
                                onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) endHourText = it },
                                modifier = Modifier.width(56.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Monospace
                                ),
                                singleLine = true
                            )
                            Text(" : ", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            OutlinedTextField(
                                value = endMinText,
                                onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) endMinText = it },
                                modifier = Modifier.width(56.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Monospace
                                ),
                                singleLine = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Days Selection (Mon to Sun)
                Text(
                    "Repeat On:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayNames.forEachIndexed { dIndex, name ->
                        val bit = 1 shl dIndex
                        val isDayActive = (daysMask and bit) != 0
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isDayActive) colors.primary
                                    else (if (colors.isDark) colors.surfaceVariant else Color(0xFFD6D4CC))
                                )
                                .clickable { daysMask = daysMask xor bit },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isDayActive) Color.White else colors.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(onClick = { daysMask = 31 }, modifier = Modifier.weight(1f)) {
                        Text("Mon-Fri", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                    }
                    TextButton(onClick = { daysMask = 96 }, modifier = Modifier.weight(1f)) {
                        Text("Weekends", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                    }
                    TextButton(onClick = { daysMask = 127 }, modifier = Modifier.weight(1f)) {
                        Text("Everyday", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sH = (startHourText.toIntOrNull() ?: 9).coerceIn(0, 23)
                    val sM = (startMinText.toIntOrNull() ?: 0).coerceIn(0, 59)
                    val eH = (endHourText.toIntOrNull() ?: 17).coerceIn(0, 23)
                    val eM = (endMinText.toIntOrNull() ?: 0).coerceIn(0, 59)
                    if (selectedProfileId > 0 && daysMask > 0) {
                        onSaveSchedule(
                            Schedule(
                                profileId = selectedProfileId,
                                daysOfWeekMask = daysMask,
                                startMinuteOfDay = sH * 60 + sM,
                                endMinuteOfDay = eH * 60 + eM,
                                isEnabled = true
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Routine", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textMuted)
            }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(16.dp)
    )
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

            // Time range display: e.g. 09:00 to 17:00
            val startH = schedule.startMinuteOfDay / 60
            val startM = schedule.startMinuteOfDay % 60
            val endH = schedule.endMinuteOfDay / 60
            val endM = schedule.endMinuteOfDay % 60

            Text(
                text = String.format("%02d:%02d to %02d:%02d", startH, startM, endH, endM),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                ),
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Days of Week Chips (Mon..Sun mapping to ISO bits 0..6)
            val days = listOf("M", "T", "W", "T", "F", "S", "S")
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
                val (activitySubtitle, activityBars) = remember(selectedFilter, insightsData.recentEvents) {
                    val now = System.currentTimeMillis()
                    val events = insightsData.recentEvents
                    when (selectedFilter) {
                        0 -> { // Week (past 7 days)
                            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                            val bars = (6 downTo 0).map { i ->
                                val dayCal = Calendar.getInstance().apply {
                                    timeInMillis = now - (i * 24 * 60 * 60 * 1000L)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val start: Long = dayCal.timeInMillis
                                val end: Long = start + (24 * 60 * 60 * 1000L)
                                val count = events.count { it.timestamp in start until end }
                                ActivityBarData(dayFormat.format(Date(start)).uppercase(), count, i == 0)
                            }
                            Pair("Real logged intercepts across the past 7 days.", bars)
                        }
                        1 -> { // Month (past 4 weeks)
                            val bars = (3 downTo 0).map { i ->
                                val start: Long = now - ((i + 1) * 7L * 24 * 60 * 60 * 1000L)
                                val end: Long = now - (i * 7L * 24 * 60 * 60 * 1000L)
                                val count = events.count { it.timestamp in start until end }
                                ActivityBarData("W${4 - i}", count, i == 0)
                            }
                            Pair("Real logged intercepts grouped by week (past 4 weeks).", bars)
                        }
                        else -> { // Year (past 6 months)
                            val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                            val bars = (5 downTo 0).map { i ->
                                val mCal = Calendar.getInstance().apply {
                                    timeInMillis = now
                                    add(Calendar.MONTH, -i)
                                    set(Calendar.DAY_OF_MONTH, 1)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val start: Long = mCal.timeInMillis
                                val endCal = Calendar.getInstance().apply {
                                    timeInMillis = start
                                    add(Calendar.MONTH, 1)
                                }
                                val end: Long = endCal.timeInMillis
                                val count = events.count { it.timestamp in start until end }
                                ActivityBarData(monthFormat.format(Date(start)).uppercase(), count, i == 0)
                            }
                            Pair("Real logged intercepts across the past 6 months.", bars)
                        }
                    }
                }

                val maxCount = remember(activityBars) {
                    activityBars.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
                }
                val maxVal = activityBars.maxOfOrNull { it.count } ?: 0

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
                    if (maxVal > 0) {
                        Text(
                            text = "Peak: $maxVal in period",
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
                    text = activitySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (activityBars.all { it.count == 0 }) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No activity recorded for this period",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    } else {
                        activityBars.forEach { bar ->
                            val heightFraction = if (bar.count == 0) 0.05f else (bar.count.toFloat() / maxCount).coerceIn(0.12f, 1f)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                if (bar.count > 0) {
                                    Text(
                                        text = "${bar.count}",
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
                                            if (bar.count > 0) colors.primary
                                            else if (bar.isCurrent) colors.primary.copy(alpha = 0.35f)
                                            else if (colors.isDark) colors.surfaceVariant else CharcoalDark.copy(alpha = 0.15f)
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = bar.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (bar.isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (bar.isCurrent) colors.primary else colors.textMuted
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

private data class ActivityBarData(
    val label: String,
    val count: Int,
    val isCurrent: Boolean
)
