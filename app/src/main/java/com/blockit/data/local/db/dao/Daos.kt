package com.blockit.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.blockit.data.local.db.entity.BlockEventEntity
import com.blockit.data.local.db.entity.BlockedAppEntity
import com.blockit.data.local.db.entity.BlockedDomainEntity
import com.blockit.data.local.db.entity.FocusSessionEntity
import com.blockit.data.local.db.entity.ProfileEntity
import com.blockit.data.local.db.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

data class ProfileWithCounts(
    val id: Long,
    val name: String,
    val colorHex: Long,
    val iconKey: String,
    val isEnabled: Boolean,
    val createdAt: Long,
    val appCount: Int,
    val domainCount: Int
)

@Dao
interface ProfileDao {
    @Query("""
        SELECT p.*, 
        (SELECT COUNT(*) FROM blocked_apps WHERE profileId = p.id) AS appCount,
        (SELECT COUNT(*) FROM blocked_domains WHERE profileId = p.id) AS domainCount
        FROM profiles p ORDER BY p.createdAt ASC
    """)
    fun observeAllProfiles(): Flow<List<ProfileWithCounts>>

    @Query("SELECT * FROM profiles WHERE id = :profileId")
    suspend fun getProfileById(profileId: Long): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE isEnabled = 1")
    suspend fun getEnabledProfiles(): List<ProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    @Query("UPDATE profiles SET isEnabled = :isEnabled WHERE id = :profileId")
    suspend fun setProfileEnabled(profileId: Long, isEnabled: Boolean)

    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    suspend fun getAllProfiles(): List<ProfileEntity>

    @Query("DELETE FROM profiles")
    suspend fun deleteAllProfiles()
}

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps WHERE profileId = :profileId ORDER BY appName ASC")
    fun observeAppsForProfile(profileId: Long): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps WHERE profileId = :profileId")
    suspend fun getAppsForProfile(profileId: Long): List<BlockedAppEntity>

    @Query("""
        SELECT DISTINCT ba.packageName FROM blocked_apps ba
        INNER JOIN profiles p ON ba.profileId = p.id
        WHERE p.isEnabled = 1
    """)
    suspend fun getAllBlockedPackageNamesForEnabledProfiles(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<BlockedAppEntity>)

    @Delete
    suspend fun deleteApp(app: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps WHERE profileId = :profileId AND packageName = :packageName")
    suspend fun deleteAppByPackage(profileId: Long, packageName: String)
}

@Dao
interface BlockedDomainDao {
    @Query("SELECT * FROM blocked_domains WHERE profileId = :profileId ORDER BY pattern ASC")
    fun observeDomainsForProfile(profileId: Long): Flow<List<BlockedDomainEntity>>

    @Query("SELECT * FROM blocked_domains WHERE profileId = :profileId")
    suspend fun getDomainsForProfile(profileId: Long): List<BlockedDomainEntity>

    @Query("""
        SELECT DISTINCT bd.pattern FROM blocked_domains bd
        INNER JOIN profiles p ON bd.profileId = p.id
        WHERE p.isEnabled = 1
    """)
    suspend fun getAllBlockedPatternsForEnabledProfiles(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomain(domain: BlockedDomainEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomains(domains: List<BlockedDomainEntity>)

    @Delete
    suspend fun deleteDomain(domain: BlockedDomainEntity)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules WHERE profileId = :profileId")
    fun observeSchedulesForProfile(profileId: Long): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE profileId = :profileId")
    suspend fun getSchedulesForProfile(profileId: Long): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE isEnabled = 1")
    suspend fun getAllActiveSchedules(): List<ScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<ScheduleEntity>)

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions WHERE isActive = 1 AND endTimestamp > :currentTimeMs")
    suspend fun getActiveSessions(currentTimeMs: Long): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions WHERE isActive = 1 AND endTimestamp > :currentTimeMs")
    fun observeActiveSessions(currentTimeMs: Long): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Query("UPDATE focus_sessions SET isActive = 0 WHERE id = :sessionId")
    suspend fun endSession(sessionId: Long)

    @Query("UPDATE focus_sessions SET isActive = 0 WHERE endTimestamp <= :currentTimeMs")
    suspend fun cleanupExpiredSessions(currentTimeMs: Long)
}

data class TargetBlockStat(
    val identifier: String,
    val targetType: String,
    val count: Int
)

@Dao
interface BlockEventDao {
    @Insert
    suspend fun insertEvent(event: BlockEventEntity): Long

    @Query("SELECT * FROM block_events ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentEvents(limit: Int = 100): Flow<List<BlockEventEntity>>

    @Query("SELECT COUNT(*) FROM block_events")
    fun observeTotalBlockedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM block_events WHERE timestamp >= :sinceTimestamp")
    fun observeBlockCountSince(sinceTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM block_events WHERE timestamp >= :sinceTimestamp")
    suspend fun getBlockCountSince(sinceTimestamp: Long): Int

    @Query("""
        SELECT identifier, targetType, COUNT(*) AS count
        FROM block_events
        GROUP BY identifier, targetType
        ORDER BY count DESC
        LIMIT :limit
    """)
    fun observeTopBlockedTargets(limit: Int = 10): Flow<List<TargetBlockStat>>

    @Query("DELETE FROM block_events WHERE timestamp < :olderThanTimestamp")
    suspend fun pruneOldEvents(olderThanTimestamp: Long)

    @Query("DELETE FROM block_events")
    suspend fun clearAllEvents()
}
