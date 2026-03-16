package com.buslk.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Data class representing a User document in Firestore.
 */
data class UserDoc(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val role: String = "Passenger",
    val points: Int = 0,
    val level: String = "Newcomer",
    @ServerTimestamp
    val joinedAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    val stats: UserStats = UserStats(),
    val preferences: UserPreferences = UserPreferences(),
    val achievements: Map<String, Achievement> = emptyMap()
)

data class Achievement(
    val name: String = "",
    val description: String = "",
    val icon: String = "",
    val unlocked: Boolean = false
)

data class UserStats(
    val totalTrips: Int = 0,
    val reportsSubmitted: Int = 0,
    val totalDistanceKm: Double = 0.0
)

data class UserPreferences(
    val language: String = "en",
    val notificationsEnabled: Boolean = true,
    val savedRoutes: List<String> = emptyList()
)

data class Trip(val id: String, val route: String, val date: String, val fare: String)
data class Feedback(val id: String, val busNo: String, val comment: String, val rating: Int)
