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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Interface representing the Authentication Use Cases.
 * OOD Principle: Abstraction & Dependency Inversion Principle (SOLID).
 * By depending on this interface rather than a concrete implementation,
 * UI and ViewModels become loosely coupled and easily testable.
 */
interface IAuthRepository {
    fun getCurrentUser(): FirebaseUser?
    fun signOut()
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser>
    suspend fun signUpWithEmailAndPassword(email: String, password: String, username: String? = null): Result<FirebaseUser>
    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser>
}

/**
 * Concrete implementation of [IAuthRepository] handling Firebase Authentication.
 * OOD Principle: Encapsulation. This class hides the complex details of
 * CredentialManager and Firebase APIs from the rest of the application.
 */
class AuthRepository : IAuthRepository {
    // OOD Principle: Encapsulation - auth instance is private.
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /** Returns the currently signed-in Firebase user, or null if not signed in. */
    override fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /** Signs the user out of Firebase. */
    override fun signOut() = auth.signOut()

    /**
     * Signs in a user with their email address and password.
     *
     * @return [Result.success] with the [FirebaseUser] on success, or [Result.failure] on error.
     */
    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return Result.failure(Exception("Sign-in succeeded but user is null"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a new user account with the given email address and password.
     * Note: Updating the display name requires a separate `updateProfile` call,
     * which can be done immediately after creation or let DataSeeder handle the name.
     *
     * @return [Result.success] with the [FirebaseUser] on success, or [Result.failure] on error.
     */
    override suspend fun signUpWithEmailAndPassword(email: String, password: String, username: String?): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return Result.failure(Exception("Sign-up succeeded but user is null"))
            
            // Sync user document in Firestore (Registration case)
            syncUserToFirestore(user, isInitialRegistration = true, username = username)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Launches the Google Sign-In bottom sheet via Credential Manager, exchanges the
     * returned Google ID token for a Firebase credential, and signs in.
     *
     * @return [Result.success] with the [FirebaseUser] on success, or [Result.failure] on error.
     */
    override suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
            val credentialManager = CredentialManager.create(context)

            // Build the Google ID option
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

            // Extract the Google ID token
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            // Exchange for a Firebase AuthCredential
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()

            val user = authResult.user ?: return Result.failure(Exception("Sign-in succeeded but user is null"))
            
            // Sync user document in Firestore (Google handles both login and registration)
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

    /**
     * Synchronizes the FirebaseUser state with the Firestore /users collection.
     * 
     * OOD Principle: Encapsulation & Robustness.
     * It ensures that new users get default fields (points, level, stats)
     * while existing users only get their 'updatedAt' timestamp or profile info refreshed,
     * protecting their accrued balance (points) and trip history.
     */
    private suspend fun syncUserToFirestore(user: FirebaseUser, isInitialRegistration: Boolean = false, username: String? = null) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user.uid)
        
        try {
            val userSnap = userRef.get().await()
            val existingUser = userSnap.toObject(UserDoc::class.java)

            if (existingUser == null || isInitialRegistration) {
                // NEW USER case: Initialize with default schema from db.md
                val newUserDoc = UserDoc(
                    uid = user.uid,
                    displayName = username ?: user.displayName ?: if (isInitialRegistration) "New User" else "Google User",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString() ?: "",
                    role = "Passenger" // Default role as per auth.md
                )
                userRef.set(newUserDoc).await()
            } else {
                // EXISTING USER case: Update volatile fields only, preserving history/points
                val updates = mutableMapOf<String, Any>(
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                
                // Refresh profile fields if they changed in Google/Auth
                user.displayName?.let { updates["displayName"] = it }
                user.photoUrl?.let { updates["photoUrl"] = it.toString() }
                
                userRef.update(updates).await()
            }
        } catch (e: Exception) {
            // Log error or handle silently depending on criticality
            println("Firestore sync failed: ${e.message}")
        }
    }
}
