package com.buslk.ui.map

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
fun LiveBusMap(modifier: Modifier = Modifier) {
    val colomboPoint = remember { GeoPoint(6.9271, 79.8612) }
    var mapReference by remember { mutableStateOf<MapView?>(null) }
    var isScannerOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        // Choose the tile source
                        setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        controller.setCenter(colomboPoint)

                        // Add BusOverlay (currently empty, waiting for RTDB)
                        overlays.add(BusOverlay(ctx))

                        mapReference = this
                    }
                },
                update = { view -> mapReference = view }
        )

        FloatingActionButton(
                onClick = { mapReference?.controller?.animateTo(colomboPoint) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Default.MyLocation, contentDescription = "Recenter") }

        FloatingActionButton(
                onClick = { isScannerOpen = true },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        ) { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR") }

        if (isScannerOpen) {
            QRScanner(
                    onQrScanned = { busId ->
                        isScannerOpen = false
                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                        if (userId != null) {
                            val database = FirebaseDatabase.getInstance()
                            val tripRef = database.getReference("trips").child(userId)

                            val tripData =
                                    mapOf(
                                            "busId" to busId,
                                            "startTime" to System.currentTimeMillis(),
                                            "status" to "active"
                                    )

                            tripRef.setValue(tripData)
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                                        context,
                                                        "Successfully Checked-in to Bus $busId",
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                                        context,
                                                        "Failed to Check-in",
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                                    }
                        } else {
                            Toast.makeText(context, "Please log in first", Toast.LENGTH_SHORT)
                                    .show()
                        }
                    },
                    onClose = { isScannerOpen = false }
            )
        }
    }
}
