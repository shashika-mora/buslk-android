package com.buslk.data

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Interface representing Trip History operations.
 * OOD Principle: Dependency Inversion.
 */
interface ITripRepository {
    suspend fun getUserTrips(userId: String): Result<List<TripDoc>>
    suspend fun startTrip(userId: String, busId: String): Result<String>
    suspend fun endTrip(tripId: String): Result<Unit>
    suspend fun reportCrowdLevel(userId: String, level: String): Result<Unit>
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

    override suspend fun startTrip(userId: String, busId: String): Result<String> {
        return try {
            val tripRef = firestore.collection("trips").document() // Auto-generate ID
            val tripData = TripDoc(
                tripId = tripRef.id,
                userId = userId,
                busId = busId,
                status = "ACTIVE"
            )
            tripRef.set(tripData).await()
            Result.success(tripRef.id)
        } catch (e: Exception) {
            Log.e("TripRepository", "Error starting trip for user $userId", e)
            Result.failure(e)
        }
    }

    override suspend fun endTrip(tripId: String): Result<Unit> {
        return try {
            val tripRef = firestore.collection("trips").document(tripId)
            // Complete the trip. (Metrics calculation normally happens server-side with Cloud Functions, but mocked here)
            tripRef.update(
                "status", "COMPLETED",
                "endTime", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TripRepository", "Error ending trip $tripId", e)
            Result.failure(e)
        }
    }

    override suspend fun reportCrowdLevel(userId: String, level: String): Result<Unit> {
        return try {
            val userRef = firestore.collection("users").document(userId)

            // OOD Principle (Encapsulation): FieldValue.increment() is an atomic,
            // server-side operation. It removes the need for a read-then-write
            // transaction entirely, eliminating the race condition where two
            // concurrent updates could overwrite each other.
            //
            // Gamification note (gamification.md §1): SUBMIT_REPORT = +15 XP.
            // Using +5 here as a lightweight MVP client-side incentive for crowd
            // level reporting. Full point logic will move to Cloud Functions post-MVP.
            userRef.update("points", FieldValue.increment(5)).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TripRepository", "Error updating crowd level points for user $userId", e)
            Result.failure(e)
        }
    }
}

