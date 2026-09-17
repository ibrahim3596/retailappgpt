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
        private val SYNC_SERVER_URL = stringPreferencesKey("sync_server_url")
        private val SYNC_ACCESS_TOKEN = stringPreferencesKey("sync_access_token")
        private val SYNC_REFRESH_TOKEN = stringPreferencesKey("sync_refresh_token")
        private val SYNC_LAST_PULLED_AT = stringPreferencesKey("sync_last_pulled_at")
        private val SYNC_MASTER_PUSHED = booleanPreferencesKey("sync_master_pushed")
    }

    val isSetupComplete: Flow<Boolean> = context.dataStore.data
        .map { it[IS_SETUP_COMPLETE] ?: false }

    val loggedInUserId: Flow<String?> = context.dataStore.data
        .map { it[LOGGED_IN_USER_ID] }

    val currentStoreId: Flow<String?> = context.dataStore.data
        .map { it[CURRENT_STORE_ID] }

    val syncServerUrl: Flow<String?> = context.dataStore.data
        .map { it[SYNC_SERVER_URL] }

    val syncAccessToken: Flow<String?> = context.dataStore.data
        .map { it[SYNC_ACCESS_TOKEN] }

    val syncRefreshToken: Flow<String?> = context.dataStore.data
        .map { it[SYNC_REFRESH_TOKEN] }

    val syncLastPulledAt: Flow<String?> = context.dataStore.data
        .map { it[SYNC_LAST_PULLED_AT] }

    val syncMasterPushed: Flow<Boolean> = context.dataStore.data
        .map { it[SYNC_MASTER_PUSHED] ?: false }

    suspend fun setSetupComplete(complete: Boolean) = edit { it[IS_SETUP_COMPLETE] = complete }

    suspend fun setLoggedInUserId(userId: String?) = edit { prefs ->
        if (userId == null) prefs.remove(LOGGED_IN_USER_ID) else prefs[LOGGED_IN_USER_ID] = userId
    }

    suspend fun setCurrentStoreId(storeId: String?) = edit { prefs ->
        if (storeId == null) prefs.remove(CURRENT_STORE_ID) else prefs[CURRENT_STORE_ID] = storeId
    }

    suspend fun setSyncServerUrl(url: String?) = edit { prefs ->
        if (url.isNullOrBlank()) prefs.remove(SYNC_SERVER_URL) else prefs[SYNC_SERVER_URL] = url
    }

    suspend fun setSyncTokens(access: String?, refresh: String?) = edit { prefs ->
        if (access == null) prefs.remove(SYNC_ACCESS_TOKEN) else prefs[SYNC_ACCESS_TOKEN] = access
        if (refresh == null) prefs.remove(SYNC_REFRESH_TOKEN) else prefs[SYNC_REFRESH_TOKEN] = refresh
    }

    suspend fun setSyncLastPulledAt(iso: String?) = edit { prefs ->
        if (iso == null) prefs.remove(SYNC_LAST_PULLED_AT) else prefs[SYNC_LAST_PULLED_AT] = iso
    }

    suspend fun setSyncMasterPushed(done: Boolean) = edit { prefs ->
        prefs[SYNC_MASTER_PUSHED] = done
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
