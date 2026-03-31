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

sealed class TripUiState {
    object Idle : TripUiState()
    object Loading : TripUiState()
    data class CheckedIn(val tripId: String, val busId: String) : TripUiState()
    object Finished : TripUiState()
    data class Error(val message: String) : TripUiState()
}

class TripViewModel(
    private val tripRepository: ITripRepository = TripRepository()
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
                _uiState.value = TripUiState.CheckedIn(tripId = tripId, busId = busId)
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
        if (tripId == null) {
            _uiState.value = TripUiState.Finished
            return
        }

        viewModelScope.launch {
            _uiState.value = TripUiState.Loading
            val result = tripRepository.endTrip(tripId)
            if (result.isSuccess) {
                activeTripId = null
                _uiState.value = TripUiState.Finished
            } else {
                _uiState.value = TripUiState.Error(result.exceptionOrNull()?.message ?: "Failed to end trip")
            }
        }
    }
}

class TripViewModelFactory(
    private val repository: ITripRepository = TripRepository()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TripViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
