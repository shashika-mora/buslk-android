package com.buslk.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create a single DataStore instance tied to the Context
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        // Define Keys
        val THEME_MODE = intPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DEFAULT_ROUTE = stringPreferencesKey("default_route")
    }

    // --- Getters (Flows) ---
    // 0 = Auto/System, 1 = Light, 2 = Dark
    val themeModeFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_MODE] ?: 0
        }

    val appLanguageFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[APP_LANGUAGE] ?: "en"
        }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: true
        }

    val defaultRouteFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[DEFAULT_ROUTE] ?: "None"
        }

    // --- Setters (Suspend Functions) ---
    suspend fun updateThemeMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    suspend fun updateAppLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_LANGUAGE] = languageCode
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateDefaultRoute(route: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_ROUTE] = route
        }
    }
}
