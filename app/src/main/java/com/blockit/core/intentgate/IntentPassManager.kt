package com.blockit.core.intentgate

import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

class IntentPassManager {
    // Map of packageName -> expirationTimestampMs
    private val activePasses = ConcurrentHashMap<String, Long>()

    // Daily pass tracking: dayOfYear -> count
    private var lastRecordedDay = -1
    private var passesUsedToday = 0

    companion object {
        const val MAX_DAILY_PASSES = 3
        const val DEFAULT_PASS_DURATION_MS = 3 * 60 * 1000L // 3 minutes
    }

    @Synchronized
    private fun checkAndResetDay() {
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        if (currentDay != lastRecordedDay) {
            lastRecordedDay = currentDay
            passesUsedToday = 0
        }
    }

    @Synchronized
    fun getPassesUsedToday(): Int {
        checkAndResetDay()
        return passesUsedToday
    }

    @Synchronized
    fun getRemainingPassesToday(): Int {
        checkAndResetDay()
        return (MAX_DAILY_PASSES - passesUsedToday).coerceAtLeast(0)
    }

    @Synchronized
    fun canRequestPass(packageName: String): Boolean {
        checkAndResetDay()
        if (hasActivePass(packageName)) return true
        return passesUsedToday < MAX_DAILY_PASSES
    }

    @Synchronized
    fun grantPass(packageName: String, durationMs: Long = DEFAULT_PASS_DURATION_MS): Boolean {
        checkAndResetDay()
        if (passesUsedToday >= MAX_DAILY_PASSES && !hasActivePass(packageName)) {
            return false
        }
        val expiresAt = System.currentTimeMillis() + durationMs
        activePasses[packageName] = expiresAt
        passesUsedToday++
        return true
    }

    fun hasActivePass(packageName: String): Boolean {
        val expiresAt = activePasses[packageName] ?: return false
        if (System.currentTimeMillis() < expiresAt) {
            return true
        } else {
            activePasses.remove(packageName)
            return false
        }
    }

    fun getRemainingPassMs(packageName: String): Long {
        val expiresAt = activePasses[packageName] ?: return 0L
        return (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun revokePass(packageName: String) {
        activePasses.remove(packageName)
    }

    fun clearAll() {
        activePasses.clear()
    }
}
