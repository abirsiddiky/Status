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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class StatusMainUiState(
    val servers: List<ServerConfig> = emptyList(),
    val activeServer: ServerConfig? = null,
    val showAddServerDialog: Boolean = false,
    val editingServer: ServerConfig? = null
)

class StatusViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val serverStore: ServerStore = ServerStore(application)
    private val repository: StatusRepository = StatusRepository()

    val appSettings: StateFlow<AppSettings> = serverStore.appSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSettings()
        )

    val telemetryState: StateFlow<TelemetryState> = repository.telemetryState

    private val _dialogState = MutableStateFlow<Pair<Boolean, ServerConfig?>>(Pair(false, null))

    val uiState: StateFlow<StatusMainUiState> = combine(
        serverStore.serversFlow,
        serverStore.activeServerIdFlow,
        _dialogState
    ) { servers, activeId, dialogState ->
        val active = servers.find { it.id == activeId } ?: servers.firstOrNull()
        StatusMainUiState(
            servers = servers,
            activeServer = active,
            showAddServerDialog = dialogState.first,
            editingServer = dialogState.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = StatusMainUiState()
    )

    private var pollingJob: Job? = null
    private var isAppActive: Boolean = false

    init {
        // Observe active server changes to restart polling or reset telemetry
        viewModelScope.launch {
            var previousActiveId: String? = null
            uiState.collect { state ->
                val active = state.activeServer
                if (active != null && active.id != previousActiveId) {
                    previousActiveId = active.id
                    repository.resetForServer(active)
                    if (isAppActive) {
                        restartPolling(active, appSettings.value.refreshIntervalSeconds)
                    }
                } else if (active == null) {
                    previousActiveId = null
                    stopPolling()
                }
            }
        }

        // Observe refresh interval changes
        viewModelScope.launch {
            appSettings.collect { settings ->
                val active = uiState.value.activeServer
                if (active != null && isAppActive) {
                    restartPolling(active, settings.refreshIntervalSeconds)
                }
            }
        }
    }

    /**
     * Called when the UI lifecycle enters STARTED.
     */
    fun onLifecycleStarted() {
        isAppActive = true
        val active = uiState.value.activeServer
        if (active != null) {
            restartPolling(active, appSettings.value.refreshIntervalSeconds)
        }
    }

    /**
     * Called when the UI lifecycle drops below STARTED (e.g. backgrounded or stopped).
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
        val active = uiState.value.activeServer ?: return
        viewModelScope.launch {
            repository.poll(active)
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
            repository.resetForServer(server)
            if (isAppActive) {
                restartPolling(server, appSettings.value.refreshIntervalSeconds)
            }
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

    fun toggleTheme() {
        val current = appSettings.value.themeMode
        val next = when (current) {
            AppThemeMode.SYSTEM -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.LIGHT
            AppThemeMode.LIGHT -> AppThemeMode.SYSTEM
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
