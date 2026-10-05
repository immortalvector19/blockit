package com.blockit.ui.screens.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockit.core.BlockItApplication
import com.blockit.core.health.BlockHealthMonitor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val healthSnapshot by BlockHealthMonitor.snapshot.collectAsState()
    val appContainer = remember { BlockItApplication.from(context) }

    // Live permission states
    var isAccessibilityRunning by remember { mutableStateOf(false) }
    var isOverlayGranted by remember { mutableStateOf(false) }
    var isBatteryExempt by remember { mutableStateOf(false) }

    fun refreshLiveStates() {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val expected = "${context.packageName}/com.blockit.service.accessibility.BlockAccessibilityService"
        val systemAccessibilityEnabled = enabledServices.contains(expected)
        isAccessibilityRunning = systemAccessibilityEnabled && healthSnapshot.isAccessibilityConnected

        isOverlayGranted = Settings.canDrawOverlays(context)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        isBatteryExempt = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false

        BlockHealthMonitor.updatePermissions(isOverlayGranted, isBatteryExempt)
    }

    // Re-check all states on every onResume
    DisposableEffect(lifecycleOwner) {
        refreshLiveStates()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshLiveStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Engine Health & Diagnostics", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refreshLiveStates() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Status Banner
            HealthSummaryBanner(
                isAllHealthy = isAccessibilityRunning && isOverlayGranted && isBatteryExempt
            )

            // Diagnostic Trigger: Test Block Now
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF3DDC97),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Real-Time Block Verification",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Trigger an immediate test block overlay on this screen to verify WindowManager attachment, touch handling, and dismiss behavior without switching apps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val trigger = BlockHealthMonitor.testBlockTrigger
                            if (trigger != null) {
                                trigger.invoke()
                            } else {
                                appContainer.overlayController.showBlockOverlay(
                                    targetIdentifier = "Diagnostics Test",
                                    profileName = "FocusShield Diagnostics",
                                    reason = "Manual test block triggered from Health Screen",
                                    onHomeClicked = {
                                        appContainer.overlayController.hideBlockOverlay()
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC97)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Block Now", color = Color(0xFF0E1621), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 1. Accessibility Service Live State
            val lastEventText = if (healthSnapshot.lastEventTimestamp > 0L) {
                val diffSecs = (System.currentTimeMillis() - healthSnapshot.lastEventTimestamp) / 1000
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(healthSnapshot.lastEventTimestamp))
                "$timeStr (${diffSecs}s ago)"
            } else {
                "No events recorded yet"
            }

            HealthItemCard(
                icon = Icons.Default.AccessibilityNew,
                title = "Accessibility Engine",
                isHealthy = isAccessibilityRunning,
                statusText = if (isAccessibilityRunning) "Connected & Listening" else "Disconnected / Action Required",
                details = listOf(
                    "Service Process" to if (isAccessibilityRunning) "Active in Background" else "Inactive",
                    "Last Event Timestamp" to lastEventText,
                    "Last Package Seen" to if (healthSnapshot.lastPackageSeen.isNotBlank()) healthSnapshot.lastPackageSeen else "None yet"
                ),
                actionText = if (!isAccessibilityRunning) "Open Accessibility Settings" else null,
                onAction = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
            )

            // 2. WindowManager Overlay Permission
            HealthItemCard(
                icon = Icons.Default.Layers,
                title = "Display Over Other Apps (Overlay)",
                isHealthy = isOverlayGranted,
                statusText = if (isOverlayGranted) "Granted (SYSTEM_ALERT_WINDOW)" else "Missing Permission",
                details = listOf(
                    "Window Type" to "TYPE_APPLICATION_OVERLAY",
                    "Render Engine" to "Hardware-Accelerated Jetpack Compose"
                ),
                actionText = if (!isOverlayGranted) "Grant Overlay Permission" else null,
                onAction = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.fromParts("package", context.packageName, null)
                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(intent)
                }
            )

            // 3. Battery Optimization Exemption
            HealthItemCard(
                icon = Icons.Default.BatteryChargingFull,
                title = "Battery Optimization Exemption",
                isHealthy = isBatteryExempt,
                statusText = if (isBatteryExempt) "Exempted (Unrestricted)" else "Optimized (Risk of OS Termination)",
                details = listOf(
                    "System API" to "PowerManager.isIgnoringBatteryOptimizations()",
                    "Status" to if (isBatteryExempt) "Background activity protected" else "May be killed in deep sleep"
                ),
                actionText = if (!isBatteryExempt) "Exempt FocusShield" else null,
                onAction = {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(fallback)
                    }
                }
            )

            // 4. Active Rules & Loaded Blocklist Telemetry
            val currentSnapshot = appContainer.ruleEvaluator.getSnapshot()
            val totalLoaded = if (currentSnapshot.hasActiveWhitelist) {
                currentSnapshot.whitelistPackages.size
            } else {
                currentSnapshot.blockedPackages.size
            }

            HealthItemCard(
                icon = Icons.Default.Shield,
                title = "Active Rules & Blocklist Telemetry",
                isHealthy = totalLoaded > 0 || currentSnapshot.hasActiveWhitelist,
                statusText = if (currentSnapshot.hasActiveWhitelist) {
                    "Whitelist Active (${currentSnapshot.whitelistProfileName ?: "Kiosk"})"
                } else if (currentSnapshot.blockedPackages.isNotEmpty()) {
                    "Blacklist Active (${currentSnapshot.blockedPackages.size} rules)"
                } else {
                    "Idle (No rules active)"
                },
                details = listOf(
                    "Loaded Blocklist Count" to "$totalLoaded targets",
                    "Mode" to if (currentSnapshot.hasActiveWhitelist) "Whitelist (Kiosk mode)" else "Blacklist (App blocking)",
                    "Blocked Domains" to "${currentSnapshot.blockedDomains.size} domains"
                ),
                actionText = null,
                onAction = null
            )
        }
    }
}

@Composable
private fun HealthSummaryBanner(isAllHealthy: Boolean) {
    val bgColor = if (isAllHealthy) Color(0xFF132F20) else Color(0xFF332014)
    val borderColor = if (isAllHealthy) Color(0xFF3DDC97) else Color(0xFFFFB74D)
    val iconColor = if (isAllHealthy) Color(0xFF3DDC97) else Color(0xFFFFB74D)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isAllHealthy) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = if (isAllHealthy) "All Core Engines Healthy" else "Attention Required",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isAllHealthy)
                        "Foreground detection, WindowManager overlay, and battery survival are verified."
                    else
                        "One or more essential background components are inactive or unexempted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCCCCCC)
                )
            }
        }
    }
}

@Composable
private fun HealthItemCard(
    icon: ImageVector,
    title: String,
    isHealthy: Boolean,
    statusText: String,
    details: List<Pair<String, String>>,
    actionText: String?,
    onAction: (() -> Unit)?
) {
    val statusColor = if (isHealthy) Color(0xFF3DDC97) else Color(0xFFFF5252)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                details.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(actionText)
                }
            }
        }
    }
}
