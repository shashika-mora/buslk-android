package com.buslk

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.annotation.StringRes
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
            val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            
            BusLKTheme(themeMode = themeMode) {
                BusLKApp(settingsViewModel = settingsViewModel)
            }
        }
    }
}

@Composable
fun BusLKApp(settingsViewModel: SettingsViewModel) {
    val authRepository = androidx.compose.runtime.remember { com.buslk.data.AuthRepository() }
    val authViewModel: com.buslk.ui.auth.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.buslk.ui.auth.AuthViewModelFactory(authRepository)
    )

    val profileViewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    var currentDestination by rememberSaveable { 
        mutableStateOf(AppDestinations.HOME) 
    }
    
    var scannedBusId by rememberSaveable {
        mutableStateOf("")
    }

    if (currentDestination == AppDestinations.OPENING) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                com.buslk.ui.screens.OpeningScreen(
                    onGetStartedClick = {
                        currentDestination = AppDestinations.LANGUAGE_SELECT
                    }
                )
            }
        }
    } else if (currentDestination == AppDestinations.LOGIN) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
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
        }
    } else if (currentDestination == AppDestinations.LANGUAGE_SELECT) {
        Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                com.buslk.ui.screens.LanguageSelectionScreen(
                    onBackClick = {
                        currentDestination = AppDestinations.OPENING
                    },
                    onLanguageSelected = {
                        currentDestination = AppDestinations.LOGIN
                    }
                )
            }
        }
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.filter {
                    it != AppDestinations.LOGIN &&
                    it != AppDestinations.OPENING &&
                    it != AppDestinations.LANGUAGE_SELECT &&
                    it != AppDestinations.SETTINGS &&
                    it != AppDestinations.SCAN_QR &&
                    it != AppDestinations.TRIP_SCREEN &&
                    it != AppDestinations.FEEDBACK
                }.forEach {
                    item(
                        icon = { Icon(it.icon, contentDescription = stringResource(it.labelResId)) },
                        label = { Text(stringResource(it.labelResId)) },
                        selected = it == currentDestination,
                        onClick = { currentDestination = it }
                    )
                }
            }
        ) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (currentDestination) {
                        AppDestinations.HOME -> com.buslk.ui.screens.HomeScreen(
                            onScanClick = { currentDestination = AppDestinations.SCAN_QR }
                        )
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
                        AppDestinations.SCAN_QR -> com.buslk.ui.screens.ScanQRScreen(
                            onCheckInSuccess = { busId ->
                                scannedBusId = busId
                                currentDestination = AppDestinations.TRIP_SCREEN
                            },
                            onBack = { currentDestination = AppDestinations.HOME }
                        )
                        AppDestinations.TRIP_SCREEN -> com.buslk.ui.screens.TripScreen(
                            busId = scannedBusId,
                            onEndTrip = { currentDestination = AppDestinations.FEEDBACK },
                            onBack = { currentDestination = AppDestinations.HOME }
                        )
                        AppDestinations.FEEDBACK -> com.buslk.ui.screens.FeedbackScreen(
                            busId = scannedBusId,
                            onBackToHome = { currentDestination = AppDestinations.HOME }
                        )
                        else -> Greeting(
                            name = stringResource(currentDestination.labelResId),
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }
}
/**
 * An Enum describing all the physical screens in our app.
 * Using an Enum prevents spelling mistakes when routing compared to using raw Strings.
 *
 * @property labelResId The Android string resource ID displayed under the icon on the nav bar.
 * @property icon The Material vector icon displayed on the nav bar.
 */

enum class AppDestinations(
    @StringRes val labelResId: Int,
    val icon: ImageVector,
) {
    OPENING(R.string.nav_home, Icons.Default.Home),
    LOGIN(R.string.tab_login, Icons.Default.AccountBox),
    LANGUAGE_SELECT(R.string.select_language, Icons.Default.Home),
    HOME(R.string.nav_home, Icons.Default.Home),
    SEARCH(R.string.nav_search, Icons.Default.Search),
    COMMUNITY(R.string.nav_community, Icons.Default.Face),
    LOST_AND_FOUND(R.string.nav_lost_found, Icons.AutoMirrored.Filled.List),
    PROFILE(R.string.nav_profile, Icons.Default.Person),
    SETTINGS(R.string.nav_settings, Icons.Default.Settings),
    SCAN_QR(R.string.nav_scan_qr, Icons.Default.Home),
    TRIP_SCREEN(R.string.nav_trip, Icons.Default.Home),
    FEEDBACK(R.string.nav_feedback, Icons.Default.Star),
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BusLKTheme {
        Greeting("Android")
    }
}