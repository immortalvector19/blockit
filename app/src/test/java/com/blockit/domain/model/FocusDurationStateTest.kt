package com.blockit.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class FocusDurationStateTest {

    private fun parseTimestamp(timeStr: String, tz: TimeZone = TimeZone.getTimeZone("UTC")): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.US).apply {
            timeZone = tz
        }
        return sdf.parse("2026-10-07 $timeStr")!!.time
    }

    @Test
    fun testCase1_3Hours2Minutes_At_5_07_PM() {
        val tz = TimeZone.getTimeZone("UTC")
        val fixedNow = parseTimestamp("5:07 PM", tz)
        val fakeClock = FocusClock { fixedNow }

        val state = FocusDurationState(hours = 3, minutes = 2, clock = fakeClock)
        assertEquals(182, state.totalMinutes)

        val formattedEndsAt = state.formatEndsAt(nowMs = fixedNow, locale = Locale.US, timeZone = tz)
        // 5:07 PM + 3h 2m = 8:09 PM
        assertEquals("Ends at 8:09 PM", formattedEndsAt)
    }

    @Test
    fun testCase2_0Hours2Minutes_At_4_04_PM() {
        val tz = TimeZone.getTimeZone("UTC")
        val fixedNow = parseTimestamp("4:04 PM", tz)
        val fakeClock = FocusClock { fixedNow }

        val state = FocusDurationState(hours = 0, minutes = 2, clock = fakeClock)
        assertEquals(2, state.totalMinutes)

        val formattedEndsAt = state.formatEndsAt(nowMs = fixedNow, locale = Locale.US, timeZone = tz)
        // 4:04 PM + 0h 2m = 4:06 PM
        assertEquals("Ends at 4:06 PM", formattedEndsAt)
    }

    @Test
    fun testCase3_0Hours25Minutes_At_4_06_PM() {
        val tz = TimeZone.getTimeZone("UTC")
        val fixedNow = parseTimestamp("4:06 PM", tz)
        val fakeClock = FocusClock { fixedNow }

        val state = FocusDurationState(hours = 0, minutes = 25, clock = fakeClock)
        assertEquals(25, state.totalMinutes)

        val formattedEndsAt = state.formatEndsAt(nowMs = fixedNow, locale = Locale.US, timeZone = tz)
        // 4:06 PM + 0h 25m = 4:31 PM
        assertEquals("Ends at 4:31 PM", formattedEndsAt)
    }

    @Test
    fun testWithTotalMinutes_presetsConversion() {
        val state = FocusDurationState().withTotalMinutes(90)
        assertEquals(1, state.hours)
        assertEquals(30, state.minutes)
        assertEquals(90, state.totalMinutes)

        val state2 = FocusDurationState().withTotalMinutes(45)
        assertEquals(0, state2.hours)
        assertEquals(45, state2.minutes)
        assertEquals(45, state2.totalMinutes)
    }

    @Test
    fun testCoerceMinimum1Minute() {
        val state = FocusDurationState(hours = 0, minutes = 0)
        assertEquals(1, state.totalMinutes)
    }
}
