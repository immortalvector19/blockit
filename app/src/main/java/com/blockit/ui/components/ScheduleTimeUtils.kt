package com.blockit.ui.components

import java.util.Calendar
import java.util.Locale

/**
 * Converts 12-hour AM/PM representation into 0..1439 minute-of-day.
 */
fun toMinuteOfDay(hour12: Int, minute: Int, isPm: Boolean): Int {
    val h24 = when {
        isPm && hour12 < 12 -> hour12 + 12
        !isPm && hour12 == 12 -> 0
        else -> hour12
    }
    return ((h24 * 60) + minute) % 1440
}

/**
 * Converts 0..1439 minute-of-day into Triple(hour12, minute, isPm).
 */
fun fromMinuteOfDay(minuteOfDay: Int): Triple<Int, Int, Boolean> {
    val sanitized = (minuteOfDay % 1440 + 1440) % 1440
    val h24 = sanitized / 60
    val m = sanitized % 60
    val isPm = h24 >= 12
    val h12 = when {
        h24 == 0 -> 12
        h24 > 12 -> h24 - 12
        else -> h24
    }
    return Triple(h12, m, isPm)
}

/**
 * Formats a 0..1439 minute-of-day into standard 12-hour AM/PM format (e.g. "9:00 AM", "5:00 PM").
 */
fun formatMinuteOfDayAmPm(minuteOfDay: Int): String {
    val (h12, m, isPm) = fromMinuteOfDay(minuteOfDay)
    return String.format(Locale.getDefault(), "%d:%02d %s", h12, m, if (isPm) "PM" else "AM")
}

/**
 * Returns true if the window spans overnight (start time is strictly greater than end time).
 */
fun isOvernightWindow(startMinute: Int, endMinute: Int): Boolean {
    return startMinute > endMinute
}

/**
 * Formats a 7-bit days mask into human-readable text.
 * Bit 0 = Mon, Bit 1 = Tue, Bit 2 = Wed, Bit 3 = Thu, Bit 4 = Fri, Bit 5 = Sat, Bit 6 = Sun.
 */
fun formatDaysSummary(mask: Int): String {
    if (mask == 127) return "Daily"
    if (mask == 31) return "Mon–Fri"
    if (mask == 96) return "Sat–Sun"
    if (mask == 0) return "No days"
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val selected = mutableListOf<String>()
    for (i in 0..6) {
        if ((mask and (1 shl i)) != 0) {
            selected.add(days[i])
        }
    }
    return selected.joinToString(", ")
}

/**
 * Calculates and formats when the schedule will next trigger.
 * Example outputs: "Today, 7:30 PM", "Tomorrow, 9:00 AM", "Mon, 9:00 AM"
 */
fun formatNextScheduleRun(
    mask: Int,
    startMinute: Int,
    currentCal: Calendar = Calendar.getInstance()
): String {
    if (mask == 0) return "No active days"

    val calDay = currentCal.get(Calendar.DAY_OF_WEEK)
    val currentIsoDay = when (calDay) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }
    val currentMinute = currentCal.get(Calendar.HOUR_OF_DAY) * 60 + currentCal.get(Calendar.MINUTE)
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    for (offset in 0..6) {
        val checkIsoDay = ((currentIsoDay - 1 + offset) % 7) + 1
        val bit = 1 shl (checkIsoDay - 1)
        if ((mask and bit) != 0) {
            if (offset == 0) {
                if (startMinute > currentMinute) {
                    return "Today, ${formatMinuteOfDayAmPm(startMinute)}"
                }
            } else if (offset == 1) {
                return "Tomorrow, ${formatMinuteOfDayAmPm(startMinute)}"
            } else {
                return "${dayNames[checkIsoDay - 1]}, ${formatMinuteOfDayAmPm(startMinute)}"
            }
        }
    }

    return "Next: ${dayNames[currentIsoDay - 1]}, ${formatMinuteOfDayAmPm(startMinute)}"
}
