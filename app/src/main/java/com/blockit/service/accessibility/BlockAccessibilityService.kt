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

        // Always ignore BlockIt itself and the home launcher to prevent device soft-locking
        if (pkgName == packageName || homePackages.contains(pkgName)) {
            if (overlayController.isOverlayShowing()) {
                overlayController.hideBlockOverlay()
            }
            return
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
                onHomeClicked = {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            )

            // Log event asynchronously
            appScope.launch(Dispatchers.IO) {
                try {
                    val container = BlockItApplication.from(this@BlockAccessibilityService)
                    container.database.blockEventDao().insertEvent(
                        BlockEventEntity(
                            timestamp = System.currentTimeMillis(),
                            targetType = TargetType.APP.name,
                            identifier = pkgName,
                            profileName = profile
                        )
                    )
                } catch (_: Exception) {}
            }
        }
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
