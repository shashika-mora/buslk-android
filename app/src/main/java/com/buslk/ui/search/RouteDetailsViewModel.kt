package com.buslk.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.buslk.data.ILiveMapRepository
import com.buslk.domain.models.BusLocation
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.buslk.data.ISearchRepository
import com.buslk.data.BusDoc

data class RouteBusItem(
    val busDoc: BusDoc,
    val liveLocation: BusLocation? = null
)

sealed class RouteDetailsUiState {
    object Idle : RouteDetailsUiState()
    object Loading : RouteDetailsUiState()
    data class Success(val buses: List<RouteBusItem>) : RouteDetailsUiState()
    data class Error(val message: String) : RouteDetailsUiState()
}

class RouteDetailsViewModel(
    private val liveMapRepository: ILiveMapRepository,
    private val searchRepository: ISearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RouteDetailsUiState>(RouteDetailsUiState.Idle)
    val uiState: StateFlow<RouteDetailsUiState> = _uiState.asStateFlow()

    private var observationJob: Job? = null
    private var currentRouteId: String? = null

    fun observeRoute(routeId: String) {
        if (currentRouteId == routeId) return // Already observing this route

        currentRouteId = routeId
        observationJob?.cancel()
        _uiState.value = RouteDetailsUiState.Loading

        observationJob = viewModelScope.launch {
            // First fetch the static list of all buses registered to this route
            val staticBusesResult = searchRepository.getBusesByRoute(routeId)
            val staticBuses = staticBusesResult.getOrNull() ?: emptyList()
            
            // If zero buses, just show empty
            if (staticBuses.isEmpty()) {
                _uiState.value = RouteDetailsUiState.Success(emptyList())
                return@launch
            }
            
            // Map the static list to a default state (no live location initially)
            val initialList = staticBuses.map { RouteBusItem(it, null) }
            _uiState.value = RouteDetailsUiState.Success(initialList)

            // Now listen to the live RTDB stream
            liveMapRepository.getLiveBusLocations().collect { result ->
                result.fold(
                    onSuccess = { allLiveBuses ->
                        // Convert to a map for O(1) lookups
                        val liveBusMap = allLiveBuses.associateBy { normalizeBusId(it.busId) }
                        
                        // Merge static documents with realtime telemetry
                        val mergedList = staticBuses.map { bus ->
                            RouteBusItem(
                                busDoc = bus,
                                liveLocation = liveBusMap[normalizeBusId(bus.registrationNumber)]
                            )
                        }
                        _uiState.value = RouteDetailsUiState.Success(mergedList)
                    },
                    onFailure = { error ->
                        // Optional: we don't crash the state, just leave the static ones
                    }
                )
            }
        }
    }

    private fun normalizeBusId(id: String): String {
        return id.lowercase().replace(Regex("[^a-z0-9]"), "")
    }

    fun clear() {
        observationJob?.cancel()
        currentRouteId = null
        _uiState.value = RouteDetailsUiState.Idle
    }
}

class RouteDetailsViewModelFactory(
    private val liveMapRepository: ILiveMapRepository,
    private val searchRepository: ISearchRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RouteDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RouteDetailsViewModel(liveMapRepository, searchRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
