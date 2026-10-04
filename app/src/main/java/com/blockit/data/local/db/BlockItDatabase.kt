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
    version = 1,
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
        private const val DATABASE_NAME = "blockit.db"

        fun create(context: Context, coroutineScope: CoroutineScope): BlockItDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                BlockItDatabase::class.java,
                DATABASE_NAME
            ).addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Prepopulate with default profiles: Focus, Sleep, Study
                    coroutineScope.launch(Dispatchers.IO) {
                        val now = System.currentTimeMillis()
                        db.execSQL(
                            "INSERT INTO profiles (id, name, colorHex, iconKey, isEnabled, createdAt) VALUES " +
                                    "(1, 'Focus', 4284612846, 'focus', 1, $now)," +
                                    "(2, 'Sleep', 4281358249, 'bed', 0, ${now + 1})," +
                                    "(3, 'Study', 4282562473, 'book', 0, ${now + 2})"
                        )
                        // Prepopulate default distracting website patterns in Focus profile
                        db.execSQL("INSERT INTO blocked_domains (profileId, pattern, isWildcard) VALUES (1, 'instagram.com', 0)")
                        db.execSQL("INSERT INTO blocked_domains (profileId, pattern, isWildcard) VALUES (1, 'tiktok.com', 0)")
                        db.execSQL("INSERT INTO blocked_domains (profileId, pattern, isWildcard) VALUES (1, 'twitter.com', 0)")
                        db.execSQL("INSERT INTO blocked_domains (profileId, pattern, isWildcard) VALUES (1, 'x.com', 0)")
                    }
                }
            }).build()
        }
    }
}
