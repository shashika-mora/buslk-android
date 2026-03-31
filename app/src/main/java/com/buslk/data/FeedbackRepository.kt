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
    suspend fun submitFeedback(feedback: FeedbackDoc): Result<Unit>
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

    override suspend fun submitFeedback(feedback: FeedbackDoc): Result<Unit> {
        return try {
            val feedbackRef = firestore.collection("feedback").document(feedback.feedbackId.ifBlank { firestore.collection("feedback").document().id })
            
            // Execute as an atomic transaction: write feedback doc + increment user points/stats
            firestore.runTransaction { transaction ->
                val userRef = firestore.collection("users").document(feedback.userId)
                val snapshot = transaction.get(userRef)
                
                // Write the feedback document securely
                transaction.set(feedbackRef, feedback.copy(feedbackId = feedbackRef.id))
                
                // Award points and increment report stat
                val currentPoints = snapshot.getLong("points") ?: 0
                transaction.update(userRef, "points", currentPoints + 15)
                
                val currentStats = snapshot.get("stats") as? Map<String, Any>
                val currentReports = (currentStats?.get("reportsSubmitted") as? Long) ?: 0L
                transaction.update(userRef, "stats.reportsSubmitted", currentReports + 1)
                
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FeedbackRepository", "Error submitting feedback", e)
            Result.failure(e)
        }
    }
}
