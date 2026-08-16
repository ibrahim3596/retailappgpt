package com.example.retailpos.engine.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID

private val Context.installationDataStore by preferencesDataStore(name = "installation_prefs")

class InstallationIdManager(private val context: Context) {

    private val KEY_INSTALLATION_ID = stringPreferencesKey("installation_id")

    suspend fun getOrCreateInstallationId(): String {
        val prefs = context.installationDataStore.data.first()
        val existing = prefs[KEY_INSTALLATION_ID]
        if (!existing.isNullOrBlank()) {
            return existing
        }

        val newId = UUID.randomUUID().toString()
        context.installationDataStore.edit { preferences ->
            preferences[KEY_INSTALLATION_ID] = newId
        }
        return newId
    }
}
