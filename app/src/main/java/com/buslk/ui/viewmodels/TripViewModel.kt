package com.buslk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.buslk.data.ITripRepository
import com.buslk.data.TripRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.buslk.data.ISearchRepository
import com.buslk.data.SearchRepository
import com.buslk.data.BusDoc
import com.buslk.data.IFeedbackRepository
import com.buslk.data.FeedbackRepository
import com.buslk.data.FeedbackDoc
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

sealed class TripUiState {
    object Idle : TripUiState()
    object Loading : TripUiState()
    data class CheckedIn(
        val tripId: String, 
        val busId: String, 
        val routeName: String = "", 
        val regNum: String = "",
        val type: String = "",
        val capacity: Int = 0,
        val owner: String = "",
        val overallRating: Double = 4.5,
        val recentComment: String = "",
        val recentUser: String = "",
        val feedbacks: List<FeedbackDoc> = emptyList()
    ) : TripUiState()
    object Finished : TripUiState()
    data class Error(val message: String) : TripUiState()
}

class TripViewModel(
    private val tripRepository: ITripRepository = TripRepository(),
    private val searchRepository: ISearchRepository = SearchRepository(),
    private val feedbackRepository: IFeedbackRepository = FeedbackRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<TripUiState>(TripUiState.Idle)
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()

    // Holds the user's active bus journey.
    private var activeTripId: String? = null

    fun checkIn(busId: String) {
        viewModelScope.launch {
            _uiState.value = TripUiState.Loading
            
            // In a real production app, we would use a robust Auth mechanism.
            // For MVP, we get it straight from FirebaseAuth if available, or simulate it.
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous_user"
            
            val result = tripRepository.startTrip(userId, busId)
            if (result.isSuccess) {
                val tripId = result.getOrNull() ?: ""
                activeTripId = tripId
                
                // Fetch dynamic bus information rather than using mocks
                val busData = searchRepository.getBusDetails(busId).getOrNull()
                val routeName = busData?.defaultRouteId ?: "Unknown Route"
                val regNum = busData?.registrationNumber ?: busId
                val type = busData?.type ?: "Standard"
                val capacity = busData?.capacity ?: 40
                val owner = busData?.owner ?: "Unknown Owner"
                
                // Fetch feedback for this bus
                val feedbackList = feedbackRepository.getBusFeedback(busId).getOrNull() ?: emptyList()
                val overallRating = if (feedbackList.isNotEmpty()) {
                    feedbackList.map { it.ratings["overall"] ?: 5 }.average()
                } else {
                    4.5 // Default fallback if no reviews exist in DB yet
                }
                val recentComment = feedbackList.firstOrNull()?.comment ?: "Punctual bus, comfortable ride!"
                val recentUser = feedbackList.firstOrNull()?.userId?.take(6) ?: "User12"
                
                _uiState.value = TripUiState.CheckedIn(
                    tripId = tripId, 
                    busId = busId, 
                    routeName = routeName, 
                    regNum = regNum,
                    type = type,
                    capacity = capacity,
                    owner = owner,
                    overallRating = overallRating,
                    recentComment = recentComment,
                    recentUser = recentUser,
                    feedbacks = feedbackList
                )
            } else {
                _uiState.value = TripUiState.Error(result.exceptionOrNull()?.message ?: "Check-in failed")
            }
        }
    }

    fun reportCrowdLevel(level: String) {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous_user"
            // We launch this fire-and-forget stat increment, giving the UI instant feedback 
            tripRepository.reportCrowdLevel(userId, level)
        }
    }

    fun endTrip() {
        val tripId = activeTripId
        val busId = (_uiState.value as? TripUiState.CheckedIn)?.busId
        if (tripId == null) {
            _uiState.value = TripUiState.Finished
            return
        }

        viewModelScope.launch {
            _uiState.value = TripUiState.Loading
            val result = tripRepository.endTrip(tripId)
            if (result.isSuccess) {
                if (busId != null) {
                    try {
                        val hasOthersResult = tripRepository.hasOtherActivePassengers(busId)
                        val hasOthers = hasOthersResult.getOrDefault(false)
                        if (!hasOthers) {
                            FirebaseDatabase.getInstance("https://buslk-app-default-rtdb.asia-southeast1.firebasedatabase.app")
                                .reference.child("bus_locations").child(busId).removeValue().await()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("TripViewModel", "Error deleting live location for bus $busId", e)
                    }
                }
                activeTripId = null
                _uiState.value = TripUiState.Finished
            } else {
                _uiState.value = TripUiState.Error(result.exceptionOrNull()?.message ?: "Failed to end trip")
            }
        }
    }

    fun checkActiveTripState() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId.isNullOrBlank()) return

        viewModelScope.launch {
            _uiState.value = TripUiState.Loading
            val tripsResult = tripRepository.getUserTrips(userId)
            val activeTrip = tripsResult.getOrNull()?.find { it.status == "ACTIVE" }
            if (activeTrip != null) {
                activeTripId = activeTrip.tripId
                val busId = activeTrip.busId
                
                // Fetch dynamic bus info
                val busData = searchRepository.getBusDetails(busId).getOrNull()
                val routeName = busData?.defaultRouteId ?: "Unknown Route"
                val regNum = busData?.registrationNumber ?: busId
                val type = busData?.type ?: "Standard"
                val capacity = busData?.capacity ?: 40
                val owner = busData?.owner ?: "Unknown Owner"
                
                // Fetch feedback
                val feedbackList = feedbackRepository.getBusFeedback(busId).getOrNull() ?: emptyList()
                val overallRating = if (feedbackList.isNotEmpty()) {
                    feedbackList.map { it.ratings["overall"] ?: 5 }.average()
                } else {
                    4.5
                }
                val recentComment = feedbackList.firstOrNull()?.comment ?: "Punctual bus, comfortable ride!"
                val recentUser = feedbackList.firstOrNull()?.userId?.take(6) ?: "User12"

                _uiState.value = TripUiState.CheckedIn(
                    tripId = activeTrip.tripId,
                    busId = busId,
                    routeName = routeName,
                    regNum = regNum,
                    type = type,
                    capacity = capacity,
                    owner = owner,
                    overallRating = overallRating,
                    recentComment = recentComment,
                    recentUser = recentUser,
                    feedbacks = feedbackList
                )
            } else {
                _uiState.value = TripUiState.Idle
            }
        }
    }

    fun resetState() {
        activeTripId = null
        _uiState.value = TripUiState.Idle
    }
}

class TripViewModelFactory(
    private val tripRepository: ITripRepository = TripRepository(),
    private val searchRepository: ISearchRepository = SearchRepository(),
    private val feedbackRepository: IFeedbackRepository = FeedbackRepository()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TripViewModel(tripRepository, searchRepository, feedbackRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
