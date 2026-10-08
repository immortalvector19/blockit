package com.blockit.ui.screens.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blockit.data.repository.InstalledAppsRepository
import com.blockit.data.repository.ProfileRepository
import com.blockit.domain.evaluator.RuleEvaluator
import com.blockit.domain.model.InstalledAppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppsPickerUiState(
    val apps: List<InstalledAppInfo> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val searchQuery: String = "",
    val showSystemApps: Boolean = false,
    val isLoading: Boolean = true
)

class AppsPickerViewModel(
    private val profileId: Long,
    private val installedAppsRepo: InstalledAppsRepository,
    private val profileRepo: ProfileRepository,
    private val evaluator: RuleEvaluator
) : ViewModel() {

    private val _allApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    private val _searchQuery = MutableStateFlow("")
    private val _showSystemApps = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<AppsPickerUiState> = combine(
        _allApps,
        _selectedPackages,
        _searchQuery,
        _showSystemApps,
        _isLoading
    ) { apps, selected, query, showSystem, loading ->
        val filtered = apps.filter { app ->
            val matchesQuery = query.isBlank() ||
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
            val matchesSystemFilter = showSystem || !app.isSystemApp
            matchesQuery && matchesSystemFilter
        }
        AppsPickerUiState(
            apps = filtered,
            selectedPackages = selected,
            searchQuery = query,
            showSystemApps = showSystem,
            isLoading = loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppsPickerUiState()
    )

    init {
        val cached = installedAppsRepo.getCachedAppsSync()
        if (cached != null && cached.isNotEmpty()) {
            _allApps.value = cached
            _isLoading.value = false
        }
        loadInstalledApps()
        loadSelectedAppsForProfile()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            if (_allApps.value.isEmpty()) {
                _isLoading.value = true
            }
            val apps = installedAppsRepo.getInstalledApps()
            _allApps.value = apps
            _isLoading.value = false
        }
    }

    private fun loadSelectedAppsForProfile() {
        viewModelScope.launch {
            val existing = profileRepo.getAppsForProfile(profileId)
            _selectedPackages.value = existing.map { it.packageName }.toSet()
        }
    }

    fun toggleAppSelection(packageName: String) {
        val current = _selectedPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedPackages.value = current
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setShowSystemApps(show: Boolean) {
        _showSystemApps.value = show
    }

    fun selectAllVisible() {
        val visible = uiState.value.apps.map { it.packageName }.toSet()
        _selectedPackages.value = _selectedPackages.value + visible
    }

    fun clearSelection() {
        _selectedPackages.value = emptySet()
    }

    fun saveSelectedApps(onComplete: () -> Unit) {
        viewModelScope.launch {
            val selected = _allApps.value.filter { _selectedPackages.value.contains(it.packageName) }
            profileRepo.syncAppsForProfile(profileId, selected)
            evaluator.refreshRules()
            onComplete()
        }
    }

    class Factory(
        private val profileId: Long,
        private val installedAppsRepo: InstalledAppsRepository,
        private val profileRepo: ProfileRepository,
        private val evaluator: RuleEvaluator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppsPickerViewModel(profileId, installedAppsRepo, profileRepo, evaluator) as T
        }
    }
}
