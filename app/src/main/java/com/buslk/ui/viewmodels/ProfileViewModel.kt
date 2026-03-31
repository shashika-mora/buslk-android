package com.buslk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buslk.data.UserDoc
import com.buslk.data.UserStats
import com.buslk.data.TripDoc
import com.buslk.data.FeedbackDoc
import com.buslk.data.AchievementDoc
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

    private val db = FirebaseFirestore.getInstance()
    private var userListener: ListenerRegistration? = null
    private var tripsListener: ListenerRegistration? = null
    private var feedbacksListener: ListenerRegistration? = null

    // Mutable state to hold realtime data
    private var currentUserDoc: UserDoc? = null
    private var currentTrips: List<TripDoc> = emptyList()
    private var currentFeedbacks: List<FeedbackDoc> = emptyList()

    fun loadProfileData(uid: String) {
        if (uid.isBlank()) {
            _uiState.value = ProfileUiState.Error("User ID is empty")
            return
        }
        
        _uiState.value = ProfileUiState.Loading
        
        // Clear previous listeners if any
        userListener?.remove()
        tripsListener?.remove()
        feedbacksListener?.remove()

        // 1. Listen to User Document
        userListener = db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                _uiState.value = ProfileUiState.Error(error.message ?: "Failed to fetch user data")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val userDoc = snapshot.toObject(UserDoc::class.java)
                if (userDoc != null) {
                    currentUserDoc = userDoc
                    emitSuccessState()
                }
            } else {
                _uiState.value = ProfileUiState.Error("User profile not found")
            }
        }

        // 2. Listen to Trips
        tripsListener = db.collection("trips")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    currentTrips = snapshot.toObjects(TripDoc::class.java).sortedByDescending { it.startTime }
                    emitSuccessState()
                }
            }

        // 3. Listen to Feedbacks
        feedbacksListener = db.collection("feedbacks")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    currentFeedbacks = snapshot.toObjects(FeedbackDoc::class.java).sortedByDescending { it.timestamp }
                    emitSuccessState()
                }
            }
    }

    private fun emitSuccessState() {
        val userItem = currentUserDoc ?: return // Don't emit success until we have the user doc
        _uiState.value = ProfileUiState.Success(
            userProfile = userItem,
            tripHistory = currentTrips,
            feedbacks = currentFeedbacks
        )
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        tripsListener?.remove()
        feedbacksListener?.remove()
    }
}
