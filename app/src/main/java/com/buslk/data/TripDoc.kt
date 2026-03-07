package com.buslk.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class TripDoc(
    val id: String = "",
    val userId: String = "",
    val busNumber: String = "",
    val type: String = "Private Bus",
    val startLocation: String = "",
    val destination: String = "",
    val points: Int = 0,
    @ServerTimestamp
    val timestamp: Timestamp? = null
)
