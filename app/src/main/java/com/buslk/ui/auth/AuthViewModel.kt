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

/** 
 * Represents all possible states for the Authentication UI (Login Screen).
 * OOD Principle: State Pattern / Algebraic Data Type.
 * By wrapping the state in a 'sealed class', the UI (Compose) only has to observe a single stream of data 
 * and can definitively know exactly what to draw (e.g. show a spinner if Loading, show an error message if Error).
 */
sealed class AuthUiState {
    /** The default state when the user is simply looking at the login form. */
    object Idle    : AuthUiState()
    /** The state when a network request is in progress. The UI should show a loading indicator. */
    object Loading : AuthUiState()
    /** 
     * The state when authentication succeeds. 
     * @property user The successfully authenticated [FirebaseUser] containing session data.
     */
    data class Success(val user: FirebaseUser)  : AuthUiState()
    /** 
     * The state when an authentication attempt fails.
     * @property message The human-readable error message to display to the user.
     */
    data class Error(val message: String)       : AuthUiState()
}

/**
 * ViewModel governing the authentication flow.
 * Translates UI events (like button clicks) into repository calls and updates the [uiState].
 * 
 * OOD Principle: Separation of Concerns (MVVM Pattern) & Observer Pattern.
 * The ViewModel acts as a middleman. It separates the visual UI logic (Composables) 
 * from the Data/Business logic (Repository/Firebase). It holds the data so it survives screen rotations.
 */
