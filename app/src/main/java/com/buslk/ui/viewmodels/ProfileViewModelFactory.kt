package com.buslk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.buslk.data.IFeedbackRepository
import com.buslk.data.ITripRepository
import com.buslk.data.IUserRepository

/**
 * Factory for injecting Repositories into the ProfileViewModel.
 */
class ProfileViewModelFactory(
    private val userRepository: IUserRepository,
    private val tripRepository: ITripRepository,
    private val feedbackRepository: IFeedbackRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(userRepository, tripRepository, feedbackRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
