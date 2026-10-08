package com.blockit.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blockit.data.local.datastore.PreferenceManager
import com.blockit.data.repository.FocusSessionManager
import com.blockit.data.repository.ProfileRepository
import com.blockit.domain.evaluator.RuleEvaluator
import com.blockit.domain.model.FocusSession
import com.blockit.domain.model.Profile
import com.blockit.domain.model.StrictModeConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfilesViewModel(
    private val profileRepository: ProfileRepository,
    private val ruleEvaluator: RuleEvaluator,
    private val focusSessionManager: FocusSessionManager,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    val profiles: StateFlow<List<Profile>> = profileRepository.observeAllProfiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeSession: StateFlow<FocusSession?> = focusSessionManager.observeActiveSession()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val strictModeConfig: StateFlow<StrictModeConfig> = preferenceManager.strictModeConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StrictModeConfig()
        )

    fun toggleProfileEnabled(profileId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            profileRepository.setProfileEnabled(profileId, isEnabled)
            ruleEvaluator.refreshRules()
        }
    }

    fun createProfile(
        name: String,
        colorHex: Long = 0xFF2563EB,
        iconKey: String = "focus",
        profileType: com.blockit.domain.model.ProfileType = com.blockit.domain.model.ProfileType.FOCUS,
        listType: com.blockit.domain.model.ListType = com.blockit.domain.model.ListType.BLOCKLIST,
        daysMask: Int = 127,
        startMinute: Int = 540,
        endMinute: Int = 1020,
        blockMode: com.blockit.domain.model.BlockMode = if (listType == com.blockit.domain.model.ListType.ALLOWLIST) com.blockit.domain.model.BlockMode.WHITELIST else com.blockit.domain.model.BlockMode.BLACKLIST
    ) {
        viewModelScope.launch {
            profileRepository.createProfile(
                name = name,
                colorHex = colorHex,
                iconKey = iconKey,
                profileType = profileType,
                listType = listType,
                daysMask = daysMask,
                startMinute = startMinute,
                endMinute = endMinute,
                blockMode = blockMode
            )
            ruleEvaluator.refreshRules()
        }
    }

    fun deleteProfile(profileId: Long) {
        viewModelScope.launch {
            profileRepository.deleteProfile(profileId)
            ruleEvaluator.refreshRules()
        }
    }

    fun startQuickSession(profileId: Long, durationMinutes: Int) {
        viewModelScope.launch {
            focusSessionManager.startSession(profileId, durationMinutes)
        }
    }

    fun endSession(sessionId: Long) {
        viewModelScope.launch {
            focusSessionManager.endSession(sessionId)
        }
    }

    class Factory(
        private val repository: ProfileRepository,
        private val evaluator: RuleEvaluator,
        private val sessionManager: FocusSessionManager,
        private val preferenceManager: PreferenceManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfilesViewModel(repository, evaluator, sessionManager, preferenceManager) as T
        }
    }
}
