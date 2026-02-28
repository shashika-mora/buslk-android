package com.buslk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.buslk.auth.GoogleAuthClient
import com.buslk.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Syncing : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val googleAuthClient: GoogleAuthClient,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Check if already signed in
        val currentUser = googleAuthClient.getSignedInUser()
        if (currentUser != null) {
            _authState.value = AuthState.Success(currentUser)
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = googleAuthClient.signInWithGoogle()
            if (result?.user != null) {
                _authState.value = AuthState.Syncing
                val syncResult = authRepository.syncUserToFirestore(result.user!!)
                if (syncResult.isSuccess) {
                    _authState.value = AuthState.Success(result.user!!)
                } else {
                    _authState.value = AuthState.Error(syncResult.exceptionOrNull()?.message ?: "Failed to sync user data")
                    googleAuthClient.signOut() // Sign out if sync fails to avoid inconsistent state
                }
            } else {
                _authState.value = AuthState.Error("Google Sign-In failed")
            }
        }
    }

    fun signOut() {
        googleAuthClient.signOut()
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

class AuthViewModelFactory(
    private val googleAuthClient: GoogleAuthClient,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(googleAuthClient, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
