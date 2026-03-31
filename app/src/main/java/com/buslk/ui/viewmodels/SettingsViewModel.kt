package com.buslk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buslk.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for Settings and Preferences.
 * Bridges the UI and the DataStore repository using StateFlows.
 */
class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // --- StateFlows ---
    // stateIn converts the cold Flow from DataStore into a hot StateFlow that Compose can easily observe.
    // SharingStarted.WhileSubscribed(5000) keeps the flow active for 5s after the UI disappears, caching it.
    
    val themeMode: StateFlow<Int> = preferencesRepository.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0 // Default to Auto
        )

    val appLanguage: StateFlow<String> = preferencesRepository.appLanguageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "en"
        )

    val notificationsEnabled: StateFlow<Boolean> = preferencesRepository.notificationsEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val defaultRoute: StateFlow<String> = preferencesRepository.defaultRouteFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "None"
        )

    // --- User Actions ---
    
    fun updateThemeMode(mode: Int) {
        viewModelScope.launch {
            preferencesRepository.updateThemeMode(mode)
        }
    }

    fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            preferencesRepository.updateAppLanguage(languageCode)
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateNotificationsEnabled(enabled)
        }
    }

    fun updateDefaultRoute(route: String) {
        viewModelScope.launch {
            preferencesRepository.updateDefaultRoute(route)
        }
    }
}
