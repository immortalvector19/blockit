package com.blockit.domain.evaluator

import com.blockit.domain.model.Schedule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleEvaluatorTest {

    @Test
    fun standardWorkdaySchedule_activeDuringWorkingHoursOnWeekdays() {
        // Mon-Fri (bits 1+2+4+8+16 = 31), 09:00 (540) to 17:00 (1020)
        val schedule = Schedule(
            profileId = 1,
            daysOfWeekMask = 31,
            startMinuteOfDay = 540,
            endMinuteOfDay = 1020,
            isEnabled = true
        )

        // Wednesday (ISO 3) at 12:30 (750) -> Active
        assertTrue(ScheduleEvaluator.isScheduleActive(schedule, isoDayOfWeek = 3, minuteOfDay = 750))

        // Exact boundary start: 09:00 (540) -> Active
        assertTrue(ScheduleEvaluator.isScheduleActive(schedule, isoDayOfWeek = 3, minuteOfDay = 540))

        // Exact boundary end: 17:00 (1020) -> Active
        assertTrue(ScheduleEvaluator.isScheduleActive(schedule, isoDayOfWeek = 3, minuteOfDay = 1020))

        // Before start: Wednesday 08:59 (539) -> Inactive
        assertFalse(ScheduleEvaluator.isScheduleActive(schedule, isoDayOfWeek = 3, minuteOfDay = 539))

        // After end: Wednesday 17:01 (1021) -> Inactive
        assertFalse(ScheduleEvaluator.isScheduleActive(schedule, isoDayOfWeek = 3, minuteOfDay = 1021))

        // Weekend: Saturday (ISO 6) at 12:00 (720) -> Inactive
        assertFalse(ScheduleEvaluator.isScheduleActive(schedule, isoDayOfWeek = 6, minuteOfDay = 720))
    }

    @Test
    fun overnightSchedule_handlesMidnightTransitionAndSpillover() {
        // Monday only (bit 1 = 1), 22:00 (1320) to 06:00 (360)
        val schedule = Schedule(
            profileId = 1,
            daysOfWeekMask = 1,
            startMinuteOfDay = 1320,
            endMinuteOfDay = 360,
            isEnabled = true
        )

        // Monday 23:00 (1380) -> Active
        assertTrue(ScheduleEvaluator.isScheduleActive(schedule, isoDayOfWeek = 1, minuteOfDay = 1380))

        // Tuesday 04:00 (240) -> Active (spillover from Monday night)
        assertTrue(ScheduleEvaluator.isScheduleActive(schedule, isoDayOfWeek = 2, minuteOfDay = 240))

        // Tuesday 06:30 (390) -> Inactive (past end time)
        assertFalse(ScheduleEvaluator.isScheduleActive(schedule, isoDayOfWeek = 2, minuteOfDay = 390))

        // Monday 15:00 (900) -> Inactive (before start time)
        assertFalse(ScheduleEvaluator.isScheduleActive(schedule, isoDayOfWeek = 1, minuteOfDay = 900))
    }

    @Test
    fun disabledSchedule_alwaysInactive() {
        val schedule = Schedule(
            profileId = 1,
            daysOfWeekMask = 127,
            startMinuteOfDay = 0,
            endMinuteOfDay = 1439,
            isEnabled = false
        )

        assertFalse(ScheduleEvaluator.isScheduleActive(schedule, isoDayOfWeek = 1, minuteOfDay = 500))
    }

    @Test
    fun eveningSchedule_730PM_retentionAndEvaluation() {
        // User sets 7:30 PM (19:30 -> 19 * 60 + 30 = 1170 minutes)
        val hour12 = 7
        val minute = 30
        val isPm = true
        val minuteOfDay = com.blockit.ui.components.toMinuteOfDay(hour12, minute, isPm)
        org.junit.Assert.assertEquals(1170, minuteOfDay)

        // Reopening / reading state from minuteOfDay must faithfully reproduce 7:30 PM
        val (reopenedHour12, reopenedMin, reopenedIsPm) = com.blockit.ui.components.fromMinuteOfDay(minuteOfDay)
        org.junit.Assert.assertEquals(7, reopenedHour12)
        org.junit.Assert.assertEquals(30, reopenedMin)
        org.junit.Assert.assertTrue(reopenedIsPm)

        val schedule = Schedule(
            profileId = 42,
            daysOfWeekMask = 127, // Daily
            startMinuteOfDay = 1170, // 7:30 PM
            endMinuteOfDay = 1320,  // 10:00 PM
            isEnabled = true
        )

        // 7:29 PM (1169) -> Not active
        assertFalse(ScheduleEvaluator.isScheduleActive(schedule, isoDayOfWeek = 1, minuteOfDay = 1169))
        // 7:30 PM (1170) -> Active
        assertTrue(ScheduleEvaluator.isScheduleActive(schedule, isoDayOfWeek = 1, minuteOfDay = 1170))
        // 8:45 PM (1245) -> Active
        assertTrue(ScheduleEvaluator.isScheduleActive(schedule, isoDayOfWeek = 1, minuteOfDay = 1245))
        // 10:00 PM (1320) -> Active
        assertTrue(ScheduleEvaluator.isScheduleActive(schedule, isoDayOfWeek = 1, minuteOfDay = 1320))
        // 10:01 PM (1321) -> Not active
        assertFalse(ScheduleEvaluator.isScheduleActive(schedule, isoDayOfWeek = 1, minuteOfDay = 1321))
    }
}
