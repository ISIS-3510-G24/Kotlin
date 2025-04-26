package com.example.unimarket.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "unimarket_prefs"

// Extension property to get DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(DATASTORE_NAME)

// Preference Keys
private object PreferenceKeys {
    val CACHE_TTL_MS = longPreferencesKey("cache_ttl_ms")
    val SYNC_INTERVAL_MIN = intPreferencesKey("sync_interval_min")
    val SYNC_ON_WIFI_ONLY = booleanPreferencesKey("sync_on_wifi_only")
    val RETRY_BACKOFF_BASE = longPreferencesKey("retry_backoff_base")
    val SELECTED_INTERESTS = stringSetPreferencesKey("selected_interests")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
}

object PreferencesManager {

    /**
     * Defaults: 1h, 15min, wifi-only, 1s backoff
     */
    fun cacheTtl(context: Context): Flow<Long> =
        context.dataStore.data.map { it[PreferenceKeys.CACHE_TTL_MS] ?: 3_600_000L }

    fun syncInterval(context: Context): Flow<Int> =
        context.dataStore.data.map { it[PreferenceKeys.SYNC_INTERVAL_MIN] ?: 15 }

    fun syncOnWifiOnly(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[PreferenceKeys.SYNC_ON_WIFI_ONLY] ?: true }

    fun retryBackoffBase(context: Context): Flow<Long> =
        context.dataStore.data.map { it[PreferenceKeys.RETRY_BACKOFF_BASE] ?: 1_000L }

    /**
     * Retrieves the set of selected interests as a Flow
     */
    fun getSelectedInterests(context: Context): Flow<Set<String>> =
        context.dataStore.data.map { it[PreferenceKeys.SELECTED_INTERESTS] ?: emptySet() }

    /**
     * Persists the set of selected interests
     */
    suspend fun setSelectedInterests(context: Context, strings: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.SELECTED_INTERESTS] = strings
        }
    }

    /**
     * Persists the onboarding completion flag
     */
    suspend fun setOnboardingCompleted(context: Context, value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.ONBOARDING_COMPLETED] = value
        }
    }
}
