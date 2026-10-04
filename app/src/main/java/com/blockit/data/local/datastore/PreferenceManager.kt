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
        val VPN_FILTER_ENABLED = booleanPreferencesKey("vpn_filter_enabled")
        val ACCESSIBILITY_URL_ENABLED = booleanPreferencesKey("accessibility_url_enabled")
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.ONBOARDING_COMPLETED] ?: false
    }

    val strictModeConfig: Flow<StrictModeConfig> = context.dataStore.data.map { preferences ->
        StrictModeConfig(
            isEnabled = preferences[Keys.STRICT_MODE_ENABLED] ?: false,
            cooldownSeconds = preferences[Keys.STRICT_MODE_COOLDOWN_SECONDS] ?: 60,
            challengeType = try {
                ChallengeType.valueOf(preferences[Keys.STRICT_MODE_CHALLENGE_TYPE] ?: ChallengeType.COOLDOWN.name)
            } catch (_: Exception) {
                ChallengeType.COOLDOWN
            },
            protectSettings = preferences[Keys.STRICT_MODE_PROTECT_SETTINGS] ?: true,
            targetPassage = preferences[Keys.STRICT_MODE_PASSAGE] ?: "I choose focus and clarity over momentary distraction."
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
        }
    }

    suspend fun setVpnFilterEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VPN_FILTER_ENABLED] = enabled }
    }

    suspend fun setAccessibilityUrlEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ACCESSIBILITY_URL_ENABLED] = enabled }
    }
}
