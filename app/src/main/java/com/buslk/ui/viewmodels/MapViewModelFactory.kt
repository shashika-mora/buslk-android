package com.buslk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.buslk.data.ILiveMapRepository

/**
 * Factory class responsible for creating the [MapViewModel].
 *
 * OOD Principle: Dependency Injection manually implemented.
 * We pass the repository interface here, meaning the ViewModel doesn't care
 * if it's talking to Firebase or a local Mock Database for testing.
 */
class MapViewModelFactory(
    private val liveMapRepository: ILiveMapRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(liveMapRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class provided to MapViewModelFactory")
    }
}
