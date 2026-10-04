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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Luggage
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
import com.blockit.domain.model.UsageInsightsData
import com.blockit.ui.components.BlockDigitalClock
import com.blockit.ui.components.SlideToActButton
import com.blockit.ui.screens.strictmode.StrictModeChallengeDialog
import com.blockit.ui.theme.BlockitOrange
import com.blockit.ui.theme.CharcoalDark
import com.blockit.ui.theme.CharcoalSurface
import com.blockit.ui.theme.CreamBackground
import com.blockit.ui.theme.CreamSurface
import com.blockit.ui.theme.CreamSurfaceVariant
import com.blockit.ui.theme.CreamText
import com.blockit.ui.theme.CreamTextMuted
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

    // Live permission check
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
        containerColor = CreamBackground,
        topBar = {
            DashboardTopBar(
                isProtected = isAccessibilityEnabled && isOverlayEnabled,
                onStatusClick = {
                    if (!isAccessibilityEnabled) {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } else if (!isOverlayEnabled) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
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
                        isProtected = isAccessibilityEnabled && isOverlayEnabled,
                        onFixPermissions = {
                            if (!isAccessibilityEnabled) {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } else if (!isOverlayEnabled) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        },
                        onSelectProfile = { viewModel.selectProfile(it) },
                        onStartSession = { profId, mins -> viewModel.startSession(profId, mins) },
                        onEndSession = { sessId ->
                            if (strictConfig.isEnabled) {
                                pendingDescription = "stop the ongoing focus session early"
                                pendingAction = { viewModel.endSession(sessId) }
                            } else {
                                viewModel.endSession(sessId)
                            }
                        }
                    )
                    1 -> BagsTabContent(
                        profiles = profiles,
                        selectedProfileId = selectedProfileId,
                        packedApps = packedApps,
                        packedDomains = packedDomains,
                        isProtected = isAccessibilityEnabled && isOverlayEnabled,
                        onFixPermissions = {
                            if (!isAccessibilityEnabled) {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } else if (!isOverlayEnabled) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        },
                        onSelectProfile = { viewModel.selectProfile(it) },
                        onSetBagMode = { id, mode -> viewModel.setBagMode(id, mode) },
                        onToggleProfile = { id, enabled ->
                            if (!enabled && strictConfig.isEnabled) {
                                pendingDescription = "turn off this bag"
                                pendingAction = { viewModel.toggleProfileEnabled(id, false) }
                            } else {
                                viewModel.toggleProfileEnabled(id, enabled)
                            }
                        },
                        onAddApps = { id -> onAddAppsClick(id) },
                        onRemoveApp = { id, pkg -> viewModel.removeApp(id, pkg) },
                        onAddDomain = { id, pattern, wildcard -> viewModel.addDomain(id, pattern, wildcard) },
                        onRemoveDomain = { domainId -> viewModel.removeDomain(domainId) },
                        onCreateBag = { name, mode -> viewModel.createBag(name, mode) }
                    )
                    2 -> RoutinesTabContent(
                        profiles = profiles,
                        selectedProfileId = selectedProfileId,
                        schedules = schedules,
                        onSelectProfile = { viewModel.selectProfile(it) },
                        onSaveSchedule = { viewModel.saveSchedule(it) },
                        onDeleteSchedule = { viewModel.deleteSchedule(it) }
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
        }
    }
}

