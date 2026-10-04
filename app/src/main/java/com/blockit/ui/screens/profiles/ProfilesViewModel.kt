package com.blockit.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blockit.data.repository.FocusSessionManager
import com.blockit.data.repository.ProfileRepository
import com.blockit.domain.evaluator.RuleEvaluator
import com.blockit.domain.model.FocusSession
import com.blockit.domain.model.Profile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfilesViewModel(
    private val profileRepository: ProfileRepository,
    private val ruleEvaluator: RuleEvaluator,
    private val focusSessionManager: FocusSessionManager
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

    fun toggleProfileEnabled(profileId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            profileRepository.setProfileEnabled(profileId, isEnabled)
            ruleEvaluator.refreshRules()
        }
    }

    fun createProfile(name: String, colorHex: Long = 0xFF6366F1, iconKey: String = "focus") {
        viewModelScope.launch {
            profileRepository.createProfile(name, colorHex, iconKey)
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
        private val sessionManager: FocusSessionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfilesViewModel(repository, evaluator, sessionManager) as T
        }
    }
}
