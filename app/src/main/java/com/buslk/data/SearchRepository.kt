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

/**
 * Concrete implementation using Firestore.
 */
class SearchRepository : ISearchRepository {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun searchRoutes(query: String): Result<List<RouteDoc>> {
        return try {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return Result.success(emptyList())

            // Firestore doesn't support native "LIKE %query%" string matching easily.
            // For MVP, if they type a number we search by routeId exactly.
            // If they type text, we'd ideally need a 3rd party search engine (Algolia/Meilisearch).
            // For now, we will fetch all routes and do local filtering since the total route count in SL is ~1500 (small enough for client-side filtering MVP).

            val snapshot = db.collection("routes").get().await()
            val allRoutes = snapshot.documents.mapNotNull { it.toObject(RouteDoc::class.java) }

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

            // Fetch buses and filter locally for MVP
            val snapshot = db.collection("buses").get().await()
            val allBuses = snapshot.documents.mapNotNull { it.toObject(BusDoc::class.java) }

            val filtered = allBuses.filter {
                it.registrationNumber.lowercase().contains(q) ||
                        it.defaultRouteId.lowercase().contains(q)
            }
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}