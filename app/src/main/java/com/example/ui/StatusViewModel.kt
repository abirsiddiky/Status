package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppSettings
import com.example.data.ConnectionTestResult
import com.example.data.ServerStore
import com.example.data.StatusRepository
import com.example.data.TelemetryState
import com.example.data.model.ServerConfig
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StatusViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val serverStore: ServerStore = ServerStore(application)
    private val repository: StatusRepository = StatusRepository()

    /**
     * Single StateFlow exposing the unified AppState loaded in ONE DataStore read:
     * - [AppState.Loading] while initial read is underway (keeps splash screen visible)
     * - [AppState.NoServer] only when DataStore has been read and 0 servers exist
     * - [AppState.Ready] with verified active server and loaded settings
     */
    val appState: StateFlow<AppState> = serverStore.snapshotFlow
        .map { snapshot ->
            val servers = snapshot.servers
            val settings = snapshot.settings

            if (servers.isEmpty()) {
                AppState.NoServer(settings = settings)
            } else {
                val active = if (servers.size == 1) {
                    servers.first()
                } else {
                    servers.firstOrNull { it.id == snapshot.activeServerId } ?: servers.first()
                }

                // If active server ID in DataStore was missing or stale, persist the fallback
                if (snapshot.activeServerId != active.id) {
                    viewModelScope.launch {
                        serverStore.setActiveServerId(active.id)
                    }
                }

                AppState.Ready(
                    servers = servers,
                    activeServer = active,
                    settings = settings
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppState.Loading
        )

    val telemetryState: StateFlow<TelemetryState> = repository.telemetryState

    private val _dialogState = MutableStateFlow<Pair<Boolean, ServerConfig?>>(Pair(false, null))
    val dialogState: StateFlow<Pair<Boolean, ServerConfig?>> = _dialogState.asStateFlow()

    private var pollingJob: Job? = null
    private var isAppActive: Boolean = false

    init {
        // Observe appState changes: start polling ONLY when state is Ready and for active server only
        viewModelScope.launch {
            var currentActiveId: String? = null
            var currentInterval = 3

            appState.collect { state ->
                if (state is AppState.Ready) {
                    val active = state.activeServer
                    val interval = state.settings.refreshIntervalSeconds

                    val serverChanged = active.id != currentActiveId
                    val intervalChanged = interval != currentInterval

                    if (serverChanged) {
                        currentActiveId = active.id
                        currentInterval = interval
                        repository.resetForServer(active)
                        if (isAppActive) {
                            restartPolling(active, interval)
                        }
                    } else if (intervalChanged) {
                        currentInterval = interval
                        if (isAppActive) {
                            restartPolling(active, interval)
                        }
                    }
                } else {
                    currentActiveId = null
                    stopPolling()
                }
            }
        }
    }

    /**
     * Called when UI lifecycle enters STARTED.
     */
    fun onLifecycleStarted() {
        isAppActive = true
        val state = appState.value
        if (state is AppState.Ready) {
            restartPolling(state.activeServer, state.settings.refreshIntervalSeconds)
        }
    }

    /**
     * Called when UI lifecycle drops below STARTED (e.g. backgrounded or stopped).
     */
    fun onLifecycleStopped() {
        isAppActive = false
        stopPolling()
    }

    private fun restartPolling(server: ServerConfig, intervalSec: Int) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            val intervalMs = (intervalSec.coerceAtLeast(1) * 1000).toLong()
            while (isActive) {
                repository.poll(server)
                delay(intervalMs)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun refreshNow() {
        val state = appState.value
        if (state is AppState.Ready) {
            viewModelScope.launch {
                repository.poll(state.activeServer)
            }
        }
    }

    fun openAddServerDialog() {
        _dialogState.value = Pair(true, null)
    }

    fun openEditServerDialog(server: ServerConfig) {
        _dialogState.value = Pair(true, server)
    }

    fun closeServerDialog() {
        _dialogState.value = Pair(false, null)
    }

    fun saveServer(server: ServerConfig, makeActive: Boolean = true) {
        viewModelScope.launch {
            serverStore.saveServer(server, makeActive)
            closeServerDialog()
        }
    }

    fun deleteServer(serverId: String) {
        viewModelScope.launch {
            serverStore.deleteServer(serverId)
        }
    }

    fun setActiveServer(serverId: String) {
        viewModelScope.launch {
            serverStore.setActiveServerId(serverId)
        }
    }

    suspend fun testConnection(server: ServerConfig): ConnectionTestResult {
        return repository.testServer(server)
    }

    fun toggleTheme(isCurrentlyDark: Boolean? = null) {
        val next = if (isCurrentlyDark != null) {
            if (isCurrentlyDark) AppThemeMode.LIGHT else AppThemeMode.DARK
        } else {
            when (appState.value.settings.themeMode) {
                AppThemeMode.DARK -> AppThemeMode.LIGHT
                AppThemeMode.LIGHT -> AppThemeMode.DARK
                AppThemeMode.SYSTEM -> AppThemeMode.DARK
            }
        }
        viewModelScope.launch {
            serverStore.setThemeMode(next)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            serverStore.setThemeMode(mode)
        }
    }

    fun setRefreshInterval(seconds: Int) {
        viewModelScope.launch {
            serverStore.setRefreshInterval(seconds)
        }
    }

    fun setReduceAnimations(reduce: Boolean) {
        viewModelScope.launch {
            serverStore.setReduceAnimations(reduce)
        }
    }
}
