package com.buslk.ui.viewmodels

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel : ViewModel() {
    private val _themeMode = MutableStateFlow(0) // 0: System, 1: Light, 2: Dark
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    private val _appLanguage = MutableStateFlow("en")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    init {
        // Initialize from current system state if possible, or leave as defaults
        _appLanguage.value = AppCompatDelegate.getApplicationLocales()[0]?.language ?: "en"
    }

    fun updateThemeMode(mode: Int) {
        _themeMode.value = mode
    }

    fun updateLanguage(lang: String) {
        _appLanguage.value = lang
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(lang)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }
}
