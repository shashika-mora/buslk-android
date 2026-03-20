package com.buslk.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.buslk.data.BusDoc
import com.buslk.data.RouteDoc
import com.buslk.data.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val routes: List<RouteDoc>, val buses: List<BusDoc>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel(private val repository: SearchRepository = SearchRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun performSearch(query: String) {
        if (query.isBlank()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        _uiState.value = SearchUiState.Loading
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            try {
                val routesResult = repository.searchRoutes(query)
                val busesResult = repository.searchBuses(query)
                
                if (routesResult.isSuccess && busesResult.isSuccess) {
                    _uiState.value = SearchUiState.Success(
                        routesResult.getOrDefault(emptyList()),
                        busesResult.getOrDefault(emptyList())
                    )
                } else {
                    val errorMsg = routesResult.exceptionOrNull()?.message 
                        ?: busesResult.exceptionOrNull()?.message 
                        ?: "Failed to fetch search results"
                    _uiState.value = SearchUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.value = SearchUiState.Idle
    }
}

class SearchViewModelFactory(private val repository: SearchRepository = SearchRepository()) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
