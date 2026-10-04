package com.blockit.domain.model
 
enum class BlockMode {
    BLACKLIST,
    WHITELIST
}

data class Profile(
    val id: Long = 0,
    val name: String,
    val colorHex: Long = 0xFF6200EE,
    val iconKey: String = "focus",
    val isEnabled: Boolean = true,
    val blockMode: BlockMode = BlockMode.BLACKLIST,
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
    COOLDOWN,
    TYPED_PASSAGE
}

data class StrictModeConfig(
    val isEnabled: Boolean = false,
    val cooldownSeconds: Int = 60,
    val challengeType: ChallengeType = ChallengeType.COOLDOWN,
    val protectSettings: Boolean = true,
    val targetPassage: String = "I choose focus and clarity over momentary distraction."
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
    ORANGE("Mechanical Orange", 0xFFFF5520),
    GREEN("Terminal Emerald", 0xFF10B981),
    BLUE("Blueprint Cyan", 0xFF0284C7),
    PURPLE("Cyber Violet", 0xFF8B5CF6),
    AMBER("Analog Amber", 0xFFD97706)
}

