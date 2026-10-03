package com.example.ui

import com.example.data.AppSettings
import com.example.data.model.ServerConfig

/**
 * Explicit startup and runtime states for the Status app:
 * - [Loading]: Initial state while the first DataStore read is in progress. The system splash screen is kept visible.
 * - [NoServer]: Emitted ONLY after DataStore has truly been read and the server list is genuinely empty.
 * - [Ready]: Emitted when one or more servers are configured, with the verified active server and loaded settings.
 */
sealed interface AppState {
    val settings: AppSettings get() = AppSettings()

    data object Loading : AppState

    data class NoServer(
        override val settings: AppSettings = AppSettings()
    ) : AppState

    data class Ready(
        val servers: List<ServerConfig>,
        val activeServer: ServerConfig,
        override val settings: AppSettings = AppSettings()
    ) : AppState
}
