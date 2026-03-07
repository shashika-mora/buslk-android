package com.buslk.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
fun LiveBusMap(modifier: Modifier = Modifier) {
    val colomboPoint = remember { GeoPoint(6.9271, 79.8612) }
    var mapReference by remember { mutableStateOf<MapView?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapView(context).apply {
                        // Choose the tile source
                        setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        controller.setCenter(colomboPoint)

                        // Add BusOverlay (currently empty, waiting for RTDB)
                        overlays.add(BusOverlay(context))

                        mapReference = this
                    }
                },
                update = { view -> mapReference = view }
        )

        FloatingActionButton(
                onClick = { mapReference?.controller?.animateTo(colomboPoint) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Default.MyLocation, contentDescription = "Recenter") }
    }
}
