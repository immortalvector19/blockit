package com.blockit.core.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager

interface SystemAppResolver {
    fun isAlwaysPermitted(packageName: String, isWhitelistMode: Boolean = false): Boolean
    fun isSystemCritical(packageName: String): Boolean
    fun getAlwaysPermittedPackages(): Set<String>
    fun getHomePackages(): Set<String>
    fun getDefaultDialerPackage(): String?
    fun getUserAlwaysAllowedPackages(): Set<String>
    fun setUserAlwaysAllowedPackages(packages: Set<String>)
}

/**
 * Dynamically queries Android system services to resolve the active home launcher,
 * default phone dialer, in-call screen, enabled keyboards (IMEs), system UI,
 * permission controllers, and this application.
 *
 * In Whitelist Mode, the third-party Home Launcher is intentionally NOT always permitted,
 * allowing Focus Home to serve as the lockdown launcher surface.
 */
class AndroidSystemAppResolver(
    private val context: Context
) : SystemAppResolver {

    private val prefs = context.getSharedPreferences("focusshield_always_allowed", Context.MODE_PRIVATE)
    private val keyAlwaysAllowed = "always_allowed_packages"

    override fun getDefaultDialerPackage(): String? {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val dialer = telecom?.defaultDialerPackage
        if (!dialer.isNullOrBlank()) return dialer

        return try {
            val dialIntent = Intent(Intent.ACTION_DIAL)
            val resolve = context.packageManager.resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)
            resolve?.activityInfo?.packageName
        } catch (_: Exception) {
            null
        }
    }

    override fun getUserAlwaysAllowedPackages(): Set<String> {
        val saved = prefs.getStringSet(keyAlwaysAllowed, null)
        if (saved != null) return saved

        // Default: add default dialer package
        val defaults = mutableSetOf<String>()
        getDefaultDialerPackage()?.let { defaults.add(it) }
        return defaults
    }

    override fun setUserAlwaysAllowedPackages(packages: Set<String>) {
        prefs.edit().putStringSet(keyAlwaysAllowed, packages).apply()
    }

    override fun getHomePackages(): Set<String> {
        val result = mutableSetOf<String>()
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
        return result
    }

    override fun isSystemCritical(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        if (packageName == context.packageName) return true

        // 1. Android core framework & SystemUI
        if (packageName == "android" || packageName == "com.android.systemui") return true

        // 2. Default Phone dialer & In-Call UI (calls must always be possible)
        val defaultDialer = getDefaultDialerPackage()
        if (packageName == defaultDialer) return true
        if (packageName == "com.android.incallui" ||
            packageName == "com.google.android.dialer" ||
            packageName == "com.samsung.android.incallui" ||
            packageName == "com.android.phone") {
            return true
        }

        // 3. Active Keyboards (IMEs)
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.enabledInputMethodList?.forEach { imi ->
                if (imi.packageName == packageName) return true
            }
        } catch (_: Exception) {}

        // 4. Permission controllers & package installers
        if (packageName == "com.android.permissioncontroller" ||
            packageName == "com.google.android.permissioncontroller" ||
            packageName == "com.android.packageinstaller" ||
            packageName == "com.google.android.packageinstaller") {
            return true
        }

        // Fallback heuristic for keyboard namespaces
        return packageName.contains("inputmethod", ignoreCase = true) ||
                packageName.contains("keyboard", ignoreCase = true)
    }

    override fun getAlwaysPermittedPackages(): Set<String> {
        val result = mutableSetOf<String>()
        result.add(context.packageName)
        result.add("android")
        result.add("com.android.systemui")
        result.addAll(getHomePackages())
        result.addAll(getUserAlwaysAllowedPackages())

        getDefaultDialerPackage()?.let { result.add(it) }

        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.enabledInputMethodList?.forEach { result.add(it.packageName) }
        } catch (_: Exception) {}

        result.add("com.android.permissioncontroller")
        result.add("com.google.android.permissioncontroller")
        result.add("com.android.packageinstaller")
        result.add("com.google.android.packageinstaller")
        result.add("com.android.incallui")
        result.add("com.android.phone")

        return result
    }

    override fun isAlwaysPermitted(packageName: String, isWhitelistMode: Boolean): Boolean {
        if (packageName.isBlank()) return false
        if (packageName == context.packageName) return true

        // System critical apps (dialer, in-call UI, keyboard, systemUI) are ALWAYS permitted
        if (isSystemCritical(packageName)) return true

        // User's custom always-allowed list is permitted
        if (getUserAlwaysAllowedPackages().contains(packageName)) return true

        // In Whitelist mode, the standard phone launcher is NOT permitted (Focus Home is the home!)
        if (isWhitelistMode) {
            return false
        }

        // In Blacklist mode, normal phone launcher is permitted
        return getHomePackages().contains(packageName)
    }
}
