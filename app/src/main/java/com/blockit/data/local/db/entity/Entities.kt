package com.blockit.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: Long,
    val iconKey: String,
    val isEnabled: Boolean,
    val blockMode: String = "BLACKLIST",
    val createdAt: Long
)

@Entity(
    tableName = "blocked_apps",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId"]), Index(value = ["profileId", "packageName"], unique = true)]
)
data class BlockedAppEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val packageName: String,
    val appName: String
)

@Entity(
    tableName = "blocked_domains",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId"]), Index(value = ["profileId", "pattern"], unique = true)]
)
data class BlockedDomainEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val pattern: String,
    val isWildcard: Boolean
)

@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId"])]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val daysOfWeekMask: Int,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val isEnabled: Boolean
)

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId"])]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isActive: Boolean
)

@Entity(tableName = "block_events")
data class BlockEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val targetType: String, // "APP" or "WEBSITE"
    val identifier: String,
    val profileName: String
)
