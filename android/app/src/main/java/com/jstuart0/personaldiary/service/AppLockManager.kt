package com.jstuart0.personaldiary.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appLockDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_lock")

/**
 * Manager for app lock functionality
 */
@Singleton
class AppLockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.appLockDataStore

    companion object {
        private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_LOCK_TIMEOUT = longPreferencesKey("lock_timeout")
        private val KEY_LAST_UNLOCK_TIME = longPreferencesKey("last_unlock_time")
        private val KEY_AUTO_LOCK_ENABLED = booleanPreferencesKey("auto_lock_enabled")

        // Lock timeout options (in milliseconds)
        const val TIMEOUT_IMMEDIATE = 0L
        const val TIMEOUT_30_SECONDS = 30_000L
        const val TIMEOUT_1_MINUTE = 60_000L
        const val TIMEOUT_5_MINUTES = 300_000L
        const val TIMEOUT_15_MINUTES = 900_000L
        const val TIMEOUT_30_MINUTES = 1_800_000L
        const val TIMEOUT_1_HOUR = 3_600_000L
    }

    /**
     * Check if app lock is enabled
     */
    val isAppLockEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_APP_LOCK_ENABLED] ?: false
    }

    /**
     * Check if biometric authentication is enabled
     */
    val isBiometricEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_BIOMETRIC_ENABLED] ?: false
    }

    /**
     * Get lock timeout
     */
    val lockTimeout: Flow<Long> = dataStore.data.map { preferences ->
        preferences[KEY_LOCK_TIMEOUT] ?: TIMEOUT_IMMEDIATE
    }

    /**
     * Check if auto lock is enabled
     */
    val isAutoLockEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_AUTO_LOCK_ENABLED] ?: true
    }

    /**
     * Enable or disable app lock
     */
    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_APP_LOCK_ENABLED] = enabled
        }
    }

    /**
     * Enable or disable biometric authentication
     */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    /**
     * Set lock timeout
     */
    suspend fun setLockTimeout(timeout: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_LOCK_TIMEOUT] = timeout
        }
    }

    /**
     * Enable or disable auto lock
     */
    suspend fun setAutoLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_LOCK_ENABLED] = enabled
        }
    }

    /**
     * Update last unlock time
     */
    suspend fun updateLastUnlockTime() {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_UNLOCK_TIME] = System.currentTimeMillis()
        }
    }

    /**
     * Check if app should be locked
     */
    suspend fun shouldLock(): Boolean {
        val preferences = dataStore.data.first()

        val appLockEnabled = preferences[KEY_APP_LOCK_ENABLED] ?: false
        if (!appLockEnabled) return false

        val autoLockEnabled = preferences[KEY_AUTO_LOCK_ENABLED] ?: true
        if (!autoLockEnabled) return false

        val lastUnlockTime = preferences[KEY_LAST_UNLOCK_TIME] ?: 0L
        val timeout = preferences[KEY_LOCK_TIMEOUT] ?: TIMEOUT_IMMEDIATE

        if (timeout == TIMEOUT_IMMEDIATE) return true

        val timeSinceUnlock = System.currentTimeMillis() - lastUnlockTime
        return timeSinceUnlock >= timeout
    }

    /**
     * Reset lock state (for logout)
     */
    suspend fun reset() {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_UNLOCK_TIME] = 0L
        }
    }

    /**
     * Get lock timeout display name
     */
    fun getLockTimeoutDisplayName(timeout: Long): String {
        return when (timeout) {
            TIMEOUT_IMMEDIATE -> "Immediately"
            TIMEOUT_30_SECONDS -> "30 seconds"
            TIMEOUT_1_MINUTE -> "1 minute"
            TIMEOUT_5_MINUTES -> "5 minutes"
            TIMEOUT_15_MINUTES -> "15 minutes"
            TIMEOUT_30_MINUTES -> "30 minutes"
            TIMEOUT_1_HOUR -> "1 hour"
            else -> "Custom"
        }
    }

    /**
     * Get all lock timeout options
     */
    fun getLockTimeoutOptions(): List<Pair<Long, String>> {
        return listOf(
            TIMEOUT_IMMEDIATE to "Immediately",
            TIMEOUT_30_SECONDS to "30 seconds",
            TIMEOUT_1_MINUTE to "1 minute",
            TIMEOUT_5_MINUTES to "5 minutes",
            TIMEOUT_15_MINUTES to "15 minutes",
            TIMEOUT_30_MINUTES to "30 minutes",
            TIMEOUT_1_HOUR to "1 hour"
        )
    }
}
