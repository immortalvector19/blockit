package com.blockit.data.repository

import com.blockit.data.local.db.dao.BlockedAppDao
import com.blockit.data.local.db.dao.BlockedDomainDao
import com.blockit.data.local.db.dao.ProfileDao
import com.blockit.data.local.db.dao.ScheduleDao
import com.blockit.data.local.db.entity.BlockedAppEntity
import com.blockit.data.local.db.entity.BlockedDomainEntity
import com.blockit.data.local.db.entity.ProfileEntity
import com.blockit.data.local.db.entity.ScheduleEntity
import com.blockit.domain.model.BlockedApp
import com.blockit.domain.model.BlockedDomain
import com.blockit.domain.model.InstalledAppInfo
import com.blockit.domain.model.Profile
import com.blockit.domain.model.Schedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(
    private val profileDao: ProfileDao,
    private val blockedAppDao: BlockedAppDao,
    private val blockedDomainDao: BlockedDomainDao,
    private val scheduleDao: ScheduleDao
) {

    fun observeAllProfiles(): Flow<List<Profile>> {
        return profileDao.observeAllProfiles().map { list ->
            list.map { item ->
                val pType = if (item.profileType == "SCHEDULE") com.blockit.domain.model.ProfileType.SCHEDULE else com.blockit.domain.model.ProfileType.FOCUS
                val lType = if (item.listType == "ALLOWLIST" || item.blockMode == "WHITELIST") com.blockit.domain.model.ListType.ALLOWLIST else com.blockit.domain.model.ListType.BLOCKLIST
                Profile(
                    id = item.id,
                    name = item.name,
                    colorHex = item.colorHex,
                    iconKey = item.iconKey,
                    isEnabled = item.isEnabled,
                    profileType = pType,
                    listType = lType,
                    scheduleDaysMask = item.scheduleDaysMask,
                    scheduleStartMinute = item.scheduleStartMinute,
                    scheduleEndMinute = item.scheduleEndMinute,
                    blockMode = if (lType == com.blockit.domain.model.ListType.ALLOWLIST) com.blockit.domain.model.BlockMode.WHITELIST else com.blockit.domain.model.BlockMode.BLACKLIST,
                    createdAt = item.createdAt,
                    appCount = item.appCount,
                    domainCount = item.domainCount
                )
            }
        }
    }

    suspend fun getProfileById(profileId: Long): Profile? {
        val entity = profileDao.getProfileById(profileId) ?: return null
        val pType = if (entity.profileType == "SCHEDULE") com.blockit.domain.model.ProfileType.SCHEDULE else com.blockit.domain.model.ProfileType.FOCUS
        val lType = if (entity.listType == "ALLOWLIST" || entity.blockMode == "WHITELIST") com.blockit.domain.model.ListType.ALLOWLIST else com.blockit.domain.model.ListType.BLOCKLIST
        return Profile(
            id = entity.id,
            name = entity.name,
            colorHex = entity.colorHex,
            iconKey = entity.iconKey,
            isEnabled = entity.isEnabled,
            profileType = pType,
            listType = lType,
            scheduleDaysMask = entity.scheduleDaysMask,
            scheduleStartMinute = entity.scheduleStartMinute,
            scheduleEndMinute = entity.scheduleEndMinute,
            blockMode = if (lType == com.blockit.domain.model.ListType.ALLOWLIST) com.blockit.domain.model.BlockMode.WHITELIST else com.blockit.domain.model.BlockMode.BLACKLIST,
            createdAt = entity.createdAt
        )
    }

    suspend fun getAllProfiles(): List<Profile> {
        return profileDao.getEnabledProfiles().map { entity ->
            val pType = if (entity.profileType == "SCHEDULE") com.blockit.domain.model.ProfileType.SCHEDULE else com.blockit.domain.model.ProfileType.FOCUS
            val lType = if (entity.listType == "ALLOWLIST" || entity.blockMode == "WHITELIST") com.blockit.domain.model.ListType.ALLOWLIST else com.blockit.domain.model.ListType.BLOCKLIST
            Profile(
                id = entity.id,
                name = entity.name,
                colorHex = entity.colorHex,
                iconKey = entity.iconKey,
                isEnabled = entity.isEnabled,
                profileType = pType,
                listType = lType,
                scheduleDaysMask = entity.scheduleDaysMask,
                scheduleStartMinute = entity.scheduleStartMinute,
                scheduleEndMinute = entity.scheduleEndMinute,
                blockMode = if (lType == com.blockit.domain.model.ListType.ALLOWLIST) com.blockit.domain.model.BlockMode.WHITELIST else com.blockit.domain.model.BlockMode.BLACKLIST,
                createdAt = entity.createdAt
            )
        }
    }

    suspend fun createProfile(
        name: String,
        colorHex: Long = 0xFF2563EB,
        iconKey: String = "focus",
        profileType: com.blockit.domain.model.ProfileType = com.blockit.domain.model.ProfileType.FOCUS,
        listType: com.blockit.domain.model.ListType = com.blockit.domain.model.ListType.BLOCKLIST,
        daysMask: Int = 127,
        startMinute: Int = 540,
        endMinute: Int = 1020,
        blockMode: com.blockit.domain.model.BlockMode = if (listType == com.blockit.domain.model.ListType.ALLOWLIST) com.blockit.domain.model.BlockMode.WHITELIST else com.blockit.domain.model.BlockMode.BLACKLIST
    ): Long {
        val effectiveListType = if (blockMode == com.blockit.domain.model.BlockMode.WHITELIST) com.blockit.domain.model.ListType.ALLOWLIST else listType
        val entity = ProfileEntity(
            name = name.trim(),
            colorHex = colorHex,
            iconKey = iconKey,
            isEnabled = true,
            blockMode = if (effectiveListType == com.blockit.domain.model.ListType.ALLOWLIST) "WHITELIST" else "BLACKLIST",
            profileType = profileType.name,
            listType = effectiveListType.name,
            scheduleDaysMask = daysMask,
            scheduleStartMinute = startMinute,
            scheduleEndMinute = endMinute,
            createdAt = System.currentTimeMillis()
        )
        return profileDao.insertProfile(entity)
    }

    suspend fun updateProfile(profile: Profile) {
        profileDao.updateProfile(
            ProfileEntity(
                id = profile.id,
                name = profile.name,
                colorHex = profile.colorHex,
                iconKey = profile.iconKey,
                isEnabled = profile.isEnabled,
                blockMode = if (profile.listType == com.blockit.domain.model.ListType.ALLOWLIST) "WHITELIST" else "BLACKLIST",
                profileType = profile.profileType.name,
                listType = profile.listType.name,
                scheduleDaysMask = profile.scheduleDaysMask,
                scheduleStartMinute = profile.scheduleStartMinute,
                scheduleEndMinute = profile.scheduleEndMinute,
                createdAt = profile.createdAt
            )
        )
    }

    suspend fun updateProfileSchedule(profileId: Long, daysMask: Int, startMin: Int, endMin: Int) {
        profileDao.updateProfileSchedule(profileId, daysMask, startMin, endMin)
    }

    suspend fun deleteProfile(profileId: Long) {
        val entity = profileDao.getProfileById(profileId) ?: return
        profileDao.deleteProfile(entity)
    }

    suspend fun setProfileEnabled(profileId: Long, isEnabled: Boolean) {
        profileDao.setProfileEnabled(profileId, isEnabled)
    }

    suspend fun setProfileMode(profileId: Long, blockMode: com.blockit.domain.model.BlockMode) {
        val listType = if (blockMode == com.blockit.domain.model.BlockMode.WHITELIST) "ALLOWLIST" else "BLOCKLIST"
        profileDao.setProfileMode(profileId, blockMode.name, listType)
    }

    suspend fun duplicateProfile(profileId: Long): Long {
        val original = profileDao.getProfileById(profileId) ?: return 0L
        val copyEntity = ProfileEntity(
            name = "${original.name} (Copy)",
            colorHex = original.colorHex,
            iconKey = original.iconKey,
            isEnabled = true,
            blockMode = original.blockMode,
            createdAt = System.currentTimeMillis()
        )
        val newId = profileDao.insertProfile(copyEntity)
        val apps = blockedAppDao.getAppsForProfile(profileId).map {
            BlockedAppEntity(profileId = newId, packageName = it.packageName, appName = it.appName)
        }
        if (apps.isNotEmpty()) {
            blockedAppDao.insertApps(apps)
        }
        val domains = blockedDomainDao.getDomainsForProfile(profileId).map {
            BlockedDomainEntity(profileId = newId, pattern = it.pattern, isWildcard = it.isWildcard)
        }
        if (domains.isNotEmpty()) {
            blockedDomainDao.insertDomains(domains)
        }
        return newId
    }

    suspend fun createTemplateProfile(templateType: String): Long {
        return when (templateType.uppercase()) {
            "WORK" -> {
                val id = createProfile(
                    name = "Work Hours",
                    colorHex = 0xFF2563EB,
                    iconKey = "work",
                    blockMode = com.blockit.domain.model.BlockMode.BLACKLIST
                )
                addDomainToProfile(id, "instagram.com", false)
                addDomainToProfile(id, "tiktok.com", false)
                addDomainToProfile(id, "twitter.com", false)
                addDomainToProfile(id, "reddit.com", false)
                id
            }
            "SLEEP" -> {
                val id = createProfile(
                    name = "Sleep & Rest",
                    colorHex = 0xFF7C3AED,
                    iconKey = "night",
                    blockMode = com.blockit.domain.model.BlockMode.BLACKLIST
                )
                addDomainToProfile(id, "youtube.com", false)
                addDomainToProfile(id, "netflix.com", false)
                addDomainToProfile(id, "twitch.tv", false)
                id
            }
            "STUDY", "DEEP_STUDY", "FOCUS" -> {
                createProfile(
                    name = "Deep Study",
                    colorHex = 0xFF2563EB,
                    iconKey = "focus",
                    blockMode = com.blockit.domain.model.BlockMode.WHITELIST
                )
            }
            else -> {
                createProfile(
                    name = "Deep Focus",
                    colorHex = 0xFF2563EB,
                    iconKey = "focus",
                    blockMode = com.blockit.domain.model.BlockMode.BLACKLIST
                )
            }
        }
    }

    fun observeAllBlockedApps(): Flow<List<BlockedApp>> {
        return blockedAppDao.observeAllBlockedApps().map { entities ->
            entities.map { BlockedApp(it.id, it.profileId, it.packageName, it.appName) }
        }
    }

    fun observeAppsForProfile(profileId: Long): Flow<List<BlockedApp>> {
        return blockedAppDao.observeAppsForProfile(profileId).map { entities ->
            entities.map { BlockedApp(it.id, it.profileId, it.packageName, it.appName) }
        }
    }

    suspend fun getAppsForProfile(profileId: Long): List<BlockedApp> {
        return blockedAppDao.getAppsForProfile(profileId).map {
            BlockedApp(it.id, it.profileId, it.packageName, it.appName)
        }
    }

    suspend fun syncAppsForProfile(profileId: Long, apps: List<InstalledAppInfo>) {
        blockedAppDao.deleteAppsForProfile(profileId)
        if (apps.isNotEmpty()) {
            val entities = apps.map {
                BlockedAppEntity(
                    profileId = profileId,
                    packageName = it.packageName,
                    appName = it.appName
                )
            }
            blockedAppDao.insertApps(entities)
        }
    }

    suspend fun addAppsToProfile(profileId: Long, apps: List<InstalledAppInfo>) {
        val entities = apps.map {
            BlockedAppEntity(
                profileId = profileId,
                packageName = it.packageName,
                appName = it.appName
            )
        }
        blockedAppDao.insertApps(entities)
    }

    suspend fun removeAppFromProfile(profileId: Long, packageName: String) {
        blockedAppDao.deleteAppByPackage(profileId, packageName)
    }

    fun observeDomainsForProfile(profileId: Long): Flow<List<BlockedDomain>> {
        return blockedDomainDao.observeDomainsForProfile(profileId).map { entities ->
            entities.map { BlockedDomain(it.id, it.profileId, it.pattern, it.isWildcard) }
        }
    }

    suspend fun addDomainToProfile(profileId: Long, pattern: String, isWildcard: Boolean): Long {
        return blockedDomainDao.insertDomain(
            BlockedDomainEntity(
                profileId = profileId,
                pattern = pattern.trim().lowercase(),
                isWildcard = isWildcard
            )
        )
    }

    suspend fun removeDomainFromProfile(domainId: Long) {
        blockedDomainDao.deleteDomainById(domainId)
    }

    fun observeAllSchedules(): Flow<List<Schedule>> {
        return scheduleDao.observeAllSchedules().map { entities ->
            entities.map {
                Schedule(
                    id = it.id,
                    profileId = it.profileId,
                    daysOfWeekMask = it.daysOfWeekMask,
                    startMinuteOfDay = it.startMinuteOfDay,
                    endMinuteOfDay = it.endMinuteOfDay,
                    isEnabled = it.isEnabled
                )
            }
        }
    }

    fun observeSchedulesForProfile(profileId: Long): Flow<List<Schedule>> {
        return scheduleDao.observeSchedulesForProfile(profileId).map { entities ->
            entities.map {
                Schedule(
                    id = it.id,
                    profileId = it.profileId,
                    daysOfWeekMask = it.daysOfWeekMask,
                    startMinuteOfDay = it.startMinuteOfDay,
                    endMinuteOfDay = it.endMinuteOfDay,
                    isEnabled = it.isEnabled
                )
            }
        }
    }

    suspend fun saveSchedule(schedule: Schedule): Long {
        val entity = ScheduleEntity(
            id = schedule.id,
            profileId = schedule.profileId,
            daysOfWeekMask = schedule.daysOfWeekMask,
            startMinuteOfDay = schedule.startMinuteOfDay,
            endMinuteOfDay = schedule.endMinuteOfDay,
            isEnabled = schedule.isEnabled
        )
        return scheduleDao.insertSchedule(entity)
    }

    suspend fun deleteSchedule(scheduleId: Long) {
        scheduleDao.deleteSchedule(
            ScheduleEntity(id = scheduleId, profileId = 0, daysOfWeekMask = 0, startMinuteOfDay = 0, endMinuteOfDay = 0, isEnabled = false)
        )
    }
}
