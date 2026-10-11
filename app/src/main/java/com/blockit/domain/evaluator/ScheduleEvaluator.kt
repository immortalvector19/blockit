package com.blockit.domain.evaluator

import com.blockit.domain.model.Schedule
import java.util.Calendar

object ScheduleEvaluator {

    /**
     * Determines whether [schedule] is active at the specified [dayOfWeek] and [minuteOfDay].
     *
     * @param dayOfWeek Calendar.MONDAY (2), TUESDAY (3), ..., SUNDAY (1) or ISO 1-7 (Mon=1, Sun=7)
     * @param minuteOfDay Minute of the day from 0 (00:00) to 1439 (23:59)
     */
    fun isScheduleActive(
        schedule: Schedule,
        isoDayOfWeek: Int, // 1 = Monday, 2 = Tuesday, ..., 7 = Sunday
        minuteOfDay: Int
    ): Boolean {
        if (!schedule.isEnabled) return false

        // Check day of week mask (Bit 0 = Mon, Bit 1 = Tue, ..., Bit 6 = Sun)
        val dayBit = 1 shl (isoDayOfWeek - 1)
        if ((schedule.daysOfWeekMask and dayBit) == 0) {
            // Note: If schedule spans overnight (e.g. Mon 22:00 to Tue 06:00),
            // early morning Tue might belong to Mon's schedule.
            // Check previous day overnight spillover:
            val prevIsoDay = if (isoDayOfWeek == 1) 7 else isoDayOfWeek - 1
            val prevDayBit = 1 shl (prevIsoDay - 1)
            val prevDayActive = (schedule.daysOfWeekMask and prevDayBit) != 0

            if (schedule.startMinuteOfDay > schedule.endMinuteOfDay && prevDayActive) {
                if (minuteOfDay < schedule.endMinuteOfDay) {
                    return true
                }
            }
            return false
        }

        // Intra-day schedule (e.g., 09:00 to 17:00)
        // End minute is exclusive: at 17:00:00 (minute 1020), the schedule has ended.
        if (schedule.startMinuteOfDay <= schedule.endMinuteOfDay) {
            return minuteOfDay in schedule.startMinuteOfDay until schedule.endMinuteOfDay
        }

        // Overnight schedule on current day (e.g., 22:00 to 06:00)
        // Active from startMinuteOfDay until midnight (1439)
        return minuteOfDay >= schedule.startMinuteOfDay
    }

    /**
     * Helper to evaluate a schedule against system wall-clock time.
     */
    fun isScheduleActiveNow(schedule: Schedule, calendar: Calendar = Calendar.getInstance()): Boolean {
        val calDay = calendar.get(Calendar.DAY_OF_WEEK)
        // Convert Calendar.DAY_OF_WEEK (Sunday=1, Monday=2) to ISO (Monday=1, Sunday=7)
        val isoDay = when (calDay) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
        val currentMinute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return isScheduleActive(schedule, isoDay, currentMinute)
    }

    /**
     * Calculates the exact end timestamp in milliseconds for an active schedule window.
     * Accurately resolves intra-day and overnight midnight transitions.
     */
    fun getScheduleEndTimestampMs(schedule: Schedule, calendar: Calendar = Calendar.getInstance()): Long {
        val currentMinute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val endCal = (calendar.clone() as Calendar).apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (schedule.startMinuteOfDay <= schedule.endMinuteOfDay) {
            endCal.set(Calendar.HOUR_OF_DAY, schedule.endMinuteOfDay / 60)
            endCal.set(Calendar.MINUTE, schedule.endMinuteOfDay % 60)
            return endCal.timeInMillis
        } else {
            if (currentMinute >= schedule.startMinuteOfDay) {
                endCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            endCal.set(Calendar.HOUR_OF_DAY, schedule.endMinuteOfDay / 60)
            endCal.set(Calendar.MINUTE, schedule.endMinuteOfDay % 60)
            return endCal.timeInMillis
        }
    }

    /**
     * Calculates the exact start timestamp in milliseconds for an active schedule window.
     */
    fun getScheduleStartTimestampMs(schedule: Schedule, calendar: Calendar = Calendar.getInstance()): Long {
        val currentMinute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startCal = (calendar.clone() as Calendar).apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (schedule.startMinuteOfDay <= schedule.endMinuteOfDay) {
            startCal.set(Calendar.HOUR_OF_DAY, schedule.startMinuteOfDay / 60)
            startCal.set(Calendar.MINUTE, schedule.startMinuteOfDay % 60)
            return startCal.timeInMillis
        } else {
            if (currentMinute < schedule.endMinuteOfDay) {
                startCal.add(Calendar.DAY_OF_YEAR, -1)
            }
            startCal.set(Calendar.HOUR_OF_DAY, schedule.startMinuteOfDay / 60)
            startCal.set(Calendar.MINUTE, schedule.startMinuteOfDay % 60)
            return startCal.timeInMillis
        }
    }
}
