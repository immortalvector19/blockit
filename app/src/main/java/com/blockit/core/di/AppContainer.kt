package com.blockit.core.di

import android.content.Context
import com.blockit.data.local.datastore.PreferenceManager
import com.blockit.data.local.db.BlockItDatabase
import com.blockit.data.repository.FocusSessionManager
import com.blockit.data.repository.InstalledAppsRepository
import com.blockit.data.repository.ProfileRepository
import com.blockit.domain.evaluator.RuleEvaluator
import com.blockit.service.overlay.OverlayController
import com.blockit.service.receiver.ScheduleManager
import com.blockit.service.usagestats.UsageStatsDetector
import com.blockit.service.worker.ServiceWatchdogWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: BlockItDatabase by lazy {
        BlockItDatabase.create(context)
    }

    val preferenceManager: PreferenceManager by lazy {
        PreferenceManager(context)
    }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepository(
            profileDao = database.profileDao(),
            blockedAppDao = database.blockedAppDao(),
            blockedDomainDao = database.blockedDomainDao(),
            scheduleDao = database.scheduleDao()
        )
    }

    val installedAppsRepository: InstalledAppsRepository by lazy {
        InstalledAppsRepository(context)
    }

    val systemAppResolver: com.blockit.core.system.SystemAppResolver by lazy {
        com.blockit.core.system.AndroidSystemAppResolver(context)
    }

    val ruleEvaluator: RuleEvaluator by lazy {
        RuleEvaluator(database, applicationScope, systemAppResolver)
    }

    val overlayController: OverlayController by lazy {
        OverlayController(context)
    }

    val usageStatsDetector: UsageStatsDetector by lazy {
        UsageStatsDetector(context)
    }

    val scheduleManager: ScheduleManager by lazy {
        ScheduleManager(context, database)
    }

    val focusNotificationManager: com.blockit.service.notification.FocusNotificationManager by lazy {
        com.blockit.service.notification.FocusNotificationManager(context)
    }

    val intentPassManager: com.blockit.core.intentgate.IntentPassManager by lazy {
        com.blockit.core.intentgate.IntentPassManager()
    }

    val focusSessionManager: FocusSessionManager by lazy {
        FocusSessionManager(database, ruleEvaluator, scheduleManager, focusNotificationManager, context)
    }

    val usageInsightsRepository: com.blockit.data.repository.UsageInsightsRepository by lazy {
        com.blockit.data.repository.UsageInsightsRepository(
            blockEventDao = database.blockEventDao(),
            installedAppsRepository = installedAppsRepository,
            focusSessionDao = database.focusSessionDao()
        )
    }

    val backupRepository: com.blockit.data.repository.BackupRepository by lazy {
        com.blockit.data.repository.BackupRepository(
            database = database,
            preferenceManager = preferenceManager,
            ruleEvaluator = ruleEvaluator,
            scheduleManager = scheduleManager
        )
    }

    init {
        // Connect intentPassManager break lifecycle to active focus sessions:
        // 1. When a break starts, immediately extend session end time in SQLite by the break duration
        //    so the session never expires or cleans up during the break!
        intentPassManager.setOnBreakStartedListener { durationMinutes ->
            val extensionMs = durationMinutes * 60 * 1000L
            applicationScope.launch {
                focusSessionManager.extendActiveSession(extensionMs)
            }
        }

        // 2. If user ends the break early (resumes), adjust back any unused break time
        intentPassManager.setOnBreakEndedEarlyListener { unusedBreakDurationMs ->
            if (unusedBreakDurationMs > 0L) {
                applicationScope.launch {
                    focusSessionManager.extendActiveSession(-unusedBreakDurationMs)
                }
            }
        }

        // Prime the rule evaluator cache and arm boundaries on startup
        applicationScope.launch {
            ruleEvaluator.refreshRules()
            scheduleManager.scheduleNextBoundaryAlarm()
            focusSessionManager.restoreActiveSessionNotification()
        }
        // Enqueue periodic background watchdog
        ServiceWatchdogWorker.enqueuePeriodic(context)
    }
}
