package com.blockit.core.intentgate

import com.blockit.domain.model.SessionStrictness
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

class IntentPassManager {
    // Map of packageName -> expirationTimestampMs
    private val activePasses = ConcurrentHashMap<String, Long>()

    // Daily pass tracking: dayOfYear -> count
    private var lastRecordedDay = -1
    private var passesUsedToday = 0
    private var maxPauses = 3

    companion object {
        const val DEFAULT_MAX_PAUSES = 3
        const val DEFAULT_PASS_DURATION_MS = 5 * 60 * 1000L // 5 minutes default
    }

    @Synchronized
    fun setMaxPauses(max: Int) {
        maxPauses = max.coerceAtLeast(0)
    }

    @Synchronized
    fun getMaxPauses(): Int = maxPauses

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
        return (maxPauses - passesUsedToday).coerceAtLeast(0)
    }

    @Synchronized
    fun canRequestPass(packageName: String, strictness: SessionStrictness = SessionStrictness.NORMAL): Boolean {
        if (strictness == SessionStrictness.LOCKED) {
            return false // Locked mode forbids all pauses
        }
        checkAndResetDay()
        if (hasActivePass(packageName)) return true
        return passesUsedToday < maxPauses
    }

    @Synchronized
    fun grantPass(packageName: String, durationMinutes: Int): Boolean {
        checkAndResetDay()
        if (passesUsedToday >= maxPauses && !hasActivePass(packageName)) {
            return false
        }
        val durationMs = durationMinutes * 60 * 1000L
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
