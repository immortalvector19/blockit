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
}
