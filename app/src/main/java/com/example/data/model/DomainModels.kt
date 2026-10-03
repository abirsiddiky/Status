package com.example.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.util.Locale

/**
 * Immutable domain models used by the presentation layer.
 * All units are normalized (bytes, %, seconds, Mbit/s).
 */

@Immutable
data class ServerStatus(
    val timestamp: Long = System.currentTimeMillis(),
    val cpu: CpuStatus = CpuStatus(),
    val memory: MemoryStatus = MemoryStatus(),
    val storage: StorageStatus = StorageStatus(),
    val network: NetworkStatus = NetworkStatus(),
    val host: HostStatus = HostStatus()
)

@Immutable
data class CpuStatus(
    val loadPercent: Float = 0f,
    val model: String = "N/A",
    val coreCount: Int? = null,
    val cacheInfo: String? = null,
    val temperatures: List<Double> = emptyList(),
    val averageTemp: Double? = null,
    val hasTemperature: Boolean = false,
    val frequencies: Map<String, CpuFrequencyDto> = emptyMap(),
    val hasFrequencies: Boolean = false
)

@Immutable
data class MemoryStatus(
    val totalBytes: Long = 0L,
    val availableBytes: Long = 0L,
    val inUseBytes: Long = 0L,
    val cachedBytes: Long = 0L,
    val usagePercent: Float = 0f,
    val swapTotalBytes: Long = 0L,
    val swapAvailableBytes: Long = 0L,
    val swapInUseBytes: Long = 0L,
    val hasSwap: Boolean = false,
    val swapUsagePercent: Float = 0f,
    val processCount: Int? = null
)

@Immutable
data class VolumeInfo(
    val name: String,
    val totalBytes: Double,
    val availableBytes: Double,
    val inUseBytes: Double,
    val usagePercent: Float
)

@Immutable
data class StorageStatus(
    val volumes: List<VolumeInfo> = emptyList(),
    val primaryVolume: VolumeInfo? = null,
    val hasVolumes: Boolean = false
)

@Immutable
data class NetworkStatus(
    val interfaceName: String = "N/A",
    val linkSpeedMbit: Double? = null,
    val rxRateBps: Double = 0.0,
    val txRateBps: Double = 0.0,
    val rxTotalBytes: Long? = null,
    val txTotalBytes: Long? = null
)

@Immutable
data class HostStatus(
    val hostname: String = "N/A",
    val os: String = "N/A",
    val uptimeSeconds: Double = 0.0,
    val uptimeFormatted: String = "N/A",
    val loadAvg: List<Double> = emptyList(),
    val appMemoryFormatted: String = "N/A"
)

/**
 * In-memory rolling history for Canvas charts (up to 60 points).
 */
@Immutable
data class ChartHistory(
    val cpuLoad: List<Float> = emptyList(),
    val cpuTemp: List<Float> = emptyList(),
    val memoryUsage: List<Float> = emptyList(),
    val storageInUse: List<Float> = emptyList(),
    val networkRx: List<Float> = emptyList(),
    val networkTx: List<Float> = emptyList()
) {
    fun addSample(
        cpuLoadVal: Float,
        cpuTempVal: Float?,
        memPercentVal: Float,
        storageInUseVal: Float?,
        rxRateVal: Float,
        txRateVal: Float,
        maxPoints: Int = 60
    ): ChartHistory {
        fun <T> List<T>.push(item: T): List<T> {
            val list = if (size >= maxPoints) drop(size - maxPoints + 1) else this
            return list + item
        }

        return ChartHistory(
            cpuLoad = cpuLoad.push(cpuLoadVal),
            cpuTemp = if (cpuTempVal != null) cpuTemp.push(cpuTempVal) else cpuTemp,
            memoryUsage = memoryUsage.push(memPercentVal),
            storageInUse = if (storageInUseVal != null) storageInUse.push(storageInUseVal) else storageInUse,
            networkRx = networkRx.push(rxRateVal),
            networkTx = networkTx.push(txRateVal)
        )
    }
}

/**
 * Defensive parsing and transformation logic from DTO to Domain.
 */
object StatusMapper {

