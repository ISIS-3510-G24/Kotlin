package com.example.unimarket.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore extension for Context
private val Context.dataStore by preferencesDataStore(name = "settings")

object PreferencesManager {

    // Key used to store the boolean value
    private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    private val CACHE_TTL_MS = longPreferencesKey("cache_ttl_ms")
    private val SYNC_INTERVAL_MIN = intPreferencesKey("sync_interval_min")
    private val SYNC_ON_WIFI_ONLY = booleanPreferencesKey("sync_on_wifi_only")
    private val RETRY_BACKOFF_BASE = longPreferencesKey("retry_backoff_base_ms")

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

    // Defaults: 1h, 15min, wifi-only, 1s backoff
    fun cacheTtl(context: Context): Flow<Long> =
        context.dataStore.data.map { it[CACHE_TTL_MS] ?: 3_600_000L }

    fun syncInterval(context: Context): Flow<Int> =
        context.dataStore.data.map { it[SYNC_INTERVAL_MIN] ?: 15 }

    fun syncOnWifiOnly(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[SYNC_ON_WIFI_ONLY] ?: true }

    fun retryBackoffBase(context: Context): Flow<Long> =
        context.dataStore.data.map { it[RETRY_BACKOFF_BASE] ?: 1_000L }
}