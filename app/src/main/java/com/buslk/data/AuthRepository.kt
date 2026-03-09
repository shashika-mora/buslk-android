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
 * UI and ViewModels become loosely coupled and easily testable.*/
interface IAuthRepository {

    /**
     * Retrieves the currently logged-in user from the local session.
     * @return [FirebaseUser]? - Returns the Firebase user object representing the profile (contains UID, Email, DisplayName). Returns null if no user is signed in.*/
    fun getCurrentUser(): FirebaseUser?


    /** Logs the current user out of the application and clears the session.*/
    fun signOut()


    /**
     * Authenticates an existing user using their email and password.
     *
     * @param email The user's email address (Data Type: String).
     * @param password The user's password (Data Type: String).
     * @return A Kotlin [Result] object. 
     *         - On success: Contains `Result.success(FirebaseUser)`.
     *         - On failure: Contains `Result.failure(Exception)`, wrapping the error (e.g., Wrong Password).*/
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser>


    /**
     * Registers a brand new user using an email and password.
     *
     * @param email The user's email address (Data Type: String).
     * @param password The user's password (Data Type: String). Must be at least 6 characters for Firebase.
     * @return [Result.success] with the [FirebaseUser] on success, or [Result.failure] on error.*/
    suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<FirebaseUser>


    /**
     * Triggers the Android native Google Sign-In bottom sheet via the Credential Manager.
     *
     * @param context The Android UI Context. This is required because launching a bottom sheet 
     *                requires a visual Context (like an Activity) to draw over.
     * @return [Result] wrapping the [FirebaseUser] on success.*/
    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser>
}


/**
 * Concrete implementation of [IAuthRepository] handling Firebase Authentication.
 * OOD Principle: Encapsulation. This class hides the complex details of
 * CredentialManager, Firebase APIs, and Network requests from the rest of the application.*/
class AuthRepository : IAuthRepository {
    // OOD Principle: Encapsulation - We make the FirebaseAuth instance 'private' 
    // so no outside class can accidentally modify the authentication state directly.
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun getCurrentUser(): FirebaseUser? = auth.currentUser

    override fun signOut() = auth.signOut()


    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            // "await()" is a Coroutine extension that pauses execution here until Firebase responds,
            // without blocking the main UI thread.
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return Result.failure(Exception("Sign-in succeeded but user is null"))
            
            Result.success(user)
        } catch (e: Exception) {
            // Catch block captures network errors or "Invalid Credentials" exceptions from Firebase.
            Result.failure(e)
        }
    }


    override suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return Result.failure(Exception("Sign-up succeeded but user is null"))
            
            // Critical Step: Once the user is created in Firebase 'Authentication', 
            // we must create a matching profile document in our Firestore 'users' Database.
            syncUserToFirestore(user, isInitialRegistration = true)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
            // CredentialManager is the modern Android API for handling saved passwords and Google Sign-in
            val credentialManager = CredentialManager.create(context)

            // Step 1: Configure the Google Request. We pass our Web Client ID to identify our app to Google.
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // false means show all Google accounts on the device
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Step 2: Launch the Bottom Sheet UI and wait for the user to select an account
            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            // Step 3: Extract the secure Token provided by Google
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            // Step 4: Give Google's Token to Firebase so Firebase can log us into our actual App database
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()

            val user = authResult.user ?: return Result.failure(Exception("Sign-in succeeded but user is null"))
            
            // Step 5: Sync the user into Firestore database. Google handles both Login and Registration simultaneously.
            syncUserToFirestore(user)

            Result.success(user)
        } catch (e: GetCredentialCancellationException) {
            // User swiped down or tapped outside the bottom sheet
            Result.failure(Exception("Sign-in cancelled"))
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            Result.failure(Exception("No saved Google passwords found. Please sign up or continue manually."))
        } catch (e: GetCredentialException) {
            Result.failure(Exception("Sign-in failed: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * Synchronizes the [FirebaseUser] state with the Firestore Database under the '/users' collection.
     * 
     * OOD Principle: Encapsulation & Robustness.
     * It ensures that new users get default fields (points, role) set up,
     * while existing users only get their 'updatedAt' timestamp or profile info refreshed.
     * This protects an existing user's accrued balance (gamification points) and trip history from being erased on login.
     *
     * @param user The authenticated FirebaseUser object containing basic identity.
     * @param isInitialRegistration Boolean flag. True if they just signed up via Email/Password. False via Google.*/
    private suspend fun syncUserToFirestore(user: FirebaseUser, isInitialRegistration: Boolean = false) {
        val db = FirebaseFirestore.getInstance()
        // Reference to specific document path: users/{Firebase_User_ID}
        val userRef = db.collection("users").document(user.uid)
        
        try {
            // Fetch the document to see if this user already exists in our database
            val userSnap = userRef.get().await()
            val existingUser = userSnap.toObject(UserDoc::class.java)

            if (existingUser == null || isInitialRegistration) {
                // NEW USER case: Instantiate our custom Data Structure (UserDoc)
                val newUserDoc = UserDoc(
                    uid = user.uid,
                    displayName = user.displayName ?: if (isInitialRegistration) "New User" else "Google User",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString() ?: "",
                    role = "Passenger" // Role-Based Access Control: Default to Passenger
                )
                // Write the Data Structure object to the database
                userRef.set(newUserDoc).await()
            } else {
                // EXISTING USER case: They already have an account. Just update their latest info.
                // We use a mutableMap (a Dictionary) to only target specific fields without touching others like 'points'.
                val updates = mutableMapOf<String, Any>(
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                
                // If they changed their name/photo in Google, sync it to our DB
                user.displayName?.let { updates["displayName"] = it }
                user.photoUrl?.let { updates["photoUrl"] = it.toString() }
                
                userRef.update(updates).await()
            }
        } catch (e: Exception) {
            // Log error to console for debugging
            println("Firestore sync failed: ${e.message}")
        }
    }
}
