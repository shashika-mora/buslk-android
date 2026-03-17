package com.buslk.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.widget.Toast
import androidx.compose.material.icons.filled.QrCodeScanner

// Project Specific Imports (Ensure your folder is named 'map')
import com.buslk.utils.OsmMapManager
import com.buslk.ui.map.QRScanner

// OSMDroid Imports
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

// Firebase Imports
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * The main Home Screen Composable containing the interactive Map.
 * 
 * OOD Principle: UI as a Function of State.
 * This function defines *what* the screen looks like. It delegates the complex
 * map initialization to the [OsmMapManager.kt'] Singleton, keeping this function focused solely on rendering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // --- UI STATE ---
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(false) }
    var isScannerOpen by rememberSaveable { mutableStateOf(false) }

    // Initialize Map Configuration
    OsmMapManager.initialize(context)

    // Maintain the MapView instance across recompositions
    val mapView = remember { MapView(context) }

    // Sync MapView with Android OS Lifecycle (Performance Optimization)
    DisposableEffect(lifecycleOwner, mapView) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDetach()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            mapView.onDetach()
        }
    }

    // --- UI LAYOUT (Z-Index Layering) ---
    Box(modifier = Modifier.fillMaxSize()) {

        // LAYER 1: The Interactive Map
        AndroidView(
            factory = {
                mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    controller.setZoom(15.0)
                    controller.setCenter(GeoPoint(6.9271, 79.8612)) // Colombo
                    setMultiTouchControls(true)

                    // Hardware acceleration for smoother panning on your Lenovo LOQ
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    isTilesScaledToDpi = true
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                }
            },
            update = { /* Real-time bus markers will be updated here later */ },
            modifier = Modifier.fillMaxSize()
        )

        // LAYER 2: Floating Search Bar (Top)
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { active = false },
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
            // Suggestion logic can be added here
        }

        // LAYER 3: QR Scanner Toggle (Bottom)
        FloatingActionButton(
            onClick = { isScannerOpen = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
        }

        // LAYER 4: QR Scanner Overlay (Conditional)
        if (isScannerOpen) {
            QRScanner(
                onQrScanned = { busId ->
                    isScannerOpen = false

                    // Passenger-as-a-Sensor: Real-time Database Check-in
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        val database = FirebaseDatabase.getInstance()
                        val tripRef = database.getReference("trips").child(userId)

                        val tripData = mapOf(
                            "busId" to busId,
                            "startTime" to System.currentTimeMillis(),
                            "status" to "active"
                        )

                        tripRef.setValue(tripData)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Checked-in to Bus: $busId", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Network error. Try again.", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "Authentication required.", Toast.LENGTH_SHORT).show()
                    }
                },
                onClose = { isScannerOpen = false }
            )
        }
    }
}