package com.buslk.data

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseRepository {
    // Firestore for static data
    val firestore = FirebaseFirestore.getInstance()

    // Realtime Database for dynamic data
    val realtimeDb = FirebaseDatabase.getInstance()
    
    // You can add helper functions related to these instances here.
}
