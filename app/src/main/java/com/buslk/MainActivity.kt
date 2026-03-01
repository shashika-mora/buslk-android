package com.buslk

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.buslk.ui.screens.LoginScreen
import com.buslk.ui.theme.BusLKTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.d("BusLK_Setup", "Firebase Initialized Successfully!")
        Toast.makeText(this, "Firebase Initialized!", Toast.LENGTH_SHORT).show()
        
        enableEdgeToEdge()
        setContent {
            BusLKTheme {
                BusLKApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun BusLKApp() {
    val context = LocalContext.current
    
    // OOD Principle: Dependency Injection Setup
    // We instantiate the AuthRepository here (the composition root) and inject it
    // into the AuthViewModelFactory. This ensures that the ViewModel doesn't
    // hardcode its dependencies, making it more modular and testable.
    val authRepository = androidx.compose.runtime.remember { com.buslk.data.AuthRepository() }
    val authViewModel: com.buslk.ui.auth.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.buslk.ui.auth.AuthViewModelFactory(authRepository)
    )

    var currentDestination by rememberSaveable { 
        mutableStateOf(if (authRepository.getCurrentUser() != null) AppDestinations.HOME else AppDestinations.OPENING) 
    }

    if (currentDestination == AppDestinations.OPENING) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            com.buslk.ui.screens.OpeningScreen(
                onGetStartedClick = {
                    currentDestination = AppDestinations.LANGUAGE_SELECT
                }
            )
        }
    } else if (currentDestination == AppDestinations.LOGIN) {
        // Hide navigation suite on login screen
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            // Pass innerPadding to LoginScreen if needed, or wrap it
            LoginScreen(
                authViewModel = authViewModel,
                onSignInSuccess = {
                    currentDestination = AppDestinations.HOME
                },
                onBackClick = {
                    currentDestination = AppDestinations.LANGUAGE_SELECT
                }
            )
        }
    } else if (currentDestination == AppDestinations.LANGUAGE_SELECT) {
        Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
            com.buslk.ui.screens.LanguageSelectionScreen(
                onBackClick = {
                    currentDestination = AppDestinations.OPENING
                },
                onLanguageSelected = { langCode ->
                    // We'll process language matching later. For now navigate to login
                    currentDestination = AppDestinations.LOGIN
                }
            )
        }
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.filter { it != AppDestinations.LOGIN && it != AppDestinations.OPENING && it != AppDestinations.LANGUAGE_SELECT }.forEach {
                    item(
                        icon = {
                            Icon(
                                it.icon,
                                contentDescription = it.label
                            )
                        },
                        label = { Text(it.label) },
                        selected = it == currentDestination,
                        onClick = { currentDestination = it }
                    )
                }
            }
        ) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                if (currentDestination == AppDestinations.HOME || currentDestination == AppDestinations.PROFILE) {
                    HomeScreen(
                        user = authRepository.getCurrentUser(),
                        onSignOut = {
                            authViewModel.signOut()
                            currentDestination = AppDestinations.OPENING
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                } else {
                    Greeting(
                        name = currentDestination.label,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    OPENING("Opening", Icons.Default.Home),
    LOGIN("Login", Icons.Default.AccountBox),
    LANGUAGE_SELECT("Language", Icons.Default.Home),
    HOME("Home", Icons.Default.Home),
    FAVORITES("Favorites", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(
            text = "Hello $name!",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
fun HomeScreen(
    user: com.google.firebase.auth.FirebaseUser?,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        if (user != null) {
            Text(text = "Welcome, ${user.displayName ?: "User"}!", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Email: ${user.email ?: "Unknown"}", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))
            androidx.compose.material3.Button(onClick = onSignOut) {
                Text("Sign Out")
            }
        } else {
            Text(text = "Welcome to Home!", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BusLKTheme {
        Greeting("Android")
    }
}