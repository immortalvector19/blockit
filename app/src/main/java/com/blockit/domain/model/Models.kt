package com.blockit.domain.model
 
enum class ProfileType {
    FOCUS,
    SCHEDULE
}

enum class ListType {
    BLOCKLIST,
    ALLOWLIST
}

enum class BlockMode {
    BLACKLIST,
    WHITELIST
}

data class Profile(
    val id: Long = 0,
    val name: String,
    val colorHex: Long = 0xFF2563EB,
    val iconKey: String = "focus",
    val isEnabled: Boolean = true,
    val profileType: ProfileType = ProfileType.FOCUS,
    val listType: ListType = ListType.BLOCKLIST,
    val scheduleDaysMask: Int = 127,
    val scheduleStartMinute: Int = 540,
    val scheduleEndMinute: Int = 1020,
    val blockMode: BlockMode = if (listType == ListType.ALLOWLIST) BlockMode.WHITELIST else BlockMode.BLACKLIST,
    val createdAt: Long = System.currentTimeMillis(),
    val appCount: Int = 0,
    val domainCount: Int = 0
)

data class BlockedApp(
    val id: Long = 0,
    val profileId: Long,
    val packageName: String,
    val appName: String
)

data class BlockedDomain(
    val id: Long = 0,
    val profileId: Long,
    val pattern: String,
    val isWildcard: Boolean = false
)

data class Schedule(
    val id: Long = 0,
    val profileId: Long,
    val daysOfWeekMask: Int = 127, // Bitmask for Mon-Sun (1=Mon, 2=Tue, 4=Wed, 8=Thu, 16=Fri, 32=Sat, 64=Sun)
    val startMinuteOfDay: Int,     // 0 to 1439 (e.g. 540 = 09:00)
    val endMinuteOfDay: Int,       // 0 to 1439 (e.g. 1020 = 17:00)
    val isEnabled: Boolean = true
)

data class FocusSession(
    val id: Long = 0,
    val profileId: Long,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isActive: Boolean = true
)

enum class SessionStrictness {
    NORMAL,
    STRICT,
    LOCKED
}

enum class TargetType {
    APP,
    WEBSITE
}

data class BlockEvent(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val targetType: TargetType,
    val identifier: String, // package name or domain
    val profileName: String
)

enum class ChallengeType {
    PIN_PASSWORD,
    COOLDOWN,
    TYPED_PASSAGE
}

data class StrictModeConfig(
    val isEnabled: Boolean = false,
    val cooldownSeconds: Int = 60,
    val challengeType: ChallengeType = ChallengeType.PIN_PASSWORD,
    val protectSettings: Boolean = true,
    val targetPassage: String = "I choose focus and clarity over momentary distraction.",
    val unlockPin: String = "",
    val requirePinToStopSession: Boolean = true,
    val maxPausesPerSession: Int = 3
)

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean
)

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class AccentTheme(val displayName: String, val primaryColorHex: Long) {
    BLUE("Blue", 0xFF2563EB),
    GREEN("Green", 0xFF059669),
    PURPLE("Purple", 0xFF7C3AED),
    AMBER("Amber", 0xFFD97706),
    SLATE("Slate", 0xFF475569);

    companion object {
        val DEFAULT = BLUE
    }
}

