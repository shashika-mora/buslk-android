package com.buslk.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.buslk.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class GoogleAuthClient(
    private val context: Context,
) {
    private val credentialManager = CredentialManager.create(context)
    private val auth = FirebaseAuth.getInstance()

    suspend fun signInWithGoogle(): AuthResult? {
        val webClientId = context.getString(R.string.default_web_client_id)
        if (webClientId == "YOUR_WEB_CLIENT_ID_HERE") {
            Log.e("GoogleAuthClient", "Web Client ID is not configured in secrets.xml")
            return null
        }

        // Generate a random nonce for security
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Allow selecting any Google account
            .setServerClientId(webClientId)
            .setNonce(hashedNonce)
            .build()
            
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
            
        return try {
            val result = credentialManager.getCredential(
                request = request,
                context = context,
            )
            handleSignIn(result)
        } catch (e: GetCredentialException) {
            Log.e("GoogleAuthClient", "Google Sign in failed: ${e.message}")
            null
        }
    }
    
    private suspend fun handleSignIn(result: GetCredentialResponse): AuthResult? {
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                
                // Now authenticate with Firebase
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                return auth.signInWithCredential(firebaseCredential).await()
            } catch (e: Exception) {
                Log.e("GoogleAuthClient", "Firebase authentication failed: ${e.message}")
                return null
            }
        }
        return null
    }

    fun getSignedInUser() = auth.currentUser
    
    fun signOut() {
        auth.signOut()
    }
}
