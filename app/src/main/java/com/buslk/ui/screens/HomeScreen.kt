package com.buslk.ui.screens

import android.widget.Toast
import android.preference.PreferenceManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.buslk.utils.OsmMapManager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * The main Home Screen Composable containing the interactive Map.
 * 
 * OOD Principle: UI as a Function of State.
 * This function defines *what* the screen looks like. It delegates the complex
 * map initialization to the [OsmMapManager] Singleton, keeping this function focused solely on rendering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    // Grab the current Android Context (Activity) needed to initialize native Android Views
    val context = LocalContext.current

    // UI State for SearchBar
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(false) }

    // Grab the current LifecycleOwner (usually the Activity or Navigation BackStackEntry)
    // We need this to know when the app goes into the background or foreground.
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize OSM configuration via Singleton Manager (OOP Encapsulation)
    // This is safe to call here because the Singleton guarantees it only runs once.
    OsmMapManager.initialize(context)

    // 'remember' tells Jetpack Compose to keep this MapView object alive across Recompositions.
    // If we didn't use 'remember', Compose would create a brand new map every time the screen redraws!
    val mapView = remember { MapView(context) }

    // Tie MapView lifecycle to the Compose Lifecycle Owner.
    // DisposableEffect runs once when the Composable enters the screen, and provides an 'onDispose'
    // block for when the Composable leaves the screen, ensuring we clean up memory.
    DisposableEffect(lifecycleOwner, mapView) {
        // Create an observer that listens for Android OS events (like switching apps)
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                // App came to the foreground
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                // App went to the background (Pause the map so it stops draining memory/battery)
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                // Activity is being destroyed
                Lifecycle.Event.ON_DESTROY -> mapView.onDetach()
                else -> {}
            }
        }
        
        // Attach our listener to the Android OS
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        
        // Cleanup routine when this Composable is completely removed from UI
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            // Detach disconnects the map from the hardware rendering, preventing strict memory leaks
            mapView.onDetach()
        }
    }

    /**
     * Wrap the legacy MapView and floating UI elements in a Box to allow layering (Z-index).
     */
    Box(modifier = Modifier.fillMaxSize()) {
        /**
     * AndroidView acts as a "bridge" to use legacy XML-based View classes inside modern Jetpack Compose.
     * Since osmdroid's MapView is an old-school Android View, we wrap it in an AndroidView.
     */
        AndroidView(
            // 'factory' runs exactly ONCE to instantiate and configure the View.
            factory = {
                mapView.apply {
                    // Set the visual style of the map to standard Mapnik (OpenStreetMap default)
                    setTileSource(TileSourceFactory.MAPNIK)
                    // Set default starting zoom level (15 is a close-up city view)
                    controller.setZoom(15.0)
                    // Center roughly on Colombo, Sri Lanka (Latitude, Longitude)
                    controller.setCenter(GeoPoint(6.9271, 79.8612))
                    // Allow pinch-to-zoom and two-finger rotation
                    setMultiTouchControls(true)
                    // --- Performance / UX Optimizations ---
                    // 1. Force hardware acceleration for map drawing to free up the CPU
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    // 2. Scale the pixel tiles to match the high-density screens (DPI) of modern phones
                    isTilesScaledToDpi = true
                    // 3. Hide the ugly default zoom buttons (+/-); users expect pinch-to-zoom
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                }
            },
            // 'update' runs every time Compose decides the screen needs to be redrawn (Recomposition).
            // This is where we will eventually put logic to draw moving bus markers when we get live data.
            update = {
                // Future updates for markers will go here (Observing ViewModel StateFlows)
            },
            // Tell the AndroidView to stretch and fill the entire available screen space
            modifier = Modifier.fillMaxSize()
        )
        /**
         * Floating Search Bar for routing.
         */
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = {
                active = false
                // Logic to be implemented later
            },
            active = active,
            onActiveChange = { active = it },
            placeholder = { Text("Search bus route (e.g. 138)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(100.dp)
        ) {
            // Placeholder for search results list (UI only)
        }
    }
}
