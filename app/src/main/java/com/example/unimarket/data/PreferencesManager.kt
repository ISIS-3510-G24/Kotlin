package com.example.unimarket.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore extension for Context
private val Context.dataStore by preferencesDataStore(name = "settings")

object PreferencesManager {

    // Key used to store the boolean value
    private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

    // Function to set the onboarding completed flag
    suspend fun setOnboardingCompleted(context: Context, completed: Boolean) {
        // Writes to DataStore in a suspend function
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    // Function to check if the onboarding is completed
    fun isOnboardingCompleted(context: Context): Flow<Boolean> {
        // Reads from DataStore as a Flow
        return context.dataStore.data.map { preferences ->
            preferences[ONBOARDING_COMPLETED] ?: false
        }
    }
}