package com.blockit.domain.backup

import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val profiles: List<ProfileBackup>,
    val strictMode: StrictModeBackup? = null
)

data class ProfileBackup(
    val name: String,
    val colorHex: Long,
    val iconKey: String,
    val isEnabled: Boolean,
    val blockedApps: List<BlockedAppBackup> = emptyList(),
    val blockedDomains: List<BlockedDomainBackup> = emptyList(),
    val schedules: List<ScheduleBackup> = emptyList()
)

data class BlockedAppBackup(
    val packageName: String,
    val appName: String
)

data class BlockedDomainBackup(
    val pattern: String,
    val isWildcard: Boolean
)

data class ScheduleBackup(
    val daysOfWeekMask: Int,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val isEnabled: Boolean
)

data class StrictModeBackup(
    val isEnabled: Boolean,
    val challengeType: String,
    val cooldownSeconds: Int,
    val targetPassage: String,
    val protectSettings: Boolean,
    val unlockPin: String = "",
    val requirePinToStopSession: Boolean = true
)

object BackupJsonSerializer {
    const val CURRENT_VERSION = 1

    fun serialize(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("version", payload.version)
        root.put("exportedAt", payload.exportedAt)

        val profilesArray = JSONArray()
        for (profile in payload.profiles) {
            val profileObj = JSONObject().apply {
                put("name", profile.name)
                put("colorHex", profile.colorHex)
                put("iconKey", profile.iconKey)
                put("isEnabled", profile.isEnabled)

                val appsArray = JSONArray()
                for (app in profile.blockedApps) {
                    appsArray.put(JSONObject().apply {
                        put("packageName", app.packageName)
                        put("appName", app.appName)
                    })
                }
                put("blockedApps", appsArray)

                val domainsArray = JSONArray()
                for (domain in profile.blockedDomains) {
                    domainsArray.put(JSONObject().apply {
                        put("pattern", domain.pattern)
                        put("isWildcard", domain.isWildcard)
                    })
                }
                put("blockedDomains", domainsArray)

                val schedulesArray = JSONArray()
                for (sched in profile.schedules) {
                    schedulesArray.put(JSONObject().apply {
                        put("daysOfWeekMask", sched.daysOfWeekMask)
                        put("startMinuteOfDay", sched.startMinuteOfDay)
                        put("endMinuteOfDay", sched.endMinuteOfDay)
                        put("isEnabled", sched.isEnabled)
                    })
                }
                put("schedules", schedulesArray)
            }
            profilesArray.put(profileObj)
        }
        root.put("profiles", profilesArray)

        payload.strictMode?.let { sm ->
            val smObj = JSONObject().apply {
                put("isEnabled", sm.isEnabled)
                put("challengeType", sm.challengeType)
                put("cooldownSeconds", sm.cooldownSeconds)
                put("targetPassage", sm.targetPassage)
                put("protectSettings", sm.protectSettings)
                put("unlockPin", sm.unlockPin)
                put("requirePinToStopSession", sm.requirePinToStopSession)
            }
            root.put("strictMode", smObj)
        }

        return root.toString(2)
    }

    fun deserialize(jsonString: String): BackupPayload {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 1)
        if (version > CURRENT_VERSION) {
            throw IllegalArgumentException("Unsupported backup version: $version (Max supported: $CURRENT_VERSION)")
        }

        val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
        val profilesArray = root.optJSONArray("profiles") ?: JSONArray()
        val profiles = mutableListOf<ProfileBackup>()

        for (i in 0 until profilesArray.length()) {
            val pObj = profilesArray.getJSONObject(i)
            val name = pObj.getString("name")
            val colorHex = pObj.optLong("colorHex", 0xFF4CAF50)
            val iconKey = pObj.optString("iconKey", "default")
            val isEnabled = pObj.optBoolean("isEnabled", true)

            val apps = mutableListOf<BlockedAppBackup>()
            val appsArray = pObj.optJSONArray("blockedApps")
            if (appsArray != null) {
                for (j in 0 until appsArray.length()) {
                    val appObj = appsArray.getJSONObject(j)
                    apps.add(
                        BlockedAppBackup(
                            packageName = appObj.getString("packageName"),
                            appName = appObj.optString("appName", appObj.getString("packageName"))
                        )
                    )
                }
            }

            val domains = mutableListOf<BlockedDomainBackup>()
            val domainsArray = pObj.optJSONArray("blockedDomains")
            if (domainsArray != null) {
                for (j in 0 until domainsArray.length()) {
                    val domObj = domainsArray.getJSONObject(j)
                    domains.add(
                        BlockedDomainBackup(
                            pattern = domObj.getString("pattern"),
                            isWildcard = domObj.optBoolean("isWildcard", false)
                        )
                    )
                }
            }

            val schedules = mutableListOf<ScheduleBackup>()
            val schedsArray = pObj.optJSONArray("schedules")
            if (schedsArray != null) {
                for (j in 0 until schedsArray.length()) {
                    val scObj = schedsArray.getJSONObject(j)
                    schedules.add(
                        ScheduleBackup(
                            daysOfWeekMask = scObj.getInt("daysOfWeekMask"),
                            startMinuteOfDay = scObj.getInt("startMinuteOfDay"),
                            endMinuteOfDay = scObj.getInt("endMinuteOfDay"),
                            isEnabled = scObj.optBoolean("isEnabled", true)
                        )
                    )
                }
            }

            profiles.add(
                ProfileBackup(
                    name = name,
                    colorHex = colorHex,
                    iconKey = iconKey,
                    isEnabled = isEnabled,
                    blockedApps = apps,
                    blockedDomains = domains,
                    schedules = schedules
                )
            )
        }

        val strictModeObj = root.optJSONObject("strictMode")
        val strictMode = strictModeObj?.let {
            StrictModeBackup(
                isEnabled = it.optBoolean("isEnabled", false),
                challengeType = it.optString("challengeType", "PIN_PASSWORD"),
                cooldownSeconds = it.optInt("cooldownSeconds", 60),
                targetPassage = it.optString("targetPassage", "I choose focus and clarity over momentary distraction."),
                protectSettings = it.optBoolean("protectSettings", true),
                unlockPin = it.optString("unlockPin", ""),
                requirePinToStopSession = it.optBoolean("requirePinToStopSession", true)
            )
        }

        return BackupPayload(
            version = version,
            exportedAt = exportedAt,
            profiles = profiles,
            strictMode = strictMode
        )
    }
}
