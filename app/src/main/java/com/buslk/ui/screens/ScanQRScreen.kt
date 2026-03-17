package com.buslk.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.buslk.ui.map.QRScanner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun ScanQRScreen(
    onCheckInSuccess: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        QRScanner(
            onQrScanned = { busId ->
                // Passenger-as-a-Sensor: Real-time Database Check-in
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    val database = FirebaseDatabase.getInstance()
                    val tripRef = database.getReference("trips").child(userId)

                    val tripData = mapOf(
                        "busId" to busId,
                        "startTime" to System.currentTimeMillis(),
                        "status" to "active"
                    )

                    tripRef.setValue(tripData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Checked-in to Bus: $busId", Toast.LENGTH_SHORT).show()
                            onCheckInSuccess(busId)
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Network error. Try again.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Authentication required.", Toast.LENGTH_SHORT).show()
                }
            },
            onClose = onBack
        )
    }
}
