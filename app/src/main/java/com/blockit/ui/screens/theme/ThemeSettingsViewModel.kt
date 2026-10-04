package com.blockit.ui.screens.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blockit.data.local.datastore.PreferenceManager
import com.blockit.domain.model.AccentTheme
import com.blockit.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeSettingsViewModel(
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferenceManager.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.LIGHT
        )

    val accentTheme: StateFlow<AccentTheme> = preferenceManager.accentTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccentTheme.ORANGE
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferenceManager.setThemeMode(mode)
        }
    }

    fun setAccentTheme(theme: AccentTheme) {
        viewModelScope.launch {
            preferenceManager.setAccentTheme(theme)
        }
    }

    class Factory(
        private val preferenceManager: PreferenceManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ThemeSettingsViewModel(preferenceManager) as T
        }
    }
}
