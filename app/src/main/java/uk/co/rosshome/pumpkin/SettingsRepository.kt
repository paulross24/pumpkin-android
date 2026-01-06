package uk.co.rosshome.pumpkin

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val serverUrlKey = stringPreferencesKey("server_url")
    private val apiKeyKey = stringPreferencesKey("api_key")
    private val includeLocationKey = booleanPreferencesKey("include_location")

    val settingsFlow: Flow<SettingsState> = context.dataStore.data.map { prefs ->
        SettingsState(
            serverUrl = normalizeServerUrl(
                prefs[serverUrlKey] ?: DEFAULT_SERVER_URL
            ),
            apiKey = prefs[apiKeyKey] ?: "",
            includeLocation = prefs[includeLocationKey] ?: false,
        )
    }

    suspend fun readSettings(): SettingsState {
        return settingsFlow.first()
    }

    suspend fun updateServerUrl(value: String) {
        val normalized = normalizeServerUrl(value)
        context.dataStore.edit { prefs ->
            prefs[serverUrlKey] = normalized
        }
    }

    suspend fun updateApiKey(value: String) {
        context.dataStore.edit { prefs ->
            prefs[apiKeyKey] = value.trim()
        }
    }

    suspend fun updateIncludeLocation(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[includeLocationKey] = enabled
        }
    }

    private fun normalizeServerUrl(value: String): String {
        return value.trim().removeSuffix("/")
    }

    companion object {
        const val DEFAULT_SERVER_URL = "https://pumpkin.rosshome.co.uk"
    }
}
