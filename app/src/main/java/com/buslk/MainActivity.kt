package com.buslk

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    var currentDestination by rememberSaveable { 
        mutableStateOf(if (auth.currentUser != null) AppDestinations.HOME else AppDestinations.OPENING) 
    }

    if (currentDestination == AppDestinations.OPENING) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            com.buslk.ui.screens.OpeningScreen(
                onGetStartedClick = {
                    currentDestination = AppDestinations.LOGIN
                }
            )
        }
    } else if (currentDestination == AppDestinations.LOGIN) {
        // Hide navigation suite on login screen
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            // Pass innerPadding to LoginScreen if needed, or wrap it
            LoginScreen(
                onSignInSuccess = {
                    currentDestination = AppDestinations.HOME
                }
            )
        }
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.filter { it != AppDestinations.LOGIN && it != AppDestinations.OPENING }.forEach {
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
                Greeting(
                    name = currentDestination.label,
                    modifier = Modifier.padding(innerPadding)
                )
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
    HOME("Home", Icons.Default.Home),
    FAVORITES("Favorites", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BusLKTheme {
        Greeting("Android")
    }
}