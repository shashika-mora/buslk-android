package com.buslk.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.buslk.ui.map.QRScanner
import com.buslk.ui.viewmodels.TripUiState
import com.buslk.ui.viewmodels.TripViewModel

/**
 * Screen that activates the CameraX/ML Kit QR scanner.
 *
 * Architectural Note (qr.md §4.1 – Structural Validation):
 * The raw scanned string MUST be validated against the BusLK payload schema
 * BEFORE any network/database call is initiated. This prevents garbage data
 * from being written to Firestore if the user accidentally scans an
 * unrelated QR code (e.g., a Wi-Fi password, a URL, a product barcode).
 *
 * Valid payload format:  buslk:checkin:{busId}
 * Example valid string:  buslk:checkin:NA-1234
 */
@Composable
fun ScanQRScreen(
    tripViewModel: TripViewModel,
    onCheckInSuccess: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by tripViewModel.uiState.collectAsState()

    // Guard flag: prevents the scanner from firing multiple times for the
    // same frame burst while the check-in network call is in-flight.
    var isProcessing by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is TripUiState.CheckedIn -> {
                isProcessing = false
                Toast.makeText(context, "Checked-in to Bus: ${state.busId}", Toast.LENGTH_SHORT).show()
                onCheckInSuccess(state.busId)
            }
            is TripUiState.Error -> {
                isProcessing = false
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            is TripUiState.Loading -> { /* keep isProcessing = true */ }
            else -> { isProcessing = false }
        }
    }

    /**
     * Payload validation handler (Structural Validation — qr.md §4.1).
     *
     * Steps:
     * 1. Split the raw string on ':' — a valid payload has exactly 3 parts.
     * 2. Verify parts[0] == "buslk"   (custom scheme identifier)
     * 3. Verify parts[1] == "checkin" (action verb)
     * 4. parts[2] is the busId — passed to the ViewModel only if all checks pass.
     *
     * OOP Principle (Encapsulation): validation logic lives here in the UI layer
     * so the TripViewModel and TripRepository only ever receive a clean busId.
     */
    val handleScan: (String) -> Unit = { rawString ->
        if (!isProcessing) {
            val parts = rawString.trim().split(":")

            val isValidPayload = parts.size == 3
                && parts[0] == "buslk"
                && parts[1] == "checkin"
                && parts[2].isNotBlank()

            if (isValidPayload) {
                val busId = parts[2]
                isProcessing = true
                tripViewModel.checkIn(busId)
            } else {
                // Scanned QR does not belong to BusLK — inform the user and stay on screen.
                Toast.makeText(
                    context,
                    "Invalid QR Code. Please scan a BusLK bus QR code.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        QRScanner(
            onQrScanned = handleScan,
            onClose = onBack
        )
    }
}
