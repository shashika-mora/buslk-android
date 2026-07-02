package com.buslk.data

import com.buslk.domain.models.BusLocation
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining the contract for accessing live map telemetry data.
 * Adheres to Dependency Inversion Principle.
 */
interface ILiveMapRepository {
    
    /**
     * Subscribes to live GPS updates.
     * High-Scale Optimization: If [routeId] is provided, filters updates at the DB query level.
     * Emits a new list every time a matching bus broadcasts a new coordinate.
     */
    fun getLiveBusLocations(routeId: String? = null): Flow<Result<List<BusLocation>>>
}
