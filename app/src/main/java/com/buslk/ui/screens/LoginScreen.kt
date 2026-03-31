package com.buslk.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.buslk.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buslk.ui.theme.BusLKTheme
import kotlinx.coroutines.launch

/**
 * The screen responsible for user Authentication (Login & Sign Up).
 * 
 * OOD Principle: Dependency Injection & State Hoisting (Abstraction).
 * Instead of creating the AuthViewModel inside this screen or managing complex Firebase
 * authentication logic directly, `LoginScreen` accepts the `AuthViewModel` as a parameter.
 * This completely decouples the UI layer from the data/business logic layer (Separation of Concerns).
 * The screen also delegates navigation decisions to its parent via `onSignInSuccess` and `onBackClick`.
 * 
 * @param authViewModel The ViewModel managing authentication state and actions.
 * @param onSignInSuccess Callback triggered when authentication successfully completes.
 * @param onBackClick Callback triggered when the back button is pressed.
 */
@Composable
fun LoginScreen(
    authViewModel: com.buslk.ui.auth.AuthViewModel,
    onSignInSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    // LocalContext allows us to access Android-specific resources like Toasts or Strings
    // from within a Composable function.
    val context = LocalContext.current
    
    // We observe the AuthViewModel's StateFlow. Every time the ViewModel emits a new state
    // (e.g., Loading -> Error), Jetpack Compose automatically re-executes (recomposes) 
    // this function to update the UI based on the new state.
    val uiState by authViewModel.uiState.collectAsState()
    
    // Local UI state: 0 represents the "Login" tab, 1 represents the "Sign Up" tab.
    var selectedTabIndex by remember { mutableIntStateOf(0) } 
    
    // LaunchedEffect allows us to trigger one-off side effects (like showing a Toast)
    // inside Composable functions. It runs its block whenever the 'key' (uiState) changes.
    LaunchedEffect(uiState) {
        when (uiState) {
            is com.buslk.ui.auth.AuthUiState.Success -> {
                Toast.makeText(context, context.getString(R.string.msg_login_success), Toast.LENGTH_SHORT).show()
                onSignInSuccess()
            }
            is com.buslk.ui.auth.AuthUiState.Error -> {
                // Cast the state to Error to access its specific 'message' property
                val msg = (uiState as com.buslk.ui.auth.AuthUiState.Error).message
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                // Reset the state to Idle so the error Toast doesn't show again uncontrollably
                authViewModel.resetState()
            }
            else -> {
                // Do nothing for Idle or Loading states
            }
        }
    }

    // A convenience variable determining if a network request is currently active
    val isLoading = uiState is com.buslk.ui.auth.AuthUiState.Loading

    // The main container for the screen's content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .statusBarsPadding() // Ensures content isn't drawn under the device's status bar
    ) {
        // --- Top Row: Back Button ---
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.Start)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // --- Header Text ---
        // Dynamically change the title based on which tab is active
        Text(
            text = if (selectedTabIndex == 0) stringResource(id = R.string.welcome_back) else stringResource(id = R.string.create_account),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // --- Custom Tab Bar ---
        // Encapsulated UI component for selecting Login vs Sign Up
        AuthTabs(selectedTabIndex = selectedTabIndex, onTabSelected = { selectedTabIndex = it })
        
        Spacer(modifier = Modifier.height(32.dp))

        // --- Google Sign-In Area ---
        GoogleSignInButton(isLoading = isLoading) {
            // Initiate the Google Sign-In flow via the ViewModel
            authViewModel.signInWithGoogle(context)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // --- "OR" Divider section ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // HorizontalDivider draws a thin line. weight(1f) tells it to fill available space
            // pushing the text to the exact center of the screen.
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
            Text(
                text = stringResource(id = R.string.lbl_or),
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color.Gray,
                fontSize = 14.sp
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // --- Email/Password Form State Variables ---
        // We manage form input state locally in the UI layer until the user clicks submit.
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }

        // Form Fields encapsulated into a separate function for cleaner code
        AuthForm(
            isLogin = selectedTabIndex == 0,
            email = email,
            onEmailChange = { email = it },
            password = password,
            onPasswordChange = { password = it },
            confirmPassword = confirmPassword,
            onConfirmPasswordChange = { confirmPassword = it },
            isLoading = isLoading,
            authViewModel = authViewModel
        )
        
        // Pushes the main action button to the bottom of the screen
        Spacer(modifier = Modifier.weight(1f))
        
        // --- Main Action Button (Login or Register) ---
        Button(
            onClick = { 
                if (selectedTabIndex == 0) {
                    // Execute Login
                    authViewModel.signInWithEmailAndPassword(email, password)
                } else {
                    // Execute Registration (with basic UI-level validation)
                    if (password == confirmPassword) {
                        authViewModel.signUpWithEmailAndPassword(email, password)
                    } else {
                        Toast.makeText(context, context.getString(R.string.msg_passwords_not_match), Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            // Disable the button if a network request is loading OR if inputs are empty
            // This is a UI-level defensive programming technique.
            enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E5DE6),
                disabledContainerColor = Color.LightGray
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            // Conditional Rendering: Show a spinner if loading, otherwise show the text
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(
                    text = if (selectedTabIndex == 0) stringResource(id = R.string.tab_login) else stringResource(id = R.string.tab_signup),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * A custom Tab component specifically designed for transitioning between Login and Signup modes.
 * 
 * @param selectedTabIndex Current state (0 = Login, 1 = Signup)
 * @param onTabSelected Callback providing the index of the newly tapped tab.
 */
@Composable
fun AuthTabs(selectedTabIndex: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFFF5F5F5), RoundedCornerShape(28.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TabButton(
            text = stringResource(id = R.string.tab_login),
            isSelected = selectedTabIndex == 0,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(0) }
        )
        TabButton(
            text = stringResource(id = R.string.tab_signup),
            isSelected = selectedTabIndex == 1,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(1) }
        )
    }
}

/**
 * Represents a single button within the AuthTabs component.
 */
@Composable
fun TabButton(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) Color(0xFF1E5DE6) else Color.Transparent
    val textColor = if (isSelected) Color.White else Color.Gray

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(backgroundColor, RoundedCornerShape(24.dp))
            .clickable { onClick() }, // Makes the box respond to touch events
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

/**
 * Encapsulates the visual design of the Google Sign-In button.
 */
@Composable
fun GoogleSignInButton(isLoading: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            // Simple generic G to represent Google (Using colors could be added later or an image icon)
            Text(text = "G", color = Color(0xFFEA4335), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(id = R.string.continue_with_google), color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Contains the actual form input fields (Email, Password, Confirm Password).
 * 
 * OOD Principle: State Hoisting.
 * This component does not manage its own state (email, password). It takes them as arguments
 * and passes changes back up via callback lambdas (`onEmailChange`, `onPasswordChange`).
 * 
 * @param isLogin Boolean indicating whether to render the form for Login or Sign Up.
 * @param isLoading Used to disable the inputs while a network request is running.
 */
@Composable
fun AuthForm(
    isLogin: Boolean,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    authViewModel: com.buslk.ui.auth.AuthViewModel
) {
    // Local state to toggle obscuring the password text (asterisks vs plain text)
    var passwordVisible by remember { mutableStateOf(false) }
    
    // We observe the AuthViewModel's state specifically to extract validation errors
    // and highlight the appropriate text fields in red.
    val uiState by authViewModel.uiState.collectAsState()
    
    // Pattern matching to detect if the error message from the ViewModel pertains to Email
    val isEmailError = uiState is com.buslk.ui.auth.AuthUiState.Error && 
            (uiState as com.buslk.ui.auth.AuthUiState.Error).message.contains("Email", ignoreCase = true)
    
    // Pattern matching to detect if the error message pertains to Password
    val isPasswordError = uiState is com.buslk.ui.auth.AuthUiState.Error && 
            (uiState as com.buslk.ui.auth.AuthUiState.Error).message.contains("Password", ignoreCase = true)

    Column {
        // --- Email Input ---
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(id = R.string.lbl_email)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isLoading,
            isError = isEmailError,
            // Configure the soft keyboard: Show an '@' symbol and a "Next" button
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = androidx.compose.ui.text.input.ImeAction.Next
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1E5DE6),
                focusedLabelColor = Color(0xFF1E5DE6),
                errorBorderColor = Color.Red
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // --- Password Input ---
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(id = R.string.lbl_password)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isLoading,
            isError = isPasswordError,
            // Toggle visual security (obscuring text) based on local state
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            // Configure the soft keyboard: Show a "Done" button if logging in, or "Next" if signing up
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (isLogin) androidx.compose.ui.text.input.ImeAction.Done else androidx.compose.ui.text.input.ImeAction.Next
            ),
            // Add a clickable eye icon to the end of the text field
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1E5DE6),
                focusedLabelColor = Color(0xFF1E5DE6),
                errorBorderColor = Color.Red
            )
        )

        // --- Additional UI unique to Sign Up vs Login ---
        if (!isLogin) {
            // Sign Up Mode: Show Confirm Password field
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text(stringResource(id = R.string.lbl_confirm_password)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1E5DE6),
                    focusedLabelColor = Color(0xFF1E5DE6)
                )
            )
        } else {
            // Login Mode: Show "Forgot Password?" Link
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(
                    onClick = { /* TODO: Implement Forgot Password Flow */ },
                    enabled = !isLoading
                ) {
                    Text(
                        text = stringResource(id = R.string.btn_forgot_password),
                        color = Color(0xFF1E5DE6),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Jetpack Compose Preview for inspecting the UI without launching the emulator.
 */
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    BusLKTheme {
        // Preview might not work fully with ViewModel injection directly without mocking, 
        // but normally we use a placeholder or fake viewmodel.
        // LoginScreen(authViewModel = ..., onSignInSuccess = {}, onBackClick = {})
    }
}

