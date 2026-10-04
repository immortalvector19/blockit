package com.blockit.core.di

import android.content.Context
import com.blockit.data.local.datastore.PreferenceManager
import com.blockit.data.local.db.BlockItDatabase
import com.blockit.data.repository.InstalledAppsRepository
import com.blockit.data.repository.ProfileRepository
import com.blockit.domain.evaluator.RuleEvaluator
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

    init {
        // Prime the rule evaluator cache on startup
        applicationScope.launch {
            ruleEvaluator.refreshRules()
        }
    }
}
