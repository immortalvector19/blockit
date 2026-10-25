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
    val profileType: String = "FOCUS",
    val listType: String = "BLOCKLIST",
    val scheduleDaysMask: Int = 127,
    val scheduleStartMinute: Int = 540,
    val scheduleEndMinute: Int = 1020,
    val overlayBackgroundType: String = "DEFAULT",
    val overlayImageUris: String = "",
    val overlayCustomText: String? = null,
    val createdAt: Long = System.currentTimeMillis()
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

@Entity(
    tableName = "focus_goals",
    indices = [
        Index(value = ["target_date"]),
        Index(value = ["session_id"])
    ]
)
data class FocusGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "target_date")
    val targetDate: String,
    @ColumnInfo(name = "session_id")
    val sessionId: Long? = null,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    @ColumnInfo(name = "completed_at_ms")
    val completedAtMs: Long? = null,
    @ColumnInfo(name = "created_at_ms")
    val createdAtMs: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0
)
