package com.buslk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buslk.data.ILostAndFoundRepository
import com.buslk.data.IAuthRepository
import com.buslk.data.LostAndFoundDoc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Represents the UI state for the Lost and Found screen list.
 */
sealed class LostAndFoundUiState {
    object Loading : LostAndFoundUiState()
    data class Success(val items: List<LostFoundItemUiStore>) : LostAndFoundUiState()
    data class Error(val message: String) : LostAndFoundUiState()
}

/**
 * UI-friendly representation of a LostAndFoundDoc.
 * Transformed from the backend model to be easily consumed by Jetpack Compose.
 */
data class LostFoundItemUiStore(
    val id: String,
    val title: String,
    val description: String,
    val route: String,
    val location: String,
    val timeAgo: String,
    val reporterName: String,
    val isFound: Boolean,
    val isClosed: Boolean
)

class LostAndFoundViewModel(
    private val repository: ILostAndFoundRepository,
    private val authRepository: IAuthRepository
) : ViewModel() {

    // Internal state to handle temporary failures or loading before the first flow emission
    private val _uiState = MutableStateFlow<LostAndFoundUiState>(LostAndFoundUiState.Loading)

    /**
     * The primary stream of Lost and Found items.
     * We map the backend Result Flow into our UI State wrapper.
     * `stateIn` converts the cold flow into a hot flow that pauses when the user leaves the screen, saving battery.
     */
    val uiState: StateFlow<LostAndFoundUiState> = repository.getLostAndFoundItemsStream()
        .map { result ->
            result.fold(
                onSuccess = { docs ->
                    LostAndFoundUiState.Success(docs.map { it.toUiStore() })
                },
                onFailure = { error ->
                    LostAndFoundUiState.Error(error.localizedMessage ?: "Unknown error occurred")
                }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Keep active for 5s after screen dies to prevent flicker on rotate
            initialValue = LostAndFoundUiState.Loading
        )

    /**
     * Extension function to cleanly map the backend Doc into the UI Object.
     * Applies logic like time-ago formatting.
     */
    private fun LostAndFoundDoc.toUiStore(): LostFoundItemUiStore {
        val calculatedTimeAgo = timestamp?.let { calculateTimeAgo(it.toDate()) } ?: "Just now"

        return LostFoundItemUiStore(
            id = this.itemId,
            title = this.title.ifBlank { "Unknown Item" },
            description = this.description.ifBlank { "No description provided." },
            route = "Route ${this.routeId}",
            location = this.location.ifBlank { "Unknown location" },
            timeAgo = calculatedTimeAgo,
            reporterName = this.reporterName.ifBlank { "Anonymous" },
            isFound = this.itemType == "FOUND", // Boolean mapping
            isClosed = this.status == "CLAIMED" || this.status == "RESOLVED"
        )
    }

    /**
     * Utility to calculate a human-readable "time ago" string.
     */
    private fun calculateTimeAgo(date: Date): String {
        val now = System.currentTimeMillis()
        val time = date.time
        val diff = now - time

        val minute = 60 * 1000L
        val hour = 60 * minute
        val day = 24 * hour

        return when {
            diff < minute -> "Just now"
            diff < 2 * minute -> "a minute ago"
            diff < 50 * minute -> "${diff / minute} minutes ago"
            diff < 90 * minute -> "an hour ago"
            diff < 24 * hour -> "${diff / hour} hours ago"
            diff < 48 * hour -> "yesterday"
            else -> "${diff / day} days ago"
        }
    }

    // --- Actions ---

    /**
     * Submits a newly created item to the remote repository, attaching the live authenticated user.
     */
    fun submitNewItem(title: String, description: String, itemType: String, routeId: String, location: String) {
        val user = authRepository.getCurrentUser()
        
        val newItem = LostAndFoundDoc(
            title = title,
            description = description,
            itemType = itemType,
            routeId = routeId,
            location = location,
            userId = user?.uid ?: "anonymous",
            reporterName = user?.displayName ?: "Anonymous User"
        )
        
        viewModelScope.launch {
            // The result is handled natively because submitting adds it to the DB,
            // which immediately triggers the SnapshotListener flow, which instantly updates `uiState`.
            repository.addLostAndFoundItem(newItem)
        }
    }
}
