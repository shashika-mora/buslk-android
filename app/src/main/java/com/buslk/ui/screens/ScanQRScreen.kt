package com.buslk.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.buslk.ui.map.QRScanner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun ScanQRScreen(
    onCheckInSuccess: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val handleScan: (String) -> Unit = { busId ->
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
            Toast.makeText(context, "Testing without auth", Toast.LENGTH_SHORT).show()
            // Allow emulator testing to proceed even without being logged in
            onCheckInSuccess(busId)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        QRScanner(
            onQrScanned = handleScan,
            onClose = onBack
        )

        // Debug button for emulator testing 
        Button(
            onClick = { handleScan("Route 138-1234") }, // Use mock ID
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp)
        ) {
            Text("Simulate Scan (Emulator)")
        }
    }
}