class AuthViewModel(
    // OOD Principle: Dependency Injection. Depending on the interface IAuthRepository
    // rather than the concrete AuthRepository allows for easier unit testing (mocking).
    private val repository: IAuthRepository = AuthRepository()
) : ViewModel() {

    // MutableStateFlow is a reactive stream that holds a single value. We make it private so ONLY the ViewModel can change it.
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    // We expose a public, read-only version (StateFlow) for the UI to observe. This enforces Unidirectional Data Flow.
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Regular Expression (Regex) to ensure the email looks like a real email address before we even contact Firebase.
    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()

    init {
        // Automatically check if the user is already logged in when the app starts.
        // If they are, jump straight to the Success state to skip the login screen.
        if (isSignedIn()) {
            _uiState.value = AuthUiState.Success(repository.getCurrentUser()!!)
        }
    }

    /** 
     * Checks if the user is currently authenticated in the local session.
     * @return Boolean true if signed in, false otherwise.
     */
    fun isSignedIn(): Boolean = repository.getCurrentUser() != null

    /**
     * Attempts to sign in the user using their email and password.
     * 
     * @param email The raw email text from the UI's text field.
     * @param pass The raw password text from the UI's text field.
     */
    fun signInWithEmailAndPassword(email: String, pass: String) {
        // Prevent multiple clicks by ignoring the action if we are already Loading.
        if (_uiState.value is AuthUiState.Loading) return
        
        // Sanitize input by removing accidental leading/trailing spaces.
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()

        // Validate the formats. If it fails, validateInputs changes the state to Error and returns false.
        if (!validateInputs(trimmedEmail, trimmedPass)) return

        // Update the state to Loading so the UI knows to show a progress spinner.
        _uiState.value = AuthUiState.Loading
        
        // viewModelScope ensures this background network task is automatically canceled if the ViewModel is destroyed.
        viewModelScope.launch {
            val result = repository.signInWithEmailAndPassword(trimmedEmail, trimmedPass)
            // 'fold' is a Kotlin function handling the Success and Failure paths of our Result object.
            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.Success(user) },
                onFailure = { e   -> AuthUiState.Error(mapErrorToMessage(e)) }
            )
        }
    }

    /**
     * Launches the native Google Sign-In bottom sheet.
     * 
     * @param context The Android context needed to overlay the UI sheet.
     */
    fun signInWithGoogle(context: Context) {
        if (_uiState.value is AuthUiState.Loading) return
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.signInWithGoogle(context)
            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.Success(user) },
                onFailure = { e   -> AuthUiState.Error(mapErrorToMessage(e)) }
            )
        }
    }

    /**
     * Registers a new account using email and password.
     * 
     * @param email The raw email text from the UI.
     * @param pass The raw password text from the UI.
     */
    fun signUpWithEmailAndPassword(email: String, pass: String, username: String) {
        if (_uiState.value is AuthUiState.Loading) return

        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()
        val trimmedUsername = username.trim()

        if (!validateInputs(trimmedEmail, trimmedPass, trimmedUsername)) return

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.signUpWithEmailAndPassword(trimmedEmail, trimmedPass, trimmedUsername)
            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.Success(user) },
                onFailure = { e   -> AuthUiState.Error(mapErrorToMessage(e)) }
            )
        }
    }

    /**
     * Updates the user's display name.
     */
    fun updateDisplayName(newName: String, onComplete: (Result<Unit>) -> Unit) {
        if (newName.isBlank()) return
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.updateDisplayName(newName)
            result.onSuccess {
                val user = repository.getCurrentUser()
                if (user != null) {
                    _uiState.value = AuthUiState.Success(user)
                }
            }.onFailure { e ->
                _uiState.value = AuthUiState.Error(mapErrorToMessage(e))
            }
            onComplete(result)
        }
    }

    /**
     * Changes the user's password.
     */
    fun changePassword(newPass: String, onComplete: (Result<Unit>) -> Unit) {
        if (newPass.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.changePassword(newPass)
            result.onSuccess {
                val user = repository.getCurrentUser()
                if (user != null) {
                    _uiState.value = AuthUiState.Success(user)
                }
            }.onFailure { e ->
                _uiState.value = AuthUiState.Error(mapErrorToMessage(e))
            }
            onComplete(result)
        }
    }

    /**
     * OOSD Principle: Defensive Programming.
     * Validates inputs before making network requests to save bandwidth and 
     * provide instant visual feedback to the user.
     * 
     * @return Boolean true if inputs are clean, false if they violate our rules.
     */
    private fun validateInputs(email: String, pass: String, username: String? = null): Boolean {
        if (username != null && username.isEmpty()) {
            _uiState.value = AuthUiState.Error("Username cannot be empty")
            return false
        }
        if (email.isEmpty()) {
            _uiState.value = AuthUiState.Error("Email cannot be empty")
            return false
        }
        if (!email.matches(emailRegex)) {
            _uiState.value = AuthUiState.Error("Invalid email format")
            return false
        }
        if (pass.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
            return false
        }
        return true
    }

    /**
     * OOSD Principle: Abstraction.
     * Maps technical or raw Firebase Exceptions to user-friendly, human-readable String messages.
     * 
     * @param e The raw Throwable/Exception captured from the repository.
     * @return A clean String that can be safely displayed in a Snackbar or Toast.
     */
    private fun mapErrorToMessage(e: Throwable): String {
        return when (e) {
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
            is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "An account already exists with this email"
            is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "No account found with this email"
            is com.google.firebase.FirebaseNetworkException -> "Network error, please check your connection"
            is com.google.firebase.auth.FirebaseAuthException -> {
                when (e.errorCode) {
                    "ERROR_WRONG_PASSWORD", "ERROR_INVALID_EMAIL" -> "Invalid email or password"
                    "ERROR_USER_NOT_FOUND" -> "No account found with this email"
                    "ERROR_USER_DISABLED" -> "This account has been disabled"
                    "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please try again later."
                    else -> e.message ?: "Authentication failed"
                }
            }
            else -> e.message ?: "Authentication failed. Please try again."
        }
    }

    /** 
     * Manually resets the UI state back to [AuthUiState.Idle]. 
     * Useful for clearing error messages off the screen after a few seconds.
     */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    /** Logs the user out via the Repository and clears the UI state. */
    fun signOut() {
        repository.signOut()
        _uiState.value = AuthUiState.Idle
    }
}

/**
 * Factory class required by Android's ViewModel architecture because our [AuthViewModel]
 * requires constructor parameters (the [IAuthRepository]). Android doesn't natively know 
 * how to instantiate ViewModels with arguments, so we teach it using this Factory.
 */
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
