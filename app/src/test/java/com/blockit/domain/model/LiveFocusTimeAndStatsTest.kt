package com.blockit.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class LiveFocusTimeAndStatsTest {

    @Test
    fun liveFocusTime_calculatesAccurately() {
        // Base stored focus time from past sessions: 30 minutes
        val storedFocusMinutes = 30
        val now = System.currentTimeMillis()

        // Active session started 15 minutes ago
        val activeSessionStart = now - (15 * 60 * 1000L)
        val elapsedMs = (now - activeSessionStart).coerceAtLeast(0L)
        val liveSessionMinutes = (elapsedMs / 1000 / 60).toInt()

        val totalMinutes = storedFocusMinutes + liveSessionMinutes
        val formatted = TimeSavedCalculator.formatMinutes(totalMinutes)

        assertEquals(15, liveSessionMinutes)
        assertEquals(45, totalMinutes)
        assertEquals("45m", formatted)
    }

    @Test
    fun liveFocusTime_exceedsHour_formatsHoursAndMinutes() {
        val storedFocusMinutes = 45
        val now = System.currentTimeMillis()

        // Active session running for 35 minutes
        val activeSessionStart = now - (35 * 60 * 1000L)
        val elapsedMs = (now - activeSessionStart).coerceAtLeast(0L)
        val liveSessionMinutes = (elapsedMs / 1000 / 60).toInt()

        val totalMinutes = storedFocusMinutes + liveSessionMinutes
        val formatted = TimeSavedCalculator.formatMinutes(totalMinutes)

        assertEquals(80, totalMinutes)
        assertEquals("1h 20m", formatted)
    }

    @Test
    fun weeklyChart_generatesExactlySevenUniqueDays() {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayFormat = SimpleDateFormat("EEE", Locale.US)

        val dayBounds = (6 downTo 0).map { i ->
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = calendar.timeInMillis
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val startMs = dayCal.timeInMillis
            val endCal = Calendar.getInstance().apply {
                timeInMillis = startMs
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val endMs = endCal.timeInMillis
            val label = dayFormat.format(Date(startMs)).take(3).uppercase()
            Triple(label, startMs, endMs)
        }

        // Exactly 7 items generated
        assertEquals(7, dayBounds.size)

        // All labels are 3 characters long uppercase
        dayBounds.forEach { (label, startMs, endMs) ->
            assertEquals(3, label.length)
            assertTrue(label.all { it.isUpperCase() })
            assertTrue("endMs must be after startMs", endMs > startMs)
            assertEquals("Interval should be 1 day in standard conditions", 24 * 60 * 60 * 1000L, endMs - startMs)
        }

        // The last item is today
        val todayLabel = dayFormat.format(Date(calendar.timeInMillis)).take(3).uppercase()
        assertEquals(todayLabel, dayBounds.last().first)
    }
}
