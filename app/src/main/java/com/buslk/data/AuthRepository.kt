package com.buslk.data

import android.content.Context
import androidx.credentials.*
import androidx.credentials.exceptions.*
import com.buslk.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Interface representing the Authentication Use Cases.
 */
interface IAuthRepository {
    fun getCurrentUser(): FirebaseUser?
    fun signOut()
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser>
    suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<FirebaseUser>
    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser>
    suspend fun updateDisplayName(newName: String): Result<Unit>
    suspend fun changePassword(newPassword: String): Result<Unit>
}

/**
 * Concrete implementation handling Firebase Authentication.
 */
class AuthRepository : IAuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun getCurrentUser(): FirebaseUser? = auth.currentUser

    override fun signOut() = auth.signOut()

    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return Result.failure(Exception("Sign-in succeeded but user is null"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return Result.failure(Exception("Sign-up succeeded but user is null"))
            syncUserToFirestore(user, isInitialRegistration = true)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user ?: return Result.failure(Exception("Sign-in succeeded but user is null"))
            syncUserToFirestore(user)
            Result.success(user)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Sign-in cancelled"))
        } catch (e: GetCredentialException) {
            Result.failure(Exception("Sign-in failed: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDisplayName(newName: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
            
            // 1. Update Firebase Auth Profile
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()
            user.updateProfile(profileUpdates).await()

            // 2. Update Firestore Document
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid)
                .update("displayName", newName)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changePassword(newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncUserToFirestore(user: FirebaseUser, isInitialRegistration: Boolean = false) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user.uid)
        
        try {
            val userSnap = userRef.get().await()
            val existingUser = userSnap.toObject(UserDoc::class.java)

            if (existingUser == null || isInitialRegistration) {
                val newUserDoc = UserDoc(
                    uid = user.uid,
                    displayName = user.displayName ?: if (isInitialRegistration) "New User" else "Google User",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString() ?: "",
                    role = "Passenger"
                )
                userRef.set(newUserDoc).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                user.displayName?.let { updates["displayName"] = it }
                user.photoUrl?.let { updates["photoUrl"] = it.toString() }
                userRef.update(updates).await()
            }
        } catch (e: Exception) {
            println("Firestore sync failed: ${e.message}")
        }
    }
}
