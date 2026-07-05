package com.buslk.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.buslk.data.BusDoc
import com.buslk.data.FeedbackDoc
import com.buslk.data.IFeedbackRepository
import com.buslk.data.ILiveMapRepository
import com.buslk.data.ISearchRepository
import com.buslk.domain.models.BusLocation
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BusDetailsUiState {
    object Idle : BusDetailsUiState()
    object Loading : BusDetailsUiState()
    data class Success(
        val busDoc: BusDoc,
        val liveLocation: BusLocation?,
        val feedbacks: List<FeedbackDoc>,
        val averageRating: Float
    ) : BusDetailsUiState()
    data class Error(val message: String) : BusDetailsUiState()
}

class BusDetailsViewModel(
    private val searchRepository: ISearchRepository,
    private val feedbackRepository: IFeedbackRepository,
    private val liveMapRepository: ILiveMapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BusDetailsUiState>(BusDetailsUiState.Idle)
    val uiState: StateFlow<BusDetailsUiState> = _uiState.asStateFlow()

    private var currentBusId: String? = null
    private var rtdbJob: Job? = null

    // Hold the static data so we can just update the live location when RTDB ticks
    private var staticBusDoc: BusDoc? = null
    private var staticFeedbacks: List<FeedbackDoc> = emptyList()
    private var avgRating: Float = 0f

    fun loadBus(busId: String) {
        if (currentBusId == busId) return
        currentBusId = busId
        
        rtdbJob?.cancel()
        _uiState.value = BusDetailsUiState.Loading

        viewModelScope.launch {
            // 1. Fetch Static Profile Data
            val busResult = searchRepository.getBusDetails(busId)
            val feedbackResult = feedbackRepository.getBusFeedback(busId)

            if (busResult.isSuccess && feedbackResult.isSuccess) {
                val doc = busResult.getOrNull()
                if (doc == null) {
                    _uiState.value = BusDetailsUiState.Error("Bus not found in registry")
                    return@launch
                }

                staticBusDoc = doc
                staticFeedbacks = feedbackResult.getOrDefault(emptyList())
                val overallRatings = staticFeedbacks.mapNotNull { it.ratings["overall"]?.toFloat() }
                avgRating = if (overallRatings.isNotEmpty()) {
                    overallRatings.average().toFloat()
                } else {
                    0f
                }

                // Initial emit without live location
                _uiState.value = BusDetailsUiState.Success(
                    busDoc = staticBusDoc!!,
                    liveLocation = null,
                    feedbacks = staticFeedbacks,
                    averageRating = avgRating
                )

                // 2. Subscribe to Live GPS Stream
                observeLiveLocation(busId)
            } else {
                val e = busResult.exceptionOrNull() ?: feedbackResult.exceptionOrNull()
                _uiState.value = BusDetailsUiState.Error(e?.localizedMessage ?: "Failed to load bus details")
            }
        }
    }

    private fun observeLiveLocation(busId: String) {
        rtdbJob = viewModelScope.launch {
            liveMapRepository.getLiveBusLocations().collect { result ->
                result.onSuccess { allBuses ->
                    val myBusLive = allBuses.find { normalizeBusId(it.busId) == normalizeBusId(busId) }
                    if (staticBusDoc != null && _uiState.value is BusDetailsUiState.Success) {
                        _uiState.value = BusDetailsUiState.Success(
                            busDoc = staticBusDoc!!,
                            liveLocation = myBusLive,
                            feedbacks = staticFeedbacks,
                            averageRating = avgRating
                        )
                    }
                }
            }
        }
    }

    private fun normalizeBusId(id: String): String {
        return id.lowercase().replace(Regex("[^a-z0-9]"), "")
    }

    fun clear() {
        rtdbJob?.cancel()
        currentBusId = null
        staticBusDoc = null
        _uiState.value = BusDetailsUiState.Idle
    }
}

class BusDetailsViewModelFactory(
    private val searchRepository: ISearchRepository,
    private val feedbackRepository: IFeedbackRepository,
    private val liveMapRepository: ILiveMapRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BusDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BusDetailsViewModel(searchRepository, feedbackRepository, liveMapRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