@Composable
private fun DashboardTopBar(
    isProtected: Boolean,
    onStatusClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Status Pill
        Surface(
            onClick = onStatusClick,
            shape = RoundedCornerShape(20.dp),
            color = if (isProtected) Color(0xFFE2E0D8) else Color(0xFFFFE4E6),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isProtected) Color(0xFFD0CDC4) else Color(0xFFFDA4AF)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isProtected) Color(0xFF22C55E) else Color(0xFFEF4444))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isProtected) "Active" else "Action Req.",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalDark
                )
            }
        }

        // Center Wordmark
        Text(
            text = "blockit",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                letterSpacing = (-1).sp
            ),
            color = CharcoalDark
        )

        // Right Settings Button
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CreamSurface)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = CharcoalDark,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DashboardBottomBar(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        shape = RoundedCornerShape(28.dp),
        color = CreamSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD5D2CA))
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
                Icons.Default.Luggage to "The Bags",
                Icons.Default.Schedule to "Routines",
                Icons.Default.BarChart to "Stats"
            )

            tabs.forEachIndexed { index, (icon, label) ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) CharcoalDark else Color.Transparent)
                        .clickable { onTabSelect(index) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color.White else Color(0xFF7A7670),
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
    onEndSession: (Long) -> Unit
) {
    var selectedDurationMinutes by remember { mutableIntStateOf(15) }
    var showCustomDurationDialog by remember { mutableStateOf(false) }

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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4E6)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDA4AF))
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
                                color = Color(0xFF9F1239)
                            )
                            Text(
                                text = "Tap here to grant Accessibility & Display Over Apps so BlockIt can lock your screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFBE123C)
                            )
                        }
                    }
                }
            }
            // Target Bag Selector
            if (profiles.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(profiles) { profile ->
                        val isSelected = profile.id == targetProfile?.id
                        Surface(
                            onClick = { onSelectProfile(profile.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) BlockitOrange else CreamSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0CDC4))
                        ) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else CharcoalDark,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Giant Block Digital Clock Display
            if (activeSession != null && displayHours == 0) {
                // Show MM : SS during sub-hour active countdown
                BlockDigitalClock(
                    firstValue = displayMins,
                    secondValue = displaySecs,
                    color = CharcoalDark
                )
            } else {
                BlockDigitalClock(
                    firstValue = displayHours,
                    secondValue = displayMins,
                    color = CharcoalDark
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2x2 Duration Selection Grid + Vertical Tick Bar on the side (frame 00:16)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left 2x2 Duration Grid
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DurationTile(
                            label = "15\nMinutes",
                            isSelected = selectedDurationMinutes == 15,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedDurationMinutes = 15 }
                        )
                        DurationTile(
                            label = "45\nMinutes",
                            isSelected = selectedDurationMinutes == 45,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedDurationMinutes = 45 }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DurationTile(
                            label = "3\nHours",
                            isSelected = selectedDurationMinutes == 180,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedDurationMinutes = 180 }
                        )
                        DurationTile(
                            label = if (selectedDurationMinutes !in listOf(15, 45, 180)) "$selectedDurationMinutes\nCustom" else "Custom\nTime",
                            isSelected = selectedDurationMinutes !in listOf(15, 45, 180),
                            modifier = Modifier.weight(1f),
                            onClick = { showCustomDurationDialog = true }
                        )
                    }
                }

                // Right Vertical Tick Mark Track (from video frame 00:16)
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CreamSurface)
                        .border(1.dp, Color(0xFFD5D2CA), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight().padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        repeat(12) { index ->
                            Box(
                                modifier = Modifier
                                    .width(if (index % 3 == 0) 24.dp else 12.dp)
                                    .height(2.5.dp)
                                    .background(if (index == 2) BlockitOrange else Color(0xFFB8B5AC))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Slide to Start / Slide to Stop
        val targetId = targetProfile?.id ?: 1L
        SlideToActButton(
            text = if (activeSession != null) "Slide to Stop" else "Slide to Start",
            isActive = activeSession != null,
            onTrigger = {
                if (activeSession != null) {
                    onEndSession(activeSession.id)
                } else {
                    onStartSession(targetId, selectedDurationMinutes)
                }
            }
        )
    }

    if (showCustomDurationDialog) {
        var inputMinutes by remember { mutableStateOf("25") }
        AlertDialog(
            onDismissRequest = { showCustomDurationDialog = false },
            title = { Text("Set Custom Duration") },
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
                    Text("Set")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDurationDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = CreamBackground,
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
    Surface(
        onClick = onClick,
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) CharcoalDark else CreamSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) CharcoalDark else Color(0xFFD5D2CA)
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
                color = if (isSelected) Color.White else CharcoalDark,
                textAlign = TextAlign.Center
            )
        }
    }
}

