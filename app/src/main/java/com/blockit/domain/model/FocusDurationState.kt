package com.blockit.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Clock interface for deterministic, testable time operations.
 */
fun interface FocusClock {
    fun currentTimeMillis(): Long

    companion object {
        val SYSTEM: FocusClock = FocusClock { System.currentTimeMillis() }
    }
}

/**
 * Single source of truth for focus session duration and "Ends at" time calculations.
 * Encapsulates hours and minutes into one unified, immutable state object.
 */
data class FocusDurationState(
    val hours: Int = 0,
    val minutes: Int = 25,
    val clock: FocusClock = FocusClock.SYSTEM
) {
    init {
        require(hours in 0..23) { "Hours must be between 0 and 23, got $hours" }
        require(minutes in 0..59) { "Minutes must be between 0 and 59, got $minutes" }
    }

    val totalMinutes: Int
        get() = ((hours * 60) + minutes).coerceAtLeast(1)

    val totalDurationMs: Long
        get() = totalMinutes * 60L * 1000L

    fun computeEndTimeMs(nowMs: Long = clock.currentTimeMillis()): Long {
        return nowMs + totalDurationMs
    }

    fun formatEndsAt(
        nowMs: Long = clock.currentTimeMillis(),
        locale: Locale = Locale.getDefault(),
        timeZone: java.util.TimeZone = java.util.TimeZone.getDefault()
    ): String {
        val endTime = computeEndTimeMs(nowMs)
        val sdf = SimpleDateFormat("h:mm a", locale).apply {
            this.timeZone = timeZone
        }
        return "Ends at ${sdf.format(Date(endTime))}"
    }

    fun withHours(newHours: Int): FocusDurationState = copy(hours = newHours.coerceIn(0, 23))

    fun withMinutes(newMinutes: Int): FocusDurationState = copy(minutes = newMinutes.coerceIn(0, 59))

    fun withDuration(newHours: Int, newMinutes: Int): FocusDurationState = copy(
        hours = newHours.coerceIn(0, 23),
        minutes = newMinutes.coerceIn(0, 59)
    )

    fun withTotalMinutes(totalMins: Int): FocusDurationState {
        val sanitized = totalMins.coerceAtLeast(1)
        val h = (sanitized / 60).coerceIn(0, 23)
        val m = (sanitized % 60).coerceIn(0, 59)
        return copy(hours = h, minutes = m)
    }
}
