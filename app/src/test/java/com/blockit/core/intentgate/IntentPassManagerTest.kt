package com.blockit.core.intentgate

import com.blockit.domain.model.SessionStrictness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IntentPassManagerTest {

    private lateinit var passManager: IntentPassManager

    @Before
    fun setup() {
        passManager = IntentPassManager()
        passManager.clearAll()
    }

    @Test
    fun grantBreak_activatesBreakAndExposesReactiveFlows() {
        var startedDurationMinutes = 0
        passManager.setOnBreakStartedListener { minutes ->
            startedDurationMinutes = minutes
        }

        val initialRemaining = passManager.getRemainingPassesToday()
        val success = passManager.grantBreak(5)

        assertTrue(success)
        assertEquals(5, startedDurationMinutes)
        assertTrue(passManager.isBreakActive())
        assertTrue(passManager.isBreakActiveFlow.value)
        assertTrue(passManager.breakExpiresAtMsFlow.value > System.currentTimeMillis())
        assertEquals(initialRemaining - 1, passManager.getRemainingPassesToday())

        // During break, all packages have active pass
        assertTrue(passManager.hasActivePass("com.instagram.android"))
        assertTrue(passManager.hasActivePass("com.twitter.android"))
    }

    @Test
    fun grantBreak_supports5_10_and_20MinuteChoices() {
        val manager = IntentPassManager()

        // 5 minute test
        manager.grantBreak(5)
        val exp5 = manager.getBreakExpiresAtMs()
        val start5 = manager.getBreakStartedAtMs()
        assertEquals(5 * 60 * 1000L, exp5 - start5)
        manager.endBreak()

        // 10 minute test
        manager.grantBreak(10)
        val exp10 = manager.getBreakExpiresAtMs()
        val start10 = manager.getBreakStartedAtMs()
        assertEquals(10 * 60 * 1000L, exp10 - start10)
        manager.endBreak()

        // 20 minute test
        manager.grantBreak(20)
        val exp20 = manager.getBreakExpiresAtMs()
        val start20 = manager.getBreakStartedAtMs()
        assertEquals(20 * 60 * 1000L, exp20 - start20)
        manager.endBreak()
    }

    @Test
    fun lockedStrictness_forbidsBreaksAndPasses() {
        assertFalse(passManager.canRequestPass("com.instagram.android", SessionStrictness.LOCKED))
        assertTrue(passManager.canRequestPass("com.instagram.android", SessionStrictness.NORMAL))
        assertTrue(passManager.canRequestPass("com.instagram.android", SessionStrictness.STRICT))
    }

    @Test
    fun endBreakEarly_reportsUnusedDuration_andClearsFlows() {
        var unusedMs = 0L
        var actualMs = 0L
        passManager.setOnBreakEndedEarlyListener { unused ->
            unusedMs = unused
        }
        passManager.setOnBreakEndedListener { actual ->
            actualMs = actual
        }

        passManager.grantBreak(10) // 10 minutes = 600,000 ms
        assertTrue(passManager.isBreakActive())

        // End break early
        passManager.endBreak()

        assertFalse(passManager.isBreakActive())
        assertFalse(passManager.isBreakActiveFlow.value)
        assertEquals(0L, passManager.breakExpiresAtMsFlow.value)
        assertEquals(0L, passManager.getBreakExpiresAtMs())

        // Since it ended immediately, unusedMs should be close to 600,000 ms
        assertTrue("Unused break duration should be positive", unusedMs > 590_000L)
        assertTrue("Unused break duration should not exceed planned 10m", unusedMs <= 600_000L)
        assertTrue("Actual duration should be small", actualMs < 10_000L)

        // After break ended, apps are no longer permitted
        assertFalse(passManager.hasActivePass("com.instagram.android"))
    }

    @Test
    fun individualPass_allowsTargetApp_butDoesNotAffectOtherApps() {
        val success = passManager.grantPass("com.instagram.android", 5)
        assertTrue(success)

        assertTrue(passManager.hasActivePass("com.instagram.android"))
        assertFalse(passManager.hasActivePass("com.twitter.android"))
    }

    @Test
    fun grantBreak_supportsCappedMinuteValues() {
        val success = passManager.grantBreak(3) // 3-minute capped break
        assertTrue(success)

        val exp = passManager.getBreakExpiresAtMs()
        val start = passManager.getBreakStartedAtMs()
        assertEquals(3 * 60 * 1000L, exp - start)
        passManager.endBreak()
    }
}

