package com.example.data

import com.example.data.model.ChartHistory
import com.example.data.model.ServerConfig
import com.example.data.model.ServerStatus
import com.example.data.model.StatusMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TelemetryState(
    val isOnline: Boolean = false,
    val isLoading: Boolean = false,
    val lastSuccessfulUpdate: Long = 0L,
    val status: ServerStatus? = null,
    val history: ChartHistory = ChartHistory(),
    val errorMessage: String? = null
)

class StatusRepository(
    private val api: StatusApi = StatusApi()
) {
    private val _telemetryState = MutableStateFlow(TelemetryState())
    val telemetryState: StateFlow<TelemetryState> = _telemetryState.asStateFlow()

    // Tracking state for network rate calculation
    private var lastRxBytes: Long? = null
    private var lastTxBytes: Long? = null
    private var lastRateTimestamp: Long? = null
    private var currentServerId: String? = null

    /**
     * Resets telemetry and history when switching to a different active server.
     */
    fun resetForServer(server: ServerConfig) {
        if (currentServerId != server.id) {
            currentServerId = server.id
            lastRxBytes = null
            lastTxBytes = null
            lastRateTimestamp = null
            _telemetryState.value = TelemetryState(isLoading = true)
        }
    }

    /**
     * Polls the active server once.
     * On network/server failure: preserves the last good status data and sets isOnline = false.
     */
    suspend fun poll(server: ServerConfig) {
        val now = System.currentTimeMillis()
        val result = api.fetchStatus(server)

        result.onSuccess { dto ->
            val (domainStatus, rates) = StatusMapper.map(
                dto = dto,
                previousRxBytes = lastRxBytes,
                previousTxBytes = lastTxBytes,
                previousTimestamp = lastRateTimestamp,
                currentTimestamp = now
            )

            // Update rate tracking
            lastRxBytes = dto.network?.rx
            lastTxBytes = dto.network?.tx
            lastRateTimestamp = now

            val (rxRateBps, txRateBps) = rates
            // Convert to Kbit/s for chart readability
            val rxRateKbit = (rxRateBps * 8.0 / 1000.0).toFloat()
            val txRateKbit = (txRateBps * 8.0 / 1000.0).toFloat()
            val storageInUseBytes = domainStatus.storage.primaryVolume?.inUseBytes?.toFloat()

            _telemetryState.update { current ->
                val updatedHistory = current.history.addSample(
                    cpuLoadVal = domainStatus.cpu.loadPercent,
                    cpuTempVal = domainStatus.cpu.averageTemp?.toFloat(),
                    memPercentVal = domainStatus.memory.usagePercent,
                    storageInUseVal = storageInUseBytes,
                    rxRateVal = rxRateKbit,
                    txRateVal = txRateKbit
                )

                current.copy(
                    isOnline = true,
                    isLoading = false,
                    lastSuccessfulUpdate = now,
                    status = domainStatus,
                    history = updatedHistory,
                    errorMessage = null
                )
            }
        }.onFailure { throwable ->
            val message = throwable.message ?: "Unknown server error"
            _telemetryState.update { current ->
                current.copy(
                    isOnline = false,
                    isLoading = false,
                    // Keep last known good status!
                    errorMessage = message
                )
            }
        }
    }

    suspend fun testServer(server: ServerConfig): ConnectionTestResult {
        return api.testConnection(server)
    }
}
