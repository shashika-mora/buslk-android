package com.buslk.utils

import android.content.Context
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration

/**
 * Singleton configuration manager for OpenStreetMap (osmdroid).
 * 
 * OOD Principle: Singleton Pattern & Separation of Concerns.
 * An 'object' in Kotlin is a Singleton - there is only ever ONE instance of this class in the entire app.
 * We extract initialization of the Map into this class so that our UI (Composables) remain 
 * "pure" and don't get cluttered with setup logic. 
 */
object OsmMapManager {
    // Keeps track of whether we have already loaded the configuration to prevent doing redundant work.
    private var isInitialized = false

    /**
     * Initializes the osmdroid library configuration.
     * Must be called before any MapView is instantiated or displayed.
     *
     * @param context The Android Application or Activity Context, needed to access 
     *                SharedPreferences (where the library caches its settings).
     */
    fun initialize(context: Context) {
        // If we've already set it up, skip to save CPU cycles.
        if (isInitialized) return
        
        // Load the OSM configuration utilizing Android's default SharedPreferences.
        // This is where map tiles are cached to save the user's mobile data.
        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(context)
        )
        
        // Critical Step: OpenStreetMap usage policy REQUIRES apps to provide a unique User-Agent.
        // If we don't set this, the map servers might block our app for downloading too many map tiles.
        // We playfully use our app's unique package name (e.g. com.buslk).
        Configuration.getInstance().userAgentValue = context.packageName
        
        // Mark as finished so we don't run this setup block ever again.
        isInitialized = true
    }
}
