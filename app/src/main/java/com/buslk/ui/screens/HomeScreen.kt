package com.buslk.ui.screens

import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buslk.R
import com.buslk.data.BusDoc
import com.buslk.data.RouteDoc
import com.buslk.ui.search.SearchViewModel
import com.buslk.ui.search.SearchViewModelFactory
import com.buslk.ui.theme.*
import com.buslk.utils.OsmMapManager
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

// --- Mock Data ---
data class MockBus(
    val route: String,
    val reg: String,
    val time: String,
    val dist: String,
    val crowd: String,
    val crowdColor: Color
)

@Composable
fun getMockBuses(): List<MockBus> = listOf(
    MockBus("138", "Bus NA-1234", "2 min", "0.5 km", "LOW", CrowdGreen),
    MockBus("176", "Bus NB-5678", "5 min", "0.8 km", "MEDIUM", CrowdYellow),
    MockBus("120", "Bus NC-9012", "8 min", "1.2 km", "HIGH", CrowdRed),
    MockBus("177", "Bus ND-3456", "12 min", "1.5 km", "LOW", CrowdGreen)
)

/**
 * The main Home Screen Composable containing the interactive Map.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onScanClick: () -> Unit,
    searchViewModel: SearchViewModel = viewModel(factory = SearchViewModelFactory())
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(false) }
    val searchUiState by searchViewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val mockBuses = getMockBuses()

    OsmMapManager.initialize(context)
    val mapView = remember { MapView(context) }

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

    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContainerColor = Color.Transparent,
        sheetPeekHeight = 220.dp,
        sheetShadowElevation = 0.dp,
        sheetDragHandle = null,
        sheetContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // SCAN Button
                FloatingActionButton(
                    onClick = onScanClick,
                    shape = CircleShape,
                    containerColor = BusLKBlue,
                    contentColor = Color.White,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .size(72.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, contentDescription = "Scan")
                        Text("SCAN", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }

                // The White Sheet Content
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(Color.LightGray, CircleShape)
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = BusLKBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Nearby Buses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(bus.reg, color = Color.Gray, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = CrowdGreen)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(bus.time, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                                            }
                                        }

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
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = {
                    mapView.apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(6.9271, 79.8612))
                        setMultiTouchControls(true)
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        isTilesScaledToDpi = true
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
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
                SearchContent(
                    uiState = searchUiState,
                    onRouteClick = { route: RouteDoc ->
                        active = false
                        searchQuery = route.routeId
                    },
                    onBusClick = { bus: BusDoc ->
                        active = false
                        searchQuery = bus.registrationNumber
                    }
                )
            }
        }
    }
}
