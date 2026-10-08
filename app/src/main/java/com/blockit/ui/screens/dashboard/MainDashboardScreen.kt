package com.blockit.ui.screens.dashboard

import com.blockit.ui.components.StopSessionFrictionDialog
import com.blockit.ui.components.formatMinuteOfDayAmPm
import com.blockit.ui.components.fromMinuteOfDay
import com.blockit.ui.components.isOvernightWindow
import com.blockit.ui.components.toMinuteOfDay

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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Shield
import com.blockit.domain.model.ThemeMode
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
import androidx.compose.material3.FilterChip
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
import com.blockit.domain.model.FocusDurationState
import com.blockit.domain.model.ListType
import com.blockit.domain.model.ProfileType
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.rounded.Schedule
import com.blockit.ui.components.CreateProfileDialog
import com.blockit.ui.components.FocusDurationStepper
import com.blockit.ui.components.EditScheduleDialog
import com.blockit.ui.components.formatDaysSummary
import com.blockit.ui.components.formatMinuteOfDayAmPm
import com.blockit.ui.components.formatNextScheduleRun
import com.blockit.ui.components.isOvernightWindow
import com.blockit.ui.components.FocusActiveSessionCard
import com.blockit.ui.components.FocusAppIcon
import com.blockit.ui.components.FocusButton
import com.blockit.ui.components.FocusDestructiveButton
import com.blockit.ui.components.FocusEmptyState
import com.blockit.ui.components.FocusAlertDialog
import com.blockit.ui.components.FocusMonolithMark
import com.blockit.ui.components.FocusNavBar
import com.blockit.ui.components.FocusOutlinedButton
import com.blockit.ui.components.FocusSectionHeader
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography
import com.blockit.ui.theme.WarningAmber
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

