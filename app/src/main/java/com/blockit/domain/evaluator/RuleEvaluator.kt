package com.blockit.domain.evaluator

import com.blockit.data.local.db.BlockItDatabase
import com.blockit.domain.matcher.DomainMatcher
import com.blockit.domain.model.Schedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.util.Calendar

data class EvaluationResult(
    val isBlocked: Boolean,
    val matchedTarget: String? = null,
    val profileName: String? = null,
    val reason: String? = null
)

data class ActiveRuleSnapshot(
    val blockedPackages: Map<String, String> = emptyMap(), // packageName -> profileName
    val blockedDomains: Map<String, String> = emptyMap()     // pattern -> profileName
)

/**
 * High-speed, lock-free rule evaluation engine.
 * Maintains an in-memory snapshot of active rules for zero-latency lookups on the UI and VPN threads.
 */
class RuleEvaluator(
    private val database: BlockItDatabase,
    private val scope: CoroutineScope
) {

    @Volatile
    private var cachedSnapshot: ActiveRuleSnapshot = ActiveRuleSnapshot()

    /**
     * Re-evaluates active rules from SQLite and updates the in-memory cache.
     * Called whenever profiles, apps, domains, schedules, or focus sessions change.
     */
    suspend fun refreshRules() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // 1. Fetch enabled profiles
        val profiles = database.profileDao().getEnabledProfiles()
        if (profiles.isEmpty()) {
            cachedSnapshot = ActiveRuleSnapshot()
            return@withContext
        }

        // 2. Fetch active focus sessions
        val activeSessions = database.focusSessionDao().getActiveSessions(now)
        val activeSessionProfileIds = activeSessions.map { it.profileId }.toSet()

        // 3. Fetch schedules for all enabled profiles
        val allSchedules = database.scheduleDao().getAllActiveSchedules()
        val schedulesByProfile = allSchedules.groupBy { it.profileId }

        val activeProfileIds = mutableSetOf<Long>()
        val profileNameMap = profiles.associate { it.id to it.name }

        for (profile in profiles) {
            // Profile is active if it has an active focus session
            if (activeSessionProfileIds.contains(profile.id)) {
                activeProfileIds.add(profile.id)
                continue
            }

            // Profile is active if any of its schedules are currently active
            val schedules = schedulesByProfile[profile.id]
            if (schedules != null && schedules.isNotEmpty()) {
                val hasMatchingSchedule = schedules.any { entity ->
                    ScheduleEvaluator.isScheduleActiveNow(
                        Schedule(
                            id = entity.id,
                            profileId = entity.profileId,
                            daysOfWeekMask = entity.daysOfWeekMask,
                            startMinuteOfDay = entity.startMinuteOfDay,
                            endMinuteOfDay = entity.endMinuteOfDay,
                            isEnabled = entity.isEnabled
                        ),
                        calendar
                    )
                }
                if (hasMatchingSchedule) {
                    activeProfileIds.add(profile.id)
                }
            } else {
                // If profile has no schedules defined, its isEnabled switch acts as always-on
                activeProfileIds.add(profile.id)
            }
        }

        // 4. Aggregate Union of blocked apps and blocked domains for active profiles
        val packageMap = mutableMapOf<String, String>()
        val domainMap = mutableMapOf<String, String>()

        for (profileId in activeProfileIds) {
            val profileName = profileNameMap[profileId] ?: "Blocked"

            val apps = database.blockedAppDao().getAppsForProfile(profileId)
            for (app in apps) {
                packageMap[app.packageName] = profileName
            }

            val domains = database.blockedDomainDao().getDomainsForProfile(profileId)
            for (domain in domains) {
                domainMap[domain.pattern] = profileName
            }
        }

        cachedSnapshot = ActiveRuleSnapshot(
            blockedPackages = packageMap,
            blockedDomains = domainMap
        )
    }

    /**
     * Zero-latency check if an Android package is currently blocked.
     */
    fun isAppBlocked(packageName: String): EvaluationResult {
        val snapshot = cachedSnapshot
        val profileName = snapshot.blockedPackages[packageName]
        return if (profileName != null) {
            EvaluationResult(
                isBlocked = true,
                matchedTarget = packageName,
                profileName = profileName,
                reason = "App blocked by $profileName"
            )
        } else {
            EvaluationResult(isBlocked = false)
        }
    }

    /**
     * Zero-latency check if a domain / URL is currently blocked.
     */
    fun isDomainBlocked(hostOrUrl: String): EvaluationResult {
        val cleanHost = DomainMatcher.extractCleanHost(hostOrUrl)
        if (cleanHost.isBlank()) return EvaluationResult(isBlocked = false)

        val snapshot = cachedSnapshot
        for ((pattern, profileName) in snapshot.blockedDomains) {
            if (DomainMatcher.matches(cleanHost, pattern)) {
                return EvaluationResult(
                    isBlocked = true,
                    matchedTarget = cleanHost,
                    profileName = profileName,
                    reason = "Website blocked by $profileName"
                )
            }
        }
        return EvaluationResult(isBlocked = false)
    }

    fun getSnapshot(): ActiveRuleSnapshot = cachedSnapshot
}
