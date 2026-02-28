package com.buslk.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun syncUserToFirestore(user: FirebaseUser): Result<Unit> {
        return try {
            val userRef = firestore.collection("users").document(user.uid)
            val snapshot = userRef.get().await()

            if (!snapshot.exists()) {
                // Create a new user profile with default schema
                val newUser = mapOf(
                    "uid" to user.uid,
                    "email" to user.email,
                    "displayName" to user.displayName,
                    "photoUrl" to user.photoUrl?.toString(),
                    "role" to "Passenger",
                    "points" to 0,
                    "level" to "Newcomer",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(newUser).await()
                Log.d("AuthRepository", "Created new Firestore user document for: ${user.uid}")
            } else {
                // Update volatile metadata for existing user
                val updates = mapOf(
                    "displayName" to user.displayName,
                    "photoUrl" to user.photoUrl?.toString(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.update(updates).await()
                Log.d("AuthRepository", "Updated existing Firestore user document for: ${user.uid}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error syncing user to Firestore", e)
            Result.failure(e)
        }
    }
}
