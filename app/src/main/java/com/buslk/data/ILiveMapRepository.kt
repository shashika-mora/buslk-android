package com.buslk.data

import com.buslk.domain.models.BusLocation
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining the contract for accessing live map telemetry data.
 * Adheres to Dependency Inversion Principle.
 */
interface ILiveMapRepository {

    /**
     * Subscribes to live GPS updates for all active buses.
     * Emits a new list every time a bus broadcasts a new coordinate.
     */
    fun getLiveBusLocations(): Flow<Result<List<BusLocation>>>
}
