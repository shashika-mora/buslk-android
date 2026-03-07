package com.buslk.ui.map

import android.content.Context
import android.graphics.Canvas
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

class BusOverlay(private val context: Context) : Overlay() {
    
    // We'll update these points via a Firebase listener
    private val busLocations = mutableListOf<GeoPoint>()

    fun updateLocations(newLocations: List<GeoPoint>) {
        busLocations.clear()
        busLocations.addAll(newLocations)
    }

    override fun draw(c: Canvas?, osmv: MapView?, shadow: Boolean) {
        if (shadow) return
        c ?: return
        osmv ?: return
        
        // TODO: iterate over busLocations and draw custom bus icons
        // This is where Firebase Real-time Database bus coordinates will be rendered natively.
    }
}
