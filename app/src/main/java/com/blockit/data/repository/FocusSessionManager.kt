package com.blockit.data.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import com.blockit.data.local.db.BlockItDatabase
import com.blockit.data.local.db.entity.FocusSessionEntity
import com.blockit.domain.evaluator.RuleEvaluator
import com.blockit.domain.model.FocusSession
import com.blockit.service.notification.FocusNotificationManager
import com.blockit.service.receiver.BoundaryAlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ReconcileBootResult(
    val hasActiveSession: Boolean,
    val activeSession: FocusSession?,
    val remainingTimeMs: Long
)

class FocusSessionManager(
    private val database: BlockItDatabase? = null,
    private val ruleEvaluator: RuleEvaluator,
    private val scheduleManager: BoundaryAlarmScheduler? = null,
    private val notificationManager: FocusNotificationManager? = null,
    private val context: Context? = null,
    customFocusSessionDao: com.blockit.data.local.db.dao.FocusSessionDao? = null,
    customProfileDao: com.blockit.data.local.db.dao.ProfileDao? = null
) {
    private val sessionDao: com.blockit.data.local.db.dao.FocusSessionDao =
        customFocusSessionDao ?: requireNotNull(database) { "Database or customFocusSessionDao must be provided" }.focusSessionDao()

    private val profDao: com.blockit.data.local.db.dao.ProfileDao =
        customProfileDao ?: requireNotNull(database) { "Database or customProfileDao must be provided" }.profileDao()

    fun observeActiveSession(): Flow<FocusSession?> {
        val now = System.currentTimeMillis()
        return sessionDao.observeActiveSessions(now).map { list ->
            list.firstOrNull()?.let {
                FocusSession(
                    id = it.id,
                    profileId = it.profileId,
                    startTimestamp = it.startTimestamp,
                    endTimestamp = it.endTimestamp,
                    isActive = it.isActive
                )
            }
        }
    }

    suspend fun startSession(profileId: Long, durationMinutes: Int): Long {
        val t0 = System.currentTimeMillis()
        val now = t0
        val end = now + (durationMinutes * 60 * 1000L)
        val entity = FocusSessionEntity(
            profileId = profileId,
            startTimestamp = now,
            endTimestamp = end,
            isActive = true
        )
        val id = sessionDao.insertSession(entity)
        ruleEvaluator.refreshRules()
        scheduleManager?.scheduleNextBoundaryAlarm()

        val profile = profDao.getProfileById(profileId)
        notificationManager?.showSessionNotification(profile?.name ?: "Focus Session", end)

        val elapsedMs = System.currentTimeMillis() - t0
        Log.i(
            "FocusShieldTiming",
            "Focus session $id started and rules enforced in ${elapsedMs}ms (Profile: ${profile?.name}, Mode: ${profile?.blockMode})"
        )

        // Whitelist / Allowlist mode is a real lockdown: immediately transfer foreground to Focus Home
        if ((profile?.listType == "ALLOWLIST" || profile?.blockMode == "WHITELIST") && context != null) {
            try {
                val intent = Intent(context, com.blockit.ui.screens.focushome.FocusHomeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("FocusSessionManager", "Error launching FocusHomeActivity", e)
            }
        }

        return id
    }

    suspend fun endSession(sessionId: Long) {
        sessionDao.endSession(sessionId)
        ruleEvaluator.refreshRules()
        scheduleManager?.scheduleNextBoundaryAlarm()
        notificationManager?.cancelSessionNotification()
    }

    suspend fun restoreActiveSessionNotification() {
        val now = System.currentTimeMillis()
        val activeSessions = sessionDao.getActiveSessions(now)
        val active = activeSessions.firstOrNull()
        if (active != null) {
            val profile = profDao.getProfileById(active.profileId)
            notificationManager?.showSessionNotification(profile?.name ?: "Focus Session", active.endTimestamp)
        } else {
            notificationManager?.cancelSessionNotification()
        }
    }

    /**
     * Reconciles focus sessions upon device boot or time shift.
     * Guaranteed to terminate and unlock any session whose end time passed during power-off,
     * while resuming an active session with exact remaining duration and re-arming boundary alarms.
     */
    suspend fun reconcileSessionOnBoot(currentTimeMs: Long): ReconcileBootResult {
        // 1. Immediately clean up sessions that expired during the power-off / reboot window
        sessionDao.cleanupExpiredSessions(currentTimeMs)

        // 2. Refresh active rules in the engine so unblocked apps are immediately freed
        ruleEvaluator.refreshRules()

        // 3. Re-arm boundary alarms that Android OS cleared during reboot
        scheduleManager?.scheduleNextBoundaryAlarm()

        // 4. Check if an active session survived the reboot
        val activeSessions = sessionDao.getActiveSessions(currentTimeMs)
        val active = activeSessions.firstOrNull()

        return if (active != null) {
            val remainingMs = (active.endTimestamp - currentTimeMs).coerceAtLeast(0L)
            val profile = profDao.getProfileById(active.profileId)
            notificationManager?.showSessionNotification(
                profile?.name ?: "Focus Session",
                active.endTimestamp
            )
            val sessionModel = FocusSession(
                id = active.id,
                profileId = active.profileId,
                startTimestamp = active.startTimestamp,
                endTimestamp = active.endTimestamp,
                isActive = active.isActive
            )
            ReconcileBootResult(
                hasActiveSession = true,
                activeSession = sessionModel,
                remainingTimeMs = remainingMs
            )
        } else {
            notificationManager?.cancelSessionNotification()
            ReconcileBootResult(
                hasActiveSession = false,
                activeSession = null,
                remainingTimeMs = 0L
            )
        }
    }
}
