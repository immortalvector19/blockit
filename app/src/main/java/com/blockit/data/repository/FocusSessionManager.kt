package com.blockit.data.repository

import com.blockit.data.local.db.BlockItDatabase
import com.blockit.data.local.db.entity.FocusSessionEntity
import com.blockit.domain.evaluator.RuleEvaluator
import com.blockit.domain.model.FocusSession
import com.blockit.service.notification.FocusNotificationManager
import com.blockit.service.receiver.ScheduleManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FocusSessionManager(
    private val database: BlockItDatabase,
    private val ruleEvaluator: RuleEvaluator,
    private val scheduleManager: ScheduleManager,
    private val notificationManager: FocusNotificationManager? = null
) {

    fun observeActiveSession(): Flow<FocusSession?> {
        val now = System.currentTimeMillis()
        return database.focusSessionDao().observeActiveSessions(now).map { list ->
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
        val now = System.currentTimeMillis()
        val end = now + (durationMinutes * 60 * 1000L)
        val entity = FocusSessionEntity(
            profileId = profileId,
            startTimestamp = now,
            endTimestamp = end,
            isActive = true
        )
        val id = database.focusSessionDao().insertSession(entity)
        ruleEvaluator.refreshRules()
        scheduleManager.scheduleNextBoundaryAlarm()

        val profile = database.profileDao().getProfileById(profileId)
        notificationManager?.showSessionNotification(profile?.name ?: "Focus Session", end)
        return id
    }

    suspend fun endSession(sessionId: Long) {
        database.focusSessionDao().endSession(sessionId)
        ruleEvaluator.refreshRules()
        scheduleManager.scheduleNextBoundaryAlarm()
        notificationManager?.cancelSessionNotification()
    }

    suspend fun restoreActiveSessionNotification() {
        val now = System.currentTimeMillis()
        val activeSessions = database.focusSessionDao().getActiveSessions(now)
        val active = activeSessions.firstOrNull()
        if (active != null) {
            val profile = database.profileDao().getProfileById(active.profileId)
            notificationManager?.showSessionNotification(profile?.name ?: "Focus Session", active.endTimestamp)
        } else {
            notificationManager?.cancelSessionNotification()
        }
    }
}
