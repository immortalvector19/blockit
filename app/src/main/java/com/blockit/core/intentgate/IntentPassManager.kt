package com.blockit.core.intentgate

import com.blockit.domain.model.SessionStrictness
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    @Volatile
    private var globalBreakStartedAtMs: Long = 0L

    @Volatile
    private var globalBreakExpiresAtMs: Long = 0L

    private val _isBreakActive = MutableStateFlow(false)
    val isBreakActiveFlow: StateFlow<Boolean> = _isBreakActive.asStateFlow()

    private val _breakExpiresAtMs = MutableStateFlow(0L)
    val breakExpiresAtMsFlow: StateFlow<Long> = _breakExpiresAtMs.asStateFlow()

    @Volatile
    private var onBreakEndedListener: ((actualBreakDurationMs: Long) -> Unit)? = null

    @Volatile
    private var onBreakStartedListener: ((durationMinutes: Int) -> Unit)? = null

    @Volatile
    private var onBreakEndedEarlyListener: ((unusedDurationMs: Long) -> Unit)? = null

    fun setOnBreakEndedListener(listener: ((actualBreakDurationMs: Long) -> Unit)?) {
        onBreakEndedListener = listener
    }

    fun setOnBreakStartedListener(listener: ((durationMinutes: Int) -> Unit)?) {
        onBreakStartedListener = listener
    }

    fun setOnBreakEndedEarlyListener(listener: ((unusedDurationMs: Long) -> Unit)?) {
        onBreakEndedEarlyListener = listener
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
            return false // Locked mode forbids all pauses and breaks
        }
        checkAndResetDay()
        if (isBreakActive() || hasActivePass(packageName)) return true
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

    @Synchronized
    fun grantBreak(durationMinutes: Int): Boolean {
        checkAndResetDay()
        if (passesUsedToday >= maxPauses && !isBreakActive()) {
            return false
        }
        val durationMs = durationMinutes * 60 * 1000L
        val now = System.currentTimeMillis()
        globalBreakStartedAtMs = now
        globalBreakExpiresAtMs = now + durationMs
        _isBreakActive.value = true
        _breakExpiresAtMs.value = now + durationMs
        passesUsedToday++
        onBreakStartedListener?.invoke(durationMinutes)
        return true
    }

    fun isBreakActive(): Boolean {
        val expiresAt = globalBreakExpiresAtMs
        if (expiresAt == 0L) {
            _isBreakActive.value = false
            return false
        }
        val now = System.currentTimeMillis()
        if (now < expiresAt) {
            _isBreakActive.value = true
            return true
        } else {
            endBreak()
            return false
        }
    }

    fun getRemainingBreakMs(): Long {
        val expiresAt = globalBreakExpiresAtMs
        return (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun getBreakStartedAtMs(): Long = globalBreakStartedAtMs

    fun getBreakExpiresAtMs(): Long = globalBreakExpiresAtMs

    /**
     * Returns the timestamp when the active break started, or null if no break is active.
     * The focus session countdown pauses at this exact timestamp.
     */
    fun getBreakPausedAtTimestampMs(): Long? {
        return if (isBreakActive()) globalBreakStartedAtMs else null
    }

    @Synchronized
    fun endBreak(): Long {
        val start = globalBreakStartedAtMs
        if (start == 0L) {
            _isBreakActive.value = false
            return 0L
        }
        val now = System.currentTimeMillis()
        val plannedExpires = globalBreakExpiresAtMs
        val actualDurationMs = (now - start).coerceAtLeast(0L)
            .coerceAtMost(if (plannedExpires > 0L) (plannedExpires - start) else Long.MAX_VALUE)
        val unusedDurationMs = if (plannedExpires > now) (plannedExpires - now) else 0L

        globalBreakStartedAtMs = 0L
        globalBreakExpiresAtMs = 0L
        _isBreakActive.value = false
        _breakExpiresAtMs.value = 0L

        if (unusedDurationMs > 0L) {
            onBreakEndedEarlyListener?.invoke(unusedDurationMs)
        }
        onBreakEndedListener?.invoke(actualDurationMs)
        return actualDurationMs
    }

    fun hasActivePass(packageName: String): Boolean {
        if (isBreakActive()) return true
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
        globalBreakStartedAtMs = 0L
        globalBreakExpiresAtMs = 0L
        _isBreakActive.value = false
        _breakExpiresAtMs.value = 0L
    }
}
