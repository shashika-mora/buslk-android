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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import com.buslk.ui.map.QRScanner
import com.buslk.ui.viewmodels.TripViewModel
import com.buslk.ui.viewmodels.TripUiState

@Composable
fun ScanQRScreen(
    tripViewModel: TripViewModel,
    onCheckInSuccess: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by tripViewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is TripUiState.CheckedIn -> {
                Toast.makeText(context, "Checked-in to Bus: ${state.busId}", Toast.LENGTH_SHORT).show()
                onCheckInSuccess(state.busId)
            }
            is TripUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    val handleScan: (String) -> Unit = { busId ->
        tripViewModel.checkIn(busId)
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
