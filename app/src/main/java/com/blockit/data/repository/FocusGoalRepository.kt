package com.blockit.data.repository

import com.blockit.data.local.db.dao.FocusGoalDao
import com.blockit.data.local.db.entity.FocusGoalEntity
import com.blockit.domain.model.FocusGoal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayOption(
    val isoDate: String,
    val dayOfWeek: String,
    val dayOfMonth: String,
    val label: String,
    val isToday: Boolean
)

class FocusGoalRepository(
    private val focusGoalDao: FocusGoalDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        fun formatDate(date: Date): String = synchronized(ISO_DATE_FORMAT) {
            ISO_DATE_FORMAT.format(date)
        }

        fun getTodayIsoDate(): String {
            return formatDate(Date())
        }

        fun getTomorrowIsoDate(): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 1)
            return formatDate(cal.time)
        }

        fun getRelativeDateLabel(isoDate: String): String {
            val today = getTodayIsoDate()
            val tomorrow = getTomorrowIsoDate()
            return when (isoDate) {
                today -> "Today"
                tomorrow -> "Tomorrow"
                else -> {
                    try {
                        val parsed = synchronized(ISO_DATE_FORMAT) { ISO_DATE_FORMAT.parse(isoDate) }
                        if (parsed != null) {
                            SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(parsed)
                        } else isoDate
                    } catch (_: Exception) {
                        isoDate
                    }
                }
            }
        }

        fun getDayOptions(daysBack: Int = 1, daysForward: Int = 6): List<DayOption> {
            val result = mutableListOf<DayOption>()
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -daysBack)
            val todayIso = getTodayIsoDate()
            val tomorrowIso = getTomorrowIsoDate()

            val totalDays = daysBack + 1 + daysForward
            val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dowFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val domFormat = SimpleDateFormat("d", Locale.getDefault())

            for (i in 0 until totalDays) {
                val iso = isoFormat.format(cal.time)
                val dow = dowFormat.format(cal.time)
                val dom = domFormat.format(cal.time)
                val label = when (iso) {
                    todayIso -> "Today"
                    tomorrowIso -> "Tomorrow"
                    else -> "$dow $dom"
                }
                result.add(
                    DayOption(
                        isoDate = iso,
                        dayOfWeek = dow,
                        dayOfMonth = dom,
                        label = label,
                        isToday = (iso == todayIso)
                    )
                )
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return result
        }
    }

    fun observeGoalsForDate(targetDate: String): Flow<List<FocusGoal>> {
        return focusGoalDao.observeGoalsForDate(targetDate).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeTodayGoals(): Flow<List<FocusGoal>> {
        return observeGoalsForDate(getTodayIsoDate())
    }

    fun observeGoalsForSession(sessionId: Long): Flow<List<FocusGoal>> {
        return focusGoalDao.observeGoalsForSession(sessionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getGoalsForDate(targetDate: String): List<FocusGoal> = withContext(ioDispatcher) {
        focusGoalDao.getGoalsForDate(targetDate).map { it.toDomain() }
    }

    suspend fun addGoal(
        title: String,
        targetDate: String = getTodayIsoDate(),
        sessionId: Long? = null
    ): Long = withContext(ioDispatcher) {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return@withContext -1L
        val existing = focusGoalDao.getGoalsForDate(targetDate)
        val entity = FocusGoalEntity(
            title = trimmed,
            targetDate = targetDate,
            sessionId = sessionId,
            isCompleted = false,
            orderIndex = existing.size
        )
        focusGoalDao.insertGoal(entity)
    }

    suspend fun toggleGoalCompletion(id: Long, isCompleted: Boolean) = withContext(ioDispatcher) {
        val completedAtMs = if (isCompleted) System.currentTimeMillis() else null
        focusGoalDao.setGoalCompletion(id, isCompleted, completedAtMs)
    }

    suspend fun linkGoalToSession(id: Long, sessionId: Long) = withContext(ioDispatcher) {
        focusGoalDao.linkGoalToSession(id, sessionId)
    }

    suspend fun deleteGoal(id: Long) = withContext(ioDispatcher) {
        focusGoalDao.deleteGoal(id)
    }

    suspend fun linkGoalsToSession(goalIds: List<Long>, sessionId: Long) = withContext(ioDispatcher) {
        goalIds.forEach { id ->
            focusGoalDao.linkGoalToSession(id, sessionId)
        }
    }

    private fun FocusGoalEntity.toDomain(): FocusGoal {
        return FocusGoal(
            id = id,
            title = title,
            targetDate = targetDate,
            sessionId = sessionId,
            isCompleted = isCompleted,
            completedAtMs = completedAtMs,
            createdAtMs = createdAtMs,
            orderIndex = orderIndex
        )
    }
}
