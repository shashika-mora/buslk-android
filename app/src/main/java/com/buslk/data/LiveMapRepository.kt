package com.buslk.data

import android.util.Log
import com.buslk.domain.models.BusLocation
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Concrete implementation of [ILiveMapRepository] that connects directly to the
 * Firebase Realtime Database (RTDB) node "bus_locations".
 */
class LiveMapRepository : ILiveMapRepository {

    // Obtain RTDB singleton reference
    private val database = FirebaseDatabase.getInstance("https://buslk-app-default-rtdb.asia-southeast1.firebasedatabase.app").reference

    override fun getLiveBusLocations(): Flow<Result<List<BusLocation>>> = callbackFlow {
        // Reference to the high-velocity GPS node
        val locationsRef = database.child("bus_locations")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val buses = mutableListOf<BusLocation>()
                    
                    // Iterate through every active bus node (e.g. "NA-1234")
                    for (child in snapshot.children) {
                        val busId = child.key ?: continue
                        // Deserialize the JSON payload directly into our Kotlin Domain Model
                        val location = child.getValue(BusLocation::class.java)
                        
                        if (location != null) {
                            // RTDB doesn't store the key inside the object by default, so we inject it
                            buses.add(location.copy(busId = busId))
                        }
                    }
                    
                    // Emit the updated list to the StateFlow collectors (e.g., the MapViewModel)
                    trySend(Result.success(buses))
                } catch (e: Exception) {
                    Log.e("LiveMapRepository", "Error parsing bus locations", e)
                    trySend(Result.failure(e))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LiveMapRepository", "Database error: ${error.message}")
                trySend(Result.failure(error.toException()))
            }
        }

        // Attach the listener. This stays active and triggers instantly on every database write.
        locationsRef.addValueEventListener(listener)

        // Important: When the collector (ViewModel) cancels the flow (e.g. user leaves Map screen),
        // we MUST remove the database listener to prevent massive memory and data leaks.
        awaitClose {
            locationsRef.removeEventListener(listener)
        }
    }
}
