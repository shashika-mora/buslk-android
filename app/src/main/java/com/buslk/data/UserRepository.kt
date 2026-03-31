package com.buslk.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Interface representing User Profile data operations.
 * OOD Principle: Dependency Inversion.
 */
interface IUserRepository {
    suspend fun getUserProfile(uid: String): Result<UserDoc?>
    suspend fun getAllUsers(): Result<List<UserDoc>>
}

class UserRepository : IUserRepository {
    private val firestore = FirebaseFirestore.getInstance()

    override suspend fun getUserProfile(uid: String): Result<UserDoc?> {
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            val user = snapshot.toObject(UserDoc::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching user profile", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllUsers(): Result<List<UserDoc>> {
        return try {
            val snapshot = firestore.collection("users").get().await()
            val users = snapshot.documents.mapNotNull { it.toObject(UserDoc::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching all users", e)
            Result.failure(e)
        }
    }
}
