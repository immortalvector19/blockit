package com.blockit.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.blockit.data.local.db.dao.BlockEventDao
import com.blockit.data.local.db.dao.BlockedAppDao
import com.blockit.data.local.db.dao.BlockedDomainDao
import com.blockit.data.local.db.dao.FocusGoalDao
import com.blockit.data.local.db.dao.FocusSessionDao
import com.blockit.data.local.db.dao.ProfileDao
import com.blockit.data.local.db.dao.ScheduleDao
import com.blockit.data.local.db.entity.BlockEventEntity
import com.blockit.data.local.db.entity.BlockedAppEntity
import com.blockit.data.local.db.entity.BlockedDomainEntity
import com.blockit.data.local.db.entity.FocusGoalEntity
import com.blockit.data.local.db.entity.FocusSessionEntity
import com.blockit.data.local.db.entity.ProfileEntity
import com.blockit.data.local.db.entity.ScheduleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProfileEntity::class,
        BlockedAppEntity::class,
        BlockedDomainEntity::class,
        ScheduleEntity::class,
        FocusSessionEntity::class,
        BlockEventEntity::class,
        FocusGoalEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class BlockItDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun blockedDomainDao(): BlockedDomainDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun blockEventDao(): BlockEventDao
    abstract fun focusGoalDao(): FocusGoalDao

    companion object {
        private const val DATABASE_NAME = "focus_shield.db"

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN blockMode TEXT NOT NULL DEFAULT 'BLACKLIST'")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN profileType TEXT NOT NULL DEFAULT 'FOCUS'")
                db.execSQL("ALTER TABLE profiles ADD COLUMN listType TEXT NOT NULL DEFAULT 'BLOCKLIST'")
                db.execSQL("ALTER TABLE profiles ADD COLUMN scheduleDaysMask INTEGER NOT NULL DEFAULT 127")
                db.execSQL("ALTER TABLE profiles ADD COLUMN scheduleStartMinute INTEGER NOT NULL DEFAULT 540")
                db.execSQL("ALTER TABLE profiles ADD COLUMN scheduleEndMinute INTEGER NOT NULL DEFAULT 1020")

                db.execSQL("UPDATE profiles SET listType = 'ALLOWLIST' WHERE blockMode = 'WHITELIST'")
                db.execSQL("UPDATE profiles SET listType = 'BLOCKLIST' WHERE blockMode != 'WHITELIST'")

                db.execSQL("""
                    UPDATE profiles 
                    SET profileType = 'SCHEDULE',
                        scheduleDaysMask = (SELECT daysOfWeekMask FROM schedules WHERE schedules.profileId = profiles.id LIMIT 1),
                        scheduleStartMinute = (SELECT startMinuteOfDay FROM schedules WHERE schedules.profileId = profiles.id LIMIT 1),
                        scheduleEndMinute = (SELECT endMinuteOfDay FROM schedules WHERE schedules.profileId = profiles.id LIMIT 1)
                    WHERE id IN (SELECT DISTINCT profileId FROM schedules)
                """)
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN overlayBackgroundType TEXT NOT NULL DEFAULT 'DEFAULT'")
                db.execSQL("ALTER TABLE profiles ADD COLUMN overlayImageUris TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE profiles ADD COLUMN overlayCustomText TEXT")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS focus_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        target_date TEXT NOT NULL,
                        session_id INTEGER,
                        is_completed INTEGER NOT NULL DEFAULT 0,
                        completed_at_ms INTEGER,
                        created_at_ms INTEGER NOT NULL,
                        order_index INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_goals_target_date ON focus_goals (target_date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_goals_session_id ON focus_goals (session_id)")
            }
        }

        fun create(context: Context): BlockItDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                BlockItDatabase::class.java,
                DATABASE_NAME
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
        }
    }
}
