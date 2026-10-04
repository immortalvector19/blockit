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
                Profile(
                    id = item.id,
                    name = item.name,
                    colorHex = item.colorHex,
                    iconKey = item.iconKey,
                    isEnabled = item.isEnabled,
                    createdAt = item.createdAt,
                    appCount = item.appCount,
                    domainCount = item.domainCount
                )
            }
        }
    }

    suspend fun getProfileById(profileId: Long): Profile? {
        val entity = profileDao.getProfileById(profileId) ?: return null
        return Profile(
            id = entity.id,
            name = entity.name,
            colorHex = entity.colorHex,
            iconKey = entity.iconKey,
            isEnabled = entity.isEnabled,
            createdAt = entity.createdAt
        )
    }

    suspend fun createProfile(name: String, colorHex: Long, iconKey: String): Long {
        val entity = ProfileEntity(
            name = name.trim(),
            colorHex = colorHex,
            iconKey = iconKey,
            isEnabled = true,
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
                createdAt = profile.createdAt
            )
        )
    }

    suspend fun deleteProfile(profileId: Long) {
        val entity = profileDao.getProfileById(profileId) ?: return
        profileDao.deleteProfile(entity)
    }

    suspend fun setProfileEnabled(profileId: Long, isEnabled: Boolean) {
        profileDao.setProfileEnabled(profileId, isEnabled)
    }

    fun observeAppsForProfile(profileId: Long): Flow<List<BlockedApp>> {
        return blockedAppDao.observeAppsForProfile(profileId).map { entities ->
            entities.map { BlockedApp(it.id, it.profileId, it.packageName, it.appName) }
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
        blockedDomainDao.deleteDomain(
            BlockedDomainEntity(id = domainId, profileId = 0, pattern = "", isWildcard = false)
        )
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
