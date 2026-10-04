package com.blockit.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TimeSavedCalculatorTest {

    @Test
    fun calculateMinutesSaved_zeroOrNegative_returnsZero() {
        assertEquals(0, TimeSavedCalculator.calculateMinutesSaved(0))
        assertEquals(0, TimeSavedCalculator.calculateMinutesSaved(-5))
    }

    @Test
    fun calculateMinutesSaved_defaultRate_multipliesByFive() {
        assertEquals(5, TimeSavedCalculator.calculateMinutesSaved(1))
        assertEquals(50, TimeSavedCalculator.calculateMinutesSaved(10))
        assertEquals(100, TimeSavedCalculator.calculateMinutesSaved(20))
    }

    @Test
    fun calculateMinutesSaved_customRate_multipliesCorrectly() {
        assertEquals(10, TimeSavedCalculator.calculateMinutesSaved(5, minutesPerAttempt = 2))
        assertEquals(30, TimeSavedCalculator.calculateMinutesSaved(3, minutesPerAttempt = 10))
    }

    @Test
    fun formatTimeSaved_zeroOrNegative_returnsZeroM() {
        assertEquals("0m", TimeSavedCalculator.formatTimeSaved(0))
        assertEquals("0m", TimeSavedCalculator.formatTimeSaved(-10))
    }

    @Test
    fun formatTimeSaved_lessThanHour_formatsMinutesOnly() {
        assertEquals("5m", TimeSavedCalculator.formatTimeSaved(5))
        assertEquals("45m", TimeSavedCalculator.formatTimeSaved(45))
        assertEquals("59m", TimeSavedCalculator.formatTimeSaved(59))
    }

    @Test
    fun formatTimeSaved_exactHours_formatsHoursOnly() {
        assertEquals("1h", TimeSavedCalculator.formatTimeSaved(60))
        assertEquals("2h", TimeSavedCalculator.formatTimeSaved(120))
        assertEquals("5h", TimeSavedCalculator.formatTimeSaved(300))
    }

    @Test
    fun formatTimeSaved_hoursAndMinutes_formatsBoth() {
        assertEquals("1h 15m", TimeSavedCalculator.formatTimeSaved(75))
        assertEquals("2h 30m", TimeSavedCalculator.formatTimeSaved(150))
        assertEquals("4h 5m", TimeSavedCalculator.formatTimeSaved(245))
    }

    @Test
    fun getStartOfDayTimestamp_alignsToMidnight() {
        val now = System.currentTimeMillis()
        val startOfDay = TimeSavedCalculator.getStartOfDayTimestamp(now)

        val cal = Calendar.getInstance().apply { timeInMillis = startOfDay }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
        assertEquals(0, cal.get(Calendar.MILLISECOND))
        assertTrue(startOfDay <= now)
    }

    @Test
    fun getStartOfWeekTimestamp_returnsSevenDaysPrior() {
        val now = 1_000_000_000_000L
        val expected = now - (7L * 24 * 60 * 60 * 1000)
        assertEquals(expected, TimeSavedCalculator.getStartOfWeekTimestamp(now))
    }
}