@OptIn(ExperimentalMaterial3Api::class)
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
    onEditProfileClick: (Long) -> Unit = onAddAppsClick,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Timer, 1: Profiles, 2: Routines, 3: Statistics

    var showHelpDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val profiles by viewModel.profiles.collectAsState()
    val isProfilesLoading by viewModel.isProfilesLoading.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val ruleSnapshot by viewModel.activeRuleSnapshot.collectAsState()
    val strictConfig by viewModel.strictModeConfig.collectAsState()
    val insightsData by viewModel.insightsData.collectAsState()
    val selectedProfileId by viewModel.selectedProfileId.collectAsState()
    val allProfileApps by viewModel.allProfileApps.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    // Strict Mode friction state
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingDescription by remember { mutableStateOf("") }
    var showStopConfirmDialog by remember { mutableStateOf(false) }
    var pendingStopSessionId by remember { mutableStateOf<Long?>(null) }
    var activeSessionStrictness by rememberSaveable { mutableStateOf(SessionStrictness.NORMAL) }
    var targetProfileIdForDuration by remember { mutableStateOf<Long?>(null) }
    var showProfileDurationSheet by remember { mutableStateOf(false) }

    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
    var showOnboardingChecklist by rememberSaveable { mutableStateOf(false) }

    // Live permission and engine status check (read real state immediately, never default to negative)
    var engineStatus by remember { mutableStateOf(com.blockit.core.permission.PermissionIntents.getAccessibilityEngineStatus(context)) }
    var isOverlayEnabled by remember { mutableStateOf(com.blockit.core.permission.PermissionIntents.canDrawOverlays(context)) }
    var isBatteryIgnored by remember { mutableStateOf(com.blockit.core.permission.PermissionIntents.isIgnoringBatteryOptimizations(context)) }

    fun refreshPermissions() {
        engineStatus = com.blockit.core.permission.PermissionIntents.getAccessibilityEngineStatus(context)
        isOverlayEnabled = com.blockit.core.permission.PermissionIntents.canDrawOverlays(context)
        isBatteryIgnored = com.blockit.core.permission.PermissionIntents.isIgnoringBatteryOptimizations(context)
    }

    val isAccessibilityEnabled = engineStatus == com.blockit.core.engine.AccessibilityEngineStatus.CONNECTED

    LaunchedEffect(onboardingCompleted) {
        if (!onboardingCompleted) {
            showOnboardingChecklist = true
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        refreshPermissions()
        if (!onboardingCompleted) {
            showOnboardingChecklist = true
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
                if (!onboardingCompleted) {
                    showOnboardingChecklist = true
                }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
                    ) {
                        FocusMonolithMark(
                            size = 24.dp,
                            tint = colors.accent
                        )
                        Text(
                            text = "FocusShield",
                            style = FocusTypography.title,
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

                // Theme Chooser Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "THEME",
                        style = FocusTypography.caption.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = colors.textTertiary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (colors.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                            .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val currentTheme = themeMode
                        val options = listOf(
                            Triple("System", Icons.Default.SettingsBrightness, ThemeMode.SYSTEM),
                            Triple("Light", Icons.Default.LightMode, ThemeMode.LIGHT),
                            Triple("Dark", Icons.Default.DarkMode, ThemeMode.DARK)
                        )

                        options.forEach { (label, icon, mode) ->
                            val isSelected = currentTheme == mode
                            Surface(
                                onClick = { viewModel.setThemeMode(mode) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) colors.surface else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.dp, colors.border) else null,
                                shadowElevation = if (isSelected) 1.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) colors.primary else colors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        style = FocusTypography.caption.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) colors.textPrimary else colors.textSecondary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
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
                    icon = { FocusMonolithMark(size = 20.dp, tint = colors.textPrimary) },
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
                    activeProfileName = activeProfile?.name ?: ruleSnapshot.activeScheduleProfileName,
                    isScheduleActive = ruleSnapshot.isScheduleActive,
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
                        isProfilesLoading = isProfilesLoading,
                        engineStatus = engineStatus,
                        isProtected = isProtected,
                        insightsData = insightsData,
                        ruleSnapshot = ruleSnapshot,
                        onFixPermissions = {
                            showOnboardingChecklist = true
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
                        onNavigateToStats = { selectedTab = 2 },
                        onCreateProfileClick = { selectedTab = 1 }
                    )
                    1 -> ProfilesTabContent(
                        profiles = profiles,
                        allProfileApps = allProfileApps,
                        activeSession = activeSession,
                        onAddApps = { id -> onEditProfileClick(id) },
                        onActivateProfile = { id ->
                            targetProfileIdForDuration = id
                            showProfileDurationSheet = true
                        },
                        onDuplicateProfile = { id -> viewModel.duplicateProfile(id) },
                        onDeleteProfile = { id ->
                            if (strictConfig.isEnabled) {
                                pendingDescription = "delete this profile"
                                pendingAction = { viewModel.deleteProfile(id) }
                            } else {
                                viewModel.deleteProfile(id)
                            }
                        },
                        onToggleProfile = { id, enabled ->
                            if (!enabled && strictConfig.isEnabled) {
                                pendingDescription = "turn off this profile"
                                pendingAction = { viewModel.toggleProfileEnabled(id, false) }
                            } else {
                                viewModel.toggleProfileEnabled(id, enabled)
                            }
                        },
                        onCreateProfile = { name, profileType, listType, daysMask, startMinute, endMinute ->
                            viewModel.createProfile(name, profileType, listType, daysMask, startMinute, endMinute)
                        },
                        onCreateTemplateProfile = { type -> viewModel.createTemplateProfile(type) }
                    )
                    2 -> StatisticsTabContent(
                        insightsData = insightsData,
                        activeSession = activeSession
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

            if (showOnboardingChecklist) {
                com.blockit.ui.screens.onboarding.OnboardingChecklistSheet(
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    isOverlayEnabled = isOverlayEnabled,
                    isBatteryIgnored = isBatteryIgnored,
                    isPinConfigured = strictConfig.unlockPin.isNotBlank(),
                    onEnableAccessibility = {
                        com.blockit.core.permission.PermissionIntents.openAccessibilitySettings(context)
                    },
                    onEnableOverlay = {
                        com.blockit.core.permission.PermissionIntents.launchOverlaySettings(context)
                    },
                    onEnableBattery = {
                        com.blockit.core.permission.PermissionIntents.requestBatteryExemption(context)
                    },
                    onSetPin = { pin ->
                        viewModel.setUnlockPin(pin)
                    },
                    onComplete = {
                        viewModel.completeOnboarding()
                        showOnboardingChecklist = false
                    },
                    onDismissRequest = {
                        if (onboardingCompleted || isProtected) {
                            showOnboardingChecklist = false
                        }
                    },
                    canDismiss = onboardingCompleted || isProtected
                )
            }

            if (showProfileDurationSheet && targetProfileIdForDuration != null) {
                val targetProfile = profiles.find { it.id == targetProfileIdForDuration }
                DurationBottomSheet(
                    targetProfile = targetProfile,
                    onDismiss = {
                        showProfileDurationSheet = false
                        targetProfileIdForDuration = null
                    },
                    onStart = { profId, mins, strictness ->
                        showProfileDurationSheet = false
                        targetProfileIdForDuration = null
                        if (!isProtected) {
                            showOnboardingChecklist = true
                        } else {
                            activeSessionStrictness = strictness
                            viewModel.startSession(profId, mins)
                        }
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
    isScheduleActive: Boolean = false,
    onMenuClick: () -> Unit
) {
    val colors = FocusTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = FocusSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Hamburger Menu Button + Logo & Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Open navigation menu",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            FocusMonolithMark(
                size = 20.dp,
                tint = colors.accent
            )

            Text(
                text = "FocusShield",
                style = FocusTypography.title,
                color = colors.textPrimary
            )
        }

        // Right: Active session or active schedule indicator
        val isAnyActive = activeSession != null || isScheduleActive
        if (isAnyActive) {
            Surface(
                shape = FocusShapes.small,
                color = colors.accentContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = FocusSpacing.sm, vertical = FocusSpacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.statusActive)
                    )
                    Text(
                        text = if (!activeProfileName.isNullOrBlank()) "Active: $activeProfileName" else "Active",
                        style = FocusTypography.label,
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
    val routes = listOf("home", "profiles", "stats")
    val currentRoute = routes.getOrElse(selectedTab) { "home" }
    FocusNavBar(
        currentRoute = currentRoute,
        onNavigate = { route ->
            val index = routes.indexOf(route)
            if (index >= 0) onTabSelect(index)
        }
    )
}

// -------------------------------------------------------------
// TAB 1: DASHBOARD HOME (Phase 2 New Structure)
// -------------------------------------------------------------
@Composable
private fun DashboardHomeContent(
    profiles: List<Profile>,
    activeSession: FocusSession?,
    selectedProfileId: Long?,
    isProfilesLoading: Boolean,
    engineStatus: com.blockit.core.engine.AccessibilityEngineStatus,
    isProtected: Boolean,
    insightsData: UsageInsightsData,
    ruleSnapshot: com.blockit.domain.evaluator.ActiveRuleSnapshot,
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
    val colors = FocusTheme.colors

    if (isProfilesLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = FocusSpacing.xl, vertical = FocusSpacing.md),
            verticalArrangement = Arrangement.spacedBy(FocusSpacing.lg)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = FocusShapes.medium,
                color = colors.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, colors.border.copy(alpha = 0.5f))
            ) {}
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp),
                    shape = FocusShapes.medium,
                    color = colors.surfaceVariant.copy(alpha = 0.5f)
                ) {}
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp),
                    shape = FocusShapes.medium,
                    color = colors.surfaceVariant.copy(alpha = 0.5f)
                ) {}
            }
        }
        return
    }

    var showDurationSheet by rememberSaveable { mutableStateOf(false) }
    var showProfilePickerDialog by rememberSaveable { mutableStateOf(false) }

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

    val targetProfile = profiles.find { it.id == (activeSession?.profileId ?: selectedProfileId) } ?: profiles.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FocusSpacing.xl, vertical = FocusSpacing.md),
        verticalArrangement = Arrangement.spacedBy(FocusSpacing.lg)
    ) {
        // SECTION 1: ENGINE STATUS CARD
        if (engineStatus == com.blockit.core.engine.AccessibilityEngineStatus.ENABLED_NOT_CONNECTED) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FocusShapes.medium)
                    .border(1.dp, colors.accent.copy(alpha = 0.3f), FocusShapes.medium)
                    .clickable { onFixPermissions() },
                color = colors.surfaceVariant,
                shape = FocusShapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(FocusSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(FocusSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Engine reconnecting",
                            style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Service recovering after background memory cleanup.",
                            style = FocusTypography.caption,
                            color = colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(FocusSpacing.sm))
                    FocusOutlinedButton(
                        text = "Refresh",
                        onClick = onFixPermissions
                    )
                }
            }
        } else if (!isProtected) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FocusShapes.medium)
                    .border(1.dp, colors.statusDestructive.copy(alpha = 0.3f), FocusShapes.medium)
                    .clickable { onFixPermissions() },
                color = if (colors.isDark) Color(0xFF261214) else Color(0xFFFEF2F2),
                shape = FocusShapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(FocusSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.statusDestructive.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = colors.statusDestructive,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(FocusSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Blocking engine needs setup",
                            style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Grant accessibility, overlay, and battery exemption to enable protection.",
                            style = FocusTypography.caption,
                            color = colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(FocusSpacing.sm))
                    FocusDestructiveButton(
                        text = "Set up",
                        onClick = onFixPermissions
                    )
                }
            }
        }

        // SECTION 2: ACTIVE SESSION CARD or START FOCUS CARD
        if (activeSession != null) {
            val totalSeconds = (activeSession.endTimestamp - activeSession.startTimestamp).coerceAtLeast(1000L) / 1000
            val remainingSeconds = remainingMs / 1000
            FocusActiveSessionCard(
                profileName = targetProfile?.name ?: "Focus",
                isWhitelist = targetProfile?.blockMode == BlockMode.WHITELIST || targetProfile?.listType == ListType.ALLOWLIST,
                remainingSeconds = remainingSeconds,
                totalSeconds = totalSeconds,
                onStopClick = { onEndSession(activeSession.id) }
            )
        } else {
            if (ruleSnapshot.isScheduleActive) {
                val schedProfileName = ruleSnapshot.activeScheduleProfileName ?: "Schedule"
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(FocusShapes.medium)
                        .border(1.dp, colors.accent.copy(alpha = 0.5f), FocusShapes.medium),
                    color = colors.surface,
                    shape = FocusShapes.medium
                ) {
                    Column(modifier = Modifier.padding(FocusSpacing.lg)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.statusActive)
                            )
                            Text(
                                text = "Scheduled session active",
                                style = FocusTypography.label,
                                color = colors.accent
                            )
                        }
                        Spacer(modifier = Modifier.height(FocusSpacing.xs))
                        Text(
                            text = schedProfileName,
                            style = FocusTypography.headline,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(FocusSpacing.xxs))
                        Text(
                            text = "This profile is active according to schedule. Unscheduled manual sessions are disabled while schedule runs.",
                            style = FocusTypography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(FocusSpacing.md))
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FocusShapes.medium)
                    .border(1.dp, colors.border, FocusShapes.medium),
                color = colors.surface,
                shape = FocusShapes.medium
            ) {
                Column(modifier = Modifier.padding(FocusSpacing.lg)) {
                    Text(
                        text = "Start focus",
                        style = FocusTypography.title,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(FocusSpacing.xxs))
                    Text(
                        text = "Select a profile to start blocking.",
                        style = FocusTypography.body,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(FocusSpacing.md))

                    // Selected Profile selector row
                    Surface(
                        onClick = { if (!ruleSnapshot.isScheduleActive) showProfilePickerDialog = true },
                        shape = FocusShapes.small,
                        color = colors.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(FocusSpacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FocusMonolithMark(
                                size = 20.dp,
                                tint = colors.accent
                            )
                            Spacer(modifier = Modifier.width(FocusSpacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                                ) {
                                    Text(
                                        text = targetProfile?.name ?: "No profile selected",
                                        style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.textPrimary
                                    )
                                    if (targetProfile != null) {
                                        val isWhitelist = targetProfile.blockMode == BlockMode.WHITELIST || targetProfile.listType == ListType.ALLOWLIST
                                        Surface(
                                            shape = FocusShapes.small,
                                            color = colors.accentContainer
                                        ) {
                                            Text(
                                                text = if (isWhitelist) "Allowlist" else "Blocklist",
                                                style = FocusTypography.label,
                                                color = colors.accent,
                                                modifier = Modifier.padding(horizontal = FocusSpacing.xs, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = when {
                                        targetProfile == null -> "Tap to choose a profile"
                                        targetProfile.blockMode == BlockMode.WHITELIST || targetProfile.listType == ListType.ALLOWLIST -> {
                                            if (targetProfile.domainCount > 0) "${targetProfile.appCount} apps, ${targetProfile.domainCount} sites"
                                            else "${targetProfile.appCount} permitted apps"
                                        }
                                        else -> "${targetProfile.appCount} apps, ${targetProfile.domainCount} sites"
                                    },
                                    style = FocusTypography.caption,
                                    color = colors.textSecondary
                                )
                            }
                            if (!ruleSnapshot.isScheduleActive) {
                                Text(
                                    text = "Change",
                                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = colors.accent
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(FocusSpacing.lg))

                    val isScheduleActive = ruleSnapshot.isScheduleActive
                    val startButtonText = when {
                        isScheduleActive -> "Start disabled — schedule running"
                        !isProtected -> "Set up engine to start"
                        targetProfile == null -> "Create profile to start"
                        else -> "Choose duration and start"
                    }

                    var lastChooseDurationClickTime by remember { mutableLongStateOf(0L) }
                    FocusButton(
                        text = startButtonText,
                        enabled = !isScheduleActive,
                        onClick = {
                            val clickNow = System.currentTimeMillis()
                            if (clickNow - lastChooseDurationClickTime >= 400L) {
                                lastChooseDurationClickTime = clickNow
                                if (!isProtected) {
                                    onFixPermissions()
                                } else if (targetProfile != null) {
                                    showDurationSheet = true
                                } else {
                                    onCreateProfileClick()
                                }
                            }
                        },
                        leadingIcon = if (!isProtected) Icons.Rounded.Warning else Icons.Rounded.PlayArrow,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isScheduleActive) {
                        Spacer(modifier = Modifier.height(FocusSpacing.xs))
                        Text(
                            text = "A scheduled focus block is currently active. Manual sessions cannot be started while a schedule runs.",
                            style = FocusTypography.caption,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // SECTION 3: TODAY'S STATS SUMMARY
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FocusShapes.medium)
                .border(1.dp, colors.border, FocusShapes.medium),
            color = colors.surface,
            shape = FocusShapes.medium
        ) {
            Column(modifier = Modifier.padding(FocusSpacing.lg)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's activity",
                        style = FocusTypography.title,
                        color = colors.textPrimary
                    )
                    TextButton(
                        onClick = onNavigateToStats,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "All stats",
                            style = FocusTypography.label,
                            color = colors.accent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FocusSpacing.md))

                val liveSessionMinutes = if (activeSession != null) {
                    val elapsedMs = (System.currentTimeMillis() - activeSession.startTimestamp).coerceAtLeast(0L)
                    (elapsedMs / 1000 / 60).toInt()
                } else 0
                val liveTotalFocusMinutes = insightsData.totalFocusMinutes + liveSessionMinutes
                val liveFormattedFocusTime = TimeSavedCalculator.formatMinutes(liveTotalFocusMinutes)

                val hasData = insightsData.blockedToday > 0 || liveTotalFocusMinutes > 0 || activeSession != null

                if (!hasData) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = FocusSpacing.sm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No distraction attempts today. Start a focus session to begin.",
                            style = FocusTypography.body,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = FocusShapes.small,
                            color = colors.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(FocusSpacing.md)) {
                                Text(
                                    text = "Focus time",
                                    style = FocusTypography.caption,
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(FocusSpacing.xxs))
                                Text(
                                    text = liveFormattedFocusTime,
                                    style = FocusTypography.headline,
                                    color = colors.textPrimary
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = FocusShapes.small,
                            color = colors.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(FocusSpacing.md)) {
                                Text(
                                    text = "Intercepted",
                                    style = FocusTypography.caption,
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(FocusSpacing.xxs))
                                Text(
                                    text = "${insightsData.blockedToday}",
                                    style = FocusTypography.headline,
                                    color = colors.accent
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION 4: PROFILES OVERVIEW & TEMPLATES
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FocusShapes.medium)
                .border(1.dp, colors.border, FocusShapes.medium),
            color = colors.surface,
            shape = FocusShapes.medium
        ) {
            Column(modifier = Modifier.padding(FocusSpacing.lg)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profiles",
                        style = FocusTypography.title,
                        color = colors.textPrimary
                    )
                    TextButton(
                        onClick = onNavigateToProfiles,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Manage",
                            style = FocusTypography.label,
                            color = colors.accent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FocusSpacing.sm))

                // Template quick-add buttons (horizontally scrollable so labels never wrap at any font scale)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
                ) {
                    FocusOutlinedButton(
                        text = "Work hours",
                        leadingIcon = Icons.Default.Add,
                        onClick = { onCreateTemplateProfile("WORK") }
                    )
                    FocusOutlinedButton(
                        text = "Sleep/rest",
                        leadingIcon = Icons.Default.Add,
                        onClick = { onCreateTemplateProfile("SLEEP") }
                    )
                    FocusOutlinedButton(
                        text = "Deep study",
                        leadingIcon = Icons.Default.Add,
                        onClick = { onCreateTemplateProfile("STUDY") }
                    )
                }

                Spacer(modifier = Modifier.height(FocusSpacing.md))

                // Profile items list
                Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)) {
                    profiles.forEach { prof ->
                        val isSelected = prof.id == targetProfile?.id
                        Surface(
                            onClick = {
                                if (activeSession == null) {
                                    onSelectProfile(prof.id)
                                }
                            },
                            shape = FocusShapes.small,
                            color = if (isSelected) colors.accentContainer else colors.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) colors.accent else colors.border
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(FocusSpacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                                    ) {
                                        Text(
                                            text = prof.name,
                                            style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = colors.textPrimary
                                        )
                                        val isWhitelist = prof.blockMode == BlockMode.WHITELIST
                                        Surface(
                                            shape = FocusShapes.small,
                                            color = colors.surface
                                        ) {
                                            Text(
                                                text = if (isWhitelist) "Allowlist" else "Blocklist",
                                                style = FocusTypography.label,
                                                color = if (isWhitelist) colors.accent else colors.textSecondary,
                                                modifier = Modifier.padding(horizontal = FocusSpacing.xs, vertical = 2.dp)
                                            )
                                        }
                                        if (activeSession != null) {
                                            Surface(
                                                shape = FocusShapes.small,
                                                color = colors.surface
                                            ) {
                                                Text(
                                                    text = "Locked",
                                                    style = FocusTypography.label,
                                                    color = WarningAmber,
                                                    modifier = Modifier.padding(horizontal = FocusSpacing.xs, vertical = 2.dp)
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
                                        style = FocusTypography.caption,
                                        color = colors.textSecondary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onDuplicateProfile(prof.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.ContentCopy,
                                            contentDescription = "Duplicate profile",
                                            tint = colors.textTertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(FocusSpacing.xs))
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = "Selected profile",
                                            tint = colors.accent,
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
    val colors = FocusTheme.colors
    var totalMinutes by rememberSaveable { mutableIntStateOf(25) }
    val durationState = remember(totalMinutes) {
        FocusDurationState().withTotalMinutes(totalMinutes)
    }
    var selectedStrictness by rememberSaveable { mutableStateOf(SessionStrictness.NORMAL) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = FocusShapes.large,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = FocusSpacing.sm)
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
                .navigationBarsPadding()
        ) {
            // Scrollable upper content
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = FocusSpacing.xl)
                    .padding(bottom = FocusSpacing.md),
                verticalArrangement = Arrangement.spacedBy(FocusSpacing.md)
            ) {
                Text(
                    text = "Start focus session",
                    style = FocusTypography.headline,
                    color = colors.textPrimary
                )

                // Target Profile Summary
                if (targetProfile != null) {
                    Surface(
                        shape = FocusShapes.medium,
                        color = colors.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(FocusSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Profile: ${targetProfile.name}",
                                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = if (targetProfile.blockMode == BlockMode.WHITELIST || targetProfile.listType == ListType.ALLOWLIST)
                                        "Allowlist mode (${targetProfile.appCount} permitted)"
                                    else
                                        "Blocklist mode (${targetProfile.appCount} blocked)",
                                    style = FocusTypography.caption,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // Duration Section
                Text(
                    text = "Duration",
                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )

                FocusDurationStepper(
                    durationState = durationState,
                    onDurationChanged = { updated ->
                        totalMinutes = updated.totalMinutes
                    }
                )

                // Strictness Selection
                Text(
                    text = "Session strictness",
                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)) {
                    listOf(
                        Triple(SessionStrictness.NORMAL, "Normal", "Stop anytime with brief reflection"),
                        Triple(SessionStrictness.STRICT, "Strict", "15s cooldown and typed phrase to unlock"),
                        Triple(SessionStrictness.LOCKED, "Locked", "Cannot stop session early until timer expires")
                    ).forEach { (level, title, desc) ->
                        val isSelected = selectedStrictness == level
                        Surface(
                            onClick = { selectedStrictness = level },
                            shape = FocusShapes.small,
                            color = if (isSelected) colors.accentContainer else colors.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(FocusSpacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = desc,
                                        style = FocusTypography.caption,
                                        color = colors.textSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Pinned Bottom Surface
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.surface,
                tonalElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FocusSpacing.xl, vertical = FocusSpacing.md)
                ) {
                    val currentMins = totalMinutes.coerceAtLeast(1)
                    var lastStartClickTime by remember { mutableLongStateOf(0L) }
                    FocusButton(
                        text = "Start focus ($currentMins min)",
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastStartClickTime >= 600L && targetProfile != null) {
                                lastStartClickTime = now
                                onStart(targetProfile.id, currentMins, selectedStrictness)
                            }
                        },
                        enabled = targetProfile != null && currentMins > 0,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
    allProfileApps: Map<Long, List<BlockedApp>>,
    activeSession: FocusSession?,
    onAddApps: (Long) -> Unit,
    onActivateProfile: (Long) -> Unit,
    onDuplicateProfile: (Long) -> Unit,
    onDeleteProfile: (Long) -> Unit,
    onToggleProfile: (Long, Boolean) -> Unit,
    onCreateProfile: (String, ProfileType, ListType, Int, Int, Int) -> Unit,
    onCreateTemplateProfile: (String) -> Unit
) {
    val colors = FocusTheme.colors
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<Profile?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FocusSpacing.lg, vertical = FocusSpacing.md)
    ) {
        if (activeSession != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = FocusSpacing.md),
                shape = FocusShapes.medium,
                color = colors.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier.padding(FocusSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(FocusSpacing.sm))
                    Text(
                        text = "Session active. Profiles and rules are locked read-only.",
                        style = FocusTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = colors.textPrimary
                    )
                }
            }
        }

        // Section Header
        FocusSectionHeader(
            title = "Profiles",
            subtitle = "Block distractions or allow only essential tools",
            action = {
                FocusOutlinedButton(
                    text = "New profile",
                    leadingIcon = Icons.Rounded.Add,
                    onClick = { showCreateDialog = true },
                    enabled = activeSession == null
                )
            }
        )

        Spacer(modifier = Modifier.height(FocusSpacing.md))

        if (profiles.isEmpty()) {
            FocusEmptyState(
                title = "No focus profiles yet",
                message = "Create a custom profile or choose a template below to get started.",
                icon = Icons.Rounded.Layers,
                actionButton = {
                    FocusButton(
                        text = "Create profile",
                        leadingIcon = Icons.Rounded.Add,
                        onClick = { showCreateDialog = true },
                        enabled = activeSession == null
                    )
                }
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.md)) {
                profiles.forEach { profile ->
                    val apps = allProfileApps[profile.id].orEmpty()
                    var menuExpanded by remember { mutableStateOf(false) }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = FocusShapes.medium,
                        color = colors.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Column(modifier = Modifier.padding(FocusSpacing.md)) {
                            // Header Row: Name + Mode Pill + Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = profile.name,
                                        style = FocusTypography.title,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(FocusSpacing.sm))
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

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = profile.isEnabled,
                                        onCheckedChange = { onToggleProfile(profile.id, it) },
                                        enabled = activeSession == null,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = colors.accent,
                                            uncheckedThumbColor = colors.textTertiary,
                                            uncheckedTrackColor = colors.surfaceVariant,
                                            uncheckedBorderColor = colors.border
                                        )
                                    )

                                    Box {
                                        IconButton(
                                            onClick = { menuExpanded = true },
                                            enabled = activeSession == null,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.MoreVert,
                                                contentDescription = "Profile actions",
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = menuExpanded,
                                            onDismissRequest = { menuExpanded = false },
                                            modifier = Modifier.background(colors.surface)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Edit profile", style = FocusTypography.bodyMedium, color = colors.textPrimary) },
                                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp)) },
                                                onClick = {
                                                    menuExpanded = false
                                                    onAddApps(profile.id)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Duplicate", style = FocusTypography.bodyMedium, color = colors.textPrimary) },
                                                leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp)) },
                                                onClick = {
                                                    menuExpanded = false
                                                    onDuplicateProfile(profile.id)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete", style = FocusTypography.bodyMedium, color = colors.statusDestructive) },
                                                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = colors.statusDestructive, modifier = Modifier.size(18.dp)) },
                                                onClick = {
                                                    menuExpanded = false
                                                    profileToDelete = profile
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(FocusSpacing.xs))

                            Text(
                                text = if (profile.blockMode == BlockMode.WHITELIST || profile.listType == ListType.ALLOWLIST)
                                    "${apps.size} permitted apps. All other apps locked."
                                else
                                    "${apps.size} apps, ${profile.domainCount} sites",
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
                                        color = colors.textPrimary
                                    )
                                }
                                Text(
                                    text = "Next: ${formatNextScheduleRun(profile.scheduleDaysMask, profile.scheduleStartMinute)}",
                                    style = FocusTypography.caption,
                                    color = colors.accent
                                )
                            }

                            Spacer(modifier = Modifier.height(FocusSpacing.sm))

                            // App Icon Previews Row
                            if (apps.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                                ) {
                                    val previewApps = apps.take(5)
                                    previewApps.forEach { app ->
                                        FocusAppIcon(
                                            packageName = app.packageName,
                                            size = 32.dp
                                        )
                                    }
                                    if (apps.size > 5) {
                                        Surface(
                                            shape = FocusShapes.small,
                                            color = colors.surfaceVariant
                                        ) {
                                            Text(
                                                text = "+${apps.size - 5}",
                                                style = FocusTypography.caption,
                                                color = colors.textSecondary,
                                                modifier = Modifier.padding(horizontal = FocusSpacing.xs, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "No apps configured yet",
                                    style = FocusTypography.caption,
                                    color = colors.textTertiary
                                )
                            }

                            Spacer(modifier = Modifier.height(FocusSpacing.md))

                            // Action Buttons: Activate & Edit
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
                            ) {
                                FocusButton(
                                    text = if (profile.profileType == ProfileType.SCHEDULE) "Start now" else "Start focus",
                                    onClick = { onActivateProfile(profile.id) },
                                    enabled = activeSession == null,
                                    modifier = Modifier.weight(1f)
                                )
                                FocusOutlinedButton(
                                    text = "Edit",
                                    onClick = { onAddApps(profile.id) },
                                    enabled = activeSession == null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(FocusSpacing.xl))

        // Templates Section
        FocusSectionHeader(
            title = "Templates",
            subtitle = "Preconfigured focus profiles ready to use"
        )

        Spacer(modifier = Modifier.height(FocusSpacing.sm))

        Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.sm)) {
            // Template 1: Work Hours
            TemplateCard(
                title = "Work hours",
                subtitle = "Blocks social media, video streaming, and games.",
                badgeText = "Blocklist",
                isWhitelist = false,
                onAdd = { onCreateTemplateProfile("WORK") },
                enabled = activeSession == null
            )

            // Template 2: Sleep & Rest
            TemplateCard(
                title = "Sleep & Rest",
                subtitle = "Blocks late-night streaming and endless video feeds.",
                badgeText = "Blocklist",
                isWhitelist = false,
                onAdd = { onCreateTemplateProfile("SLEEP") },
                enabled = activeSession == null
            )

            // Template 3: Deep Study
            TemplateCard(
                title = "Deep Study",
                subtitle = "Locks phone down to only permitted study and reference tools.",
                badgeText = "Allowlist",
                isWhitelist = true,
                onAdd = { onCreateTemplateProfile("STUDY") },
                enabled = activeSession == null
            )
        }

        Spacer(modifier = Modifier.height(FocusSpacing.xxl))
    }

    // Create Profile Dialog
    if (showCreateDialog) {
        CreateProfileDialog(
            initialType = ProfileType.FOCUS,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, profileType, listType, daysMask, startMinute, endMinute ->
                onCreateProfile(name, profileType, listType, daysMask, startMinute, endMinute)
                showCreateDialog = false
            }
        )
    }

    // Delete confirmation dialog
    if (profileToDelete != null) {
        val target = profileToDelete!!
        FocusAlertDialog(
            title = "Delete profile",
            text = "Are you sure you want to delete '${target.name}'? Configured rules and apps for this profile will be removed.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                onDeleteProfile(target.id)
                profileToDelete = null
            },
            onDismiss = {
                profileToDelete = null
            }
        )
    }
}

@Composable
private fun TemplateCard(
    title: String,
    subtitle: String,
    badgeText: String,
    isWhitelist: Boolean,
    onAdd: () -> Unit,
    enabled: Boolean
) {
    val colors = FocusTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FocusShapes.medium,
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FocusSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(FocusSpacing.xs))
                    Surface(
                        shape = FocusShapes.small,
                        color = colors.surfaceVariant
                    ) {
                        Text(
                            text = badgeText,
                            style = FocusTypography.caption,
                            color = if (isWhitelist) colors.accent else colors.textSecondary,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = FocusSpacing.xs, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = FocusTypography.caption,
                    color = colors.textSecondary
                )
            }
            Spacer(modifier = Modifier.width(FocusSpacing.sm))
            FocusOutlinedButton(
                text = "Use",
                onClick = onAdd,
                enabled = enabled
            )
        }
    }
}


