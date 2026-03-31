package com.buslk.domain.models

/**
 * Domain model representing the live location of a bus.
 * This class maps directly to the structure stored in the Firebase Realtime Database.
 */
data class BusLocation(
    val busId: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val heading: Float = 0f,
    val speed: Float = 0f,
    val routeId: String = "",
    val crowdLevel: String = "",
    val registrationNumber: String? = null
)
