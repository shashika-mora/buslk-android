package com.buslk.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Data class representing an item submitted to the Lost and Found system.
 * This class accurately models the `lost_and_found` Firestore collection schema.
 */
data class LostAndFoundDoc(
    val itemId: String = "",
    val userId: String = "",          // FK -> users. DisplayName can be pulled or denormalized.
    val reporterName: String = "",    // Denormalized name to prevent N+1 User queries
    val busId: String = "",           // FK -> buses
    val routeId: String = "",         // FK -> routes
    val itemType: String = "LOST",    // "LOST" or "FOUND"
    val title: String = "",
    val description: String = "",
    val location: String = "",        // e.g "Seat 12A", "Under the back row"
    val imageUrl: String = "",
    val status: String = "OPEN",      // "OPEN", "RESOLVED", "CLAIMED"
    // ServerTimestamp tells the SDK to mark exactly when it reached the cloud
    @ServerTimestamp
    val timestamp: Timestamp? = null
)
