package com.blockit.ui.screens.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockit.core.BlockItApplication
import com.blockit.core.health.BlockHealthMonitor
import com.blockit.ui.components.FocusButton
import com.blockit.ui.components.FocusOutlinedButton
import com.blockit.ui.components.FocusTopBar
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HealthScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = FocusTheme.colors

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
        isBatteryExempt = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshLiveStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        refreshLiveStates()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            FocusTopBar(
                title = "System health",
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { refreshLiveStates() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = colors.textPrimary
                        )
                    }
                }
            )
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FocusSpacing.lg, vertical = FocusSpacing.md),
            verticalArrangement = Arrangement.spacedBy(FocusSpacing.md)
        ) {
            // Live Status Banner
            HealthSummaryBanner(
                isAllHealthy = isAccessibilityRunning && isOverlayGranted && isBatteryExempt
            )

            // Diagnostic Trigger: Test Block Now
            Card(
                shape = FocusShapes.medium,
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(FocusSpacing.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(FocusSpacing.xs))
                        Text(
                            text = "Block screen verification",
                            style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(FocusSpacing.xs))
                    Text(
                        text = "Trigger an immediate test block screen to verify WindowManager attachment and touch handling without switching apps.",
                        style = FocusTypography.caption,
                        color = colors.textMuted
                    )
                    Spacer(modifier = Modifier.height(FocusSpacing.md))
                    FocusButton(
                        text = "Test block screen",
                        onClick = {
                            val trigger = BlockHealthMonitor.testBlockTrigger
                            if (trigger != null) {
                                trigger.invoke()
                            } else {
                                appContainer.overlayController.showBlockOverlay(
                                    targetIdentifier = "Diagnostics test",
                                    profileName = "FocusShield",
                                    reason = "Manual test block triggered from health screen",
                                    onHomeClicked = {
                                        appContainer.overlayController.hideBlockOverlay()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                title = "Accessibility service",
                isHealthy = isAccessibilityRunning,
                statusText = if (isAccessibilityRunning) "Connected" else "Needs setup",
                details = listOf(
                    "Service state" to if (isAccessibilityRunning) "Active" else "Inactive",
                    "Last event" to lastEventText,
                    "Last app detected" to if (healthSnapshot.lastPackageSeen.isNotBlank()) healthSnapshot.lastPackageSeen else "None yet"
                ),
                actionText = if (!isAccessibilityRunning) "Open accessibility settings" else null,
                onAction = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
            )

            // 2. WindowManager Overlay Permission
            HealthItemCard(
                icon = Icons.Default.Layers,
                title = "Display over other apps",
                isHealthy = isOverlayGranted,
                statusText = if (isOverlayGranted) "Granted" else "Needs setup",
                details = listOf(
                    "Overlay permission" to if (isOverlayGranted) "Granted" else "Missing"
                ),
                actionText = if (!isOverlayGranted) "Grant overlay permission" else null,
                onAction = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                }
            )

            // 3. Battery Optimization Exemption
            HealthItemCard(
                icon = Icons.Default.BatteryChargingFull,
                title = "Battery optimization",
                isHealthy = isBatteryExempt,
                statusText = if (isBatteryExempt) "Exempt" else "Needs setup",
                details = listOf(
                    "Standby exemption" to if (isBatteryExempt) "Unrestricted" else "Optimized (risk of background kill)"
                ),
                actionText = if (!isBatteryExempt) "Request battery exemption" else null,
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun HealthSummaryBanner(isAllHealthy: Boolean) {
    val colors = FocusTheme.colors
    Card(
        shape = FocusShapes.medium,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(
            1.dp,
            if (isAllHealthy) colors.statusActive.copy(alpha = 0.3f) else colors.statusWarning.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FocusSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAllHealthy) colors.statusActive.copy(alpha = 0.12f)
                        else colors.statusWarning.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAllHealthy) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (isAllHealthy) colors.statusActive else colors.statusWarning,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(FocusSpacing.md))
            Column {
                Text(
                    text = if (isAllHealthy) "All engines active" else "Setup required",
                    style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isAllHealthy)
                        "FocusShield is fully configured and enforcing rules."
                    else
                        "One or more required permissions need configuration below.",
                    style = FocusTypography.caption,
                    color = colors.textMuted
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
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = FocusTheme.colors
    Card(
        shape = FocusShapes.medium,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(FocusSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(FocusSpacing.xs))
                    Text(
                        text = title,
                        style = FocusTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary
                    )
                }

                Surface(
                    shape = FocusShapes.small,
                    color = if (isHealthy) colors.statusActive.copy(alpha = 0.12f) else colors.statusDestructive.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statusText,
                        color = if (isHealthy) colors.statusActive else colors.statusDestructive,
                        style = FocusTypography.caption.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(FocusSpacing.sm))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                details.forEach { (key, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = key,
                            style = FocusTypography.caption,
                            color = colors.textMuted
                        )
                        Text(
                            text = value,
                            style = FocusTypography.caption.copy(fontWeight = FontWeight.Medium),
                            color = colors.textPrimary
                        )
                    }
                }
            }

            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(FocusSpacing.md))
                FocusOutlinedButton(
                    text = actionText,
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
