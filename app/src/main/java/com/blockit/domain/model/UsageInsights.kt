package com.blockit.domain.model

import com.blockit.data.local.db.entity.BlockEventEntity
import java.util.Calendar

data class TargetStatItem(
    val identifier: String,
    val displayName: String,
    val targetType: String, // "APP" or "WEBSITE"
    val count: Int
)

data class DailyStatItem(
    val dayLabel: String,
    val count: Int,
    val dateMs: Long
)

data class UsageInsightsData(
    val totalBlocked: Int = 0,
    val blockedToday: Int = 0,
    val blockedThisWeek: Int = 0,
    val totalFocusMinutes: Int = 0,
    val formattedFocusTime: String = "0m",
    val estimatedMinutesSaved: Int = 0,
    val formattedTimeSaved: String = "0m",
    val dailyStats: List<DailyStatItem> = emptyList(),
    val topTargets: List<TargetStatItem> = emptyList(),
    val recentEvents: List<BlockEventEntity> = emptyList()
)

object TimeSavedCalculator {
    const val DEFAULT_MINUTES_SAVED_PER_BLOCK = 5

    fun calculateMinutesSaved(
        blockedCount: Int,
        minutesPerAttempt: Int = DEFAULT_MINUTES_SAVED_PER_BLOCK
    ): Int {
        if (blockedCount <= 0) return 0
        return blockedCount * minutesPerAttempt
    }

    fun formatMinutes(totalMinutes: Int): String {
        if (totalMinutes <= 0) return "0m"
        val hours = totalMinutes / 60
        val remainingMinutes = totalMinutes % 60

        return when {
            hours == 0 -> "${remainingMinutes}m"
            remainingMinutes == 0 -> "${hours}h"
            else -> "${hours}h ${remainingMinutes}m"
        }
    }

    fun formatTimeSaved(totalMinutes: Int): String = formatMinutes(totalMinutes)

    fun getStartOfDayTimestamp(currentTimeMs: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getStartOfWeekTimestamp(currentTimeMs: Long = System.currentTimeMillis()): Long {
        return currentTimeMs - (7L * 24 * 60 * 60 * 1000)
    }
}
