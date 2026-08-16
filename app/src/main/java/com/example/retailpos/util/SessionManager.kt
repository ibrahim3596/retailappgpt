package com.example.retailpos.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

class SessionManager(private val context: Context) {

    companion object {
        private val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
        private val LOGGED_IN_USER_ID = stringPreferencesKey("logged_in_user_id")
        private val CURRENT_STORE_ID = stringPreferencesKey("current_store_id")
    }

    val isSetupComplete: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_SETUP_COMPLETE] ?: false
        }

    val loggedInUserId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[LOGGED_IN_USER_ID]
        }

    val currentStoreId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[CURRENT_STORE_ID]
        }

    suspend fun setSetupComplete(complete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_SETUP_COMPLETE] = complete
        }
    }

    suspend fun setLoggedInUserId(userId: String?) {
        context.dataStore.edit { preferences ->
            if (userId == null) {
                preferences.remove(LOGGED_IN_USER_ID)
            } else {
                preferences[LOGGED_IN_USER_ID] = userId
            }
        }
    }

    suspend fun setCurrentStoreId(storeId: String?) {
        context.dataStore.edit { preferences ->
            if (storeId == null) {
                preferences.remove(CURRENT_STORE_ID)
            } else {
                preferences[CURRENT_STORE_ID] = storeId
            }
        }
    }
}
