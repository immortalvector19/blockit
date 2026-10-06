package com.blockit.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.blockit.core.BlockItApplication
import com.blockit.data.local.db.entity.BlockEventEntity
import com.blockit.domain.evaluator.RuleEvaluator
import com.blockit.domain.model.TargetType
import com.blockit.service.overlay.OverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BlockAccessibilityService : AccessibilityService() {

    private lateinit var ruleEvaluator: RuleEvaluator
    private lateinit var overlayController: OverlayController
    private lateinit var appScope: CoroutineScope
    private lateinit var intentPassManager: com.blockit.core.intentgate.IntentPassManager
    private lateinit var systemAppResolver: com.blockit.core.system.SystemAppResolver
    private var homePackages: Set<String> = emptySet()
    private val lastLoggedBlockTime = java.util.concurrent.ConcurrentHashMap<String, Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        val container = BlockItApplication.from(this)
        ruleEvaluator = container.ruleEvaluator
        overlayController = OverlayController(this)
        appScope = container.applicationScope
        intentPassManager = container.intentPassManager
        systemAppResolver = container.systemAppResolver

        com.blockit.core.health.BlockHealthMonitor.updateAccessibilityStatus(true)
        com.blockit.core.health.BlockHealthMonitor.testBlockTrigger = {
            overlayController.showBlockOverlay(
                targetIdentifier = "Diagnostic Test",
                profileName = "FocusShield Diagnostics",
                reason = "Testing WindowManager overlay rendering & input dispatch",
                onHomeClicked = {
                    overlayController.hideBlockOverlay()
                }
            )
        }

        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 50
        }

        resolveHomePackages()

        // Active Session Watchdog: polls foreground package every 1s during active blocks to prevent bypass
        appScope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(1000)
                val snapshot = ruleEvaluator.getSnapshot()
                val hasActiveRules = snapshot.blockedPackages.isNotEmpty() || snapshot.hasActiveWhitelist
                if (hasActiveRules && !overlayController.isOverlayShowing()) {
                    val root = rootInActiveWindow
                    val fgPkg = root?.packageName?.toString() ?: ""
                    if (fgPkg.isNotBlank() && !isHomePackage(fgPkg) && fgPkg != packageName) {
                        checkAndBlockForegroundApp(fgPkg)
                    }
                }
            }
        }

        // Reactively observe snapshot changes
        appScope.launch(Dispatchers.Main) {
            ruleEvaluator.snapshotFlow.collect { snapshot ->
                if (!snapshot.hasActiveWhitelist && overlayController.isOverlayShowing()) {
                    val root = rootInActiveWindow
                    val pkg = root?.packageName?.toString() ?: ""
                    if (isHomePackage(pkg)) {
                        overlayController.hideBlockOverlay()
                    }
                }
            }
        }
    }

    private fun resolveHomePackages() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfos = packageManager.queryIntentActivities(homeIntent, 0)
            val pkgs = resolveInfos.mapNotNull { it.activityInfo?.packageName }.toMutableSet()
            packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName?.let {
                pkgs.add(it)
            }
            homePackages = pkgs
        } catch (_: Exception) {}
    }

    private fun isHomePackage(pkgName: String): Boolean {
        if (pkgName.isBlank()) return false
        return homePackages.contains(pkgName) ||
                pkgName.contains("launcher", ignoreCase = true) ||
                pkgName == "com.miui.home" ||
                pkgName == "com.google.android.apps.nexuslauncher" ||
                pkgName == "com.android.launcher3" ||
                pkgName == "com.sec.android.app.launcher" ||
                pkgName == "com.huawei.android.launcher" ||
                pkgName == "com.oppo.launcher" ||
                pkgName == "com.coloros.launcher" ||
                pkgName == "com.oneplus.launcher"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: return
        com.blockit.core.health.BlockHealthMonitor.recordAccessibilityEvent(pkgName)
        val snapshot = ruleEvaluator.getSnapshot()

        // Ignore events from our own app, UNLESS the user opened the main settings screen
        if (pkgName == packageName) {
            val cls = event.className?.toString() ?: ""
            if (cls.contains("MainActivity")) {
                if (overlayController.isOverlayShowing()) {
                    overlayController.hideBlockOverlay()
                }
            }
            return
        }

        // Ignore essential system framework and UI components dynamically
        if (::systemAppResolver.isInitialized && systemAppResolver.isAlwaysPermitted(pkgName)) {
            return
        }

        // For app blocking, only react to window state / window transitions, not rapid scroll redraws
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
            !BrowserUrlDetector.SUPPORTED_BROWSERS.contains(pkgName)
        ) {
            return
        }

        if (intentPassManager.hasActivePass(pkgName)) {
            if (overlayController.isOverlayShowing()) {
                overlayController.hideBlockOverlay()
            }
            return
        }

        if (homePackages.isEmpty()) {
            resolveHomePackages()
        }
        val isHome = isHomePackage(pkgName)

        // If user is on the Home Launcher:
        if (isHome) {
            if (snapshot.hasActiveWhitelist) {
                // Whitelist mode: overlay the home screen and show permitted apps
                overlayController.showBlockOverlay(
                    targetIdentifier = "Home Screen",
                    profileName = snapshot.whitelistProfileName ?: "Whitelist",
                    reason = "Home screen covered during Whitelist Focus Mode",
                    isWhitelistBlock = true,
                    allowedApps = snapshot.whitelistPackages.toList(),
                    onHomeClicked = {},
                    onOpenAllowedApp = { appPkg ->
                        overlayController.hideBlockOverlay()
                        val launchIntent = packageManager.getLaunchIntentForPackage(appPkg)
                        launchIntent?.let {
                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                            startActivity(it)
                        }
                    }
                )
            } else {
                // Blacklist mode: normal phone usage on home screen
                if (overlayController.isOverlayShowing()) {
                    overlayController.hideBlockOverlay()
                }
            }
            return
        }

        // Strict Mode: Guard Android Settings against deactivating or uninstalling FocusShield
        if (pkgName == "com.android.settings") {
            val hasActiveBlocks = snapshot.blockedPackages.isNotEmpty() || snapshot.blockedDomains.isNotEmpty() || snapshot.hasActiveWhitelist
            if (hasActiveBlocks) {
                val root = rootInActiveWindow ?: event.source
                if (root != null && isSettingsTamperingAttempt(root)) {
                    overlayController.showBlockOverlay(
                        targetIdentifier = "Android Settings",
                        profileName = "Strict Mode",
                        reason = "Settings access guarded during active focus rules",
                        onHomeClicked = { performGlobalAction(GLOBAL_ACTION_HOME) }
                    )
                    return
                }
            }
        }

        // Evaluate if the foreground app is blocked
        if (checkAndBlockForegroundApp(pkgName)) {
            return
        } else {
            // An allowed user app has come to the foreground.
            // Dismiss the overlay only if Whitelist is NOT active, or if this app is an explicitly permitted whitelisted app.
            if (!snapshot.hasActiveWhitelist || snapshot.whitelistPackages.contains(pkgName)) {
                if (overlayController.isOverlayShowing()) {
                    overlayController.hideBlockOverlay()
                }
            }
        }

        // Secondary Layer: If the foreground app is a browser, inspect current URL
        if (BrowserUrlDetector.SUPPORTED_BROWSERS.contains(pkgName)) {
            val root = rootInActiveWindow ?: event.source
            if (root != null) {
                val detectedDomain = BrowserUrlDetector.extractUrlFromBrowser(root, pkgName)
                if (detectedDomain != null) {
                    val domainEval = ruleEvaluator.isDomainBlocked(detectedDomain)
                    if (domainEval.isBlocked) {
                        val profile = domainEval.profileName ?: "Focus"
                        val reason = domainEval.reason ?: "Website blocked"

                        overlayController.showBlockOverlay(
                            targetIdentifier = detectedDomain,
                            profileName = profile,
                            reason = reason,
                            onHomeClicked = {
                                performGlobalAction(GLOBAL_ACTION_HOME)
                            }
                        )

                        logBlockEvent(detectedDomain, TargetType.WEBSITE, profile)
                    }
                }
            }
        }
    }

    private fun checkAndBlockForegroundApp(pkgName: String): Boolean {
        if (intentPassManager.hasActivePass(pkgName)) return false
        val result = ruleEvaluator.isAppBlocked(pkgName)
        if (result.isBlocked) {
            val profile = result.profileName ?: "Focus"
            val reason = result.reason ?: "App blocked"

            overlayController.showBlockOverlay(
                targetIdentifier = pkgName,
                profileName = profile,
                reason = reason,
                isWhitelistBlock = result.isWhitelistBlock,
                allowedApps = result.allowedApps,
                isHardLock = false,
                canRequestPass = intentPassManager.canRequestPass(pkgName),
                remainingPasses = intentPassManager.getRemainingPassesToday(),
                onHomeClicked = {
                    overlayController.hideBlockOverlay()
                    performGlobalAction(GLOBAL_ACTION_HOME)
                },
                onOpenAllowedApp = { appPkg ->
                    overlayController.hideBlockOverlay()
                    val launchIntent = packageManager.getLaunchIntentForPackage(appPkg)
                    launchIntent?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        startActivity(it)
                    }
                },
                onGrantPass = {
                    intentPassManager.grantPass(pkgName)
                    overlayController.hideBlockOverlay()
                    val launchIntent = packageManager.getLaunchIntentForPackage(pkgName)
                    launchIntent?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        startActivity(it)
                    }
                }
            )

            logBlockEvent(pkgName, TargetType.APP, profile)
            return true
        }
        return false
    }

    private fun logBlockEvent(identifier: String, type: TargetType, profile: String) {
        val now = System.currentTimeMillis()
        val last = lastLoggedBlockTime[identifier] ?: 0L
        if (now - last < 4000L) {
            return
        }
        lastLoggedBlockTime[identifier] = now

        appScope.launch(Dispatchers.IO) {
            try {
                val container = BlockItApplication.from(this@BlockAccessibilityService)
                container.database.blockEventDao().insertEvent(
                    BlockEventEntity(
                        timestamp = now,
                        targetType = type.name,
                        identifier = identifier,
                        profileName = profile
                    )
                )
            } catch (_: Exception) {}
        }
    }

    private fun isSettingsTamperingAttempt(node: android.view.accessibility.AccessibilityNodeInfo, depth: Int = 0): Boolean {
        if (depth > 6) return false
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        if (text?.contains("BlockIt", ignoreCase = true) == true ||
            text?.contains("FocusShield", ignoreCase = true) == true ||
            desc?.contains("BlockIt", ignoreCase = true) == true ||
            desc?.contains("FocusShield", ignoreCase = true) == true
        ) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (isSettingsTamperingAttempt(child, depth + 1)) return true
        }
        return false
    }

    override fun onInterrupt() {
        if (::overlayController.isInitialized) {
            overlayController.hideBlockOverlay()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.blockit.core.health.BlockHealthMonitor.updateAccessibilityStatus(false)
        com.blockit.core.health.BlockHealthMonitor.testBlockTrigger = null
        if (::overlayController.isInitialized) {
            overlayController.hideBlockOverlay()
        }
    }
}
