package com.buslk.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buslk.data.UserDoc
import com.buslk.data.UserStats
import com.buslk.data.Trip
import com.buslk.data.Feedback
import com.buslk.data.Achievement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    data class Success(
        val userProfile: UserDoc,
        val tripHistory: List<Trip> = emptyList(),
        val feedbacks: List<Feedback> = emptyList()
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfileData(uid: String) {
        // We load mock data even if uid is empty so you can see the UI in the emulator
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            try {
                _uiState.value = ProfileUiState.Success(
                    userProfile = UserDoc(
                        uid = if (uid.isBlank()) "guest_123" else uid, 
                        displayName = "Malini Perera", 
                        email = "malini.p@example.com",
                        points = 2450, 
                        level = "Elite Commuter",
                        stats = UserStats(totalTrips = 124, reportsSubmitted = 15),
                        achievements = mapOf(
                            "1" to Achievement("First Trip", "Completed your first journey", "", true),
                            "2" to Achievement("Early Bird", "Travel before 6 AM", "", true),
                            "3" to Achievement("Reporter", "Submit 10 valid reports", "", false)
                        )
                    ),
                    tripHistory = listOf(
                        Trip("1", "138 - Maharagama", "Today, 08:30 AM", "Rs. 120.00"),
                        Trip("2", "120 - Colombo Fort", "Yesterday", "Rs. 85.00"),
                        Trip("3", "122 - Avissawella", "25 Oct 2023", "Rs. 310.00")
                    ),
                    feedbacks = listOf(
                        Feedback("1", "NB-5521", "The driver was very polite.", 5),
                        Feedback("2", "LY-8842", "Bus was slightly overcrowded.", 3)
                    )
                )
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Failed to load profile")
            }
        }
    }
}
