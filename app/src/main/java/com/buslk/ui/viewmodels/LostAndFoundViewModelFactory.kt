package com.buslk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.buslk.data.ILostAndFoundRepository
import com.buslk.data.IAuthRepository

/**
 * Dependency Injection factory for LostAndFoundViewModel.
 * Ensures the viewModel is initialized with the correct injected repository.
 */
class LostAndFoundViewModelFactory(
    private val repository: ILostAndFoundRepository,
    private val authRepository: IAuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LostAndFoundViewModel::class.java)) {
            return LostAndFoundViewModel(repository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
