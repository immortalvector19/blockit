package com.blockit.data.repository

import com.blockit.data.local.db.dao.BlockEventDao
import com.blockit.data.local.db.dao.FocusSessionDao
import com.blockit.data.local.db.entity.BlockEventEntity
import com.blockit.data.local.db.entity.FocusSessionEntity
import com.blockit.domain.model.DailyStatItem
import com.blockit.domain.model.TargetStatItem
import com.blockit.domain.model.TimeSavedCalculator
import com.blockit.domain.model.UsageInsightsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UsageInsightsRepository(
    private val blockEventDao: BlockEventDao,
    private val installedAppsRepository: InstalledAppsRepository,
    private val focusSessionDao: FocusSessionDao? = null
) {

    fun observeInsights(): Flow<UsageInsightsData> {
        val now = System.currentTimeMillis()
        val startOfDay = TimeSavedCalculator.getStartOfDayTimestamp(now)
        val startOfWeek = TimeSavedCalculator.getStartOfWeekTimestamp(now)

        val oneYearAgo = now - (365L * 24 * 60 * 60 * 1000L)

        val sessionsFlow: Flow<List<FocusSessionEntity>> = focusSessionDao?.observeAllSessions() ?: flowOf(emptyList())

        val countsFlow = combine(
            blockEventDao.observeTotalBlockedCount(),
            blockEventDao.observeBlockCountSince(startOfDay),
            blockEventDao.observeBlockCountSince(startOfWeek)
        ) { total, today, week -> Triple(total, today, week) }

        return combine(
            countsFlow,
            blockEventDao.observeEventsSince(oneYearAgo),
            blockEventDao.observeTopBlockedTargets(10),
            sessionsFlow
        ) { counts, yearEvents, topTargets, sessions ->
            val currentTime = System.currentTimeMillis()
            val total = counts.first
            val today = counts.second
            val week = counts.third
            // Calculate real focus minutes from stored sessions
            val totalFocusMs = sessions.sumOf { sess ->
                val end = if (sess.isActive) currentTime.coerceAtMost(sess.endTimestamp) else sess.endTimestamp
                (end - sess.startTimestamp).coerceAtLeast(0L)
            }
            val totalFocusMinutes = (totalFocusMs / 1000 / 60).toInt()
            val formattedFocusTime = TimeSavedCalculator.formatMinutes(totalFocusMinutes)

            // Calculate real 7-day daily histogram
            val dailyStats = computeDailyStats(yearEvents, currentTime)

            val minutesSaved = TimeSavedCalculator.calculateMinutesSaved(total)
            val formattedTime = TimeSavedCalculator.formatTimeSaved(minutesSaved)

            val resolvedTopTargets = topTargets.map { stat ->
                val displayName = if (stat.targetType == "APP") {
                    installedAppsRepository.resolveAppName(stat.identifier)
                } else {
                    stat.identifier
                }
                TargetStatItem(
                    identifier = stat.identifier,
                    displayName = displayName,
                    targetType = stat.targetType,
                    count = stat.count
                )
            }

            UsageInsightsData(
                totalBlocked = total,
                blockedToday = today,
                blockedThisWeek = week,
                totalFocusMinutes = totalFocusMinutes,
                formattedFocusTime = formattedFocusTime,
                estimatedMinutesSaved = minutesSaved,
                formattedTimeSaved = formattedTime,
                dailyStats = dailyStats,
                topTargets = resolvedTopTargets,
                recentEvents = yearEvents.reversed()
            )
        }
    }

    private fun computeDailyStats(events: List<BlockEventEntity>, now: Long): List<DailyStatItem> {
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val result = mutableListOf<DailyStatItem>()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Generate past 7 days from oldest to today
        val dayBounds = mutableListOf<Triple<String, Long, Long>>()
        for (i in 6 downTo 0) {
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
            dayBounds.add(Triple(label, startMs, endMs))
        }

        for ((label, startMs, endMs) in dayBounds) {
            val count = events.count { it.timestamp in startMs until endMs }
            result.add(DailyStatItem(dayLabel = label, count = count, dateMs = startMs))
        }

        return result
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        blockEventDao.clearAllEvents()
    }
}
