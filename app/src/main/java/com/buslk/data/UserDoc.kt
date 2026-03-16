package com.buslk.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Data class representing a User document in Firestore.
 * 
 * OOD Principle: Data Transfer Object (DTO) / Entity.
 * This class simply holds data representing the "User" state. It does
 * not contain business logic, adhering to the Single Responsibility Principle.
 * By using a `data class`, Kotlin automatically generates useful methods like 
 * `equals()`, `hashCode()`, and `copy()`, making it ideal for holding state.
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
    // @ServerTimestamp tells the Firebase Android SDK to automatically populate 
    // this field with the server's exact time when writing to the database,
    // avoiding issues with incorrect client device clocks.
    @ServerTimestamp
    val joinedAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    val stats: UserStats = UserStats(),
    val preferences: UserPreferences = UserPreferences()
)

/**
 * Nested stats for the user, demonstrating object composition.
 */
data class UserStats(
    val totalTrips: Int = 0,
    val reportsSubmitted: Int = 0,
    val totalDistanceKm: Double = 0.0
)

/**
 * User preferences, keeping the data structure organized and modular.
 */
data class UserPreferences(
    val language: String = "en",
    val notificationsEnabled: Boolean = true,
    val savedRoutes: List<String> = emptyList()
)
