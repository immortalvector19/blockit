package com.blockit.service.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.blockit.core.BlockItApplication
import java.util.concurrent.TimeUnit

class ServiceWatchdogWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val container = BlockItApplication.from(applicationContext)
            val now = System.currentTimeMillis()

            // 1. Clean up expired focus sessions
            container.database.focusSessionDao().cleanupExpiredSessions(now)

            // 2. Refresh rule evaluation engine
            container.ruleEvaluator.refreshRules()

            // 3. Ensure next boundary exact alarm is armed
            container.scheduleManager.scheduleNextBoundaryAlarm()

            // 4. Restore active session notification if ongoing
            container.focusSessionManager.restoreActiveSessionNotification()

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "blockit_service_watchdog"

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