    fun map(
        dto: StatusDto,
        previousRxBytes: Long?,
        previousTxBytes: Long?,
        previousTimestamp: Long?,
        currentTimestamp: Long
    ): Pair<ServerStatus, Pair<Double, Double>> {
        // --- 1. CPU PARSING ---
        val rawCpuLoad = dto.cpu?.utilisation ?: 0.0
        val clampedCpuLoad = rawCpuLoad.toFloat().coerceIn(0f, 100f)
        val cpuModel = dto.cpu?.model?.takeIf { it.isNotBlank() } ?: "N/A"
        val coreCount = dto.cpu?.cores ?: dto.cpu?.count

        // Defensive temperatures extraction:
        // May be list of numbers, objects, or empty
        val temps = mutableListOf<Double>()
        dto.cpu?.temperatures?.forEach { elem ->
            when (elem) {
                is JsonPrimitive -> {
                    elem.doubleOrNull?.let { temps.add(it) }
                }
                is JsonObject -> {
                    val num = elem["temp"]?.let { (it as? JsonPrimitive)?.doubleOrNull }
                        ?: elem["value"]?.let { (it as? JsonPrimitive)?.doubleOrNull }
                        ?: elem.values.firstOrNull { it is JsonPrimitive && it.doubleOrNull != null }
                            ?.let { (it as JsonPrimitive).doubleOrNull }
                    if (num != null) temps.add(num)
                }
                else -> {}
            }
        }
        val avgTemp = if (temps.isNotEmpty()) temps.average() else null

        // Cache formatting
        val cacheStr = when (val c = dto.cpu?.cache) {
            is JsonPrimitive -> c.content.takeIf { it.isNotBlank() && it != "null" }
            is JsonObject -> c.toString()
            else -> null
        }

        // Frequencies filtering: only keep if at least one core has a non-null frequency
        val freqMap = dto.cpu?.frequencies?.mapNotNull { (core, freq) ->
            if (freq != null && (freq.now != null || freq.max != null)) core to freq else null
        }?.toMap() ?: emptyMap()

        val cpuStatus = CpuStatus(
            loadPercent = clampedCpuLoad,
            model = cpuModel,
            coreCount = coreCount,
            cacheInfo = cacheStr,
            temperatures = temps,
            averageTemp = avgTemp,
            hasTemperature = temps.isNotEmpty(),
            frequencies = freqMap,
            hasFrequencies = freqMap.isNotEmpty()
        )

        // --- 2. MEMORY PARSING ---
        // Memory values are in kB
        val memTotalKb = dto.memory?.total ?: 0L
        val memAvailKb = dto.memory?.available ?: 0L
        val memCachedKb = dto.memory?.cached ?: 0L
        val memInUseKb = (memTotalKb - memAvailKb).coerceAtLeast(0L)
        val memUsagePercent = if (memTotalKb > 0L) {
            ((memInUseKb.toDouble() / memTotalKb.toDouble()) * 100.0).toFloat().coerceIn(0f, 100f)
        } else 0f

        val swapTotalKb = dto.memory?.swapTotal ?: 0L
        val swapAvailKb = dto.memory?.swapAvailable ?: 0L
        val swapInUseKb = (swapTotalKb - swapAvailKb).coerceAtLeast(0L)
        val hasSwap = swapTotalKb > 0L
        val swapUsagePercent = if (hasSwap) {
            ((swapInUseKb.toDouble() / swapTotalKb.toDouble()) * 100.0).toFloat().coerceIn(0f, 100f)
        } else 0f

        val memoryStatus = MemoryStatus(
            totalBytes = memTotalKb * 1024L,
            availableBytes = memAvailKb * 1024L,
            inUseBytes = memInUseKb * 1024L,
            cachedBytes = memCachedKb * 1024L,
            usagePercent = memUsagePercent,
            swapTotalBytes = swapTotalKb * 1024L,
            swapAvailableBytes = swapAvailKb * 1024L,
            swapInUseBytes = swapInUseKb * 1024L,
            hasSwap = hasSwap,
            swapUsagePercent = swapUsagePercent,
            processCount = dto.memory?.processes
        )

        // --- 3. STORAGE PARSING ---
        // Storage is keyed by volume name; total and available are in BYTES
        val volumeList = mutableListOf<VolumeInfo>()
        dto.storage?.forEach { (volumeName, volDto) ->
            val totalBytes = volDto.total ?: 0.0
            val availBytes = volDto.available ?: 0.0
            val inUseBytes = (totalBytes - availBytes).coerceAtLeast(0.0)
            val usagePct = if (totalBytes > 0.0) {
                ((inUseBytes / totalBytes) * 100.0).toFloat().coerceIn(0f, 100f)
            } else 0f
            volumeList.add(
                VolumeInfo(
                    name = volumeName,
                    totalBytes = totalBytes,
                    availableBytes = availBytes,
                    inUseBytes = inUseBytes,
                    usagePercent = usagePct
                )
            )
        }
        val storageStatus = StorageStatus(
            volumes = volumeList,
            primaryVolume = volumeList.firstOrNull(),
            hasVolumes = volumeList.isNotEmpty()
        )

        // --- 4. NETWORK PARSING & RATE CALCULATION ---
        val currentRx = dto.network?.rx
        val currentTx = dto.network?.tx
        var rxRateBps = 0.0
        var txRateBps = 0.0

        if (previousRxBytes != null && previousTxBytes != null && previousTimestamp != null &&
            currentRx != null && currentTx != null
        ) {
            val elapsedSec = (currentTimestamp - previousTimestamp).toDouble() / 1000.0
            if (elapsedSec > 0.3) {
                // If counter decreased, treat as reset and skip rate calculation
                if (currentRx >= previousRxBytes) {
                    rxRateBps = (currentRx - previousRxBytes).toDouble() / elapsedSec
                }
                if (currentTx >= previousTxBytes) {
                    txRateBps = (currentTx - previousTxBytes).toDouble() / elapsedSec
                }
            }
        }

        val networkStatus = NetworkStatus(
            interfaceName = dto.network?.interfaceName?.takeIf { it.isNotBlank() } ?: "N/A",
            linkSpeedMbit = dto.network?.speed,
            rxRateBps = rxRateBps,
            txRateBps = txRateBps,
            rxTotalBytes = currentRx,
            txTotalBytes = currentTx
        )

        // --- 5. HOST PARSING ---
        val uptimeSec = dto.host?.uptime ?: 0.0
        val uptimeFormatted = formatUptime(uptimeSec)
        val loadavg = dto.host?.loadavg ?: emptyList()
        val appMem = when (val am = dto.host?.appMemory) {
            is JsonPrimitive -> {
                val num = am.doubleOrNull
                if (num != null) formatBytes(num * 1024.0) else am.content
            }
            else -> "N/A"
        }

        val hostStatus = HostStatus(
            hostname = dto.host?.hostname?.takeIf { it.isNotBlank() } ?: "N/A",
            os = dto.host?.os?.takeIf { it.isNotBlank() } ?: "N/A",
            uptimeSeconds = uptimeSec,
            uptimeFormatted = uptimeFormatted,
            loadAvg = loadavg,
            appMemoryFormatted = appMem
        )

        val serverStatus = ServerStatus(
            timestamp = currentTimestamp,
            cpu = cpuStatus,
            memory = memoryStatus,
            storage = storageStatus,
            network = networkStatus,
            host = hostStatus
        )

        return Pair(serverStatus, Pair(rxRateBps, txRateBps))
    }

    private fun formatUptime(uptimeSeconds: Double): String {
        if (uptimeSeconds <= 0.0) return "N/A"
        val totalSec = uptimeSeconds.toLong()
        val days = totalSec / 86400
        val hours = (totalSec % 86400) / 3600
        val minutes = (totalSec % 3600) / 60

        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m ${(totalSec % 60)}s"
        }
    }

    fun formatBytes(bytes: Double): String {
        if (bytes <= 0.0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        var value = bytes
        var unitIndex = 0
        while (value >= 1024.0 && unitIndex < units.size - 1) {
            value /= 1024.0
            unitIndex++
        }
        return if (value >= 100 || unitIndex == 0) {
            String.format(Locale.US, "%.0f %s", value, units[unitIndex])
        } else {
            String.format(Locale.US, "%.1f %s", value, units[unitIndex])
        }
    }
}
