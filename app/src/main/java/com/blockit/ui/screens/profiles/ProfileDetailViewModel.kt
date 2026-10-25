package com.blockit.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blockit.data.repository.ProfileRepository
import com.blockit.domain.evaluator.RuleEvaluator
import com.blockit.domain.model.BlockedApp
import com.blockit.domain.model.BlockedDomain
import com.blockit.domain.model.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileDetailViewModel(
    private val profileId: Long,
    private val repository: ProfileRepository,
    private val evaluator: RuleEvaluator
) : ViewModel() {

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    val blockedApps: StateFlow<List<BlockedApp>> = repository.observeAppsForProfile(profileId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val blockedDomains: StateFlow<List<BlockedDomain>> = repository.observeDomainsForProfile(profileId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _profile.value = repository.getProfileById(profileId)
        }
    }

    fun setBlockMode(mode: com.blockit.domain.model.BlockMode) {
        viewModelScope.launch {
            repository.setProfileMode(profileId, mode)
            loadProfile()
            evaluator.refreshRules()
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch {
            repository.removeAppFromProfile(profileId, packageName)
            evaluator.refreshRules()
        }
    }

    fun addDomain(pattern: String, isWildcard: Boolean) {
        if (pattern.isBlank()) return
        viewModelScope.launch {
            repository.addDomainToProfile(profileId, pattern.trim(), isWildcard)
            evaluator.refreshRules()
        }
    }

    fun removeDomain(domainId: Long) {
        viewModelScope.launch {
            repository.removeDomainFromProfile(domainId)
            evaluator.refreshRules()
        }
    }

    fun setOverlayBackgroundType(type: com.blockit.domain.model.OverlayBackgroundType) {
        val current = _profile.value ?: return
        viewModelScope.launch {
            repository.updateOverlayCustomization(
                profileId = profileId,
                backgroundType = type,
                imageUris = current.overlayImageUris,
                customText = current.overlayCustomText
            )
            loadProfile()
        }
    }

    fun setOverlayCustomText(text: String?) {
        val current = _profile.value ?: return
        viewModelScope.launch {
            repository.updateOverlayCustomization(
                profileId = profileId,
                backgroundType = current.overlayBackgroundType,
                imageUris = current.overlayImageUris,
                customText = text
            )
            loadProfile()
        }
    }

    fun updateOverlayImage(slotIndex: Int, filePath: String) {
        val current = _profile.value ?: return
        val currentUris = current.overlayImageUris.toMutableList()
        while (currentUris.size <= slotIndex) {
            currentUris.add("")
        }
        currentUris[slotIndex] = filePath
        viewModelScope.launch {
            repository.updateOverlayCustomization(
                profileId = profileId,
                backgroundType = current.overlayBackgroundType,
                imageUris = currentUris,
                customText = current.overlayCustomText
            )
            loadProfile()
        }
    }

    fun removeOverlayImage(slotIndex: Int) {
        val current = _profile.value ?: return
        val currentUris = current.overlayImageUris.toMutableList()
        if (slotIndex in currentUris.indices) {
            currentUris[slotIndex] = ""
        }
        viewModelScope.launch {
            repository.updateOverlayCustomization(
                profileId = profileId,
                backgroundType = current.overlayBackgroundType,
                imageUris = currentUris,
                customText = current.overlayCustomText
            )
            loadProfile()
        }
    }

    fun setMultipleOverlayImages(filePaths: List<String>) {
        val current = _profile.value ?: return
        viewModelScope.launch {
            repository.updateOverlayCustomization(
                profileId = profileId,
                backgroundType = current.overlayBackgroundType,
                imageUris = filePaths,
                customText = current.overlayCustomText
            )
            loadProfile()
        }
    }

    class Factory(
        private val profileId: Long,
        private val repository: ProfileRepository,
        private val evaluator: RuleEvaluator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileDetailViewModel(profileId, repository, evaluator) as T
        }
    }
}
