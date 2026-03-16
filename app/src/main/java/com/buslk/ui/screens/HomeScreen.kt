package com.buslk.ui.screens

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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.buslk.utils.OsmMapManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.ui.res.stringResource
import com.buslk.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buslk.ui.search.SearchViewModel
import com.buslk.ui.search.SearchViewModelFactory
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.IconButton
import com.buslk.ui.viewmodels.MapViewModel
import com.buslk.ui.viewmodels.MapUiState
import org.osmdroid.views.overlay.Marker
import androidx.core.content.ContextCompat
import java.util.Locale
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

// --- New Imports for BottomSheet & Mock UI ---
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.outlined.Search
import com.buslk.ui.theme.BusLKBlue
import com.buslk.ui.theme.CrowdGreen
import com.buslk.ui.theme.CrowdYellow
import com.buslk.ui.theme.CrowdRed

// --- Mock Data ---
data class MockBus(
    val route: String,
    val reg: String,
    val time: String,
    val dist: String,
    val crowd: String,
    val crowdColor: Color
)
val mockBuses = listOf(
    MockBus("138", "Bus NA-1234", "2 min", "0.5 km", "LOW", CrowdGreen),
    MockBus("176", "Bus NB-5678", "5 min", "0.8 km", "MEDIUM", CrowdYellow),
    MockBus("120", "Bus NC-9012", "8 min", "1.2 km", "HIGH", CrowdRed),
    MockBus("177", "Bus ND-3456", "12 min", "1.5 km", "LOW", CrowdGreen)
)

