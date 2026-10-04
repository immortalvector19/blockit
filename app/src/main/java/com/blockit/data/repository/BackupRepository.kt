package com.blockit.data.repository

import androidx.room.withTransaction
import com.blockit.data.local.datastore.PreferenceManager
import com.blockit.data.local.db.BlockItDatabase
import com.blockit.data.local.db.entity.BlockedAppEntity
import com.blockit.data.local.db.entity.BlockedDomainEntity
import com.blockit.data.local.db.entity.ProfileEntity
import com.blockit.data.local.db.entity.ScheduleEntity
import com.blockit.domain.backup.BackupJsonSerializer
import com.blockit.domain.backup.BackupPayload
import com.blockit.domain.backup.BlockedAppBackup
import com.blockit.domain.backup.BlockedDomainBackup
import com.blockit.domain.backup.ProfileBackup
import com.blockit.domain.backup.ScheduleBackup
import com.blockit.domain.backup.StrictModeBackup
import com.blockit.domain.evaluator.RuleEvaluator
import com.blockit.domain.model.ChallengeType
import com.blockit.domain.model.StrictModeConfig
import com.blockit.service.receiver.ScheduleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class BackupRepository(
    private val database: BlockItDatabase,
    private val preferenceManager: PreferenceManager,
    private val ruleEvaluator: RuleEvaluator,
    private val scheduleManager: ScheduleManager
) {

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val profiles = database.profileDao().getAllProfiles()
        val profileBackups = profiles.map { profile ->
            val apps = database.blockedAppDao().getAppsForProfile(profile.id)
                .map { BlockedAppBackup(packageName = it.packageName, appName = it.appName) }
            val domains = database.blockedDomainDao().getDomainsForProfile(profile.id)
                .map { BlockedDomainBackup(pattern = it.pattern, isWildcard = it.isWildcard) }
            val schedules = database.scheduleDao().getSchedulesForProfile(profile.id)
                .map {
                    ScheduleBackup(
                        daysOfWeekMask = it.daysOfWeekMask,
                        startMinuteOfDay = it.startMinuteOfDay,
                        endMinuteOfDay = it.endMinuteOfDay,
                        isEnabled = it.isEnabled
                    )
                }

            ProfileBackup(
                name = profile.name,
                colorHex = profile.colorHex,
                iconKey = profile.iconKey,
                isEnabled = profile.isEnabled,
                blockedApps = apps,
                blockedDomains = domains,
                schedules = schedules
            )
        }

        val strictMode = preferenceManager.strictModeConfig.first()
        val strictModeBackup = StrictModeBackup(
            isEnabled = strictMode.isEnabled,
            challengeType = strictMode.challengeType.name,
            cooldownSeconds = strictMode.cooldownSeconds,
            targetPassage = strictMode.targetPassage,
            protectSettings = strictMode.protectSettings
        )

        val payload = BackupPayload(
            version = BackupJsonSerializer.CURRENT_VERSION,
            exportedAt = System.currentTimeMillis(),
            profiles = profileBackups,
            strictMode = strictModeBackup
        )

        BackupJsonSerializer.serialize(payload)
    }

    suspend fun restoreBackupJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val payload = BackupJsonSerializer.deserialize(jsonString)

            database.withTransaction {
                // Delete existing profiles; cascade deletes apps, domains, schedules
                database.profileDao().deleteAllProfiles()

                val now = System.currentTimeMillis()
                for (pBackup in payload.profiles) {
                    val profileEntity = ProfileEntity(
                        name = pBackup.name,
                        colorHex = pBackup.colorHex,
                        iconKey = pBackup.iconKey,
                        isEnabled = pBackup.isEnabled,
                        createdAt = now
                    )
                    val newProfileId = database.profileDao().insertProfile(profileEntity)

                    if (pBackup.blockedApps.isNotEmpty()) {
                        val appEntities = pBackup.blockedApps.map {
                            BlockedAppEntity(
                                profileId = newProfileId,
                                packageName = it.packageName,
                                appName = it.appName
                            )
                        }
                        database.blockedAppDao().insertApps(appEntities)
                    }

                    if (pBackup.blockedDomains.isNotEmpty()) {
                        val domainEntities = pBackup.blockedDomains.map {
                            BlockedDomainEntity(
                                profileId = newProfileId,
                                pattern = it.pattern,
                                isWildcard = it.isWildcard
                            )
                        }
                        database.blockedDomainDao().insertDomains(domainEntities)
                    }

                    if (pBackup.schedules.isNotEmpty()) {
                        val scheduleEntities = pBackup.schedules.map {
                            ScheduleEntity(
                                profileId = newProfileId,
                                daysOfWeekMask = it.daysOfWeekMask,
                                startMinuteOfDay = it.startMinuteOfDay,
                                endMinuteOfDay = it.endMinuteOfDay,
                                isEnabled = it.isEnabled
                            )
                        }
                        database.scheduleDao().insertSchedules(scheduleEntities)
                    }
                }
            }

            // Restore strict mode settings if present
            payload.strictMode?.let { sm ->
                val challengeType = try {
                    ChallengeType.valueOf(sm.challengeType)
                } catch (_: Exception) {
                    ChallengeType.COOLDOWN
                }
                preferenceManager.updateStrictMode(
                    StrictModeConfig(
                        isEnabled = sm.isEnabled,
                        cooldownSeconds = sm.cooldownSeconds,
                        challengeType = challengeType,
                        protectSettings = sm.protectSettings,
                        targetPassage = sm.targetPassage
                    )
                )
            }

            // Invalidate rules cache and refresh boundary alarms
            ruleEvaluator.refreshRules()
            scheduleManager.scheduleNextBoundaryAlarm()

            Result.success(payload.profiles.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