// -------------------------------------------------------------
// TAB 3: SCHEDULES (Option B Minimal Design)
// -------------------------------------------------------------
@Composable
private fun RoutinesTabContent(
    profiles: List<Profile>,
    allProfileApps: Map<Long, List<BlockedApp>>,
    isProfilesLoading: Boolean,
    onAddApps: (Long) -> Unit,
    onToggleProfile: (Long, Boolean) -> Unit,
    onUpdateSchedule: (Long, Int, Int, Int) -> Unit,
    onCreateScheduleProfile: (String, ProfileType, ListType, Int, Int, Int) -> Unit,
    onDeleteProfile: (Long) -> Unit,
    onDuplicateProfile: (Long) -> Unit
) {
    val colors = FocusTheme.colors
    val scheduleProfiles = remember(profiles) {
        profiles.filter { it.profileType == ProfileType.SCHEDULE }
    }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var profileToEditSchedule by remember { mutableStateOf<Profile?>(null) }
    var profileToDelete by remember { mutableStateOf<Profile?>(null) }

    val presets = remember {
        listOf(
            RoutinePresetItem("Workday", "9:00 AM – 5:00 PM (Mon–Fri)", 31, 540, 1020),
            RoutinePresetItem("Deep work", "8:00 AM – 12:00 PM (Mon–Fri)", 31, 480, 720),
            RoutinePresetItem("Evening", "8:30 PM – 11:30 PM (Daily)", 127, 1230, 1410),
            RoutinePresetItem("Sleep", "10:30 PM – 6:30 AM (Daily)", 127, 1350, 390),
            RoutinePresetItem("Weekend", "10:00 AM – 6:00 PM (Sat–Sun)", 96, 600, 1080)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FocusSpacing.lg, vertical = FocusSpacing.md)
    ) {
        FocusSectionHeader(
            title = "Schedules",
            subtitle = "Turn focus modes on and off automatically",
            action = {
                FocusOutlinedButton(
                    text = "New schedule",
                    leadingIcon = Icons.Rounded.Add,
                    onClick = { showCreateDialog = true }
                )
            }
        )

        Spacer(modifier = Modifier.height(FocusSpacing.md))

        // Quick Presets
        Text(
            text = "Quick presets",
            style = FocusTypography.label,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(FocusSpacing.xs))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presets) { preset ->
                Surface(
                    onClick = {
                        onCreateScheduleProfile(
                            preset.name,
                            ProfileType.SCHEDULE,
                            ListType.BLOCKLIST,
                            preset.daysMask,
                            preset.startMinute,
                            preset.endMinute
                        )
                    },
                    shape = FocusShapes.small,
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(modifier = Modifier.padding(horizontal = FocusSpacing.sm, vertical = FocusSpacing.xs)) {
                        Text(
                            text = "+ ${preset.name}",
                            style = FocusTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.accent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = preset.timeRangeText,
                            style = FocusTypography.caption,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(FocusSpacing.xl))

        FocusSectionHeader(
            title = "Active schedules",
            subtitle = "Recurring automation rules"
        )
        Spacer(modifier = Modifier.height(FocusSpacing.sm))

        if (isProfilesLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FocusSpacing.xl),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = colors.accent
                )
            }
        } else if (scheduleProfiles.isEmpty()) {
            FocusEmptyState(
                title = "No schedules active",
                message = "Add a schedule to automatically enforce focus rules on recurring days and times.",
                icon = Icons.Rounded.Schedule,
                actionButton = {
                    FocusButton(
                        text = "Create schedule",
                        leadingIcon = Icons.Rounded.Add,
                        onClick = { showCreateDialog = true }
                    )
                }
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(FocusSpacing.md)) {
                scheduleProfiles.forEach { profile ->
                    ScheduleProfileCard(
                        profile = profile,
                        apps = allProfileApps[profile.id].orEmpty(),
                        onToggle = { isEnabled -> onToggleProfile(profile.id, isEnabled) },
                        onEditSchedule = { profileToEditSchedule = profile },
                        onManageApps = { onAddApps(profile.id) },
                        onDelete = { profileToDelete = profile },
                        onDuplicate = { onDuplicateProfile(profile.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(FocusSpacing.xxl))
    }

    if (showCreateDialog) {
        CreateProfileDialog(
            initialType = ProfileType.SCHEDULE,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, profileType, listType, daysMask, startMinute, endMinute ->
                onCreateScheduleProfile(name, profileType, listType, daysMask, startMinute, endMinute)
                showCreateDialog = false
            }
        )
    }

    if (profileToEditSchedule != null) {
        val target = profileToEditSchedule!!
        EditScheduleDialog(
            profileName = target.name,
            initialDaysMask = target.scheduleDaysMask,
            initialStartMinute = target.scheduleStartMinute,
            initialEndMinute = target.scheduleEndMinute,
            onDismiss = { profileToEditSchedule = null },
            onConfirm = { daysMask, startMinute, endMinute ->
                onUpdateSchedule(target.id, daysMask, startMinute, endMinute)
                profileToEditSchedule = null
            }
        )
    }

    if (profileToDelete != null) {
        val target = profileToDelete!!
        FocusAlertDialog(
            title = "Delete schedule",
            text = "Are you sure you want to delete '${target.name}'?",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                onDeleteProfile(target.id)
                profileToDelete = null
            },
            onDismiss = { profileToDelete = null }
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
private fun ScheduleProfileCard(
    profile: Profile,
    apps: List<BlockedApp>,
    onToggle: (Boolean) -> Unit,
    onEditSchedule: () -> Unit,
    onManageApps: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    val colors = FocusTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }
    val isOvernight = isOvernightWindow(profile.scheduleStartMinute, profile.scheduleEndMinute)
    val timeRangeStr = "${formatMinuteOfDayAmPm(profile.scheduleStartMinute)} – ${formatMinuteOfDayAmPm(profile.scheduleEndMinute)}"
    val daysSummary = formatDaysSummary(profile.scheduleDaysMask)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FocusShapes.medium,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(FocusSpacing.md)) {
            // Header Row: Name + List Badge + Switch + More Actions Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = profile.name,
                        style = FocusTypography.title,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(FocusSpacing.xs))
                    Surface(
                        shape = FocusShapes.small,
                        color = colors.surfaceVariant
                    ) {
                        Text(
                            text = if (profile.listType == ListType.ALLOWLIST || profile.blockMode == BlockMode.WHITELIST) "Allowlist" else "Blocklist",
                            style = FocusTypography.caption,
                            color = if (profile.listType == ListType.ALLOWLIST || profile.blockMode == BlockMode.WHITELIST) colors.accent else colors.textSecondary,
                            modifier = Modifier.padding(horizontal = FocusSpacing.xs, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
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

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Schedule actions",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(colors.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit schedule", style = FocusTypography.bodyMedium, color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onEditSchedule()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Manage apps", style = FocusTypography.bodyMedium, color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onManageApps()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate", style = FocusTypography.bodyMedium, color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onDuplicate()
                                }
                            )
                            HorizontalDivider(color = colors.border)
                            DropdownMenuItem(
                                text = { Text("Delete", style = FocusTypography.bodyMedium, color = colors.statusDestructive) },
                                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = colors.statusDestructive, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(FocusSpacing.sm))

            // Time & Days summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = timeRangeStr,
                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )
                Text(
                    text = "•",
                    style = FocusTypography.bodySmall,
                    color = colors.textTertiary
                )
                Text(
                    text = daysSummary,
                    style = FocusTypography.bodySmall,
                    color = colors.textSecondary
                )
                if (isOvernight) {
                    Spacer(modifier = Modifier.width(FocusSpacing.xs))
                    Surface(
                        shape = FocusShapes.small,
                        color = colors.surfaceVariant
                    ) {
                        Text(
                            text = "+1d",
                            style = FocusTypography.caption,
                            color = colors.accent,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(FocusSpacing.xs))

            // Apps & sites summary
            Text(
                text = "${apps.size} apps, ${profile.domainCount} sites",
                style = FocusTypography.caption,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(FocusSpacing.md))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
            ) {
                FocusOutlinedButton(
                    text = "Edit time",
                    leadingIcon = Icons.Rounded.Schedule,
                    onClick = onEditSchedule,
                    modifier = Modifier.weight(1f)
                )
                FocusButton(
                    text = "Manage apps",
                    leadingIcon = Icons.Rounded.Edit,
                    onClick = onManageApps,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: STATISTICS (Matching video frame 00:31)
// -------------------------------------------------------------
// -------------------------------------------------------------
// TAB 4: STATISTICS (Option B Minimal Design)
// -------------------------------------------------------------
@Composable
private fun StatisticsTabContent(
    insightsData: UsageInsightsData,
    activeSession: FocusSession? = null
) {
    val colors = FocusTheme.colors
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: Week, 1: Month, 2: Year
    val currentMonthYear = remember {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    }

    val liveSessionMinutes = if (activeSession != null) {
        val elapsedMs = (System.currentTimeMillis() - activeSession.startTimestamp).coerceAtLeast(0L)
        ((elapsedMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
    } else 0
    val liveTotalFocusMinutes = insightsData.totalFocusMinutes + liveSessionMinutes
    val liveFormattedFocusTime = TimeSavedCalculator.formatMinutes(liveTotalFocusMinutes)
    val hasData = insightsData.totalBlocked > 0 || liveTotalFocusMinutes > 0 || activeSession != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FocusSpacing.lg, vertical = FocusSpacing.md)
    ) {
        // Date Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentMonthYear,
                style = FocusTypography.headline,
                color = colors.textPrimary
            )
            Surface(
                shape = FocusShapes.small,
                color = colors.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "$liveFormattedFocusTime focused",
                    style = FocusTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primary,
                    modifier = Modifier.padding(horizontal = FocusSpacing.sm, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(FocusSpacing.md))

        // Period filter buttons: Week, Month, Year
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
        ) {
            listOf("Week", "Month", "Year").forEachIndexed { index, label ->
                val isSelected = selectedFilter == index
                Surface(
                    onClick = { selectedFilter = index },
                    shape = FocusShapes.small,
                    color = if (isSelected) colors.primary else colors.surface,
                    border = BorderStroke(1.dp, if (isSelected) colors.primary else colors.border),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(modifier = Modifier.padding(vertical = FocusSpacing.sm), contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = FocusTypography.caption.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
                            color = if (isSelected) Color.White else colors.textPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(FocusSpacing.lg))

        // Real Histogram / Bar Chart with Labeled Axes
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = FocusShapes.medium,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FocusSpacing.md)
            ) {
                val (activitySubtitle, activityBars) = remember(selectedFilter, insightsData.recentEvents) {
                    val now = System.currentTimeMillis()
                    val events = insightsData.recentEvents
                    when (selectedFilter) {
                        0 -> { // Week (past 7 days)
                            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                            val bars = (6 downTo 0).map { i ->
                                val dayCal = Calendar.getInstance().apply {
                                    timeInMillis = now
                                    add(Calendar.DAY_OF_YEAR, -i)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val start: Long = dayCal.timeInMillis
                                val endCal = Calendar.getInstance().apply {
                                    timeInMillis = start
                                    add(Calendar.DAY_OF_YEAR, 1)
                                }
                                val end: Long = endCal.timeInMillis
                                val count = events.count { it.timestamp in start until end }
                                val label = dayFormat.format(Date(start)).take(3)
                                ActivityBarData(label, count, i == 0)
                            }
                            Pair("Logged intercepts across the past 7 days.", bars)
                        }
                        1 -> { // Month (past 4 weeks)
                            val bars = (3 downTo 0).map { i ->
                                val start: Long = now - ((i + 1) * 7L * 24 * 60 * 60 * 1000L)
                                val end: Long = now - (i * 7L * 24 * 60 * 60 * 1000L)
                                val count = events.count { it.timestamp in start until end }
                                ActivityBarData("W${4 - i}", count, i == 0)
                            }
                            Pair("Logged intercepts grouped by week (past 4 weeks).", bars)
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
                                ActivityBarData(monthFormat.format(Date(start)), count, i == 0)
                            }
                            Pair("Logged intercepts across the past 6 months.", bars)
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
                        text = "Intercept activity",
                        style = FocusTypography.title,
                        color = colors.textPrimary
                    )
                    if (maxVal > 0) {
                        Text(
                            text = "Peak: $maxVal",
                            style = FocusTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = activitySubtitle,
                    style = FocusTypography.bodySmall,
                    color = colors.textMuted
                )

                Spacer(modifier = Modifier.height(FocusSpacing.md))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
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
                                style = FocusTypography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    } else {
                        activityBars.forEach { bar ->
                            val heightFraction = if (bar.count == 0) 0.08f else (bar.count.toFloat() / maxCount).coerceIn(0.15f, 1f)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                // Upper area containing the bar and count
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom
                                    ) {
                                        if (bar.count > 0) {
                                            Text(
                                                text = "${bar.count}",
                                                style = FocusTypography.caption.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = colors.primary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(18.dp)
                                                .height((70.dp * heightFraction).coerceIn(6.dp, 70.dp))
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(
                                                    if (bar.count > 0) colors.primary
                                                    else if (bar.isCurrent) colors.primary.copy(alpha = 0.35f)
                                                    else colors.surfaceVariant
                                                )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                // Every bar has a clearly rendered day label
                                Text(
                                    text = bar.label,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = FocusTypography.caption.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (bar.isCurrent) FontWeight.Bold else FontWeight.Normal
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
            Spacer(modifier = Modifier.height(FocusSpacing.md))
            FocusEmptyState(
                title = "No activity yet",
                message = "Start a focus session to block distracting apps and track your stats here."
            )
        }

        Spacer(modifier = Modifier.height(FocusSpacing.lg))

        // Summary Metric Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
        ) {
            MetricCard(
                title = "Total intercepts",
                value = "${insightsData.totalBlocked}",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Focus time",
                value = liveFormattedFocusTime,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(FocusSpacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm)
        ) {
            MetricCard(
                title = "Blocked today",
                value = "${insightsData.blockedToday}",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Apps protected",
                value = "${insightsData.topTargets.size}",
                modifier = Modifier.weight(1f)
            )
        }

        // Top Intercepted Apps
        if (insightsData.topTargets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(FocusSpacing.lg))
            FocusSectionHeader(title = "Most intercepted")
            Spacer(modifier = Modifier.height(FocusSpacing.xs))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = FocusShapes.medium,
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier.padding(FocusSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs)
                ) {
                    insightsData.topTargets.take(5).forEach { target ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = FocusSpacing.xs, horizontal = FocusSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (target.targetType == "APP") {
                                    FocusAppIcon(packageName = target.identifier, modifier = Modifier.size(28.dp))
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(FocusShapes.small)
                                            .background(colors.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = target.displayName,
                                    style = FocusTypography.bodyMedium,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${target.count} blocked",
                                style = FocusTypography.caption.copy(fontWeight = FontWeight.SemiBold),
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
    val colors = FocusTheme.colors
    Card(
        modifier = modifier,
        shape = FocusShapes.medium,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(FocusSpacing.md)) {
            Text(title, style = FocusTypography.caption, color = colors.textMuted)
            Spacer(modifier = Modifier.height(FocusSpacing.xs))
            Text(
                text = value,
                style = FocusTypography.headline,
                color = colors.textPrimary
            )
        }
    }
}

private data class ActivityBarData(
    val label: String,
    val count: Int,
    val isCurrent: Boolean
)
