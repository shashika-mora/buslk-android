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

@Composable
fun HomeScreen() {
    val context = LocalContext.current

    // Initialize OSM configuration. This is required before creating the MapView
    // We use the application ID as the user agent to comply with OSM usage policies
    Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
    Configuration.getInstance().userAgentValue = context.packageName

    val mapView = remember { MapView(context) }

    DisposableEffect(mapView) {
        onDispose {
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                setTileSource(TileSourceFactory.MAPNIK)
                controller.setZoom(15.0)
                // Center roughly on Colombo, Sri Lanka
                controller.setCenter(GeoPoint(6.9271, 79.8612))
                setMultiTouchControls(true)
            }
        },
        update = {
            // Future updates for markers will go here
        },
        modifier = Modifier.fillMaxSize()
    )
}
