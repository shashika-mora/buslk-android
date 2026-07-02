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
    suspend fun getAllRoutes(): Result<List<RouteDoc>>
    suspend fun getBusDetails(busId: String): Result<BusDoc?>
    suspend fun getBusesByRoute(routeId: String): Result<List<BusDoc>>
}

/**
 * Concrete implementation using Firestore.
 * 
 * Architecture Principle: Repository Pattern.
 * This class abstracts away *how* the data is fetched (via Firebase API). 
 * The ViewModel only knows it gets a `Result<List<RouteDoc>>`, meaning later on, we could easily
 * swap Firestore out for a local SQLite database (Room) or a REST API, and the ViewModel wouldn't 
 * need to change a single line of code.
 */
class SearchRepository : ISearchRepository {
    private val db = FirebaseFirestore.getInstance()

    // OOD Principle (Optimization & Performance):
    // In-memory cache of static metadata (Routes and Bus profiles).
    // Because routes and registered buses are mostly static, downloading the entire
    // collection once per app lifecycle and filtering locally prevents billions of
    // Firestore read operations under a 100,000+ user load.
    private var cachedRoutes: List<RouteDoc>? = null
    private var cachedBuses: List<BusDoc>? = null

    override suspend fun searchRoutes(query: String): Result<List<RouteDoc>> {
        return try {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return Result.success(emptyList())

            // Fetch from Firestore only if the local cache is empty
            val allRoutes = cachedRoutes ?: run {
                val snapshot = db.collection("routes").get().await()
                val routes = snapshot.documents.mapNotNull { it.toObject(RouteDoc::class.java) }
                cachedRoutes = routes
                routes
            }
            
            val filtered = allRoutes.filter { 
                it.routeId.lowercase().contains(q) || 
                it.name.lowercase().contains(q) ||
                it.startLocation.lowercase().contains(q) ||
                it.endLocation.lowercase().contains(q)
            }
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchBuses(query: String): Result<List<BusDoc>> {
        return try {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return Result.success(emptyList())

            // Fetch from Firestore only if the local cache is empty
            val allBuses = cachedBuses ?: run {
                val snapshot = db.collection("buses").get().await()
                val buses = snapshot.documents.mapNotNull { it.toObject(BusDoc::class.java) }
                cachedBuses = buses
                buses
            }
            
            val filtered = allBuses.filter { 
                it.registrationNumber.lowercase().contains(q) || 
                it.defaultRouteId.lowercase().contains(q)
            }
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllRoutes(): Result<List<RouteDoc>> {
        return try {
            val allRoutes = cachedRoutes ?: run {
                val snapshot = db.collection("routes").get().await()
                val routes = snapshot.documents.mapNotNull { it.toObject(RouteDoc::class.java) }
                cachedRoutes = routes
                routes
            }
            Result.success(allRoutes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBusDetails(busId: String): Result<BusDoc?> {
        return try {
            val doc = db.collection("buses").document(busId).get().await()
            Result.success(doc.toObject(BusDoc::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBusesByRoute(routeId: String): Result<List<BusDoc>> {
        return try {
            val snapshot = db.collection("buses").whereEqualTo("defaultRouteId", routeId).get().await()
            val buses = snapshot.documents.mapNotNull { it.toObject(BusDoc::class.java) }
            Result.success(buses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
