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
 * Repository responsible for handling all Firebase Authentication operations.
 * Supports Email/Password login, signup, and Google Sign-In via Android Credential Manager.
 */
class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /** Returns the currently signed-in Firebase user, or null if not signed in. */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /** Signs the user out of Firebase. */
    fun signOut() = auth.signOut()

    /**
     * Signs in a user with their email address and password.
     *
     * @return [Result.success] with the [FirebaseUser] on success, or [Result.failure] on error.
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
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
    suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return Result.failure(Exception("Sign-up succeeded but user is null"))
            
            // Create user document in Firestore
            val db = FirebaseFirestore.getInstance()
            val userDoc = UserDoc(
                uid = user.uid,
                displayName = "New User",
                email = user.email ?: email,
                points = 0,
                level = "Beginner"
            )
            db.collection("users").document(user.uid).set(userDoc).await()

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
    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
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
            
            // Generate user document in Firestore if it is a new user
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(user.uid)
            val userSnap = userRef.get().await()
            if (!userSnap.exists()) {
                val userDoc = UserDoc(
                    uid = user.uid,
                    displayName = user.displayName ?: "Google User",
                    email = user.email ?: "",
                    points = 0,
                    level = "Beginner"
                )
                userRef.set(userDoc).await()
            }

            Result.success(user)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Sign-in cancelled"))
        } catch (e: GetCredentialException) {
            Result.failure(Exception("Sign-in failed: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
