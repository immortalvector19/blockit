package com.blockit.core.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager

interface SystemAppResolver {
    fun isAlwaysPermitted(packageName: String): Boolean
    fun getAlwaysPermittedPackages(): Set<String>
}

/**
 * Dynamically queries Android system services to resolve the active home launcher,
 * default phone dialer, enabled keyboards (IMEs), system UI, permission controllers,
 * and this application. None of these packages should ever be blocked or trapped by overlays.
 */
class AndroidSystemAppResolver(
    private val context: Context
) : SystemAppResolver {

    override fun getAlwaysPermittedPackages(): Set<String> {
        val result = mutableSetOf<String>()

        // 1. This application itself
        result.add(context.packageName)

        // 2. Android framework & SystemUI
        result.add("android")
        result.add("com.android.systemui")

        // 3. Dynamic Home Launcher resolution
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val launchers = context.packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolve in launchers) {
                resolve.activityInfo?.packageName?.let { result.add(it) }
            }
            context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName?.let {
                result.add(it)
            }
        } catch (_: Exception) {}

        // 4. Dynamic Phone Dialer resolution
        try {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecom?.defaultDialerPackage?.let { result.add(it) }

            val dialIntent = Intent(Intent.ACTION_DIAL)
            val dialers = context.packageManager.queryIntentActivities(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolve in dialers) {
                resolve.activityInfo?.packageName?.let { result.add(it) }
            }
        } catch (_: Exception) {}

        // 5. Dynamic Active Keyboards (IMEs) resolution
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.enabledInputMethodList?.forEach { imi ->
                result.add(imi.packageName)
            }
        } catch (_: Exception) {}

        // 6. Permission Controller & Package Installers
        try {
            val permIntent = Intent("android.content.pm.action.REQUEST_PERMISSIONS")
            context.packageManager.resolveActivity(permIntent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName?.let {
                result.add(it)
            }
        } catch (_: Exception) {}
        result.add("com.android.permissioncontroller")
        result.add("com.google.android.permissioncontroller")
        result.add("com.android.packageinstaller")
        result.add("com.google.android.packageinstaller")

        return result
    }

    override fun isAlwaysPermitted(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        if (packageName == context.packageName) return true

        val dynamicPermitted = getAlwaysPermittedPackages()
        if (dynamicPermitted.contains(packageName)) return true

        // Fallback safety heuristic for standard Android system namespaces
        return packageName == "android" ||
                packageName == "com.android.systemui" ||
                packageName == "com.android.permissioncontroller" ||
                packageName == "com.google.android.permissioncontroller" ||
                packageName == "com.google.android.packageinstaller" ||
                packageName == "com.android.packageinstaller" ||
                packageName.contains("inputmethod", ignoreCase = true) ||
                packageName.contains("keyboard", ignoreCase = true)
    }
}
