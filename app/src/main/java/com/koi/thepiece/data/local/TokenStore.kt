package com.koi.thepiece.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Extension property defining a DataStore instance
 * for storing authentication-related preferences.
 *
 * Uses Preferences DataStore for lightweight key-value persistence.
 */
private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

/**
 * Manages persistent storage of authentication tokens.
 *
 * This class provides a reactive and secure way to:
 * - Save session tokens after login
 * - Retrieve current token
 * - Clear token during logout
 *
 * DataStore is used instead of SharedPreferences
 * for improved safety, coroutine support, and data consistency.
 */
class TokenStore(private val context: Context) {

    /** Preference key used to store the session token. */
    private val TOKEN_KEY = stringPreferencesKey("auth_token")

    /**
     * Reactive stream exposing the current authentication token.
     *
     * Returns null if no token is stored.
     * Used to determine login state and attach tokens to API requests.
     */
    val tokenFlow: Flow<String?> =
        context.dataStore.data.map { prefs ->
            prefs[TOKEN_KEY]
        }

    /**
     * Saves the session token to persistent storage.
     *
     * Called after successful login.
     */
    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    /**
     * Clears the stored session token.
     *
     * Called during logout or session expiration.
     */
    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
        }
    }
}