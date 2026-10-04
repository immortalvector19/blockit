package com.blockit.data.repository

import com.blockit.data.local.db.dao.BlockEventDao
import com.blockit.domain.model.TargetStatItem
import com.blockit.domain.model.TimeSavedCalculator
import com.blockit.domain.model.UsageInsightsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class UsageInsightsRepository(
    private val blockEventDao: BlockEventDao,
    private val installedAppsRepository: InstalledAppsRepository
) {

    fun observeInsights(): Flow<UsageInsightsData> {
        val now = System.currentTimeMillis()
        val startOfDay = TimeSavedCalculator.getStartOfDayTimestamp(now)
        val startOfWeek = TimeSavedCalculator.getStartOfWeekTimestamp(now)

        return combine(
            blockEventDao.observeTotalBlockedCount(),
            blockEventDao.observeBlockCountSince(startOfDay),
            blockEventDao.observeBlockCountSince(startOfWeek),
            blockEventDao.observeTopBlockedTargets(10),
            blockEventDao.observeRecentEvents(50)
        ) { total, today, week, topTargets, recentEvents ->
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
                estimatedMinutesSaved = minutesSaved,
                formattedTimeSaved = formattedTime,
                topTargets = resolvedTopTargets,
                recentEvents = recentEvents
            )
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        blockEventDao.clearAllEvents()
    }
}
