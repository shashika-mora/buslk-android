package com.buslk.ui.viewmodels

import com.buslk.data.FeedbackDoc
import com.buslk.data.TripDoc
import com.buslk.data.UserDoc

/**
 * Represents the current state of the Profile screen.
 * Follows the Unidirectional Data Flow (UDF) architectural pattern.
 */
sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    data class Success(
        val userProfile: UserDoc,
        val tripHistory: List<TripDoc>,
        val feedbacks: List<FeedbackDoc>
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}
