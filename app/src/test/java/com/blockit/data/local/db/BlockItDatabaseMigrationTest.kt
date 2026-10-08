package com.blockit.data.local.db

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class BlockItDatabaseMigrationTest {

    @Test
    fun migration_2_3_addsRequiredColumnsAndMigratesData() {
        val executedQueries = mutableListOf<String>()

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                val sql = args[0] as String
                executedQueries.add(sql.trim())
            }
            null
        } as SupportSQLiteDatabase

        BlockItDatabase.MIGRATION_2_3.migrate(dbProxy)

        // Verify alter table statements were executed for all 5 new columns
        assertTrue(
            "Should add profileType column",
            executedQueries.any { it.contains("ALTER TABLE profiles ADD COLUMN profileType TEXT NOT NULL DEFAULT 'FOCUS'") }
        )
        assertTrue(
            "Should add listType column",
            executedQueries.any { it.contains("ALTER TABLE profiles ADD COLUMN listType TEXT NOT NULL DEFAULT 'BLOCKLIST'") }
        )
        assertTrue(
            "Should add scheduleDaysMask column",
            executedQueries.any { it.contains("ALTER TABLE profiles ADD COLUMN scheduleDaysMask INTEGER NOT NULL DEFAULT 127") }
        )
        assertTrue(
            "Should add scheduleStartMinute column",
            executedQueries.any { it.contains("ALTER TABLE profiles ADD COLUMN scheduleStartMinute INTEGER NOT NULL DEFAULT 540") }
        )
        assertTrue(
            "Should add scheduleEndMinute column",
            executedQueries.any { it.contains("ALTER TABLE profiles ADD COLUMN scheduleEndMinute INTEGER NOT NULL DEFAULT 1020") }
        )

        // Verify data migration of existing WHITELIST profiles to ALLOWLIST
        assertTrue(
            "Should migrate WHITELIST to ALLOWLIST",
            executedQueries.any { it.contains("UPDATE profiles SET listType = 'ALLOWLIST' WHERE blockMode = 'WHITELIST'") }
        )

        // Verify data migration of existing schedules into SCHEDULE profiles
        assertTrue(
            "Should migrate schedules table into profileType SCHEDULE",
            executedQueries.any {
                it.contains("UPDATE profiles") &&
                it.contains("SET profileType = 'SCHEDULE'") &&
                it.contains("WHERE id IN (SELECT DISTINCT profileId FROM schedules)")
            }
        )

        assertEquals("Total migration statements count", 8, executedQueries.size)
    }

    @Test
    fun migration_1_2_addsBlockMode() {
        val executedQueries = mutableListOf<String>()

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                val sql = args[0] as String
                executedQueries.add(sql.trim())
            }
            null
        } as SupportSQLiteDatabase

        BlockItDatabase.MIGRATION_1_2.migrate(dbProxy)

        assertTrue(
            "Should add blockMode column",
            executedQueries.any { it.contains("ALTER TABLE profiles ADD COLUMN blockMode TEXT NOT NULL DEFAULT 'BLACKLIST'") }
        )
    }
}
