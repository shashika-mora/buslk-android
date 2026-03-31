package com.buslk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.buslk.data.FeedbackDoc
import com.buslk.data.FeedbackRepository
import com.buslk.data.IFeedbackRepository
import com.buslk.data.ISearchRepository
import com.buslk.data.SearchRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FeedbackUiState {
    object Idle : FeedbackUiState()
    object Submitting : FeedbackUiState()
    object Success : FeedbackUiState()
    data class Error(val message: String) : FeedbackUiState()
}

class FeedbackViewModel(
    private val feedbackRepository: IFeedbackRepository = FeedbackRepository(),
    private val searchRepository: ISearchRepository = SearchRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeedbackUiState>(FeedbackUiState.Idle)
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    fun submitFeedback(
        busId: String,
        overallRating: Int,
        cleanlinessRating: Int,
        comfortRating: Int,
        driverRating: Int,
        comment: String,
        tags: List<String>
    ) {
        viewModelScope.launch {
            _uiState.value = FeedbackUiState.Submitting
            
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                _uiState.value = FeedbackUiState.Error("You must be logged in to submit feedback.")
                return@launch
            }

            val ratingsMap = mapOf(
                "overall" to overallRating,
                "cleanliness" to cleanlinessRating,
                "comfort" to comfortRating,
                "driver" to driverRating
            )

            // Fetch actual route mappings
            val busDoc = searchRepository.getBusDetails(busId).getOrNull()
            val routeName = busDoc?.defaultRouteId ?: "Unknown Route"

            val feedbackDoc = FeedbackDoc(
                feedbackId = "", // Firestore auto-id handles this
                userId = userId,
                busId = busId,
                routeId = routeName,
                comment = comment,
                ratings = ratingsMap,
                tags = tags,
                timestamp = Timestamp.now()
            )

            val result = feedbackRepository.submitFeedback(feedbackDoc)
            if (result.isSuccess) {
                _uiState.value = FeedbackUiState.Success
            } else {
                _uiState.value = FeedbackUiState.Error(result.exceptionOrNull()?.message ?: "Submission failed")
            }
        }
    }

    fun resetState() {
        _uiState.value = FeedbackUiState.Idle
    }
}

class FeedbackViewModelFactory(
    private val feedbackRepository: IFeedbackRepository = FeedbackRepository(),
    private val searchRepository: ISearchRepository = SearchRepository()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedbackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedbackViewModel(feedbackRepository, searchRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
