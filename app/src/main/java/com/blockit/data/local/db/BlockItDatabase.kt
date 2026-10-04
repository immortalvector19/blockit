package com.blockit.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.blockit.data.local.db.dao.BlockEventDao
import com.blockit.data.local.db.dao.BlockedAppDao
import com.blockit.data.local.db.dao.BlockedDomainDao
import com.blockit.data.local.db.dao.FocusSessionDao
import com.blockit.data.local.db.dao.ProfileDao
import com.blockit.data.local.db.dao.ScheduleDao
import com.blockit.data.local.db.entity.BlockEventEntity
import com.blockit.data.local.db.entity.BlockedAppEntity
import com.blockit.data.local.db.entity.BlockedDomainEntity
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
        BlockEventEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BlockItDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun blockedDomainDao(): BlockedDomainDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun blockEventDao(): BlockEventDao

    companion object {
        private const val DATABASE_NAME = "focus_shield.db"

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN blockMode TEXT NOT NULL DEFAULT 'BLACKLIST'")
            }
        }

        fun create(context: Context): BlockItDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                BlockItDatabase::class.java,
                DATABASE_NAME
            ).addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
