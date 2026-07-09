package com.buslk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.buslk.data.UserPreferencesRepository
import com.buslk.data.ISearchRepository

/**
 * Factory for injecting UserPreferencesRepository into the SettingsViewModel.
 */
class SettingsViewModelFactory(
    private val preferencesRepository: UserPreferencesRepository,
    private val searchRepository: ISearchRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(preferencesRepository, searchRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
