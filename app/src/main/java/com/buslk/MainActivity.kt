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
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import androidx.appcompat.app.AppCompatActivity
import com.buslk.ui.screens.LoginScreen
import com.buslk.ui.theme.BusLKTheme
import com.google.firebase.FirebaseApp

/**
 * The main entry point of the Android application.
 * 
 * OOD Principle: Single Responsibility.
 * This Activity's only job is to initialize global tools (like Firebase) and host the 
 * root Jetpack Compose UI. It does not contain any actual UI drawing logic itself.
 * Note: We inherit from AppCompatActivity (instead of ComponentActivity) to support
 * the Android 13+ Per-App Language APIs.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Backend connection globally when the app first launches
        FirebaseApp.initializeApp(this)
        Log.d("BusLK_Setup", "Firebase Initialized Successfully!")
        Toast.makeText(this, "Firebase Initialized!", Toast.LENGTH_SHORT).show()
        
        // Tells Android to draw the app behind the transparent system navigation/status bars
        enableEdgeToEdge()
        
        // Set the root UI content using Jetpack Compose
        setContent {
            // BusLKTheme applies our global colors, typography, and shapes
            BusLKTheme {
                BusLKApp()
            }
        }
    }
}

/**
 * The root Composable function that acts as the Navigation Controller for the entire app.
 * 
 * Centralizing navigation here means child screens (like Login or Home) don't need 
 * to know about each other, reducing coupling.
 */
@PreviewScreenSizes
@Composable
fun BusLKApp() {
    val context = LocalContext.current
    
    // OOD Principle: Dependency Injection Setup (Composition Root)
    // We instantiate the AuthRepository here at the top level and inject it
    // into the AuthViewModelFactory. This ensures that the ViewModel doesn't
    // hardcode its dependencies, making it modular and allowing us to easily swap out 
    // real databases for fake/mock databases during automated testing.
    val authRepository = androidx.compose.runtime.remember { com.buslk.data.AuthRepository() }
    val authViewModel: com.buslk.ui.auth.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.buslk.ui.auth.AuthViewModelFactory(authRepository)
    )

    // State variable holding the current screen the user is looking at.
    // 'rememberSaveable' ensures this state survives if Android temporarily kills the app
    // or if the user rotates their phone screen.
    var currentDestination by rememberSaveable { 
        mutableStateOf(AppDestinations.HOME) 
    }

    // Basic Routing Logic: Determine which Screen Composable to draw based on currentDestination
    if (currentDestination == AppDestinations.OPENING) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            com.buslk.ui.screens.OpeningScreen(
                onGetStartedClick = {
                    currentDestination = AppDestinations.LANGUAGE_SELECT
                }
            )
        }
    } else if (currentDestination == AppDestinations.LOGIN) {
        // Hide the bottom navigation bar by keeping the Login screen outside the NavigationSuiteScaffold
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
                    currentDestination = AppDestinations.LOGIN
                }
            )
        }
    } else {
        // App is in "Main Mode" (logged in/past intro). Show the Navigation Bar.
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                // Loop through destinations and create a button on the bottom bar for each one,
                // ignoring the intro screens.
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
                        // Change screen when clicked
                        onClick = { currentDestination = it }
                    )
                }
            }
        ) {
            // This Scaffold holds the actual content *above* the bottom navigation bar
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                if (currentDestination == AppDestinations.HOME) {
                    com.buslk.ui.screens.HomeScreen()
                } else {
                    // Placeholder for screens we haven't built yet
                    Greeting(
                        name = currentDestination.label,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/**
 * An Enum describing all the physical screens in our app.
 * Using an Enum prevents spelling mistakes when routing compared to using raw Strings.
 * 
 * @property label The text displayed under the icon on the nav bar.
 * @property icon The Material vector icon displayed on the nav bar.
 */
enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    OPENING("Opening", Icons.Default.Home),
    LOGIN("Login", Icons.Default.AccountBox),
    LANGUAGE_SELECT("Language", Icons.Default.Home),
    HOME("Home", Icons.Default.Home),
    SEARCH("Search", Icons.Default.Search),
    COMMUNITY("Community", Icons.Default.Face),
    LOST_AND_FOUND("Lost & Found", Icons.AutoMirrored.Filled.List),
    PROFILE("Profile", Icons.Default.Person),
}

/** Placeholder component for unbuilt screens. */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(
            text = "Hello $name!",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BusLKTheme {
        Greeting("Android")
    }
}