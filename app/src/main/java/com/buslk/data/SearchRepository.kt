package com.buslk.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Abstract interface for Search operations.
 * OOD Principle: Dependency Inversion.
 */
interface ISearchRepository {
    suspend fun searchRoutes(query: String): Result<List<RouteDoc>>
    suspend fun searchBuses(query: String): Result<List<BusDoc>>
}