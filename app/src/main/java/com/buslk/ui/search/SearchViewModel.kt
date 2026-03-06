package com.buslk.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.buslk.data.BusDoc
import com.buslk.data.ISearchRepository
import com.buslk.data.RouteDoc
import com.buslk.data.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the UI state of the search operation.
 */
sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(
        val routes: List<RouteDoc>,
        val buses: List<BusDoc>
    ) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

/**
 * ViewModel managing search state and interactions.
 */
class SearchViewModel(
    private val repository: ISearchRepository = SearchRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    /**
     * Executes a combined search for both routes and buses.
     * Uses debounce to prevent hammering the database on every single keystroke.
     */
    fun performSearch(query: String) {
        // Cancel any previous search that was still waiting in the debounce period
        searchJob?.cancel()

        if (query.trim().isEmpty()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            // Debounce for 500ms so we don't query while the user is actively typing "1...3...8"
            delay(500)
            
            _uiState.value = SearchUiState.Loading

            // Fetch in parallel or sequentially. For MVP, we'll do sequentially.
            val routeResult = repository.searchRoutes(query)
            val busResult = repository.searchBuses(query)

            if (routeResult.isSuccess && busResult.isSuccess) {
                _uiState.value = SearchUiState.Success(
                    routes = routeResult.getOrDefault(emptyList()),
                    buses = busResult.getOrDefault(emptyList())
                )
            } else {
                val error = routeResult.exceptionOrNull() ?: busResult.exceptionOrNull()
                _uiState.value = SearchUiState.Error(error?.message ?: "Unknown search error")
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.value = SearchUiState.Idle
    }
}

/**
 * Factory class required by Android to instantiate ViewModels with constructor arguments.
 */
class SearchViewModelFactory(
    private val repository: ISearchRepository = SearchRepository()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
