package com.buslk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buslk.data.ILiveMapRepository
import com.buslk.domain.models.BusLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * OOP: Sealed class representing the finite, mutually exclusive states of the Map Screen.
 * The UI can exhaustively check 'when (state)' and never be in a weird intermediate state.
 */
sealed class MapUiState {
    object Loading : MapUiState()
    data class Success(val activeBuses: List<BusLocation>) : MapUiState()
    data class Error(val message: String) : MapUiState()
}

/**
 * The "Brain" of the HomeScreen Map.
 * It subscribes to the highly volatile RTDB Flow and transforms it into a stable
 * StateFlow that Jetpack Compose can react to cleanly.
 */
class MapViewModel(
    private val liveMapRepository: ILiveMapRepository
) : ViewModel() {

    // Internal mutable state
    private val _mapState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    
    // Public immutable state for the View to observe
    val mapState: StateFlow<MapUiState> = _mapState.asStateFlow()

    // Track the active location tracking coroutine job so we can cancel and restart
    // it dynamically when the route filter changes.
    private var observeJob: kotlinx.coroutines.Job? = null

    init {
        // Automatically start listening to the GPS stream upon creation (no filter)
        observeBusLocations(null)
    }

    /**
     * Updates the route filter and restarts the location listener.
     */
    fun setRouteFilter(routeId: String?) {
        observeBusLocations(routeId)
    }

    private fun observeBusLocations(routeId: String?) {
        // Cancel the previous listener job to prevent duplicate connections and memory leaks
        observeJob?.cancel()
        
        // viewModelScope ensures this coroutine dies immediately when the ViewModel is destroyed
        observeJob = viewModelScope.launch {
            liveMapRepository.getLiveBusLocations(routeId).collect { result ->
                result.fold(
                    onSuccess = { buses ->
                        // The repository decoded the RTDB perfectly, update the UI with new markers
                        _mapState.value = MapUiState.Success(buses)
                    },
                    onFailure = { exception ->
                        // A network drop or JSON parse error occurred
                        _mapState.value = MapUiState.Error(exception.message ?: "Lost GPS connection")
                    }
                )
            }
        }
    }
}
