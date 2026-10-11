package com.blockit.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BreakValidatorTest {

    @Test
    fun evaluate_whenBreakExceedsRemainingSession_flagsWarningAndCalculatesCappedDuration() {
        val now = 1_000_000L
        val remainingMs = 3 * 60 * 1000L // 3 minutes remaining
        val sessionEndMs = now + remainingMs

        // User requests a 10-minute break
        val result = BreakValidator.evaluate(
            requestedMinutes = 10,
            remainingSessionMs = remainingMs,
            sessionEndMs = sessionEndMs,
            now = now
        )

        assertTrue(result.exceedsRemainingSession)
        assertEquals(7, result.excessMinutes) // 10m - 3m = 7m
        assertEquals(3, result.cappedBreakMinutes)
        assertEquals(sessionEndMs + (10 * 60 * 1000L), result.newSessionEndMs)
    }

    @Test
    fun evaluate_whenBreakIsWithinRemainingSession_doesNotFlagWarning() {
        val now = 1_000_000L
        val remainingMs = 25 * 60 * 1000L // 25 minutes remaining
        val sessionEndMs = now + remainingMs

        // User requests a 5-minute break
        val result = BreakValidator.evaluate(
            requestedMinutes = 5,
            remainingSessionMs = remainingMs,
            sessionEndMs = sessionEndMs,
            now = now
        )

        assertFalse(result.exceedsRemainingSession)
        assertFalse(result.exceedsScheduleBoundary)
        assertEquals(0, result.excessMinutes)
    }

    @Test
    fun evaluate_whenSessionHasNoEndTimestamp_doesNotFlagSessionExceeded() {
        val now = 1_000_000L

        // Untimed session (sessionEndMs = 0L)
        val result = BreakValidator.evaluate(
            requestedMinutes = 10,
            remainingSessionMs = 0L,
            sessionEndMs = 0L,
            now = now
        )

        assertFalse(result.exceedsRemainingSession)
    }

    @Test
    fun evaluate_whenBreakExtendsPastScheduleEnd_flagsScheduleWarning() {
        val now = 1_000_000L
        val remainingMs = 5 * 60 * 1000L // 5 minutes remaining
        val sessionEndMs = now + remainingMs
        val scheduleEndMs = sessionEndMs // Schedule ends at current session end

        // User requests a 20-minute break
        val result = BreakValidator.evaluate(
            requestedMinutes = 20,
            remainingSessionMs = remainingMs,
            sessionEndMs = sessionEndMs,
            scheduleEndMs = scheduleEndMs,
            now = now
        )

        assertTrue(result.exceedsRemainingSession)
        assertTrue(result.exceedsScheduleBoundary)
        assertEquals(15, result.excessMinutes)
        assertEquals(5, result.cappedBreakMinutes)
    }
}
