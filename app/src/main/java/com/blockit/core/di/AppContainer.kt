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
        BlockItDatabase.create(context, applicationScope)
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

    val ruleEvaluator: RuleEvaluator by lazy {
        RuleEvaluator(database, applicationScope)
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

    val focusSessionManager: FocusSessionManager by lazy {
        FocusSessionManager(database, ruleEvaluator, scheduleManager)
    }

    val usageInsightsRepository: com.blockit.data.repository.UsageInsightsRepository by lazy {
        com.blockit.data.repository.UsageInsightsRepository(
            blockEventDao = database.blockEventDao(),
            installedAppsRepository = installedAppsRepository
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
        // Prime the rule evaluator cache and arm boundaries on startup
        applicationScope.launch {
            ruleEvaluator.refreshRules()
            scheduleManager.scheduleNextBoundaryAlarm()
        }
        // Enqueue periodic background watchdog
        ServiceWatchdogWorker.enqueuePeriodic(context)
    }
}
