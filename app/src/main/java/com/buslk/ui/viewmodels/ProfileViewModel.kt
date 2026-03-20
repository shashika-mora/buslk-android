package com.buslk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buslk.data.UserDoc
import com.buslk.data.UserStats
import com.buslk.data.TripDoc
import com.buslk.data.FeedbackDoc
import com.buslk.data.AchievementDoc
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    data class Success(
        val userProfile: UserDoc,
        val tripHistory: List<TripDoc> = emptyList(),
        val feedbacks: List<FeedbackDoc> = emptyList()
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfileData(uid: String) {
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            try {
                // Mocking data for the advanced UI
                _uiState.value = ProfileUiState.Success(
                    userProfile = UserDoc(
                        uid = if (uid.isBlank()) "guest_123" else uid,
                        displayName = "Malini Perera",
                        email = "malini.p@example.com",
                        points = 2450,
                        level = "Elite Commuter",
                        stats = UserStats(totalTrips = 124, reportsSubmitted = 15),
                        achievements = mapOf(
                            "early_bird" to AchievementDoc("Early Bird", "10 morning trips", "🌅", true, 10, 10),
                            "reporter" to AchievementDoc("Reporter", "20 crowd reports", "📊", false, 12, 20)
                        )
                    ),
                    tripHistory = listOf(
                        TripDoc(
                            tripId = "1",
                            userId = uid,
                            routeId = "138-homagama", 
                            busId = "NB-5521", 
                            startLocationName = "Maharagama", 
                            endLocationName = "Homagama",
                            distanceKm = 12.5,
                            totalFare = 120.0,
                            pointsEarned = 15,
                            status = "COMPLETED",
                            startTime = Timestamp(Date())
                        ),
                        TripDoc(
                            tripId = "2",
                            userId = uid,
                            routeId = "120-fort", 
                            busId = "LY-8842", 
                            startLocationName = "Piliyandala", 
                            endLocationName = "Colombo Fort",
                            distanceKm = 18.2,
                            totalFare = 180.0,
                            pointsEarned = 20,
                            status = "COMPLETED",
                            startTime = Timestamp(Date(System.currentTimeMillis() - 86400000))
                        )
                    ),
                    feedbacks = listOf(
                        FeedbackDoc(
                            id = "1",
                            busId = "NB-5521",
                            routeId = "138",
                            comment = "The driver was very polite and the bus was clean.",
                            ratings = mapOf("overall" to 5, "cleanliness" to 5, "comfort" to 4, "driver" to 5),
                            tags = listOf("Clean", "On-Time"),
                            timestamp = Timestamp(Date())
                        )
                    )
                )
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Failed to load profile")
            }
        }
    }
}
