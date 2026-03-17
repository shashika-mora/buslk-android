package com.buslk

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
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
import com.buslk.ui.screens.ProfileScreen
import com.buslk.ui.screens.SettingsScreen
import com.buslk.ui.viewmodels.ProfileViewModel
import com.buslk.ui.viewmodels.SettingsViewModel
import com.buslk.ui.theme.BusLKTheme
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
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
    
    val authRepository = androidx.compose.runtime.remember { com.buslk.data.AuthRepository() }
    val authViewModel: com.buslk.ui.auth.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.buslk.ui.auth.AuthViewModelFactory(authRepository)
    )

    val profileViewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    var currentDestination by rememberSaveable { 
        mutableStateOf(AppDestinations.HOME) 
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
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.filter { 
                    it != AppDestinations.LOGIN && 
                    it != AppDestinations.OPENING && 
                    it != AppDestinations.LANGUAGE_SELECT &&
                    it != AppDestinations.SETTINGS 
                }.forEach {
                    item(
                        icon = { Icon(it.icon, contentDescription = it.label) },
                        label = { Text(it.label) },
                        selected = it == currentDestination,
                        onClick = { currentDestination = it }
                    )
                }
            }
        ) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                when (currentDestination) {
                    AppDestinations.HOME -> com.buslk.ui.screens.HomeScreen()
                    AppDestinations.PROFILE -> ProfileScreen(
                        authViewModel = authViewModel,
                        profileViewModel = profileViewModel,
                        onSettingsClick = { 
                            currentDestination = AppDestinations.SETTINGS
                        }
                    )
                    AppDestinations.SETTINGS -> SettingsScreen(
                        onBack = { currentDestination = AppDestinations.PROFILE },
                        authViewModel = authViewModel,
                        settingsViewModel = settingsViewModel,
                        onLogoutSuccess = { currentDestination = AppDestinations.LOGIN }
                    )
                    else -> Greeting(
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
    SETTINGS("Settings", Icons.Default.Settings),
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
