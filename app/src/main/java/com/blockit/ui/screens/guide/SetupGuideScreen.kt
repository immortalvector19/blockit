package com.blockit.ui.screens.guide

import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.QueryBuilder
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockit.service.admin.BlockItDeviceAdminReceiver
import com.blockit.ui.components.FocusButton
import com.blockit.ui.components.FocusSectionHeader
import com.blockit.ui.components.FocusTopBar
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography

@Composable
fun SetupGuideScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = FocusTheme.colors

    var isAccessibilityGranted by remember { mutableStateOf(checkAccessibility(context)) }
    var isOverlayGranted by remember { mutableStateOf(checkOverlay(context)) }
    var isVpnGranted by remember { mutableStateOf(checkVpn(context)) }
    var isUsageAccessGranted by remember { mutableStateOf(checkUsageAccess(context)) }
    var isBatteryOptExempt by remember { mutableStateOf(checkBatteryOptimization(context)) }
    var isExactAlarmGranted by remember { mutableStateOf(checkExactAlarm(context)) }
    var isDeviceAdminGranted by remember { mutableStateOf(checkDeviceAdmin(context)) }

    fun refreshPermissions() {
        isAccessibilityGranted = checkAccessibility(context)
        isOverlayGranted = checkOverlay(context)
        isVpnGranted = checkVpn(context)
        isUsageAccessGranted = checkUsageAccess(context)
        isBatteryOptExempt = checkBatteryOptimization(context)
        isExactAlarmGranted = checkExactAlarm(context)
        isDeviceAdminGranted = checkDeviceAdmin(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refreshPermissions()
    }

    Scaffold(
        modifier = modifier,
        containerColor = colors.background,
        topBar = {
            FocusTopBar(
                title = "Setup guide",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(FocusSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FocusSpacing.lg)
        ) {
            FocusSectionHeader(title = "Required permissions")

            Text(
                text = "FocusShield runs completely on-device without internet access. Granting these system permissions ensures reliable blocking without battery drain.",
                style = FocusTypography.body,
                color = colors.textSecondary
            )

            // 1. Accessibility Service
            PermissionCard(
                title = "Accessibility service",
                description = "Detects app switches instantly and reads browser address bars to intercept blocked domains.",
                isGranted = isAccessibilityGranted,
                icon = Icons.Rounded.AccessibilityNew,
                buttonText = "Open accessibility settings",
                onConfigure = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )

            // 2. Overlay
            PermissionCard(
                title = "Display over other apps",
                description = "Shows the full-screen pause view over blocked apps without delay.",
                isGranted = isOverlayGranted,
                icon = Icons.Rounded.Layers,
                buttonText = "Grant overlay permission",
                onConfigure = {
                    com.blockit.core.permission.PermissionIntents.launchOverlaySettings(context)
                }
            )

            // 3. Local VPN Filter
            PermissionCard(
                title = "On-device DNS filter",
                description = "Filters DNS requests locally on your device to block domains across all apps and browsers. Zero traffic leaves the phone.",
                isGranted = isVpnGranted,
                icon = Icons.Rounded.VpnKey,
                buttonText = "Authorize local VPN",
                onConfigure = {
                    val intent = VpnService.prepare(context)
                    if (intent != null) {
                        vpnLauncher.launch(intent)
                    } else {
                        refreshPermissions()
                    }
                }
            )

            // 4. Usage Access
            PermissionCard(
                title = "Usage access",
                description = "Secondary fallback mechanism to detect foreground apps if accessibility is temporarily interrupted.",
                isGranted = isUsageAccessGranted,
                icon = Icons.Rounded.QueryBuilder,
                buttonText = "Grant usage access",
                onConfigure = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )

            // 5. Battery Optimization Exemption
            PermissionCard(
                title = "Battery optimization exemption",
                description = "Prevents Android battery management from stopping FocusShield when the screen is locked.",
                isGranted = isBatteryOptExempt,
                icon = Icons.Rounded.BatteryChargingFull,
                buttonText = "Disable battery optimization",
                onConfigure = {
                    com.blockit.core.permission.PermissionIntents.launchBatteryExemption(context)
                }
            )

            // 6. Exact Alarms
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PermissionCard(
                    title = "Exact alarms",
                    description = "Ensures scheduled routines trigger at the exact second without system drift.",
                    isGranted = isExactAlarmGranted,
                    icon = Icons.Rounded.Alarm,
                    buttonText = "Allow exact alarms",
                    onConfigure = {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // 7. Device Admin
            PermissionCard(
                title = "Device admin (optional)",
                description = "Prevents uninstallation while a focus session is active.",
                isGranted = isDeviceAdminGranted,
                icon = Icons.Rounded.Security,
                buttonText = "Activate device admin",
                onConfigure = {
                    val adminComponent = ComponentName(context, BlockItDeviceAdminReceiver::class.java)
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                        putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Enable device admin to prevent uninstalling FocusShield during active focus sessions."
                        )
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )

            FocusSectionHeader(title = "Manufacturer background settings")

            Text(
                text = "Aggressive battery savers on Samsung, Xiaomi, or OnePlus devices may terminate background services. Follow the steps for your device:",
                style = FocusTypography.body,
                color = colors.textSecondary
            )

            OemGuideCard(
                brand = "Samsung (One UI)",
                steps = listOf(
                    "Settings > Apps > FocusShield > Battery -> Select 'Unrestricted'",
                    "Settings > Battery > Background usage limits > 'Never auto sleeping apps' -> Add FocusShield",
                    "Settings > Battery > More battery settings -> Turn off 'Put unused apps to sleep'"
                )
            )

            OemGuideCard(
                brand = "Xiaomi / Redmi / POCO (HyperOS / MIUI)",
                steps = listOf(
                    "Long-press FocusShield icon > App Info > Enable 'Autostart'",
                    "In App Info > Battery saver -> Select 'No restrictions'",
                    "Security app > Manage apps > Permissions > Autostart -> Enable FocusShield",
                    "Open Recent Apps screen, pull down on FocusShield, and tap the Lock icon"
                )
            )

            OemGuideCard(
                brand = "OnePlus / OPPO / Realme",
                steps = listOf(
                    "Settings > Apps > App management > FocusShield > Battery usage",
                    "Enable both 'Allow background activity' and 'Allow auto-launch'"
                )
            )

            OemGuideCard(
                brand = "Google Pixel & stock Android",
                steps = listOf(
                    "Settings > Apps > FocusShield > App battery usage -> Select 'Unrestricted'"
                )
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector,
    buttonText: String,
    onConfigure: () -> Unit
) {
    val colors = FocusTheme.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.divider, FocusShapes.medium),
        shape = FocusShapes.medium,
        color = colors.surface
    ) {
        Column(modifier = Modifier.padding(FocusSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) colors.accent else colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(FocusSpacing.sm))
                Text(
                    text = title,
                    style = FocusTypography.bodyBold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isGranted) colors.accent else colors.textMuted.copy(alpha = 0.3f),
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Text(
                        text = if (isGranted) "Granted" else "Needs setup",
                        style = FocusTypography.caption,
                        color = if (isGranted) colors.accent else colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(FocusSpacing.xs))

            Text(
                text = description,
                style = FocusTypography.caption,
                color = colors.textSecondary
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(FocusSpacing.sm))
                FocusButton(
                    text = buttonText,
                    onClick = onConfigure,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun OemGuideCard(
    brand: String,
    steps: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = FocusTheme.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.divider, FocusShapes.medium)
            .clickable { expanded = !expanded },
        shape = FocusShapes.medium,
        color = colors.surface
    ) {
        Column(modifier = Modifier.padding(FocusSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = brand,
                    style = FocusTypography.bodyBold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = FocusSpacing.sm)) {
                    steps.forEachIndexed { index, step ->
                        Text(
                            text = "${index + 1}. $step",
                            style = FocusTypography.caption,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun checkAccessibility(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val expected = "${context.packageName}/com.blockit.service.accessibility.BlockAccessibilityService"
    return enabledServices.contains(expected)
}

private fun checkOverlay(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

private fun checkVpn(context: Context): Boolean {
    return VpnService.prepare(context) == null
}

private fun checkUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun checkBatteryOptimization(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun checkExactAlarm(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.canScheduleExactAlarms()
    } else {
        true
    }
}

private fun checkDeviceAdmin(context: Context): Boolean {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val adminComponent = ComponentName(context, BlockItDeviceAdminReceiver::class.java)
    return dpm.isAdminActive(adminComponent)
}
