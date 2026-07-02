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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
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
import com.buslk.ui.screens.ChatScreen
import com.buslk.ui.viewmodels.ProfileViewModel
import com.buslk.ui.viewmodels.SettingsViewModel
import com.buslk.ui.theme.BusLKTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.buslk.data.LiveMapRepository
import com.buslk.ui.viewmodels.MapViewModel
import com.buslk.ui.viewmodels.MapViewModelFactory

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        enableEdgeToEdge()
        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val userPreferencesRepository = androidx.compose.runtime.remember { com.buslk.data.UserPreferencesRepository(context) }
            val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.buslk.ui.viewmodels.SettingsViewModelFactory(userPreferencesRepository)
            )
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val isDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            
            BusLKTheme(darkTheme = isDarkTheme) {
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

    val userRepository = androidx.compose.runtime.remember { com.buslk.data.UserRepository() }
    val tripRepository = androidx.compose.runtime.remember { com.buslk.data.TripRepository() }
    val feedbackRepository = androidx.compose.runtime.remember { com.buslk.data.FeedbackRepository() }
    val profileViewModel: com.buslk.ui.viewmodels.ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.buslk.ui.viewmodels.ProfileViewModelFactory(userRepository, tripRepository, feedbackRepository)
    )

    // Instantiate Map dependencies
    val liveMapRepository = androidx.compose.runtime.remember { LiveMapRepository() }
    val mapViewModel: MapViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = MapViewModelFactory(liveMapRepository)
    )

    // Instantiate Lost & Found dependencies
    val lostAndFoundRepository = androidx.compose.runtime.remember { com.buslk.data.LostAndFoundRepository() }
    val lostAndFoundViewModel: com.buslk.ui.viewmodels.LostAndFoundViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.buslk.ui.viewmodels.LostAndFoundViewModelFactory(lostAndFoundRepository, authRepository)
    )

    // Instantiate Search & Details dependencies
    val searchRepository = androidx.compose.runtime.remember { com.buslk.data.SearchRepository() }
    val searchViewModel: com.buslk.ui.search.SearchViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.buslk.ui.search.SearchViewModelFactory(searchRepository)
    )
    val routeDetailsViewModel: com.buslk.ui.search.RouteDetailsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.buslk.ui.search.RouteDetailsViewModelFactory(liveMapRepository, searchRepository)
    )
    val busDetailsViewModel: com.buslk.ui.search.BusDetailsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.buslk.ui.search.BusDetailsViewModelFactory(searchRepository, feedbackRepository, liveMapRepository)
    )

    // Instantiate Trip & Feedback dependencies
    // OOD Principle (DI / Dependency Inversion): TripViewModel needs SearchRepository to
    // resolve bus metadata (route name, reg number) after a successful QR check-in.
    // We pass the SAME shared searchRepository instance created above so the ViewModel
    // reuses an existing Firestore connection rather than opening a duplicate one.
    val tripViewModel: com.buslk.ui.viewmodels.TripViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.buslk.ui.viewmodels.TripViewModelFactory(tripRepository, searchRepository)
    )
    val feedbackViewModel: com.buslk.ui.viewmodels.FeedbackViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.buslk.ui.viewmodels.FeedbackViewModelFactory(feedbackRepository)
    )

    var currentDestination by rememberSaveable {
        val startDestination = if (authViewModel.isSignedIn()) AppDestinations.HOME else AppDestinations.OPENING
        // Check if the saved state is empty (e.g., first launch) and if so, use the startDestination
        mutableStateOf(startDestination)
    }
    
    var scannedBusId by rememberSaveable {
        mutableStateOf("")
    }

    var chatPartnerName by rememberSaveable {
        mutableStateOf("")
    }

    var previousDestination by rememberSaveable {
        mutableStateOf(AppDestinations.SOCIAL)
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
                    it != AppDestinations.FEEDBACK &&
                    it != AppDestinations.CHAT
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
                            mapViewModel = mapViewModel,
                            searchViewModel = searchViewModel,
                            onScanClick = { currentDestination = AppDestinations.SCAN_QR }
                        )
                        AppDestinations.SEARCH -> com.buslk.ui.screens.SearchScreen(
                            searchViewModel = searchViewModel,
                            routeDetailsViewModel = routeDetailsViewModel,
                            busDetailsViewModel = busDetailsViewModel
                        )
                        AppDestinations.LOST_AND_FOUND -> com.buslk.ui.screens.LostAndFoundScreen(
                            viewModel = lostAndFoundViewModel,
                            onContactClick = { reporterName ->
                                chatPartnerName = reporterName
                                previousDestination = AppDestinations.LOST_AND_FOUND
                                currentDestination = AppDestinations.CHAT
                            }
                        )
                        AppDestinations.SOCIAL -> com.buslk.ui.screens.FriendsScreen(
                            onChatClick = { friendName ->
                                chatPartnerName = friendName
                                previousDestination = AppDestinations.SOCIAL
                                currentDestination = AppDestinations.CHAT
                            }
                        )
                        AppDestinations.CHAT -> ChatScreen(
                            friendName = chatPartnerName,
                            onBackClick = {
                                currentDestination = previousDestination
                            }
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
                            tripViewModel = tripViewModel,
                            onCheckInSuccess = { busId ->
                                scannedBusId = busId
                                currentDestination = AppDestinations.TRIP_SCREEN
                            },
                            onBack = { currentDestination = AppDestinations.HOME }
                        )
                        AppDestinations.TRIP_SCREEN -> com.buslk.ui.screens.TripScreen(
                            tripViewModel = tripViewModel,
                            busId = scannedBusId,
                            onEndTrip = { currentDestination = AppDestinations.FEEDBACK },
                            onBack = { currentDestination = AppDestinations.HOME }
                        )
                        AppDestinations.FEEDBACK -> com.buslk.ui.screens.FeedbackScreen(
                            feedbackViewModel = feedbackViewModel,
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
    SOCIAL(R.string.nav_friends, Icons.Default.Face),
    LOST_AND_FOUND(R.string.nav_lost_found, Icons.AutoMirrored.Filled.List),
    PROFILE(R.string.nav_profile, Icons.Default.Person),

    SCAN_QR(R.string.nav_scan_qr, Icons.Default.Home),
    TRIP_SCREEN(R.string.nav_trip, Icons.Default.Home),
    FEEDBACK(R.string.nav_feedback, Icons.Default.Star),
    SETTINGS(R.string.nav_settings, Icons.Default.Settings),
    CHAT(R.string.nav_chat, Icons.Default.Face),
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