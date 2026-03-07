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

// OOD Principle: Dependency Injection & State Hoisting.
// Instead of creating the ViewModel inside the screen or managing its own complex
// authentication logic, LoginScreen accepts the AuthViewModel as a parameter.
// This decouples the UI from the business logic.
@Composable
fun LoginScreen(
    authViewModel: com.buslk.ui.auth.AuthViewModel,
    onSignInSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by authViewModel.uiState.collectAsState()
    
    var selectedTabIndex by remember { mutableStateOf(0) } // 0 for Login, 1 for Sign Up
    
    LaunchedEffect(uiState) {
        when (uiState) {
            is com.buslk.ui.auth.AuthUiState.Success -> {
                Toast.makeText(context, "Sign in & Sync successful!", Toast.LENGTH_SHORT).show()
                onSignInSuccess()
            }
            is com.buslk.ui.auth.AuthUiState.Error -> {
                val msg = (uiState as com.buslk.ui.auth.AuthUiState.Error).message
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    val isLoading = uiState is com.buslk.ui.auth.AuthUiState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .statusBarsPadding()
    ) {
        // Back Button
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

        // Title
        Text(
            text = if (selectedTabIndex == 0) "Welcome Back!" else "Create Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Tabs
        AuthTabs(selectedTabIndex = selectedTabIndex, onTabSelected = { selectedTabIndex = it })
        
        Spacer(modifier = Modifier.height(32.dp))

        // Google SignIn Button
        GoogleSignInButton(isLoading = isLoading) {
            authViewModel.signInWithGoogle(context)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // OR Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
            Text(
                text = "Or",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color.Gray,
                fontSize = 14.sp
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        var username by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }

        // Form Fields
        AuthForm(
            isLogin = selectedTabIndex == 0,
            username = username,
            onUsernameChange = { username = it },
            email = email,
            onEmailChange = { email = it },
            password = password,
            onPasswordChange = { password = it },
            confirmPassword = confirmPassword,
            onConfirmPasswordChange = { confirmPassword = it },
            isLoading = isLoading,
            authViewModel = authViewModel
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Main Action Button
        Button(
            onClick = { 
                if (selectedTabIndex == 0) {
                    authViewModel.signInWithEmailAndPassword(email, password)
                } else {
                    if (password == confirmPassword) {
                        authViewModel.signUpWithEmailAndPassword(email, password, username)
                    } else {
                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty() && (selectedTabIndex == 0 || username.isNotEmpty()),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E5DE6),
                disabledContainerColor = Color.LightGray
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(
                    text = if (selectedTabIndex == 0) "Login" else "Sign Up",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

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
            text = "Login",
            isSelected = selectedTabIndex == 0,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(0) }
        )
        TabButton(
            text = "Sign Up",
            isSelected = selectedTabIndex == 1,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(1) }
        )
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) Color(0xFF1E5DE6) else Color.Transparent
    val textColor = if (isSelected) Color.White else Color.Gray

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(backgroundColor, RoundedCornerShape(24.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

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
            Text("Continue with Google", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun AuthForm(
    isLogin: Boolean,
    username: String,
    onUsernameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    authViewModel: com.buslk.ui.auth.AuthViewModel
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val uiState by authViewModel.uiState.collectAsState()
    
    val isEmailError = uiState is com.buslk.ui.auth.AuthUiState.Error && 
            (uiState as com.buslk.ui.auth.AuthUiState.Error).message.contains("Email", ignoreCase = true)
    
    val isPasswordError = uiState is com.buslk.ui.auth.AuthUiState.Error && 
            (uiState as com.buslk.ui.auth.AuthUiState.Error).message.contains("Password", ignoreCase = true)

    Column {
        if (!isLogin) {
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = !isLoading,
                isError = uiState is com.buslk.ui.auth.AuthUiState.Error && 
                        (uiState as com.buslk.ui.auth.AuthUiState.Error).message.contains("Username", ignoreCase = true),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1E5DE6),
                    focusedLabelColor = Color(0xFF1E5DE6),
                    errorBorderColor = Color.Red
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isLoading,
            isError = isEmailError,
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
        
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isLoading,
            isError = isPasswordError,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (isLogin) androidx.compose.ui.text.input.ImeAction.Done else androidx.compose.ui.text.input.ImeAction.Next
            ),
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

        if (!isLogin) {
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Confirm Password") },
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
            // Forgot Password Link
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(
                    onClick = { /* TODO: Implement Forgot Password */ },
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Forgot Password?",
                        color = Color(0xFF1E5DE6),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    BusLKTheme {
        // Preview might not work fully with ViewModel injection directly without mocking, 
        // but normally we use a placeholder or fake viewmodel.
        // LoginScreen(authViewModel = ..., onSignInSuccess = {}, onBackClick = {})
    }
}
