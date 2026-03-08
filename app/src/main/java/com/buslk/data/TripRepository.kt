package com.buslk.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
// import com.google.firebase.firestore.Query // Removed unused import
import kotlinx.coroutines.tasks.await

/**
 * Interface representing Trip History operations.
 * OOD Principle: Dependency Inversion.
 */
interface ITripRepository {
    suspend fun getUserTrips(userId: String): Result<List<TripDoc>>
}

class TripRepository : ITripRepository {
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Fetches all trips for a specific user, ordered by the most recent first locally.
     * Note: Avoids requiring a composite Firestore index.
     */
    override suspend fun getUserTrips(userId: String): Result<List<TripDoc>> {
        return try {
            val snapshot = firestore.collection("trips")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val trips = snapshot.toObjects(TripDoc::class.java)
            val sorted = trips.sortedByDescending { it.startTime }
            Result.success(sorted)
        } catch (e: Exception) {
            Log.e("TripRepository", "Error fetching trips for user $userId", e)
            Result.failure(e)
        }
    }
}
