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
import com.blockit.ui.overlay.BlockOverlayActivity
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
    private lateinit var focusSessionManager: com.blockit.data.repository.FocusSessionManager
    private var homePackages: Set<String> = emptySet()
    private val lastLoggedBlockTime = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private var currentStrictness = com.blockit.domain.model.SessionStrictness.NORMAL

    companion object {
        @Volatile
        var isServiceConnected: Boolean = false
            private set
    }

    @Volatile
    private var currentlyBlockedPackage: String? = null

    @Volatile
    private var lastBlockedTimestamp: Long = 0L

    @Volatile
    private var lastFocusHomeLaunchTime: Long = 0L

    private fun getActiveSessionEndTimestamp(): Long {
        val now = System.currentTimeMillis()
        val snapshot = ruleEvaluator.getSnapshot()
        val sessionEnd = snapshot.activeSessionEndTimestampMs
        if (sessionEnd != null && sessionEnd > now) return sessionEnd
        val schedEnd = snapshot.activeScheduleEndTimestampMs
        if (schedEnd != null && schedEnd > now) return schedEnd
        if (::focusSessionManager.isInitialized) {
            val session = focusSessionManager.getCachedActiveSession()
            if (session != null && session.endTimestamp > now) return session.endTimestamp
        }
        return 0L
    }

    override fun onCreate() {
        super.onCreate()
        com.blockit.core.engine.EngineLifecycleMonitor.recordLifecycleEvent("BlockAccessibilityService.onCreate")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceConnected = true
        com.blockit.core.engine.EngineLifecycleMonitor.updateEngineStatus(
            com.blockit.core.engine.AccessibilityEngineStatus.CONNECTED
        )
        com.blockit.core.engine.EngineLifecycleMonitor.recordLifecycleEvent("BlockAccessibilityService.onServiceConnected")
        val container = BlockItApplication.from(this)
        ruleEvaluator = container.ruleEvaluator
        overlayController = OverlayController(this)
        appScope = container.applicationScope
        intentPassManager = container.intentPassManager
        systemAppResolver = container.systemAppResolver
        focusSessionManager = container.focusSessionManager

        appScope.launch(Dispatchers.Main) {
            container.preferenceManager.strictModeConfig.collect { config ->
                currentStrictness = if (config.isEnabled && config.requirePinToStopSession) {
                    com.blockit.domain.model.SessionStrictness.STRICT
                } else {
                    com.blockit.domain.model.SessionStrictness.NORMAL
                }
                intentPassManager.setMaxPauses(config.maxPausesPerSession)
            }
        }

        com.blockit.core.health.BlockHealthMonitor.updateAccessibilityStatus(true)
        com.blockit.core.health.BlockHealthMonitor.testBlockTrigger = {
            BlockOverlayActivity.start(
                context = this,
                target = "Diagnostic Test",
                profile = "FocusShield Diagnostics",
                reason = "Testing block screen rendering & input dispatch",
                strictness = com.blockit.domain.model.SessionStrictness.NORMAL
            )
        }

        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }

        resolveHomePackages()

        // Active Session Watchdog: polls foreground package every 1s during active blocks to prevent bypass
        appScope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(1000)
                val snapshot = ruleEvaluator.getSnapshot()
                val now = System.currentTimeMillis()
                if (snapshot.hasActiveWhitelist && snapshot.isWhitelistExpired(now)) {
                    ruleEvaluator.refreshRules()
                    continue
                }
                val hasActiveRules = snapshot.blockedPackages.isNotEmpty() || (snapshot.hasActiveWhitelist && !snapshot.isWhitelistExpired(now))
                if (hasActiveRules) {
                    if (com.blockit.core.engine.EngineLifecycleMonitor.isAnyPermittedLaunchInGracePeriod()) {
                        continue
                    }
                    val root = rootInActiveWindow
                    val fgPkg = root?.packageName?.toString() ?: ""
                    if (fgPkg.isNotBlank() && !isHomePackage(fgPkg) && fgPkg != packageName) {
                        if (::systemAppResolver.isInitialized && systemAppResolver.isAlwaysPermitted(fgPkg, isWhitelistMode = snapshot.hasActiveWhitelist)) {
                            continue
                        }
                        if (BlockOverlayActivity.isShowing && BlockOverlayActivity.currentBlockedTarget == fgPkg) {
                            continue
                        }
                        if (intentPassManager.hasActivePass(fgPkg)) {
                            continue
                        }
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
        val cls = event.className?.toString() ?: ""
        val eventTypeStr = try { AccessibilityEvent.eventTypeToString(event.eventType) } catch (_: Exception) { "${event.eventType}" }
        android.util.Log.i("FocusShieldEngine", "A11yEvent: pkg=$pkgName, cls=$cls, type=$eventTypeStr, time=${event.eventTime}")

        com.blockit.core.health.BlockHealthMonitor.recordAccessibilityEvent(pkgName)
        val snapshot = ruleEvaluator.getSnapshot()

        // Ignore events from our own app, UNLESS the user opened the main settings screen
        if (pkgName == packageName || pkgName.startsWith("com.blockit")) {
            if (cls.contains("FocusHomeActivity")) {
                currentlyBlockedPackage = null
                return
            }
            if (cls.contains("BlockOverlayActivity")) {
                return
            }
            if (cls.contains("MainActivity")) {
                currentlyBlockedPackage = null
                if (overlayController.isOverlayShowing()) {
                    overlayController.hideBlockOverlay()
                }
            }
            return
        }

        // If this package is currently latched as blocked, ignore repeated events and touches under the block screen
        // ONLY if BlockOverlayActivity is currently actively visible for this target.
        // If the overlay was closed or dismissed, clear the latch so blocks engage reliably on subsequent opens!
        if (pkgName == currentlyBlockedPackage) {
            if (BlockOverlayActivity.isShowing && BlockOverlayActivity.currentBlockedTarget == pkgName) {
                return
            } else {
                currentlyBlockedPackage = null
            }
        }

        // Ignore essential system framework and UI components dynamically
        if (::systemAppResolver.isInitialized && systemAppResolver.isAlwaysPermitted(pkgName, isWhitelistMode = snapshot.hasActiveWhitelist)) {
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
            currentlyBlockedPackage = null
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
            if (intentPassManager.isBreakActive()) {
                currentlyBlockedPackage = null
                if (overlayController.isOverlayShowing()) {
                    overlayController.hideBlockOverlay()
                }
                return
            }
            if (snapshot.hasActiveWhitelist) {
                val now = System.currentTimeMillis()
                if (snapshot.isWhitelistExpired(now)) {
                    appScope.launch { ruleEvaluator.refreshRules() }
                    return
                }
                // Debounce FocusHomeActivity launches to eliminate high-frequency flicker loops
                if (now - lastFocusHomeLaunchTime < 1500L) {
                    return
                }
                lastFocusHomeLaunchTime = now

                // If the user legitimately tapped a permitted app moments ago, do NOT redirect back to Focus Home during transition!
                if (com.blockit.core.engine.EngineLifecycleMonitor.isAnyPermittedLaunchInGracePeriod()) {
                    return
                }
                // Whitelist mode: redirect to Focus Home kiosk surface
                currentlyBlockedPackage = null
                val focusHomeIntent = Intent(this, com.blockit.ui.screens.focushome.FocusHomeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(focusHomeIntent)
                com.blockit.core.engine.EngineLifecycleMonitor.recordForegroundTransition(packageName, "FOCUS_HOME_KIOSK_REDIRECT")
            } else {
                // Blacklist mode: normal phone usage on home screen
                currentlyBlockedPackage = null
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
                    currentlyBlockedPackage = "com.android.settings"
                    lastBlockedTimestamp = System.currentTimeMillis()
                    BlockOverlayActivity.start(
                        context = this,
                        target = "Android Settings",
                        profile = "Strict Mode",
                        reason = "Settings access guarded during active focus rules",
                        strictness = currentStrictness,
                        canRequestPause = false,
                        sessionEndMs = getActiveSessionEndTimestamp()
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
            // Clear the latch so new blocks can engage cleanly.
            currentlyBlockedPackage = null
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
                        currentlyBlockedPackage = pkgName
                        lastBlockedTimestamp = System.currentTimeMillis()

                        BlockOverlayActivity.start(
                            context = this,
                            target = detectedDomain,
                            profile = profile,
                            reason = reason,
                            strictness = currentStrictness,
                            canRequestPause = false,
                            sessionEndMs = getActiveSessionEndTimestamp()
                        )

                        logBlockEvent(detectedDomain, TargetType.WEBSITE, profile)
                    }
                }
            }
        }
    }

    private fun checkAndBlockForegroundApp(pkgName: String): Boolean {
        if (com.blockit.core.engine.EngineLifecycleMonitor.isPermittedAppInGracePeriod(pkgName)) {
            return false
        }
        if (intentPassManager.hasActivePass(pkgName)) return false
        val result = ruleEvaluator.isAppBlocked(pkgName)
        if (result.isBlocked) {
            val profile = result.profileName ?: "Focus"
            val reason = result.reason ?: "App blocked"
            if (result.isWhitelistBlock) {
                val now = System.currentTimeMillis()
                val snapshot = ruleEvaluator.getSnapshot()
                if (snapshot.isWhitelistExpired(now)) {
                    appScope.launch { ruleEvaluator.refreshRules() }
                    return false
                }
                if (now - lastFocusHomeLaunchTime < 1500L) {
                    return false
                }
                lastFocusHomeLaunchTime = now

                currentlyBlockedPackage = null
                val focusHomeIntent = Intent(this, com.blockit.ui.screens.focushome.FocusHomeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(focusHomeIntent)
                com.blockit.core.engine.EngineLifecycleMonitor.recordForegroundTransition(pkgName, "BLOCKED_WHITELIST_REDIRECT")
                logBlockEvent(pkgName, TargetType.APP, profile)
                return true
            }

            currentlyBlockedPackage = pkgName
            lastBlockedTimestamp = System.currentTimeMillis()

            val canPause = intentPassManager.canRequestPass(pkgName, currentStrictness)
            val remaining = intentPassManager.getRemainingPassesToday()
            val maxPauses = intentPassManager.getMaxPauses()

            if (BlockOverlayActivity.isShowing && BlockOverlayActivity.currentBlockedTarget == pkgName) {
                return true
            }

            BlockOverlayActivity.start(
                context = this,
                target = pkgName,
                profile = profile,
                reason = reason,
                strictness = currentStrictness,
                canRequestPause = canPause,
                remainingPauses = remaining,
                maxPauses = maxPauses,
                sessionEndMs = getActiveSessionEndTimestamp()
            )

            com.blockit.core.engine.EngineLifecycleMonitor.recordForegroundTransition(pkgName, "BLOCKED_BLACKLIST_OVERLAY")
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
        currentlyBlockedPackage = null
        com.blockit.core.engine.EngineLifecycleMonitor.recordLifecycleEvent("BlockAccessibilityService.onInterrupt")
        if (::overlayController.isInitialized) {
            overlayController.hideBlockOverlay()
            overlayController.hidePauseCountdownPill()
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isServiceConnected = false
        com.blockit.core.engine.EngineLifecycleMonitor.updateEngineStatus(
            com.blockit.core.engine.AccessibilityEngineStatus.ENABLED_NOT_CONNECTED
        )
        com.blockit.core.engine.EngineLifecycleMonitor.recordLifecycleEvent("BlockAccessibilityService.onUnbind")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceConnected = false
        currentlyBlockedPackage = null
        com.blockit.core.engine.EngineLifecycleMonitor.updateEngineStatus(
            com.blockit.core.engine.AccessibilityEngineStatus.ENABLED_NOT_CONNECTED
        )
        com.blockit.core.engine.EngineLifecycleMonitor.recordLifecycleEvent("BlockAccessibilityService.onDestroy")
        com.blockit.core.health.BlockHealthMonitor.updateAccessibilityStatus(false)
        com.blockit.core.health.BlockHealthMonitor.testBlockTrigger = null
        if (::overlayController.isInitialized) {
            overlayController.hideBlockOverlay()
            overlayController.hidePauseCountdownPill()
        }
    }
}
