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
    val achievements: Map<String, AchievementDoc> = emptyMap()
)

data class AchievementDoc(
    val name: String = "",
    val description: String = "",
    val icon: String = "",
    val unlocked: Boolean = false,
    val progress: Int = 0,
    val target: Int = 0
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

data class TripDoc(
    val id: String = "",
    val routeId: String = "",
    val busId: String = "",
    val startLocationName: String = "",
    val endLocationName: String = "",
    val distanceKm: Double = 0.0,
    val totalFare: Double = 0.0,
    val pointsEarned: Int = 0,
    val status: String = "COMPLETED",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null
)

data class FeedbackDoc(
    val id: String = "",
    val busId: String = "",
    val routeId: String = "",
    val comment: String = "",
    val ratings: Map<String, Int> = emptyMap(),
    val tags: List<String> = emptyList(),
    val timestamp: Timestamp? = null
)