/**
 * The main Home Screen Composable containing the interactive Map.
 * 
 * OOD Principle: UI as a Function of State.
 * This function defines *what* the screen looks like. It delegates the complex
 * map initialization to the [OsmMapManager] Singleton, keeping this function focused solely on rendering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    searchViewModel: SearchViewModel = viewModel(factory = SearchViewModelFactory()),
    mapViewModel: MapViewModel
) {
    // Grab the current Android Context (Activity) needed to initialize native Android Views
    val context = LocalContext.current
    
    // UI State for SearchBar
    // Using 'rememberSaveable' ensures that if the user rotates their phone or the OS temporarily
    // kills the app to save memory, their typed search query won't be erased.
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(false) }
    
    // Observe Business Logic State
    // 'collectAsState()' transforms the ViewModel's Kotlin Flow into Compose State.
    // Whenever the ViewModel changes this state, this entire HomeScreen function will cleanly
    // and automatically redraw itself (Recomposition).
    val searchUiState by searchViewModel.uiState.collectAsState()
    val mapUiState by mapViewModel.mapState.collectAsState()

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
    val scaffoldState = rememberBottomSheetScaffoldState()

    /**
     * BottomSheetScaffold allows building a map base with a sheet that slides up over it.
     */
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        // The container color must be transparent so our SCAN button looks like it's floating above the sheet
        sheetContainerColor = Color.Transparent,
        sheetPeekHeight = 220.dp, // How much of the sheet is visible when collapsed
        sheetShadowElevation = 0.dp,
        sheetDragHandle = null, // We'll rely on the surface drag
        sheetContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // SCAN Button floating above the white sheet
                FloatingActionButton(
                    onClick = { /* TODO: Scanner */ },
                    shape = CircleShape,
                    containerColor = BusLKBlue,
                    contentColor = Color.White,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .size(72.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Using a standard icon as placeholder for a QR scanner icon
                        Icon(Icons.Default.Search, contentDescription = "Scan") 
                        Text("SCAN", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
                
                // The White Sheet Content
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp), // Height of the sheet when dragged up
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Drag Indicator Bar (Aesthetic)
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(Color.LightGray, CircleShape)
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = BusLKBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Nearby Buses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Bus List
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(mockBuses) { bus ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(), 
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left Col: Route & Dist
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Surface(color = BusLKBlue, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(56.dp)) {
                                                Box(contentAlignment = Alignment.Center) { 
                                                    Text(bus.route, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) 
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(border = BorderStroke(1.dp, Color.LightGray), shape = RoundedCornerShape(50)) {
                                                Text(bus.dist, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.DarkGray)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        
                                        // Mid Col: Reg & Time
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(bus.reg, color = Color.Gray, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = CrowdGreen)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(bus.time, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                                            }
                                        }
                                        
                                        // Right Col: Crowd
                                        Column(horizontalAlignment = Alignment.End) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Box(modifier = Modifier
                                                    .size(10.dp)
                                                    .background(bus.crowdColor, CircleShape))
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(color = bus.crowdColor, shape = RoundedCornerShape(50)) {
                                                Text(
                                                    text = bus.crowd, 
                                                    color = Color.White, 
                                                    fontSize = 10.sp, 
                                                    fontWeight = FontWeight.Bold, 
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        // The background content (Map and SearchBar)
        // We intentionally DO NOT apply paddingValues here so the Map draws underneath 
        // the curved corners of the transparent BottomSheet, removing the white block.
        Box(modifier = Modifier.fillMaxSize()) {
            
            AndroidView(
                factory = {
                    mapView.apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        controller.setZoom(14.0)
                        controller.setCenter(GeoPoint(6.9271, 79.8612)) // Center on Colombo
                        setMultiTouchControls(true)
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                        isTilesScaledToDpi = true
                        zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                    }
                },
                update = { view ->
                    if (mapUiState is MapUiState.Success) {
                        val allBuses = (mapUiState as MapUiState.Success).activeBuses
                        
                        // Filter Logic: If searchQuery is active, strictly show matching buses
                        val filteredBuses = if (searchQuery.isNotBlank()) {
                            val lowerQuery = searchQuery.lowercase(Locale.getDefault())
                            allBuses.filter { bus ->
                                bus.routeId.lowercase(Locale.getDefault()) == lowerQuery ||
                                bus.busId.lowercase(Locale.getDefault()).contains(lowerQuery)
                            }
                        } else {
                            allBuses // Show all if search is empty
                        }
                        
                        // Clear existing markers to prevent ghost trails
                        view.overlays.clear()
                        
                        // Cache the custom red marker drawable to avoid reading disk inside the loop
                        val redBusIcon = ContextCompat.getDrawable(context, R.drawable.ic_bus_marker_red)
                        
                        filteredBuses.forEach { bus ->
                            val marker = Marker(view).apply {
                                position = GeoPoint(bus.lat, bus.lng)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                rotation = bus.heading
                                title = "Reg: ${bus.busId} | Crowd: ${bus.crowdLevel}"
                                subDescription = "Speed: ${bus.speed} km/h | Route: ${bus.routeId}"
                                icon = redBusIcon  // Apply the custom red Material Vector Graphic
                            }
                            view.overlays.add(marker)
                        }
                        
                        // Force OSM to redraw the canvas with the new pins
                        view.invalidate()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            @Suppress("DEPRECATION")
            SearchBar(
                query = searchQuery,
                onQueryChange = { 
                    searchQuery = it 
                    searchViewModel.performSearch(it)
                },
                onSearch = { 
                    searchViewModel.performSearch(it)
                },
                active = active,
                onActiveChange = { 
                    active = it 
                    if (!it) searchViewModel.clearSearch()
                },
                colors = SearchBarDefaults.colors(containerColor = Color.White),
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.icon_search)) },
                trailingIcon = {
                    if (active) {
                        IconButton(onClick = { 
                            if (searchQuery.isNotEmpty()) {
                                searchQuery = ""
                                searchViewModel.clearSearch()
                            } else {
                                active = false
                                searchViewModel.clearSearch()
                            }
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.icon_clear))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (active) 0.dp else 16.dp, vertical = if (active) 0.dp else 24.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(if (active) 0.dp else 100.dp)
            ) {
                when (val state = searchUiState) {
                    is com.buslk.ui.search.SearchUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is com.buslk.ui.search.SearchUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is com.buslk.ui.search.SearchUiState.Success -> {
                        LazyColumn {
                            items(state.routes) { route ->
                                androidx.compose.material3.ListItem(
                                    headlineContent = { Text(route.routeId) },
                                    supportingContent = { Text("${route.startLocation} to ${route.endLocation}") },
                                    modifier = Modifier.clickable {
                                        active = false
                                        searchQuery = route.routeId
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
