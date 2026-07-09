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
    object Loading : SearchUiState()
    data class Success(val routes: List<RouteDoc>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

/**
 * ViewModel managing search state and interactions.
 * 
 * Architecture Principle: Model-View-ViewModel (MVVM).
 * The ViewModel sits between the UI (View) and the Repository (Model). It holds the search logic 
 * and data state so that if the Android OS destroys and recreates the UI (e.g., screen rotation), 
 * the ongoing search and its results are safely preserved in memory.
 */
class SearchViewModel(
    private val repository: ISearchRepository = SearchRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Loading)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var allRoutes: List<RouteDoc> = emptyList()

    init {
        loadAllRoutes()
    }

    private fun loadAllRoutes() {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            val result = repository.getAllRoutes()
            if (result.isSuccess) {
                allRoutes = result.getOrDefault(emptyList())
                // Ensure routes are reasonably sorted by ID length then alphabetically
                allRoutes = allRoutes.sortedWith(compareBy({ it.routeId.length }, { it.routeId }))
                _uiState.value = SearchUiState.Success(allRoutes)
            } else {
                _uiState.value = SearchUiState.Error(result.exceptionOrNull()?.message ?: "Failed to load routes")
            }
        }
    }

    fun refreshRoutes() {
        loadAllRoutes()
    }

    fun performSearch(query: String) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            _uiState.value = SearchUiState.Success(allRoutes)
            return
        }

        val filtered = allRoutes.filter {
            it.routeId.lowercase().contains(q) ||
            it.name.lowercase().contains(q) ||
            it.startLocation.lowercase().contains(q) ||
            it.endLocation.lowercase().contains(q)
        }
        _uiState.value = SearchUiState.Success(filtered)
    }

    fun clearSearch() {
        _uiState.value = SearchUiState.Success(allRoutes)
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