// -------------------------------------------------------------
// TAB 2: THE BAGS (PROFILES & WHITELIST) (Matching video frame 00:20)
// -------------------------------------------------------------
@Composable
private fun BagsTabContent(
    profiles: List<Profile>,
    selectedProfileId: Long?,
    packedApps: List<BlockedApp>,
    packedDomains: List<BlockedDomain>,
    isProtected: Boolean,
    onFixPermissions: () -> Unit,
    onSelectProfile: (Long) -> Unit,
    onSetBagMode: (Long, BlockMode) -> Unit,
    onToggleProfile: (Long, Boolean) -> Unit,
    onAddApps: (Long) -> Unit,
    onRemoveApp: (Long, String) -> Unit,
    onAddDomain: (Long, String, Boolean) -> Unit,
    onRemoveDomain: (Long) -> Unit,
    onCreateBag: (String, BlockMode) -> Unit
) {
    val currentBag = profiles.find { it.id == selectedProfileId } ?: profiles.firstOrNull()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddDomainDialog by remember { mutableStateOf(false) }

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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4E6)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDA4AF))
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
                            color = Color(0xFF9F1239)
                        )
                        Text(
                            text = "Tap here to grant Accessibility & Display Over Apps so BlockIt can lock your screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFBE123C)
                        )
                    }
                }
            }
        }
        // Visual luggage bag icons header matching video frame 00:20
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            profiles.take(3).forEach { p ->
                val isActive = p.id == currentBag?.id
                Icon(
                    imageVector = Icons.Default.Luggage,
                    contentDescription = null,
                    tint = if (isActive) BlockitOrange else Color(0xFF9E9B93),
                    modifier = Modifier.size(54.dp).padding(horizontal = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Profile / Bag Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            profiles.forEach { profile ->
                val isSelected = profile.id == currentBag?.id
                Surface(
                    onClick = { onSelectProfile(profile.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) CharcoalDark else CreamSurface,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else CharcoalDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            // Add Bag button
            IconButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(CreamSurface)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Bag", tint = CharcoalDark)
            }
        }

        if (currentBag != null) {
            Spacer(modifier = Modifier.height(16.dp))

            // Bag description tooltip matching video
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CreamSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentBag.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = CharcoalDark
                        )
                        Switch(
                            checked = currentBag.isEnabled,
                            onCheckedChange = { onToggleProfile(currentBag.id, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BlockitOrange
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (currentBag.blockMode == BlockMode.WHITELIST)
                            "The apps you pack stay usable during a block. All other apps on your phone are locked."
                        else
                            "The apps you pack will be blocked during focus sessions. Rest of phone stays normal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CreamTextMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode Switcher buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { onSetBagMode(currentBag.id, BlockMode.WHITELIST) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (currentBag.blockMode == BlockMode.WHITELIST) CharcoalDark else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0CDC4))
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "🔒 Whitelist",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (currentBag.blockMode == BlockMode.WHITELIST) Color.White else CharcoalDark
                                )
                            }
                        }
                        Surface(
                            onClick = { onSetBagMode(currentBag.id, BlockMode.BLACKLIST) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (currentBag.blockMode == BlockMode.BLACKLIST) CharcoalDark else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0CDC4))
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "🛡️ Blacklist",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (currentBag.blockMode == BlockMode.BLACKLIST) Color.White else CharcoalDark
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3-Column Packed Apps Grid with "+" buttons
            Text(
                text = if (currentBag.blockMode == BlockMode.WHITELIST) "Packed Apps (Stay Usable)" else "Packed Apps (Blocked)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CharcoalDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Grid of app slots (3 columns)
            val slots = packedApps + listOf(null) // append null for the "+" button slot
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
                                    color = CreamSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD5D2CA))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                                        IconButton(
                                            onClick = { onRemoveApp(currentBag.id, app.packageName) },
                                            modifier = Modifier.size(18.dp).align(Alignment.TopEnd)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFF8C8881), modifier = Modifier.size(14.dp))
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
                                                color = CharcoalDark,
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
                                    onClick = { onAddApps(currentBag.id) },
                                    modifier = Modifier.weight(1f).height(92.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.Transparent,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC8C5BD))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add", tint = BlockitOrange, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Pack App", style = MaterialTheme.typography.labelSmall, color = CreamTextMuted)
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
                    color = CharcoalDark
                )
                TextButton(onClick = { showAddDomainDialog = true }) {
                    Text("+ Add Domain", color = BlockitOrange, fontWeight = FontWeight.Bold)
                }
            }

            if (packedDomains.isEmpty()) {
                Text(
                    text = "No websites added yet. Add domains like 'instagram.com' or '*.reddit.com'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CreamTextMuted
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    packedDomains.forEach { domain ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = CreamSurface
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(domain.pattern, style = MaterialTheme.typography.bodyMedium, color = CharcoalDark)
                                IconButton(
                                    onClick = { onRemoveDomain(domain.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color(0xFF8C8881), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var bagName by remember { mutableStateOf("") }
        var bagMode by remember { mutableStateOf(BlockMode.WHITELIST) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Bag", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = bagName,
                        onValueChange = { bagName = it },
                        label = { Text("Bag Name (e.g. Work, Study)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Bag Type:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { bagMode = BlockMode.WHITELIST },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (bagMode == BlockMode.WHITELIST) BlockitOrange else CreamSurface
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Whitelist", color = if (bagMode == BlockMode.WHITELIST) Color.White else CharcoalDark)
                        }
                        Button(
                            onClick = { bagMode = BlockMode.BLACKLIST },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (bagMode == BlockMode.BLACKLIST) BlockitOrange else CreamSurface
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Blacklist", color = if (bagMode == BlockMode.BLACKLIST) Color.White else CharcoalDark)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (bagName.isNotBlank()) {
                        onCreateBag(bagName.trim(), bagMode)
                        showCreateDialog = false
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            },
            containerColor = CreamBackground,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showAddDomainDialog && currentBag != null) {
        var domainPattern by remember { mutableStateOf("") }
        var isWildcard by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDomainDialog = false },
            title = { Text("Block Domain") },
            text = {
                Column {
                    OutlinedTextField(
                        value = domainPattern,
                        onValueChange = { domainPattern = it },
                        label = { Text("Domain (e.g. twitter.com)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (domainPattern.isNotBlank()) {
                        onAddDomain(currentBag.id, domainPattern.trim(), isWildcard)
                        showAddDomainDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDomainDialog = false }) { Text("Cancel") }
            },
            containerColor = CreamBackground,
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
    onDeleteSchedule: (Long) -> Unit
) {
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
                color = CharcoalDark
            )
            Text(
                "24:00",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                ),
                color = CharcoalDark
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Pixel Timeline Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFD6D4CC))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .fillMaxSize()
                    .background(BlockitOrange)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Target Bag Selector for Routines
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
                        color = if (isSelected) BlockitOrange else CreamSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0CDC4))
                    ) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else CharcoalDark,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = "Control center",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = CharcoalDark
        )
        Text(
            text = "Routines start a block on their own, at the time you set, on the days you pick.",
            style = MaterialTheme.typography.bodySmall,
            color = CreamTextMuted
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (schedules.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CreamSurface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No automated routines active.",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = CharcoalDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Add a schedule to lock distractions automatically (e.g. 09:00 - 17:00 Mon-Fri).",
                        style = MaterialTheme.typography.bodySmall,
                        color = CreamTextMuted,
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
                        colors = ButtonDefaults.buttonColors(containerColor = CharcoalDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Workday Routine (9am - 5pm)")
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD5D2CA))
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
                    color = CharcoalDark
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = schedule.isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BlockitOrange
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFF8C8881), modifier = Modifier.size(18.dp))
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
                color = CharcoalDark
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
                            .background(if (isDayActive) BlockitOrange else Color(0xFFD6D4CC))
                            .clickable {
                                val newMask = schedule.daysOfWeekMask xor bit
                                onUpdateDays(newMask)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDayActive) Color.White else CharcoalDark
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
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: Day, 1: Month, 2: Year

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        // Date Header matching video frame 00:31
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "OCTOBER 2026",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = CharcoalDark
            )
            Text(
                text = "${insightsData.estimatedMinutesSaved} MIN",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                ),
                color = BlockitOrange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Period filter buttons: Day, Month, Year
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Day", "Month", "Year").forEachIndexed { index, label ->
                val isSelected = selectedFilter == index
                Surface(
                    onClick = { selectedFilter = index },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) CharcoalDark else CreamSurface,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else CharcoalDark
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Visual Histogram / Bar Chart matching video frame 00:32
        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CreamSurface)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalDark
                )
                Text(
                    text = "Every blocked minute ends up here: by day, month or year.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CreamTextMuted
                )

                // Simulated / real bar heights
                Row(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val heights = listOf(0.2f, 0.5f, 0.85f, 0.4f, 0.7f, 0.3f, 0.9f)
                    heights.forEachIndexed { i, h ->
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .fillMaxHeight(h)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (i == 2 || i == 6) BlockitOrange else CharcoalDark)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Summary Metric Tiles
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
                title = "Time Saved",
                value = "${insightsData.estimatedMinutesSaved}m",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = CreamTextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                ),
                color = CharcoalDark
            )
        }
    }
}

@Composable
private fun AppIconView(packageName: String, modifier: Modifier = Modifier) {
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
            modifier = modifier.clip(RoundedCornerShape(8.dp)).background(BlockitOrange),
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
