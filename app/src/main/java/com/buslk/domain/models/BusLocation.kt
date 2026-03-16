package com.buslk.domain.models

/**
 * Domain-level representation of a Live Bus Location emitted from Firebase RTDB.
 *
 * @param busId The unqiue registration plate or key (e.g. "NA-1234").
 * @param lat Latitude coordinate.
 * @param lng Longitude coordinate.
 * @param heading Direction the bus is facing (0-360 degrees).
 * @param speed Current speed in km/h.
 * @param lastUpdated Epoch timestamp of the last GPS ping.
 * @param routeId The route the bus is currently operating on.
 * @param crowdLevel Estimated crowd: "LOW", "MEDIUM", "HIGH".
 * @param activePassengerCount Number of passengers currently tracked on board.
 */
data class BusLocation(
    val busId: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val heading: Float = 0f,
    val speed: Float = 0f,
    val lastUpdated: Long = 0L,
    val routeId: String = "",
    val crowdLevel: String = "LOW",
    val activePassengerCount: Int = 0
)
