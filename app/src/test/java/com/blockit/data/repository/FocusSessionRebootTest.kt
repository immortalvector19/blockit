package com.blockit.data.repository

import com.blockit.data.local.db.BlockItDatabase
import com.blockit.data.local.db.dao.FocusSessionDao
import com.blockit.data.local.db.dao.ProfileDao
import com.blockit.data.local.db.entity.FocusSessionEntity
import com.blockit.data.local.db.entity.ProfileEntity
import com.blockit.domain.evaluator.RuleEvaluator
import com.blockit.service.receiver.BoundaryAlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class FocusSessionRebootTest {

    private val persistentSessions = mutableListOf<FocusSessionEntity>()
    private var alarmScheduleCount = 0
    private var rulesRefreshCount = 0

    private val fakeBoundaryAlarmScheduler = object : BoundaryAlarmScheduler {
        override suspend fun scheduleNextBoundaryAlarm() {
            alarmScheduleCount++
        }
    }

    private val fakeFocusSessionDao = object : FocusSessionDao {
        override suspend fun getActiveSessions(currentTimeMs: Long): List<FocusSessionEntity> {
            return persistentSessions.filter { it.isActive && it.endTimestamp > currentTimeMs }
        }

        override fun observeActiveSessions(currentTimeMs: Long): Flow<List<FocusSessionEntity>> {
            return flowOf(persistentSessions.filter { it.isActive && it.endTimestamp > currentTimeMs })
        }

        override fun observeAllSessions(): Flow<List<FocusSessionEntity>> {
            return flowOf(persistentSessions.toList())
        }

        override suspend fun insertSession(session: FocusSessionEntity): Long {
            persistentSessions.add(session)
            return persistentSessions.size.toLong()
        }

        override suspend fun endSession(sessionId: Long) {
            val idx = persistentSessions.indexOfFirst { it.id == sessionId }
            if (idx >= 0) {
                persistentSessions[idx] = persistentSessions[idx].copy(isActive = false)
            }
        }

        override suspend fun extendSessionEndTime(sessionId: Long, extensionMs: Long) {
            val idx = persistentSessions.indexOfFirst { it.id == sessionId }
            if (idx >= 0) {
                persistentSessions[idx] = persistentSessions[idx].copy(
                    endTimestamp = persistentSessions[idx].endTimestamp + extensionMs
                )
            }
        }

        override suspend fun cleanupExpiredSessions(currentTimeMs: Long) {
            for (i in persistentSessions.indices) {
                if (persistentSessions[i].endTimestamp <= currentTimeMs) {
                    persistentSessions[i] = persistentSessions[i].copy(isActive = false)
                }
            }
        }
    }

    private val fakeProfileDao = Proxy.newProxyInstance(
        ProfileDao::class.java.classLoader,
        arrayOf(ProfileDao::class.java)
    ) { _, method, args ->
        if (method.name == "getProfileById") {
            ProfileEntity(
                id = (args?.get(0) as? Long) ?: 1L,
                name = "Deep Focus",
                colorHex = 0xFF2563EBL,
                iconKey = "ic_focus",
                isEnabled = true,
                blockMode = "BLACKLIST",
                createdAt = 1000L
            )
        } else null
    } as ProfileDao

    private val fakeRuleEvaluator = object : RuleEvaluator(systemAppResolver = null) {
        override suspend fun refreshRules() {
            rulesRefreshCount++
        }
    }

    private lateinit var sessionManager: FocusSessionManager

    @Before
    fun setUp() {
        persistentSessions.clear()
        alarmScheduleCount = 0
        rulesRefreshCount = 0

        sessionManager = FocusSessionManager(
            database = null,
            ruleEvaluator = fakeRuleEvaluator,
            scheduleManager = fakeBoundaryAlarmScheduler,
            notificationManager = null,
            context = null,
            customFocusSessionDao = fakeFocusSessionDao,
            customProfileDao = fakeProfileDao
        )
    }

    @Test
    fun rebootBeforeEndTime_resumesSessionWithExactRemainingTime_andRearmsAlarms() = runTest {
        val t0 = 1_000_000L
        val duration15MinMs = 15 * 60 * 1000L
        val tEnd = t0 + duration15MinMs

        // Start a 15-minute session before phone powers down
        fakeFocusSessionDao.insertSession(
            FocusSessionEntity(
                id = 101L,
                profileId = 1L,
                startTimestamp = t0,
                endTimestamp = tEnd,
                isActive = true
            )
        )

        // Simulate phone rebooting 5 minutes into the 15-minute session
        val tReboot = t0 + (5 * 60 * 1000L) // 10 minutes remaining
        val result = sessionManager.reconcileSessionOnBoot(tReboot)

        assertTrue("Session must remain active across reboot", result.hasActiveSession)
        assertNotNull("Active session model must be returned", result.activeSession)
        assertEquals(101L, result.activeSession?.id)
        assertEquals(10 * 60 * 1000L, result.remainingTimeMs) // 10 minutes left
        assertEquals(1, alarmScheduleCount) // Exact boundary alarm re-armed
        assertEquals(1, rulesRefreshCount) // Rules refreshed
    }

    @Test
    fun rebootAfterEndTime_terminatesSessionImmediately_andUnlocksDevice() = runTest {
        val t0 = 1_000_000L
        val duration15MinMs = 15 * 60 * 1000L
        val tEnd = t0 + duration15MinMs

        // Session was active when the device powered off
        fakeFocusSessionDao.insertSession(
            FocusSessionEntity(
                id = 102L,
                profileId = 1L,
                startTimestamp = t0,
                endTimestamp = tEnd,
                isActive = true
            )
        )

        // Device was powered off or rebooting, and boots back up 20 minutes later (past end time)
        val tReboot = t0 + (20 * 60 * 1000L) // 5 minutes after session ended
        val result = sessionManager.reconcileSessionOnBoot(tReboot)

        assertFalse("Session must be inactive since end time passed during power off", result.hasActiveSession)
        assertNull("Active session model must be null", result.activeSession)
        assertEquals(0L, result.remainingTimeMs)

        // Verify the database record was updated so isActive = false
        assertFalse(persistentSessions.first().isActive)
        assertEquals(1, rulesRefreshCount) // Rules refreshed to clear blockers
    }

    @Test
    fun extendActiveSession_extendsEndTimestamp_andPreservesActiveRemainingDuration() = runTest {
        val t0 = System.currentTimeMillis()
        val duration15MinMs = 15 * 60 * 1000L
        val tEnd = t0 + duration15MinMs

        fakeFocusSessionDao.insertSession(
            FocusSessionEntity(
                id = 201L,
                profileId = 1L,
                startTimestamp = t0,
                endTimestamp = tEnd,
                isActive = true
            )
        )

        val beforeRefreshCount = rulesRefreshCount
        val beforeAlarmCount = alarmScheduleCount

        // User takes a 5-minute break (300,000 ms)
        val breakDurationMs = 5 * 60 * 1000L
        sessionManager.extendActiveSession(breakDurationMs)

        // Verify end timestamp in SQLite database was shifted forward by exactly breakDurationMs
        val updatedSession = persistentSessions.first { it.id == 201L }
        assertEquals(tEnd + breakDurationMs, updatedSession.endTimestamp)
        assertEquals(tEnd + breakDurationMs, sessionManager.getCachedActiveSession()?.endTimestamp)

        // Verify rule evaluator and alarms were refreshed for the new boundary
        assertTrue(rulesRefreshCount > beforeRefreshCount)
        assertTrue(alarmScheduleCount > beforeAlarmCount)
    }

    @Test
    fun breakExtension_doesNotMutateProfileConfigurations() = runTest {
        val originalProfile = ProfileEntity(
            id = 1L,
            name = "Work Hours",
            colorHex = 0xFF2563EBL,
            iconKey = "work",
            isEnabled = true,
            blockMode = "WHITELIST",
            profileType = "SCHEDULE",
            listType = "ALLOWLIST",
            scheduleDaysMask = 31,
            scheduleStartMinute = 540,
            scheduleEndMinute = 1020,
            createdAt = 1000L
        )

        // Active session started under this profile
        val t0 = 1_000_000L
        val tEnd = t0 + (30 * 60 * 1000L)
        fakeFocusSessionDao.insertSession(
            FocusSessionEntity(
                id = 301L,
                profileId = 1L,
                startTimestamp = t0,
                endTimestamp = tEnd,
                isActive = true
            )
        )

        // Break occurs
        sessionManager.extendActiveSession(10 * 60 * 1000L)

        // Verify profile entity configuration was NOT altered
        assertEquals(31, originalProfile.scheduleDaysMask)
        assertEquals(540, originalProfile.scheduleStartMinute)
        assertEquals(1020, originalProfile.scheduleEndMinute)
        assertEquals("ALLOWLIST", originalProfile.listType)
        assertEquals("SCHEDULE", originalProfile.profileType)
    }

    @Test
    fun breakEarlyResume_adjustsEndTimestampCorrectly_andPreservesProfile() = runTest {
        val t0 = System.currentTimeMillis()
        val originalEnd = t0 + (20 * 60 * 1000L) // 20 minutes session

        fakeFocusSessionDao.insertSession(
            FocusSessionEntity(
                id = 401L,
                profileId = 1L,
                startTimestamp = t0,
                endTimestamp = originalEnd,
                isActive = true
            )
        )

        // 1. Break starts: 10 minutes granted
        val plannedBreakMs = 10 * 60 * 1000L
        sessionManager.extendActiveSession(plannedBreakMs)
        assertEquals(originalEnd + plannedBreakMs, persistentSessions.first { it.id == 401L }.endTimestamp)

        // 2. User resumes early after 3 minutes: unused break is 7 minutes (420,000 ms)
        val unusedBreakMs = 7 * 60 * 1000L
        sessionManager.extendActiveSession(-unusedBreakMs)

        // End timestamp should now be exactly originalEnd + 3 minutes!
        val finalEnd = persistentSessions.first { it.id == 401L }.endTimestamp
        assertEquals(originalEnd + (3 * 60 * 1000L), finalEnd)
        assertEquals(finalEnd, sessionManager.getCachedActiveSession()?.endTimestamp)
    }
}
