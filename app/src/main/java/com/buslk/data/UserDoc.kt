package com.buslk.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Data class representing a User document in Firestore.
 * OOD Principle: Data Transfer Object (DTO) / Entity.
 * This class simply holds data representing the "User" state. It does
 * not contain business logic, adhering to the Single Responsibility Principle.
 * 
 * Aligned with docs/db.md specification.
 */
data class UserDoc(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val role: String = "Passenger", // "Passenger" | "Driver" | "Admin"
    val points: Int = 0,
    val level: String = "Newcomer",
    @ServerTimestamp
    val joinedAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    val stats: UserStats = UserStats(),
    val preferences: UserPreferences = UserPreferences()
)

/**
 * Nested stats for the user.
 */
data class UserStats(
    val totalTrips: Int = 0,
    val reportsSubmitted: Int = 0,
    val totalDistanceKm: Double = 0.0
)

/**
 * User preferences.
 */
data class UserPreferences(
    val language: String = "en",
    val notificationsEnabled: Boolean = true,
    val savedRoutes: List<String> = emptyList()
)
