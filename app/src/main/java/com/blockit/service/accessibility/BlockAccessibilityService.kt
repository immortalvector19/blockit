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
import kotlinx.coroutines.launch

class BlockAccessibilityService : AccessibilityService() {

    private lateinit var ruleEvaluator: RuleEvaluator
    private lateinit var overlayController: OverlayController
    private lateinit var appScope: CoroutineScope
    private var homePackages: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        val container = BlockItApplication.from(this)
        ruleEvaluator = container.ruleEvaluator
        overlayController = OverlayController(this)
        appScope = container.applicationScope

        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 50
        }

        resolveHomePackages()
    }

    private fun resolveHomePackages() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfos = packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        homePackages = resolveInfos.mapNotNull { it.activityInfo?.packageName }.toSet()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: return
        val snapshot = ruleEvaluator.getSnapshot()

        // Always ignore BlockIt itself to prevent self-locking
        if (pkgName == packageName) {
            if (overlayController.isOverlayShowing()) {
                overlayController.hideBlockOverlay()
            }
            return
        }

        // Ignore essential system UI components (system bars, keyboards)
        if (pkgName == "com.android.systemui" ||
            pkgName.contains("inputmethod", ignoreCase = true) ||
            pkgName.contains("keyboard", ignoreCase = true)
        ) {
            return
        }

        // If user is on the Home Launcher:
        if (homePackages.contains(pkgName)) {
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
                        val launchIntent = packageManager.getLaunchIntentForPackage(appPkg)
                        launchIntent?.let {
                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

        // Strict Mode: Guard Android Settings against deactivating or uninstalling BlockIt
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
                onHomeClicked = {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                },
                onOpenAllowedApp = { appPkg ->
                    val launchIntent = packageManager.getLaunchIntentForPackage(appPkg)
                    launchIntent?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(it)
                    }
                }
            )

            // Log event asynchronously
            logBlockEvent(pkgName, TargetType.APP, profile)
            return
        } else {
            // Foreground app is allowed: dismiss overlay if showing
            if (overlayController.isOverlayShowing()) {
                overlayController.hideBlockOverlay()
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

    private fun logBlockEvent(identifier: String, type: TargetType, profile: String) {
        appScope.launch(Dispatchers.IO) {
            try {
                val container = BlockItApplication.from(this@BlockAccessibilityService)
                container.database.blockEventDao().insertEvent(
                    BlockEventEntity(
                        timestamp = System.currentTimeMillis(),
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
            desc?.contains("BlockIt", ignoreCase = true) == true
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
        if (::overlayController.isInitialized) {
            overlayController.hideBlockOverlay()
        }
    }
}
