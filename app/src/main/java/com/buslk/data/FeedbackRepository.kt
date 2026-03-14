package com.buslk.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Interface representing Feedback operations.
 * OOD Principle: Dependency Inversion.
 */
interface IFeedbackRepository {
    suspend fun getUserFeedback(userId: String): Result<List<FeedbackDoc>>
    suspend fun getBusFeedback(busId: String): Result<List<FeedbackDoc>>
}

class FeedbackRepository : IFeedbackRepository {
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Fetches all feedback left by a specific user, ordered locally by timestamp.
     * Note: Avoids requiring a composite Firestore index.
     */
    override suspend fun getUserFeedback(userId: String): Result<List<FeedbackDoc>> {
        return try {
            val snapshot = firestore.collection("feedback")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val feedback = snapshot.toObjects(FeedbackDoc::class.java)
            val sorted = feedback.sortedByDescending { it.timestamp }
            Result.success(sorted)
        } catch (e: Exception) {
            Log.e("FeedbackRepository", "Error fetching feedback for user $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches all feedback left for a specific bus, ordered by timestamp.
     */
    override suspend fun getBusFeedback(busId: String): Result<List<FeedbackDoc>> {
        return try {
            val snapshot = firestore.collection("feedback")
                .whereEqualTo("busId", busId)
                .get()
                .await()
            
            val feedback = snapshot.toObjects(FeedbackDoc::class.java)
            val sorted = feedback.sortedByDescending { it.timestamp }
            Result.success(sorted)
        } catch (e: Exception) {
            Log.e("FeedbackRepository", "Error fetching feedback for bus $busId", e)
            Result.failure(e)
        }
    }
}
