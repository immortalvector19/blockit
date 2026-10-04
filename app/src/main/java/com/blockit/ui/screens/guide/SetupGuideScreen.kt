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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockit.service.admin.BlockItDeviceAdminReceiver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupGuideScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Live permission states updated on resume
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
        topBar = {
            TopAppBar(
                title = { Text("Permissions & Setup Guide") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Required Permissions",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "BlockIt runs completely on-device without internet access. Because it is sideloaded for personal use, it utilizes Android's most effective system APIs to guarantee reliable blocking.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 1. Accessibility Service
            PermissionCard(
                title = "Accessibility Service",
                description = "Detects foreground app switches instantly and reads browser URL bars to intercept distracting websites.",
                isGranted = isAccessibilityGranted,
                icon = Icons.Default.AccessibilityNew,
                buttonText = "Open Accessibility Settings",
                onConfigure = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )

            // 2. Draw Over Apps (Overlay)
            PermissionCard(
                title = "Display Over Other Apps",
                description = "Renders the calm full-screen blocking overlay over distracting apps without delay.",
                isGranted = isOverlayGranted,
                icon = Icons.Default.Layers,
                buttonText = "Grant Overlay Permission",
                onConfigure = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(intent)
                }
            )

            // 3. Local VPN Filter
            PermissionCard(
                title = "On-Device DNS Filter (VPN)",
                description = "Filters DNS port 53 packets entirely on your device (localhost) to block distracting domains across all apps and browsers. Zero internet traffic is sent to external servers.",
                isGranted = isVpnGranted,
                icon = Icons.Default.VpnKey,
                buttonText = "Authorize Local VPN",
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
                title = "Usage Access (Fallback)",
                description = "Secondary fallback mechanism to detect foreground applications if the accessibility service is ever restarted by the system.",
                isGranted = isUsageAccessGranted,
                icon = Icons.Default.QueryBuilder,
                buttonText = "Grant Usage Access",
                onConfigure = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )

            // 5. Battery Optimization Exemption
            PermissionCard(
                title = "Battery Optimization Exemption",
                description = "Prevents Android Doze mode and OS battery killers from terminating BlockIt when the screen is locked.",
                isGranted = isBatteryOptExempt,
                icon = Icons.Default.BatteryChargingFull,
                buttonText = "Disable Battery Optimization",
                onConfigure = {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}")
                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(intent)
                }
            )

            // 6. Exact Alarms
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PermissionCard(
                    title = "Exact Alarms",
                    description = "Guarantees recurring schedule start and end boundaries activate at the exact second without system drift.",
                    isGranted = isExactAlarmGranted,
                    icon = Icons.Default.Alarm,
                    buttonText = "Allow Exact Alarms",
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
                title = "Device Admin (Optional Anti-Uninstall)",
                description = "Prevents uninstalling BlockIt from the launcher while an active focus session is locked.",
                isGranted = isDeviceAdminGranted,
                icon = Icons.Default.Security,
                buttonText = "Activate Device Admin",
                onConfigure = {
                    val adminComponent = ComponentName(context, BlockItDeviceAdminReceiver::class.java)
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                        putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Enable Device Admin to prevent uninstalling BlockIt during active focus sessions."
                        )
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // OEM Optimization Section
            Text(
                text = "OEM Background Guides",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Aggressive battery managers on Samsung, Xiaomi, and OnePlus may kill background services unless configured properly. Follow the instructions for your device:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OemGuideCard(
                brand = "Samsung (One UI)",
                steps = listOf(
                    "Settings > Apps > BlockIt > Battery -> Select 'Unrestricted'",
                    "Settings > Battery > Background usage limits > 'Never auto sleeping apps' -> Tap '+' and add BlockIt",
                    "Settings > Battery > More battery settings -> Turn OFF 'Put unused apps to sleep'"
                )
            )

            OemGuideCard(
                brand = "Xiaomi / Redmi / POCO (HyperOS / MIUI)",
                steps = listOf(
                    "Long-press BlockIt icon > App Info > Enable 'Autostart'",
                    "In App Info > Battery saver -> Select 'No restrictions'",
                    "Security app > Manage apps > Permissions > Autostart -> Enable BlockIt",
                    "Open Recent Apps screen, pull down on the BlockIt preview, and tap the Lock icon"
                )
            )

            OemGuideCard(
                brand = "OnePlus / OPPO / Realme (OxygenOS / ColorOS)",
                steps = listOf(
                    "Settings > Apps > App management > BlockIt > Battery usage",
                    "Enable both 'Allow background activity' and 'Allow auto-launch'"
                )
            )

            OemGuideCard(
                brand = "Huawei / Honor (EMUI / MagicOS)",
                steps = listOf(
                    "Settings > Battery > App launch > Find BlockIt",
                    "Turn OFF 'Manage automatically' and enable: Auto-launch, Secondary launch, and Run in background"
                )
            )

            OemGuideCard(
                brand = "Google Pixel & Stock Android",
                steps = listOf(
                    "Settings > Apps > BlockIt > App battery usage -> Select 'Unrestricted'"
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = if (isGranted) "Granted" else "Needed",
                    tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onConfigure,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonText)
                }
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = brand,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    steps.forEachIndexed { index, step ->
                        Text(
                            text = "${index + 1}. $step",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

// Helper permission checking functions
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
