package com.blockit.ui.screens.profiles

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockit.domain.model.BlockMode
import com.blockit.domain.model.FocusSession
import com.blockit.domain.model.Profile
import com.blockit.ui.screens.strictmode.StrictModeChallengeDialog
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    viewModel: ProfilesViewModel,
    onProfileClick: (Long) -> Unit,
    onOpenSettingsClick: () -> Unit,
    onOpenInsightsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profiles by viewModel.profiles.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val strictConfig by viewModel.strictModeConfig.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

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
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("BlockIt")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenInsightsClick) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Usage Insights"
                        )
                    }
                    IconButton(onClick = onOpenSettingsClick) {
                        Icon(
                            imageVector = if (strictConfig.isEnabled) Icons.Default.Lock else Icons.Default.Settings,
                            contentDescription = "Strict Mode & Settings",
                            tint = if (strictConfig.isEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Profile")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Protection Status Banner
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No focus profiles yet.\nTap '+' to create one.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = "Profiles",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
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
                onConfirm = { name, mode ->
                    viewModel.createProfile(name = name, blockMode = mode)
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
    if (!isAccessibilityEnabled || !isOverlayEnabled) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E151B)),
            border = BorderStroke(1.dp, Color(0xFFDC2626)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Protection Inactive",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFEF2F2)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (!isAccessibilityEnabled && !isOverlayEnabled)
                        "Accessibility Service and Display Over Other Apps permissions are required to detect foreground distractions and show the block screen."
                    else if (!isAccessibilityEnabled)
                        "Accessibility Service is disabled. BlockIt cannot detect foreground apps or enforce whitelist/blacklist modes without it."
                    else
                        "Display Over Other Apps permission is required to draw the block overlay on top of distractions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFCA5A5)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (!isAccessibilityEnabled) onEnableAccessibility() else onEnableOverlay()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (!isAccessibilityEnabled) "Activate Accessibility Service" else "Grant Overlay Permission")
                }
            }
        }
    } else {
        val anyWhitelistActive = profiles.any { it.isEnabled && it.blockMode == BlockMode.WHITELIST }
        val anyBlacklistActive = profiles.any { it.isEnabled && it.blockMode == BlockMode.BLACKLIST }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D281E)),
            border = BorderStroke(1.dp, Color(0xFF10B981)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Protection Active & Guarding",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFECFDF5)
                    )
                    Text(
                        text = when {
                            anyWhitelistActive -> "Whitelist Kiosk Active - Home screen locked to permitted apps"
                            activeSession != null -> "Quick Focus Session Active"
                            anyBlacklistActive -> "Blacklist Guard Active (${profiles.count { it.isEnabled }} profiles active)"
                            else -> "Standby - Enable a profile or start a quick session"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA7F3D0)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (activeSession != null) Color(0xFF1E293B) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (activeSession != null) {
                // Active Countdown View
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
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = Color(0xFF818CF8)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Focus Session Active",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFF1F5F9)
                        )
                        Text(
                            text = String.format("%02d:%02d remaining", mins, secs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Button(
                        onClick = { onEndSession(activeSession.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("End")
                    }
                }
            } else {
                // Inactive Session Starter View
                Text(
                    text = "Quick Focus Session",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Block all distracting apps immediately:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                val targetProfileId = profiles.firstOrNull()?.id ?: 1L
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 25, 45, 60).forEach { mins ->
                        Surface(
                            onClick = { onStartSession(targetProfileId, mins) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$mins",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = Color(0xFF818CF8)
                                )
                                Text(
                                    text = "min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (profile.blockMode == BlockMode.WHITELIST) Color(0xFF4338CA).copy(alpha = 0.35f) else Color(0xFF0369A1).copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = if (profile.blockMode == BlockMode.WHITELIST) "Whitelist" else "Blacklist",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (profile.blockMode == BlockMode.WHITELIST) Color(0xFF818CF8) else Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (profile.blockMode == BlockMode.WHITELIST)
                        "${profile.appCount} allowed apps"
                    else
                        "${profile.appCount} apps, ${profile.domainCount} websites",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = profile.isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun CreateProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, BlockMode) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(BlockMode.BLACKLIST) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Focus Profile") },
        text = {
            Column {
                Text(
                    "Choose a profile type and name:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text(if (selectedMode == BlockMode.BLACKLIST) "e.g. Social Media Lock" else "e.g. Study Kiosk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Mode:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { selectedMode = BlockMode.BLACKLIST },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedMode == BlockMode.BLACKLIST) Color(0xFF0369A1).copy(alpha = 0.35f) else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (selectedMode == BlockMode.BLACKLIST) Color(0xFF38BDF8) else Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "Blacklist",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedMode == BlockMode.BLACKLIST) Color(0xFF38BDF8) else Color(0xFFF1F5F9)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Block specific apps & sites",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    Surface(
                        onClick = { selectedMode = BlockMode.WHITELIST },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedMode == BlockMode.WHITELIST) Color(0xFF4338CA).copy(alpha = 0.35f) else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (selectedMode == BlockMode.WHITELIST) Color(0xFF818CF8) else Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "Whitelist",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedMode == BlockMode.WHITELIST) Color(0xFF818CF8) else Color(0xFFF1F5F9)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Lock down phone to allowed apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selectedMode) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
