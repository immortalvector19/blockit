package com.blockit.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.blockit.domain.model.ChallengeType
import com.blockit.domain.model.StrictModeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "blockit_settings")

class PreferenceManager(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val STRICT_MODE_ENABLED = booleanPreferencesKey("strict_mode_enabled")
        val STRICT_MODE_COOLDOWN_SECONDS = intPreferencesKey("strict_mode_cooldown_seconds")
        val STRICT_MODE_CHALLENGE_TYPE = stringPreferencesKey("strict_mode_challenge_type")
        val STRICT_MODE_PROTECT_SETTINGS = booleanPreferencesKey("strict_mode_protect_settings")
        val STRICT_MODE_PASSAGE = stringPreferencesKey("strict_mode_passage")
        val STRICT_MODE_PIN = stringPreferencesKey("strict_mode_pin")
        val STRICT_MODE_REQUIRE_PIN = booleanPreferencesKey("strict_mode_require_pin")
        val STRICT_MODE_MAX_PAUSES = intPreferencesKey("strict_mode_max_pauses")
        val VPN_FILTER_ENABLED = booleanPreferencesKey("vpn_filter_enabled")
        val ACCESSIBILITY_URL_ENABLED = booleanPreferencesKey("accessibility_url_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.ONBOARDING_COMPLETED] ?: false
    }

    val strictModeConfig: Flow<StrictModeConfig> = context.dataStore.data.map { preferences ->
        StrictModeConfig(
            isEnabled = preferences[Keys.STRICT_MODE_ENABLED] ?: false,
            cooldownSeconds = preferences[Keys.STRICT_MODE_COOLDOWN_SECONDS] ?: 60,
            challengeType = try {
                ChallengeType.valueOf(preferences[Keys.STRICT_MODE_CHALLENGE_TYPE] ?: ChallengeType.PIN_PASSWORD.name)
            } catch (_: Exception) {
                ChallengeType.PIN_PASSWORD
            },
            protectSettings = preferences[Keys.STRICT_MODE_PROTECT_SETTINGS] ?: true,
            targetPassage = preferences[Keys.STRICT_MODE_PASSAGE] ?: "I choose focus and clarity over momentary distraction.",
            unlockPin = preferences[Keys.STRICT_MODE_PIN] ?: "",
            requirePinToStopSession = preferences[Keys.STRICT_MODE_REQUIRE_PIN] ?: true,
            maxPausesPerSession = preferences[Keys.STRICT_MODE_MAX_PAUSES] ?: 3
        )
    }

    val vpnFilterEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.VPN_FILTER_ENABLED] ?: true
    }

    val accessibilityUrlEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.ACCESSIBILITY_URL_ENABLED] ?: true
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun updateStrictMode(config: StrictModeConfig) {
        context.dataStore.edit {
            it[Keys.STRICT_MODE_ENABLED] = config.isEnabled
            it[Keys.STRICT_MODE_COOLDOWN_SECONDS] = config.cooldownSeconds
            it[Keys.STRICT_MODE_CHALLENGE_TYPE] = config.challengeType.name
            it[Keys.STRICT_MODE_PROTECT_SETTINGS] = config.protectSettings
            it[Keys.STRICT_MODE_PASSAGE] = config.targetPassage
            it[Keys.STRICT_MODE_PIN] = config.unlockPin
            it[Keys.STRICT_MODE_REQUIRE_PIN] = config.requirePinToStopSession
            it[Keys.STRICT_MODE_MAX_PAUSES] = config.maxPausesPerSession
        }
    }

    suspend fun setUnlockPin(pin: String) {
        context.dataStore.edit { it[Keys.STRICT_MODE_PIN] = pin }
    }

    suspend fun setVpnFilterEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VPN_FILTER_ENABLED] = enabled }
    }

    suspend fun setAccessibilityUrlEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ACCESSIBILITY_URL_ENABLED] = enabled }
    }

    val themeMode: Flow<com.blockit.domain.model.ThemeMode> = context.dataStore.data.map { preferences ->
        try {
            com.blockit.domain.model.ThemeMode.valueOf(
                preferences[Keys.THEME_MODE] ?: com.blockit.domain.model.ThemeMode.LIGHT.name
            )
        } catch (_: Exception) {
            com.blockit.domain.model.ThemeMode.LIGHT
        }
    }

    val accentTheme: Flow<com.blockit.domain.model.AccentTheme> = context.dataStore.data.map { preferences ->
        try {
            com.blockit.domain.model.AccentTheme.valueOf(
                preferences[Keys.ACCENT_THEME] ?: com.blockit.domain.model.AccentTheme.ORANGE.name
            )
        } catch (_: Exception) {
            com.blockit.domain.model.AccentTheme.ORANGE
        }
    }

    suspend fun setThemeMode(mode: com.blockit.domain.model.ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentTheme(theme: com.blockit.domain.model.AccentTheme) {
        context.dataStore.edit { it[Keys.ACCENT_THEME] = theme.name }
    }

    suspend fun toggleDarkMode() {
        context.dataStore.edit { prefs ->
            val current = try {
                com.blockit.domain.model.ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: com.blockit.domain.model.ThemeMode.LIGHT.name)
            } catch (_: Exception) {
                com.blockit.domain.model.ThemeMode.LIGHT
            }
            prefs[Keys.THEME_MODE] = if (current == com.blockit.domain.model.ThemeMode.DARK) {
                com.blockit.domain.model.ThemeMode.LIGHT.name
            } else {
                com.blockit.domain.model.ThemeMode.DARK.name
            }
        }
    }
}
