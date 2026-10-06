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
    val reason: String? = null,
    val isWhitelistBlock: Boolean = false,
    val allowedApps: List<String> = emptyList()
)

data class ActiveRuleSnapshot(
    val blockedPackages: Map<String, String> = emptyMap(), // packageName -> profileName
    val blockedDomains: Map<String, String> = emptyMap(),  // pattern -> profileName
    val hasActiveWhitelist: Boolean = false,
    val whitelistProfileName: String? = null,
    val whitelistPackages: Set<String> = emptySet()
)

/**
 * High-speed, lock-free rule evaluation engine.
 * Maintains an in-memory snapshot of active rules for zero-latency lookups on the UI and VPN threads.
 */
class RuleEvaluator(
    private val database: BlockItDatabase? = null,
    private val scope: CoroutineScope? = null,
    private val systemAppResolver: com.blockit.core.system.SystemAppResolver? = null
) {

    @Volatile
    private var cachedSnapshot: ActiveRuleSnapshot = ActiveRuleSnapshot()

    private val _snapshotFlow = kotlinx.coroutines.flow.MutableStateFlow(cachedSnapshot)
    val snapshotFlow: StateFlow<ActiveRuleSnapshot> = _snapshotFlow

    /**
     * Re-evaluates active rules from SQLite and updates the in-memory cache.
     * Called whenever profiles, apps, domains, schedules, or focus sessions change.
     */
    suspend fun refreshRules() = withContext(Dispatchers.IO) {
        val db = database ?: return@withContext
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // 1. Fetch enabled profiles
        val profiles = db.profileDao().getEnabledProfiles()
        if (profiles.isEmpty()) {
            cachedSnapshot = ActiveRuleSnapshot()
            _snapshotFlow.value = cachedSnapshot
            return@withContext
        }

        // 2. Fetch active focus sessions
        val activeSessions = db.focusSessionDao().getActiveSessions(now)
        val activeSessionProfileIds = activeSessions.map { it.profileId }.toSet()

        // 3. Fetch schedules for all enabled profiles
        val allSchedules = db.scheduleDao().getAllActiveSchedules()
        val schedulesByProfile = allSchedules.groupBy { it.profileId }

        val activeProfileIds = mutableSetOf<Long>()
        val profileMap = profiles.associateBy { it.id }

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
                    entity.isEnabled && ScheduleEvaluator.isScheduleActiveNow(
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
            }
            // Profiles without an active session or active schedule are idle (not blocking)
        }

        // 4. Aggregate Whitelist and Blacklist rules for active profiles
        val packageMap = mutableMapOf<String, String>()
        val domainMap = mutableMapOf<String, String>()
        val whitelistPackages = mutableSetOf<String>()
        var activeWhitelistName: String? = null
        var hasWhitelist = false

        for (profileId in activeProfileIds) {
            val profile = profileMap[profileId] ?: continue
            val profileName = profile.name

            val apps = db.blockedAppDao().getAppsForProfile(profileId)

            if (profile.blockMode == "WHITELIST") {
                hasWhitelist = true
                activeWhitelistName = profileName
                for (app in apps) {
                    whitelistPackages.add(app.packageName)
                }
            } else {
                for (app in apps) {
                    packageMap[app.packageName] = profileName
                }
                val domains = db.blockedDomainDao().getDomainsForProfile(profileId)
                for (domain in domains) {
                    domainMap[domain.pattern] = profileName
                }
            }
        }

        val newSnapshot = ActiveRuleSnapshot(
            blockedPackages = packageMap,
            blockedDomains = domainMap,
            hasActiveWhitelist = hasWhitelist,
            whitelistProfileName = activeWhitelistName,
            whitelistPackages = whitelistPackages
        )
        cachedSnapshot = newSnapshot
        _snapshotFlow.value = newSnapshot
    }

    /**
     * Zero-latency check if an Android package is currently blocked.
     */
    fun isAppBlocked(packageName: String): EvaluationResult {
        if (packageName.isBlank()) return EvaluationResult(isBlocked = false)

        // Essential System apps (launcher, dialer, keyboard, system UI, permission dialog, FocusShield) are NEVER blocked
        if (systemAppResolver?.isAlwaysPermitted(packageName) == true) {
            return EvaluationResult(isBlocked = false)
        }

        val snapshot = cachedSnapshot

        // Whitelist mode: if active, ONLY explicitly whitelisted apps are allowed
        if (snapshot.hasActiveWhitelist) {
            if (snapshot.whitelistPackages.contains(packageName)) {
                return EvaluationResult(isBlocked = false)
            }
            return EvaluationResult(
                isBlocked = true,
                matchedTarget = packageName,
                profileName = snapshot.whitelistProfileName ?: "Whitelist Mode",
                reason = "Only permitted apps can be opened during Whitelist Focus Mode",
                isWhitelistBlock = true,
                allowedApps = snapshot.whitelistPackages.toList()
            )
        }

        // Blacklist mode: check if package is on the blocklist
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

    fun setSnapshot(snapshot: ActiveRuleSnapshot) {
        cachedSnapshot = snapshot
        _snapshotFlow.value = snapshot
    }
}
