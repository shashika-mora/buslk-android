package com.buslk.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.buslk.data.AchievementDoc
import com.buslk.data.ProfileRepository
import com.buslk.data.TripDoc
import com.buslk.data.UserDoc
import com.buslk.data.IAuthRepository
import com.buslk.data.AuthRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AchievementUI(
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val iconName: String
)

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val userDoc: UserDoc,
        val trips: List<TripDoc>,
        val achievements: List<AchievementUI>
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(
    private val profileRepository: ProfileRepository = ProfileRepository(),
    private val authRepository: IAuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfileData() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.value = ProfileUiState.Error("User not logged in")
            return
        }

        val userId = currentUser.uid
        _uiState.value = ProfileUiState.Loading

        viewModelScope.launch {
            try {
                val result = kotlinx.coroutines.withTimeoutOrNull(10000L) {
                    // Fetch all data in parallel
                    val userResultDeferred = async { profileRepository.getUserProfile(userId) }
                    val tripsResultDeferred = async { profileRepository.getUserTrips(userId) }
                    val allAchievementsDeferred = async { profileRepository.getAchievements() }
                    val unlockedAchievementsDeferred = async { profileRepository.getUserUnlockedAchievements(userId) }
    
                    val userResult = userResultDeferred.await()
                    val tripsResult = tripsResultDeferred.await()
                    val allAchievementsResult = allAchievementsDeferred.await()
                    val unlockedAchievementsResult = unlockedAchievementsDeferred.await()
    
                    if (userResult.isSuccess) {
                        val userDoc = userResult.getOrThrow()
                        val trips = tripsResult.getOrDefault(emptyList())
                        val allAchievements = allAchievementsResult.getOrDefault(emptyList())
                        val unlockedIds = unlockedAchievementsResult.getOrDefault(emptyList())
    
                        // Map achievements to UI models
                        val achievementsUi = allAchievements.map { doc ->
                            AchievementUI(
                                title = doc.title,
                                description = doc.description,
                                isUnlocked = unlockedIds.contains(doc.id),
                                iconName = doc.iconName
                            )
                        }
    
                        _uiState.value = ProfileUiState.Success(
                            userDoc = userDoc,
                            trips = trips,
                            achievements = achievementsUi
                        )
                    } else {
                        _uiState.value = ProfileUiState.Error(userResult.exceptionOrNull()?.message ?: "Failed to load profile")
                    }
                    true // Return value to indicate completion
                }
                
                if (result == null) {
                    _uiState.value = ProfileUiState.Error("Timed out waiting for Firebase. Please check your internet connection.")
                }

            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.javaClass.simpleName + ": " + e.message)
            }
        }
    }
}

class ProfileViewModelFactory(
    private val profileRepository: ProfileRepository = ProfileRepository(),
    private val authRepository: IAuthRepository = AuthRepository()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(profileRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
