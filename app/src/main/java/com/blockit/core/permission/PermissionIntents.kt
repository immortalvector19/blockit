package com.blockit.core.permission

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object PermissionIntents {
    private const val TAG = "PermissionIntents"

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val expected = "${context.packageName}/com.blockit.service.accessibility.BlockAccessibilityService"
        return enabledServices.contains(expected)
    }

    fun isOverlayGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun canDrawOverlays(context: Context): Boolean = isOverlayGranted(context)

    fun isBatteryExempt(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean = isBatteryExempt(context)

    fun openAccessibilitySettings(context: Context) = launchAccessibilitySettings(context)
    fun openOverlaySettings(context: Context) = launchOverlaySettings(context)
    fun requestBatteryExemption(context: Context) = launchBatteryExemption(context)

    fun launchOverlaySettings(context: Context) {
        val packageUri = Uri.fromParts("package", context.packageName, null)
        val primaryIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(primaryIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Direct overlay intent failed, falling back to application details", e)
            try {
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "All overlay permission intents failed", fallbackEx)
            }
        }
    }

    fun launchBatteryExemption(context: Context) {
        val packageUri = Uri.fromParts("package", context.packageName, null)

        // 1. Direct system prompt dialog (declared in Manifest)
        try {
            val directIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = packageUri
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(directIntent)
            return
        } catch (e: Exception) {
            Log.w(TAG, "Direct ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS failed, trying list", e)
        }

        // 2. Battery Optimization Settings List
        try {
            val listIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(listIntent)
            return
        } catch (e: Exception) {
            Log.w(TAG, "ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS failed, trying OEM pages", e)
        }

        // 3. OEM Specific autostart / power manager intents
        val oemIntents = listOf(
            // Xiaomi / MIUI
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            Intent().setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")),
            // Huawei
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),
            // Samsung
            Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")),
            Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.battery.ui.BatteryActivity")),
            // Oppo / ColorOS
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgauge.PowerUsageModelActivity")),
            // Vivo
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"))
        )

        for (oemIntent in oemIntents) {
            try {
                oemIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(oemIntent)
                return
            } catch (_: Exception) {}
        }

        // 4. Final Fallback: App Details Settings
        try {
            val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(detailsIntent)
        } catch (e: Exception) {
            Log.e(TAG, "All battery exemption intents failed", e)
        }
    }

    fun launchAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch accessibility settings", e)
        }
    }
}
