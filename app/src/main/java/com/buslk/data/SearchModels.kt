package com.buslk.data

import com.google.firebase.firestore.PropertyName

/**
 * Data model for a Bus Route fetched from Firestore.
 */
data class RouteDoc(
    val routeId: String = "",
    val name: String = "",
    val startLocation: String = "",
    val endLocation: String = "",
    val distanceKm: Double = 0.0,
    val stops: List<RouteStop> = emptyList()
)

data class RouteStop(
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val order: Int = 0,
    @get:PropertyName("isTransferHub")
    @set:PropertyName("isTransferHub")
    var isTransferHub: Boolean = false
)