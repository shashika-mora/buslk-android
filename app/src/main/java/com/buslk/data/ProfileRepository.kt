package com.buslk.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

interface IProfileRepository {
    suspend fun getUserProfile(userId: String): Result<UserDoc>
    suspend fun getUserTrips(userId: String): Result<List<TripDoc>>
    suspend fun getAchievements(): Result<List<AchievementDoc>>
    suspend fun getUserUnlockedAchievements(userId: String): Result<List<String>>
}

class ProfileRepository : IProfileRepository {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun getUserProfile(userId: String): Result<UserDoc> {
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            val user = snapshot.toObject(UserDoc::class.java)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserTrips(userId: String): Result<List<TripDoc>> {
        return try {
            val snapshot = db.collection("trips")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val trips = snapshot.documents.mapNotNull { doc ->
                doc.toObject(TripDoc::class.java)?.copy(id = doc.id)
            }
            Result.success(trips)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAchievements(): Result<List<AchievementDoc>> {
        return try {
            val snapshot = db.collection("achievements").get().await()
            val achievements = snapshot.documents.mapNotNull { doc ->
                doc.toObject(AchievementDoc::class.java)?.copy(id = doc.id)
            }
            Result.success(achievements)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserUnlockedAchievements(userId: String): Result<List<String>> {
        return try {
            val snapshot = db.collection("users").document(userId)
                .collection("user_achievements")
                .get()
                .await()
            
            val unlockedIds = snapshot.documents.mapNotNull { doc ->
                doc.getString("achievementId") ?: doc.id // Fallback to doc ID if field doesn't exist
            }
            Result.success(unlockedIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
