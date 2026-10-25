package com.blockit.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blockit.data.local.datastore.PreferenceManager
import com.blockit.data.repository.FocusGoalRepository
import com.blockit.data.repository.FocusSessionManager
import com.blockit.data.repository.ProfileRepository
import com.blockit.data.repository.UsageInsightsRepository
import com.blockit.domain.evaluator.RuleEvaluator
import com.blockit.domain.model.BlockMode
import com.blockit.domain.model.BlockedApp
import com.blockit.domain.model.BlockedDomain
import com.blockit.domain.model.FocusGoal
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainDashboardViewModel(
    private val profileRepository: ProfileRepository,
    private val ruleEvaluator: RuleEvaluator,
    private val focusSessionManager: FocusSessionManager,
    private val scheduleManager: ScheduleManager,
    private val usageInsightsRepository: UsageInsightsRepository,
    private val preferenceManager: PreferenceManager,
    private val focusGoalRepository: FocusGoalRepository
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

    val activeRuleSnapshot: StateFlow<com.blockit.domain.evaluator.ActiveRuleSnapshot> = ruleEvaluator.snapshotFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ruleEvaluator.getSnapshot()
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

    val onboardingCompleted: StateFlow<Boolean> = preferenceManager.onboardingCompleted
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    private val _selectedGoalDate = MutableStateFlow(FocusGoalRepository.getTodayIsoDate())
    val selectedGoalDate: StateFlow<String> = _selectedGoalDate.asStateFlow()

    val goalsForSelectedDate: StateFlow<List<FocusGoal>> = _selectedGoalDate.flatMapLatest { date ->
        focusGoalRepository.observeGoalsForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayGoals: StateFlow<List<FocusGoal>> = focusGoalRepository.observeTodayGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectGoalDate(dateIso: String) {
        _selectedGoalDate.value = dateIso
    }

    fun addGoal(title: String, targetDate: String = _selectedGoalDate.value, sessionId: Long? = null) {
        viewModelScope.launch {
            focusGoalRepository.addGoal(title, targetDate, sessionId)
        }
    }

    fun toggleGoal(id: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            focusGoalRepository.toggleGoalCompletion(id, isCompleted)
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            focusGoalRepository.deleteGoal(id)
        }
    }

    fun startSessionWithGoals(
        profileId: Long,
        durationMinutes: Int,
        selectedGoalIds: List<Long> = emptyList(),
        newGoalTitles: List<String> = emptyList()
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val sessionId = focusSessionManager.startSession(profileId, durationMinutes)
            val today = FocusGoalRepository.getTodayIsoDate()
            if (selectedGoalIds.isNotEmpty()) {
                focusGoalRepository.linkGoalsToSession(selectedGoalIds, sessionId)
            }
            newGoalTitles.forEach { title ->
                if (title.isNotBlank()) {
                    focusGoalRepository.addGoal(
                        title = title.trim(),
                        targetDate = today,
                        sessionId = sessionId
                    )
                }
            }
            ruleEvaluator.refreshRules()
            scheduleManager.scheduleNextBoundaryAlarm()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferenceManager.setOnboardingCompleted(true)
        }
    }

    fun setUnlockPin(pin: String) {
        viewModelScope.launch {
            preferenceManager.setUnlockPin(pin)
        }
    }

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

    fun setThemeMode(mode: com.blockit.domain.model.ThemeMode) {
        viewModelScope.launch {
            preferenceManager.setThemeMode(mode)
        }
    }

    private val _isProfilesLoading = MutableStateFlow(true)
    val isProfilesLoading: StateFlow<Boolean> = _isProfilesLoading.asStateFlow()

    private val _selectedProfileId = MutableStateFlow<Long?>(null)
    val selectedProfileId: StateFlow<Long?> = _selectedProfileId.asStateFlow()

    init {
        viewModelScope.launch {
            profiles.collect { list ->
                _isProfilesLoading.value = false
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

    val allProfileApps: StateFlow<Map<Long, List<BlockedApp>>> = profileRepository.observeAllBlockedApps()
        .map { list -> list.groupBy { it.profileId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val packedDomains: StateFlow<List<BlockedDomain>> = _selectedProfileId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else profileRepository.observeDomainsForProfile(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val schedules: StateFlow<List<Schedule>> = profileRepository.observeAllSchedules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startSession(profileId: Long, durationMinutes: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            focusSessionManager.startSession(profileId, durationMinutes)
            ruleEvaluator.refreshRules()
            scheduleManager.scheduleNextBoundaryAlarm()
        }
    }

    fun endSession(sessionId: Long) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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

    fun toggleProfileEnabled(profileId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            profileRepository.setProfileEnabled(profileId, isEnabled)
            ruleEvaluator.refreshRules()
            scheduleManager.scheduleNextBoundaryAlarm()
        }
    }

    fun createProfile(
        name: String,
        profileType: com.blockit.domain.model.ProfileType = com.blockit.domain.model.ProfileType.FOCUS,
        listType: com.blockit.domain.model.ListType = com.blockit.domain.model.ListType.BLOCKLIST,
        daysMask: Int = 127,
        startMinute: Int = 540,
        endMinute: Int = 1020,
        domains: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val id = profileRepository.createProfile(
                name = name,
                profileType = profileType,
                listType = listType,
                daysMask = daysMask,
                startMinute = startMinute,
                endMinute = endMinute
            )
            for (domain in domains) {
                profileRepository.addDomainToProfile(id, domain, domain.contains('*'))
            }
            _selectedProfileId.value = id
            ruleEvaluator.refreshRules()
            scheduleManager.scheduleNextBoundaryAlarm()
        }
    }

    fun createProfile(name: String, mode: BlockMode) {
        val lType = if (mode == BlockMode.WHITELIST) com.blockit.domain.model.ListType.ALLOWLIST else com.blockit.domain.model.ListType.BLOCKLIST
        createProfile(name = name, profileType = com.blockit.domain.model.ProfileType.FOCUS, listType = lType)
    }

    fun updateProfileSchedule(
        profileId: Long,
        daysMask: Int,
        startMinute: Int,
        endMinute: Int
    ) {
        viewModelScope.launch {
            profileRepository.updateProfileSchedule(profileId, daysMask, startMinute, endMinute)
            ruleEvaluator.refreshRules()
            scheduleManager.scheduleNextBoundaryAlarm()
        }
    }

    fun duplicateProfile(profileId: Long) {
        viewModelScope.launch {
            val id = profileRepository.duplicateProfile(profileId)
            if (id > 0) {
                _selectedProfileId.value = id
                ruleEvaluator.refreshRules()
            }
        }
    }

    fun createTemplateProfile(templateType: String) {
        viewModelScope.launch {
            val id = profileRepository.createTemplateProfile(templateType)
            if (id > 0) {
                _selectedProfileId.value = id
                ruleEvaluator.refreshRules()
            }
        }
    }

    fun deleteProfile(profileId: Long) {
        viewModelScope.launch {
            profileRepository.deleteProfile(profileId)
            val remaining = profileRepository.getAllProfiles()
            _selectedProfileId.value = remaining.firstOrNull()?.id
            ruleEvaluator.refreshRules()
        }
    }

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
        private val preferenceManager: PreferenceManager,
        private val focusGoalRepository: FocusGoalRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainDashboardViewModel(
                profileRepository,
                ruleEvaluator,
                focusSessionManager,
                scheduleManager,
                usageInsightsRepository,
                preferenceManager,
                focusGoalRepository
            ) as T
        }
    }
}
