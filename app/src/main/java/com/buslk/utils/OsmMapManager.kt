package com.buslk.utils

import android.content.Context
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration

/**
 * Singleton configuration manager for OpenStreetMap (osmdroid).
 * Encapsulates the initialization logic to keep Composables pure.
 */
object OsmMapManager {
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        
        // Initialize OSM configuration.
        // We use the application ID as the user agent to comply with OSM usage policies
        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(context)
        )
        Configuration.getInstance().userAgentValue = context.packageName
        
        isInitialized = true
    }
}
