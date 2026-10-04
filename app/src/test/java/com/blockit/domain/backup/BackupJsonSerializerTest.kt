package com.blockit.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupJsonSerializerTest {

    @Test
    fun roundtrip_fullPayload_matchesOriginal() {
        val original = BackupPayload(
            version = 1,
            exportedAt = 1700000000000L,
            profiles = listOf(
                ProfileBackup(
                    name = "Deep Work",
                    colorHex = 0xFF2196F3,
                    iconKey = "work",
                    isEnabled = true,
                    blockedApps = listOf(
                        BlockedAppBackup(packageName = "com.twitter.android", appName = "Twitter"),
                        BlockedAppBackup(packageName = "com.instagram.android", appName = "Instagram")
                    ),
                    blockedDomains = listOf(
                        BlockedDomainBackup(pattern = "*.youtube.com", isWildcard = true),
                        BlockedDomainBackup(pattern = "reddit.com", isWildcard = false)
                    ),
                    schedules = listOf(
                        ScheduleBackup(
                            daysOfWeekMask = 0b00111110, // Mon-Fri
                            startMinuteOfDay = 540,      // 09:00
                            endMinuteOfDay = 1020,     // 17:00
                            isEnabled = true
                        )
                    )
                )
            ),
            strictMode = StrictModeBackup(
                isEnabled = true,
                challengeType = "TYPED_PASSAGE",
                cooldownSeconds = 60,
                targetPassage = "Focus on what matters.",
                protectSettings = true
            )
        )

        val json = BackupJsonSerializer.serialize(original)
        assertTrue(json.contains("Deep Work"))
        assertTrue(json.contains("*.youtube.com"))
        assertTrue(json.contains("Focus on what matters."))

        val deserialized = BackupJsonSerializer.deserialize(json)

        assertEquals(1, deserialized.version)
        assertEquals(1700000000000L, deserialized.exportedAt)
        assertEquals(1, deserialized.profiles.size)

        val p = deserialized.profiles[0]
        assertEquals("Deep Work", p.name)
        assertEquals(0xFF2196F3, p.colorHex)
        assertTrue(p.isEnabled)
        assertEquals(2, p.blockedApps.size)
        assertEquals("com.twitter.android", p.blockedApps[0].packageName)
        assertEquals(2, p.blockedDomains.size)
        assertEquals("*.youtube.com", p.blockedDomains[0].pattern)
        assertTrue(p.blockedDomains[0].isWildcard)
        assertEquals(1, p.schedules.size)
        assertEquals(540, p.schedules[0].startMinuteOfDay)

        assertNotNull(deserialized.strictMode)
        val sm = deserialized.strictMode!!
        assertTrue(sm.isEnabled)
        assertEquals("TYPED_PASSAGE", sm.challengeType)
        assertEquals(60, sm.cooldownSeconds)
        assertEquals("Focus on what matters.", sm.targetPassage)
        assertTrue(sm.protectSettings)
    }

    @Test
    fun roundtrip_emptyProfiles_succeeds() {
        val payload = BackupPayload(
            version = 1,
            exportedAt = 123456L,
            profiles = emptyList(),
            strictMode = null
        )

        val json = BackupJsonSerializer.serialize(payload)
        val deserialized = BackupJsonSerializer.deserialize(json)

        assertEquals(1, deserialized.version)
        assertEquals(0, deserialized.profiles.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun deserialize_futureVersion_throwsException() {
        val json = """{"version": 99, "profiles": []}"""
        BackupJsonSerializer.deserialize(json)
    }
}
