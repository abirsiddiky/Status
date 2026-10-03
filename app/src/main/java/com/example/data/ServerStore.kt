package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ServerConfig
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "status_preferences")

data class AppSettings(
    val refreshIntervalSeconds: Int = 3,
    val reduceAnimations: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
)

class ServerStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    private object PreferencesKeys {
        val SERVERS_JSON = stringPreferencesKey("servers_json")
        val ACTIVE_SERVER_ID = stringPreferencesKey("active_server_id")
        val REFRESH_INTERVAL_SECONDS = intPreferencesKey("refresh_interval_seconds")
        val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val serversFlow: Flow<List<ServerConfig>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val raw = prefs[PreferencesKeys.SERVERS_JSON]
            if (raw.isNullOrBlank()) {
                emptyList()
            } else {
                try {
                    json.decodeFromString<List<ServerConfig>>(raw)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

    val activeServerIdFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[PreferencesKeys.ACTIVE_SERVER_ID] }

    val appSettingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val refresh = prefs[PreferencesKeys.REFRESH_INTERVAL_SECONDS] ?: 3
            val reduceAnim = prefs[PreferencesKeys.REDUCE_ANIMATIONS] ?: false
            val themeStr = prefs[PreferencesKeys.THEME_MODE] ?: AppThemeMode.SYSTEM.name
            val themeMode = try {
                AppThemeMode.valueOf(themeStr)
            } catch (e: Exception) {
                AppThemeMode.SYSTEM
            }
            AppSettings(
                refreshIntervalSeconds = refresh,
                reduceAnimations = reduceAnim,
                themeMode = themeMode
            )
        }

    suspend fun saveServer(server: ServerConfig, makeActive: Boolean = true) {
        context.dataStore.edit { prefs ->
            val currentServers = getCurrentServers(prefs).toMutableList()
            val index = currentServers.indexOfFirst { it.id == server.id }
            if (index >= 0) {
                currentServers[index] = server
            } else {
                currentServers.add(server)
            }
            prefs[PreferencesKeys.SERVERS_JSON] = json.encodeToString(currentServers)
            if (makeActive || prefs[PreferencesKeys.ACTIVE_SERVER_ID] == null) {
                prefs[PreferencesKeys.ACTIVE_SERVER_ID] = server.id
            }
        }
    }

    suspend fun deleteServer(serverId: String) {
        context.dataStore.edit { prefs ->
            val currentServers = getCurrentServers(prefs).toMutableList()
            currentServers.removeAll { it.id == serverId }
            prefs[PreferencesKeys.SERVERS_JSON] = json.encodeToString(currentServers)

            val currentActive = prefs[PreferencesKeys.ACTIVE_SERVER_ID]
            if (currentActive == serverId) {
                // Set active to first available or clear
                prefs[PreferencesKeys.ACTIVE_SERVER_ID] = currentServers.firstOrNull()?.id ?: ""
            }
        }
    }

    suspend fun setActiveServerId(serverId: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ACTIVE_SERVER_ID] = serverId
        }
    }

    suspend fun setRefreshInterval(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.REFRESH_INTERVAL_SECONDS] = seconds
        }
    }

    suspend fun setReduceAnimations(reduce: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.REDUCE_ANIMATIONS] = reduce
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    private fun getCurrentServers(prefs: Preferences): List<ServerConfig> {
        val raw = prefs[PreferencesKeys.SERVERS_JSON] ?: return emptyList()
        return try {
            json.decodeFromString<List<ServerConfig>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
