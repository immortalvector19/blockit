package com.blockit.domain.model

import com.blockit.ui.components.formatMinuteOfDayAmPm
import com.blockit.ui.components.fromMinuteOfDay
import com.blockit.ui.components.isOvernightWindow
import com.blockit.ui.components.toMinuteOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTimeFormatterTest {

    @Test
    fun testToMinuteOfDay() {
        // 9:00 AM -> 9 * 60 = 540
        assertEquals(540, toMinuteOfDay(hour12 = 9, minute = 0, isPm = false))

        // 5:00 PM -> (5 + 12) * 60 = 1020
        assertEquals(1020, toMinuteOfDay(hour12 = 5, minute = 0, isPm = true))

        // 12:00 AM (midnight) -> 0
        assertEquals(0, toMinuteOfDay(hour12 = 12, minute = 0, isPm = false))

        // 12:00 PM (noon) -> 12 * 60 = 720
        assertEquals(720, toMinuteOfDay(hour12 = 12, minute = 0, isPm = true))

        // 11:59 PM -> (11 + 12) * 60 + 59 = 1439
        assertEquals(1439, toMinuteOfDay(hour12 = 11, minute = 59, isPm = true))
    }

    @Test
    fun testFromMinuteOfDay() {
        assertEquals(Triple(9, 0, false), fromMinuteOfDay(540))
        assertEquals(Triple(5, 0, true), fromMinuteOfDay(1020))
        assertEquals(Triple(12, 0, false), fromMinuteOfDay(0))
        assertEquals(Triple(12, 0, true), fromMinuteOfDay(720))
        assertEquals(Triple(11, 59, true), fromMinuteOfDay(1439))
    }

    @Test
    fun testFormatMinuteOfDayAmPm() {
        assertEquals("9:00 AM", formatMinuteOfDayAmPm(540))
        assertEquals("5:00 PM", formatMinuteOfDayAmPm(1020))
        assertEquals("12:00 AM", formatMinuteOfDayAmPm(0))
        assertEquals("12:00 PM", formatMinuteOfDayAmPm(720))
        assertEquals("11:59 PM", formatMinuteOfDayAmPm(1439))
        assertEquals("8:30 PM", formatMinuteOfDayAmPm(1230))
    }

    @Test
    fun testOvernightWindowDetection() {
        // 9:00 AM to 5:00 PM is NOT overnight
        assertFalse(isOvernightWindow(startMinute = 540, endMinute = 1020))

        // 10:00 PM (1320) to 6:00 AM (360) IS overnight
        assertTrue(isOvernightWindow(startMinute = 1320, endMinute = 360))

        // Same minute is not overnight
        assertFalse(isOvernightWindow(startMinute = 540, endMinute = 540))
    }

    @Test
    fun testFormatNextScheduleRun() {
        val testCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 14) // 2:00 PM (840)
            set(java.util.Calendar.MINUTE, 0)
        }

        // Today later: 7:30 PM (1170) on Monday with Mon active (bit 0 = 1)
        val todayLater = com.blockit.ui.components.formatNextScheduleRun(mask = 1, startMinute = 1170, currentCal = testCal)
        assertEquals("Today, 7:30 PM", todayLater)

        // Today earlier (9:00 AM, 540) on Monday with Tue active (bit 1 = 2)
        val tomorrowRun = com.blockit.ui.components.formatNextScheduleRun(mask = 3, startMinute = 540, currentCal = testCal)
        assertEquals("Tomorrow, 9:00 AM", tomorrowRun)

        // Only Wednesday active (bit 2 = 4)
        val wedRun = com.blockit.ui.components.formatNextScheduleRun(mask = 4, startMinute = 600, currentCal = testCal)
        assertEquals("Wed, 10:00 AM", wedRun)

        // Empty mask
        val emptyRun = com.blockit.ui.components.formatNextScheduleRun(mask = 0, startMinute = 600, currentCal = testCal)
        assertEquals("No active days", emptyRun)
    }
}
