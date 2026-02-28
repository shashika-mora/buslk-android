package com.buslk.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.buslk.data.AuthRepository
import com.buslk.data.IAuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** All possible states for the sign-in UI. */
sealed class AuthUiState {
    object Idle    : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: FirebaseUser)  : AuthUiState()
    data class Error(val message: String)       : AuthUiState()
}

/**
 * ViewModel governing the authentication flow.
 * Translates UI events (like button clicks) into repository calls and updates the [uiState].
 * OOD Principle: Separation of Concerns (MVVM Pattern) & Observer Pattern.
 * The ViewModel separates the UI logic from the Data/Business logic.
 */
class AuthViewModel(
    // OOD Principle: Dependency Injection. Depending on the interface IAuthRepository
    // rather than the concrete AuthRepository allows for easier unit testing (mocking).
    private val repository: IAuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Auto-skip logic if already signed in
        if (isSignedIn()) {
            _uiState.value = AuthUiState.Success(repository.getCurrentUser()!!)
        }
    }

    /** True if the user is already signed in (used to auto-skip the splash screen). */
    fun isSignedIn(): Boolean = repository.getCurrentUser() != null

    /**
     * Signs in using email and password.
     * Updates [uiState] to Loading → Success or Error.
     */
    fun signInWithEmailAndPassword(email: String, pass: String) {
        if (_uiState.value is AuthUiState.Loading) return
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.signInWithEmailAndPassword(email, pass)
            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.Success(user) },
                onFailure = { e   -> AuthUiState.Error(e.message ?: "Unknown error") }
            )
        }
    }

    /**
     * Launches Google Sign-In flow.
     * Updates [uiState] to Loading → Success or Error.
     */
    fun signInWithGoogle(context: Context) {
        if (_uiState.value is AuthUiState.Loading) return
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.signInWithGoogle(context)
            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.Success(user) },
                onFailure = { e   -> AuthUiState.Error(e.message ?: "Unknown error") }
            )
        }
    }

    /**
     * Signs up a new user using email and password.
     * Updates [uiState] to Loading → Success or Error.
     */
    fun signUpWithEmailAndPassword(email: String, pass: String) {
        if (_uiState.value is AuthUiState.Loading) return
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.signUpWithEmailAndPassword(email, pass)
            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.Success(user) },
                onFailure = { e   -> AuthUiState.Error(e.message ?: "Unknown error") }
            )
        }
    }

    /** Resets state to Idle (e.g. after showing an error Snackbar). */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun signOut() {
        repository.signOut()
        _uiState.value = AuthUiState.Idle
    }
}

class AuthViewModelFactory(
    private val repository: IAuthRepository = AuthRepository()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
