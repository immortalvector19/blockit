package com.blockit.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blockit.data.repository.ProfileRepository
import com.blockit.domain.evaluator.RuleEvaluator
import com.blockit.domain.model.Profile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfilesViewModel(
    private val profileRepository: ProfileRepository,
    private val ruleEvaluator: RuleEvaluator
) : ViewModel() {

    val profiles: StateFlow<List<Profile>> = profileRepository.observeAllProfiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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

    class Factory(
        private val repository: ProfileRepository,
        private val evaluator: RuleEvaluator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfilesViewModel(repository, evaluator) as T
        }
    }
}
