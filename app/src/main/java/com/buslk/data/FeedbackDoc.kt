package com.buslk.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Data class representing a Feedback/Rating document submitted by a passenger.
 */
data class FeedbackDoc(
    val feedbackId: String = "",
    val tripId: String = "",
    val userId: String = "",
    val busId: String = "",
    val routeId: String = "",
    val ratings: Map<String, Int> = emptyMap(), // Ex: { "overall": 4, "cleanliness": 5 }
    val tags: List<String> = emptyList(),
    val comment: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null
)
