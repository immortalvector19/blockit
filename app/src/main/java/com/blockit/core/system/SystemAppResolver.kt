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

    @Volatile
    private var cachedDialer: String? = null
    @Volatile
    private var lastDialerCheckTime: Long = 0L

    @Volatile
    private var cachedKeyboards: Set<String> = emptySet()
    @Volatile
    private var lastKeyboardsCheckTime: Long = 0L

    @Volatile
    private var cachedHomePackages: Set<String> = emptySet()
    @Volatile
    private var lastHomeCheckTime: Long = 0L

    @Volatile
    private var cachedUserAllowed: Set<String>? = null

    override fun getDefaultDialerPackage(): String? {
        val now = System.currentTimeMillis()
        if (cachedDialer != null && (now - lastDialerCheckTime < 60_000L)) {
            return cachedDialer
        }
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val dialer = telecom?.defaultDialerPackage
        val resolved = if (!dialer.isNullOrBlank()) {
            dialer
        } else {
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL)
                val resolve = context.packageManager.resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)
                resolve?.activityInfo?.packageName
            } catch (_: Exception) {
                null
            }
        }
        cachedDialer = resolved
        lastDialerCheckTime = now
        return resolved
    }

    override fun getUserAlwaysAllowedPackages(): Set<String> {
        cachedUserAllowed?.let { return it }
        val saved = prefs.getStringSet(keyAlwaysAllowed, null)
        val result = if (saved != null) {
            saved.toSet()
        } else {
            val defaults = mutableSetOf<String>()
            getDefaultDialerPackage()?.let { defaults.add(it) }
            defaults
        }
        cachedUserAllowed = result
        return result
    }

    override fun setUserAlwaysAllowedPackages(packages: Set<String>) {
        cachedUserAllowed = packages.toSet()
        prefs.edit().putStringSet(keyAlwaysAllowed, packages).apply()
    }

    override fun getHomePackages(): Set<String> {
        val now = System.currentTimeMillis()
        if (cachedHomePackages.isNotEmpty() && (now - lastHomeCheckTime < 60_000L)) {
            return cachedHomePackages
        }
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
        cachedHomePackages = result
        lastHomeCheckTime = now
        return result
    }

    private fun getEnabledKeyboardPackages(): Set<String> {
        val now = System.currentTimeMillis()
        if (cachedKeyboards.isNotEmpty() && (now - lastKeyboardsCheckTime < 60_000L)) {
            return cachedKeyboards
        }
        val result = mutableSetOf<String>()
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.enabledInputMethodList?.forEach { imi ->
                result.add(imi.packageName)
            }
        } catch (_: Exception) {}
        cachedKeyboards = result
        lastKeyboardsCheckTime = now
        return result
    }

    override fun isSystemCritical(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        if (packageName == context.packageName ||
            packageName == "com.blockit" ||
            packageName.startsWith("com.blockit.") ||
            packageName == "com.blockit.debug"
        ) return true

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

        // 3. Active Keyboards (IMEs) - fast cached check
        if (getEnabledKeyboardPackages().contains(packageName)) {
            return true
        }

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
        result.add("com.blockit")
        result.add("com.blockit.debug")
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
        if (packageName == context.packageName ||
            packageName == "com.blockit" ||
            packageName.startsWith("com.blockit.") ||
            packageName == "com.blockit.debug"
        ) return true

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
