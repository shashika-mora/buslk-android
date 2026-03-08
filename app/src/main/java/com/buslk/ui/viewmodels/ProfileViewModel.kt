package com.buslk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buslk.data.IFeedbackRepository
import com.buslk.data.ITripRepository
import com.buslk.data.IUserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

/**
 * ViewModel responsible for orchestrating Profile data fetching.
 */
class ProfileViewModel(
    private val userRepository: IUserRepository,
    private val tripRepository: ITripRepository,
    private val feedbackRepository: IFeedbackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfileData(uid: String) {
        if (uid.isBlank()) {
            _uiState.value = ProfileUiState.Error("Invalid User session. Please log in again.")
            return
        }

        _uiState.value = ProfileUiState.Loading

        viewModelScope.launch {
            try {
                // Fetch user document
                val userResult = userRepository.getUserProfile(uid)
                val userProfile = userResult.getOrNull()
                
                if (userProfile == null) {
                    val errorMsg = userResult.exceptionOrNull()?.message ?: "Could not find user profile in database."
                    _uiState.value = ProfileUiState.Error(errorMsg)
                    return@launch
                }

                // Fetch trips and feedback
                val tripsResult = tripRepository.getUserTrips(uid)
                val feedbackResult = feedbackRepository.getUserFeedback(uid)

                _uiState.value = ProfileUiState.Success(
                    userProfile = userProfile,
                    tripHistory = tripsResult.getOrDefault(emptyList()),
                    feedbacks = feedbackResult.getOrDefault(emptyList())
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = ProfileUiState.Error("An error occurred while loading your profile. ${e.message}")
            }
        }
    }
}
