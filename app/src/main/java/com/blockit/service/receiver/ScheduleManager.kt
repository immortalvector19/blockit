package com.blockit.service.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.blockit.data.local.db.BlockItDatabase
import com.blockit.domain.evaluator.ScheduleEvaluator
import com.blockit.domain.model.Schedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class ScheduleManager(
    private val context: Context,
    private val database: BlockItDatabase
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Re-calculates and schedules the next exact boundary alarm for active schedules and focus sessions.
     */
    suspend fun scheduleNextBoundaryAlarm() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }

        val activeSchedules = database.scheduleDao().getAllActiveSchedules()
        val activeSessions = database.focusSessionDao().getActiveSessions(now)

        var earliestBoundaryMs = Long.MAX_VALUE

        // 1. Check earliest focus session end time
        for (session in activeSessions) {
            if (session.endTimestamp > now && session.endTimestamp < earliestBoundaryMs) {
                earliestBoundaryMs = session.endTimestamp
            }
        }

        // 2. Check upcoming schedule transitions (start & end)
        val currentMinute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        for (schedule in activeSchedules) {
            val startMs = getNextTimestampForMinute(schedule.startMinuteOfDay, calendar)
            val endMs = getNextTimestampForMinute(schedule.endMinuteOfDay, calendar)

            if (startMs in (now + 1000) until earliestBoundaryMs) {
                earliestBoundaryMs = startMs
            }
            if (endMs in (now + 1000) until earliestBoundaryMs) {
                earliestBoundaryMs = endMs
            }
        }

        if (earliestBoundaryMs != Long.MAX_VALUE) {
            setExactAlarm(earliestBoundaryMs)
        }
    }

    private fun getNextTimestampForMinute(minuteOfDay: Int, currentCal: Calendar): Long {
        val targetCal = currentCal.clone() as Calendar
        val hour = minuteOfDay / 60
        val minute = minuteOfDay % 60
        targetCal.set(Calendar.HOUR_OF_DAY, hour)
        targetCal.set(Calendar.MINUTE, minute)
        targetCal.set(Calendar.SECOND, 0)
        targetCal.set(Calendar.MILLISECOND, 0)

        // If target time already passed today, advance to tomorrow
        if (targetCal.timeInMillis <= currentCal.timeInMillis) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return targetCal.timeInMillis
    }

    private fun setExactAlarm(triggerAtMillis: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SCHEDULE_BOUNDARY
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Permission SCHEDULE_EXACT_ALARM revoked or restricted on some OEM builds
        }
    }

    companion object {
        private const val ALARM_REQUEST_CODE = 4001
    }
}
