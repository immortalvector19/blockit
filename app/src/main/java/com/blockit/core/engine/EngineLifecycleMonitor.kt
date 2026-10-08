package com.blockit.core.engine

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

enum class AccessibilityEngineStatus {
    NOT_ENABLED,
    ENABLED_NOT_CONNECTED,
    CONNECTED
}

data class ServiceLifecycleEvent(
    val event: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isoTimestamp: String = formatIso(timestamp)
)

data class ForegroundAppLogEntry(
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isoTimestamp: String = formatIso(timestamp),
    val reason: String
)

private fun formatIso(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
    return sdf.format(Date(timestamp))
}

/**
 * Diagnostic and lifecycle monitor for FocusShield's accessibility engine.
 * Records service lifecycle transitions, tracks foreground app changes with reasons,
 * and maintains an active launch latch to prevent app-switch redirect conflicts.
 */
object EngineLifecycleMonitor {

    private const val TAG = "EngineLifecycle"
    private const val MAX_LOG_SIZE = 100

    private val _engineStatus = MutableStateFlow(AccessibilityEngineStatus.NOT_ENABLED)
    val engineStatus: StateFlow<AccessibilityEngineStatus> = _engineStatus.asStateFlow()

    private val lifecycleEvents = ConcurrentLinkedDeque<ServiceLifecycleEvent>()
    private val foregroundTransitions = ConcurrentLinkedDeque<ForegroundAppLogEntry>()

    // Latch for apps legitimately launched from Focus Home (Allowlist lockdown)
    @Volatile
    private var activePermittedLaunch: Pair<String, Long>? = null

    fun updateEngineStatus(status: AccessibilityEngineStatus) {
        _engineStatus.value = status
        Log.i(TAG, "Engine status changed -> $status")
    }

    fun recordLifecycleEvent(eventName: String) {
        val entry = ServiceLifecycleEvent(eventName)
        lifecycleEvents.addFirst(entry)
        while (lifecycleEvents.size > MAX_LOG_SIZE) {
            lifecycleEvents.removeLast()
        }
        Log.i(TAG, "[LIFECYCLE ${entry.isoTimestamp}] $eventName")
    }

    fun recordForegroundTransition(packageName: String, reason: String) {
        val entry = ForegroundAppLogEntry(packageName = packageName, reason = reason)
        foregroundTransitions.addFirst(entry)
        while (foregroundTransitions.size > MAX_LOG_SIZE) {
            foregroundTransitions.removeLast()
        }
        Log.i("ForegroundTracker", "[FOREGROUND ${entry.isoTimestamp}] $packageName (Reason: $reason)")
    }

    fun markPermittedAppLaunched(packageName: String) {
        val now = System.currentTimeMillis()
        activePermittedLaunch = packageName to now
        recordForegroundTransition(packageName, "USER_LAUNCHED_PERMITTED")
        Log.i(TAG, "Permitted launch latch engaged for $packageName at $now")
    }

    fun isPermittedAppInGracePeriod(packageName: String, graceMs: Long = 3500L): Boolean {
        val launch = activePermittedLaunch ?: return false
        val (pkg, timestamp) = launch
        val diff = System.currentTimeMillis() - timestamp
        val inGrace = diff in 0..graceMs
        if (inGrace) {
            // Also allow system assistant / splash / autofill related to this package
            if (pkg == packageName || packageName.startsWith("com.google.android.gms") || packageName == "android") {
                return true
            }
        }
        return false
    }

    fun isAnyPermittedLaunchInGracePeriod(graceMs: Long = 3500L): Boolean {
        val launch = activePermittedLaunch ?: return false
        val diff = System.currentTimeMillis() - launch.second
        return diff in 0..graceMs
    }

    fun clearPermittedLaunchLatch() {
        activePermittedLaunch = null
    }

    fun getRecentLifecycleEvents(): List<ServiceLifecycleEvent> = lifecycleEvents.toList()

    fun getRecentForegroundTransitions(): List<ForegroundAppLogEntry> = foregroundTransitions.toList()
}
