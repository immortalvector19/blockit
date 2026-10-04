package com.blockit.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blockit.data.local.datastore.PreferenceManager
import com.blockit.data.repository.FocusSessionManager
import com.blockit.data.repository.ProfileRepository
import com.blockit.data.repository.UsageInsightsRepository
import com.blockit.domain.evaluator.RuleEvaluator
import com.blockit.domain.model.BlockMode
import com.blockit.domain.model.BlockedApp
import com.blockit.domain.model.BlockedDomain
import com.blockit.domain.model.FocusSession
import com.blockit.domain.model.Profile
import com.blockit.domain.model.Schedule
import com.blockit.domain.model.StrictModeConfig
import com.blockit.domain.model.UsageInsightsData
import com.blockit.service.receiver.ScheduleManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainDashboardViewModel(
    private val profileRepository: ProfileRepository,
    private val ruleEvaluator: RuleEvaluator,
    private val focusSessionManager: FocusSessionManager,
    private val scheduleManager: ScheduleManager,
    private val usageInsightsRepository: UsageInsightsRepository,
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

    val insightsData: StateFlow<UsageInsightsData> = usageInsightsRepository.observeInsights()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UsageInsightsData()
        )

    val themeMode: StateFlow<com.blockit.domain.model.ThemeMode> = preferenceManager.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.blockit.domain.model.ThemeMode.LIGHT
        )

    fun toggleDarkMode() {
        viewModelScope.launch {
            preferenceManager.toggleDarkMode()
        }
    }

    private val _selectedProfileId = MutableStateFlow<Long?>(null)
    val selectedProfileId: StateFlow<Long?> = _selectedProfileId.asStateFlow()

    init {
        viewModelScope.launch {
            profiles.collect { list ->
                if (_selectedProfileId.value == null && list.isNotEmpty()) {
                    _selectedProfileId.value = list.first().id
                }
            }
        }
    }

    fun selectProfile(id: Long) {
        _selectedProfileId.value = id
    }

    val packedApps: StateFlow<List<BlockedApp>> = _selectedProfileId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else profileRepository.observeAppsForProfile(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val packedDomains: StateFlow<List<BlockedDomain>> = _selectedProfileId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else profileRepository.observeDomainsForProfile(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val schedules: StateFlow<List<Schedule>> = _selectedProfileId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else profileRepository.observeSchedulesForProfile(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startSession(profileId: Long, durationMinutes: Int) {
        viewModelScope.launch {
            focusSessionManager.startSession(profileId, durationMinutes)
            ruleEvaluator.refreshRules()
            scheduleManager.scheduleNextBoundaryAlarm()
        }
    }

    fun endSession(sessionId: Long) {
        viewModelScope.launch {
            focusSessionManager.endSession(sessionId)
            ruleEvaluator.refreshRules()
            scheduleManager.scheduleNextBoundaryAlarm()
        }
    }

    fun setProfileMode(profileId: Long, mode: BlockMode) {
        viewModelScope.launch {
            profileRepository.setProfileMode(profileId, mode)
            ruleEvaluator.refreshRules()
        }
    }
    fun setBagMode(profileId: Long, mode: BlockMode) = setProfileMode(profileId, mode)

    fun toggleProfileEnabled(profileId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            profileRepository.setProfileEnabled(profileId, isEnabled)
            ruleEvaluator.refreshRules()
            scheduleManager.scheduleNextBoundaryAlarm()
        }
    }

    fun createProfile(name: String, mode: BlockMode) {
        viewModelScope.launch {
            val id = profileRepository.createProfile(name = name, blockMode = mode)
            _selectedProfileId.value = id
            ruleEvaluator.refreshRules()
        }
    }
    fun createBag(name: String, mode: BlockMode) = createProfile(name, mode)

    fun deleteProfile(profileId: Long) {
        viewModelScope.launch {
            profileRepository.deleteProfile(profileId)
            val remaining = profileRepository.getAllProfiles()
            _selectedProfileId.value = remaining.firstOrNull()?.id
            ruleEvaluator.refreshRules()
        }
    }
    fun deleteBag(profileId: Long) = deleteProfile(profileId)

    fun removeApp(profileId: Long, packageName: String) {
        viewModelScope.launch {
            profileRepository.removeAppFromProfile(profileId, packageName)
            ruleEvaluator.refreshRules()
        }
    }

    fun addDomain(profileId: Long, pattern: String, isWildcard: Boolean) {
        if (pattern.isBlank()) return
        viewModelScope.launch {
            profileRepository.addDomainToProfile(profileId, pattern.trim(), isWildcard)
            ruleEvaluator.refreshRules()
        }
    }

    fun removeDomain(domainId: Long) {
        viewModelScope.launch {
            profileRepository.removeDomainFromProfile(domainId)
            ruleEvaluator.refreshRules()
        }
    }

    fun saveSchedule(schedule: Schedule) {
        viewModelScope.launch {
            profileRepository.saveSchedule(schedule)
            ruleEvaluator.refreshRules()
            scheduleManager.scheduleNextBoundaryAlarm()
        }
    }

    fun deleteSchedule(scheduleId: Long) {
        viewModelScope.launch {
            profileRepository.deleteSchedule(scheduleId)
            ruleEvaluator.refreshRules()
            scheduleManager.scheduleNextBoundaryAlarm()
        }
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val ruleEvaluator: RuleEvaluator,
        private val focusSessionManager: FocusSessionManager,
        private val scheduleManager: ScheduleManager,
        private val usageInsightsRepository: UsageInsightsRepository,
        private val preferenceManager: PreferenceManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainDashboardViewModel(
                profileRepository,
                ruleEvaluator,
                focusSessionManager,
                scheduleManager,
                usageInsightsRepository,
                preferenceManager
            ) as T
        }
    }
}
