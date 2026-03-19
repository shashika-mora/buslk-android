package com.buslk.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buslk.ui.search.SearchViewModel
import com.buslk.ui.search.SearchViewModelFactory
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.IconButton
import com.buslk.ui.theme.BusLKBlue
import androidx.compose.ui.graphics.Color

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

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    controller.setZoom(15.0)
                    controller.setCenter(GeoPoint(6.9271, 79.8612))
                    setMultiTouchControls(true)
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    isTilesScaledToDpi = true
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

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
            placeholder = { Text("Search bus route (e.g. 138) or Bus NO") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
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
                        Icon(Icons.Default.Clear, contentDescription = "Clear Search")
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
                onRouteClick = { route ->
                    active = false
                    searchQuery = route.routeId
                },
                onBusClick = { bus ->
                    active = false
                    searchQuery = bus.registrationNumber
                }
            )
        }

        // Floating Action Button for QR Scanning
        if (!active) {
            FloatingActionButton(
                onClick = onScanClick,
                containerColor = BusLKBlue,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 16.dp) // Extra padding to stay above nav bar
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan QR Code",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
