package com.example.data.model

import androidx.compose.runtime.Immutable
import com.example.util.FormatUtils
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import java.util.Locale

/**
 * Single temperature reading from a sensor or core.
 */
@Immutable
data class TempReading(
    val label: String,
    val current: Double,
    val limit: Double? = null
)

/**
 * Helper conversions on [JsonElement] for resilient parsing of numbers
 * arriving as Int, Long, Double or numeric String.
 */
fun JsonElement?.asDoubleOrNull(): Double? {
    if (this == null || this is JsonNull) return null
    return when (this) {
        is JsonPrimitive -> this.doubleOrNull ?: this.content.toDoubleOrNull()
        else -> null
    }
}

fun JsonElement?.asLongOrNull(): Long? {
    if (this == null || this is JsonNull) return null
    return when (this) {
        is JsonPrimitive -> this.longOrNull ?: this.content.toLongOrNull() ?: this.doubleOrNull?.toLong()
        else -> null
    }
}

fun JsonElement?.asIntOrNull(): Int? {
    if (this == null || this is JsonNull) return null
    return when (this) {
        is JsonPrimitive -> this.intOrNull ?: this.content.toIntOrNull() ?: this.doubleOrNull?.toInt()
        else -> null
    }
}

fun JsonElement?.asStringOrNull(): String? {
    if (this == null || this is JsonNull) return null
    return when (this) {
        is JsonPrimitive -> {
            if (this.isString) this.content
            else if (this.content != "null") this.content
            else null
        }
        else -> null
    }
}

/**
 * Tolerantly parses "temperatures" in any shape returned by Status servers:
 * a) empty list: []
 * b) object keyed by sensor/core name with array [current, limit]: {"Core 0": [43.0, 100.0]}
 * c) list of numbers: [43.0, 41.0]
 * d) object with plain numbers: {"Package": 43.5}
 * e) list of [current, limit] pairs
 *
 * For arrays/objects of numbers, takes the FIRST numeric value as current temperature
 * and the second (if any) as the limit; skips values that are null, non-numeric,
 * or outside -50..200; keeps sensor name as the label (or "Core N" for list items).
 * Returns an empty list for anything unexpected instead of throwing.
 */
fun parseTemperatures(el: JsonElement?): List<TempReading> {
    if (el == null || el is JsonNull) return emptyList()

    fun extractValidNumbers(element: JsonElement): List<Double> {
        return when (element) {
            is JsonPrimitive -> {
                val d = element.asDoubleOrNull()
                if (d != null && d in -50.0..200.0) listOf(d) else emptyList()
            }
            is JsonArray -> {
                element.mapNotNull { it.asDoubleOrNull() }.filter { it in -50.0..200.0 }
            }
            is JsonObject -> {
                element.values.mapNotNull { it.asDoubleOrNull() }.filter { it in -50.0..200.0 }
            }
            else -> emptyList()
        }
    }

    return try {
        when (el) {
            is JsonArray -> {
                val readings = mutableListOf<TempReading>()
                el.forEachIndexed { index, item ->
                    val nums = extractValidNumbers(item)
                    if (nums.isNotEmpty()) {
                        val current = nums[0]
                        val limit = nums.getOrNull(1)
                        readings.add(TempReading(label = "Core $index", current = current, limit = limit))
                    }
                }
                readings
            }
            is JsonObject -> {
                val readings = mutableListOf<TempReading>()
                for ((sensorName, value) in el) {
                    val nums = extractValidNumbers(value)
                    if (nums.isNotEmpty()) {
                        val current = nums[0]
                        val limit = nums.getOrNull(1)
                        readings.add(TempReading(label = sensorName, current = current, limit = limit))
                    }
                }
                readings
            }
            else -> emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * Immutable domain models used by the presentation layer.
 */
@Immutable
data class ServerStatus(
    val timestamp: Long = System.currentTimeMillis(),
    val cpu: CpuStatus = CpuStatus(),
    val memory: MemoryStatus = MemoryStatus(),
    val storage: StorageStatus = StorageStatus(),
    val network: NetworkStatus = NetworkStatus(),
    val host: HostStatus = HostStatus(),
    val partialDataSections: List<String> = emptyList()
)

@Immutable
data class CpuStatus(
    val loadPercent: Float = 0f,
    val model: String = "N/A",
    val coreCount: Int? = null,
    val cacheInfo: String = "N/A",
    val tempReadings: List<TempReading> = emptyList(),
    val averageTemp: Double? = null,
    val maxTemp: Double? = null,
    val hasTemperature: Boolean = false,
    val frequencies: Map<String, CpuFrequencyDto> = emptyMap(),
    val hasFrequencies: Boolean = false
) {
    val temperatures: List<Double> get() = tempReadings.map { it.current }
}

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
    val processCount: Int? = null,
    val totalKb: Long = 0L,
    val availableKb: Long = 0L
) {
    val totalGbFormatted: String get() = FormatUtils.formatMemoryGb(totalBytes)
}

@Immutable
data class VolumeInfo(
    val name: String,
    val totalBytes: Double,
    val availableBytes: Double,
    val inUseBytes: Double,
    val usagePercent: Float,
    val iconHint: String = "storage"
)

@Immutable
data class StorageStatus(
    val volumes: List<VolumeInfo> = emptyList(),
    val totalBytes: Double = 0.0,
    val availableBytes: Double = 0.0,
    val inUseBytes: Double = 0.0,
    val usagePercent: Float = 0f,
    val hasVolumes: Boolean = false
) {
    val volumeCountSummary: String
        get() = "${volumes.size} ${if (volumes.size == 1) "volume" else "volumes"}"

    val primaryVolume: VolumeInfo?
        get() = volumes.firstOrNull()
}

@Immutable
data class NetworkStatus(
    val interfaceName: String = "N/A",
    val linkSpeedMbit: Double? = null,
    val rxRateBps: Double = 0.0,
    val txRateBps: Double = 0.0,
    val rxTotalBytes: Long? = null,
    val txTotalBytes: Long? = null
) {
    val linkSpeedFormatted: String
        get() = if (linkSpeedMbit == null || linkSpeedMbit <= 0.0) "Unknown" else "${linkSpeedMbit.toInt()} Mbit/s"
}

@Immutable
data class HostStatus(
    val hostname: String = "N/A",
    val os: String = "N/A",
    val uptimeSeconds: Double = 0.0,
    val uptimeFormatted: String = "N/A",
    val loadAvg: List<Double> = emptyList(),
    val appMemoryKb: Double? = null,
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
 * Each section is parsed in its own runCatching block so an issue in one
 * section never fails the entire response.
 */
object StatusMapper {

    fun parseTemperatures(el: JsonElement?): List<TempReading> = com.example.data.model.parseTemperatures(el)

    fun map(
        dto: StatusDto,
        previousRxBytes: Long?,
        previousTxBytes: Long?,
        previousTimestamp: Long?,
        currentTimestamp: Long
    ): Pair<ServerStatus, Pair<Double, Double>> {
        val failedSections = mutableListOf<String>()

        // --- 1. CPU PARSING ---
        val cpuStatus = runCatching {
            val cpuEl = dto.cpu
            if (cpuEl == null || cpuEl !is JsonObject) {
                if (dto.cpu != null && dto.cpu !is JsonNull) failedSections.add("CPU")
                CpuStatus()
            } else {
                parseCpu(cpuEl)
            }
        }.getOrElse {
            failedSections.add("CPU")
            CpuStatus()
        }

        // --- 2. MEMORY PARSING ---
        val memoryStatus = runCatching {
            val memEl = dto.memory
            if (memEl == null || memEl !is JsonObject) {
                if (dto.memory != null && dto.memory !is JsonNull) failedSections.add("Memory")
                MemoryStatus()
            } else {
                parseMemory(memEl)
            }
        }.getOrElse {
            failedSections.add("Memory")
            MemoryStatus()
        }

        // --- 3. STORAGE PARSING ---
        val storageStatus = runCatching {
            val storageEl = dto.storage
            if (storageEl == null || storageEl !is JsonObject) {
                if (dto.storage != null && dto.storage !is JsonNull) failedSections.add("Storage")
                StorageStatus()
            } else {
                parseStorage(storageEl)
            }
        }.getOrElse {
            failedSections.add("Storage")
            StorageStatus()
        }

        // --- 4. NETWORK PARSING & RATE CALCULATION ---
        var rxRateBps = 0.0
        var txRateBps = 0.0
        val networkStatus = runCatching {
            val netEl = dto.network
            if (netEl == null || netEl !is JsonObject) {
                if (dto.network != null && dto.network !is JsonNull) failedSections.add("Network")
                NetworkStatus()
            } else {
                val (netStatus, rates) = parseNetwork(
                    obj = netEl,
                    previousRxBytes = previousRxBytes,
                    previousTxBytes = previousTxBytes,
                    previousTimestamp = previousTimestamp,
                    currentTimestamp = currentTimestamp
                )
                rxRateBps = rates.first
                txRateBps = rates.second
                netStatus
            }
        }.getOrElse {
            failedSections.add("Network")
            NetworkStatus()
        }

        // --- 5. HOST PARSING ---
        val hostStatus = runCatching {
            val hostEl = dto.host
            if (hostEl == null || hostEl !is JsonObject) {
                if (dto.host != null && dto.host !is JsonNull) failedSections.add("Host")
                HostStatus()
            } else {
                parseHost(hostEl)
            }
        }.getOrElse {
            failedSections.add("Host")
            HostStatus()
        }

        val serverStatus = ServerStatus(
            timestamp = currentTimestamp,
            cpu = cpuStatus,
            memory = memoryStatus,
            storage = storageStatus,
            network = networkStatus,
            host = hostStatus,
            partialDataSections = failedSections
        )

        return Pair(serverStatus, Pair(rxRateBps, txRateBps))
    }

    private fun parseCpu(obj: JsonObject): CpuStatus {
        val rawLoad = obj["utilisation"].asDoubleOrNull() ?: 0.0
        // cpu.utilisation is a FRACTION from 0.0 to 1.0 (0.0251 means 2.5% load).
        // Multiply by 100 for percentage and clamp to 0..100.
        val loadPercent = if (rawLoad in 0.0..1.0) {
            (rawLoad * 100.0).toFloat().coerceIn(0f, 100f)
        } else {
            rawLoad.toFloat().coerceIn(0f, 100f)
        }

        val model = obj["model"].asStringOrNull()?.takeIf { it.isNotBlank() } ?: "N/A"
        val coreCount = obj["cores"].asIntOrNull() ?: obj["count"].asIntOrNull()

        val tempReadings = parseTemperatures(obj["temperatures"])
        val avgTemp = if (tempReadings.isNotEmpty()) tempReadings.map { it.current }.average() else null
        val maxTemp = if (tempReadings.isNotEmpty()) tempReadings.maxOf { it.current } else null

        // cpu.cache is in kB (6144 = 6 MB, show as "6 MB"); null means "N/A"
        val cacheRaw = obj["cache"]
        val cacheStr = when {
            cacheRaw == null || cacheRaw is JsonNull -> "N/A"
            cacheRaw is JsonPrimitive -> {
                val kb = cacheRaw.asDoubleOrNull()
                if (kb != null) FormatUtils.formatCache(kb)
                else cacheRaw.asStringOrNull()?.takeIf { it.isNotBlank() && it != "null" } ?: "N/A"
            }
            else -> cacheRaw.toString()
        }

        // cpu.frequencies: values are MHz integers. Skip cores whose "now" is null.
        val freqMap = mutableMapOf<String, CpuFrequencyDto>()
        val freqEl = obj["frequencies"]
        if (freqEl is JsonObject) {
            for ((core, fVal) in freqEl) {
                if (fVal is JsonObject) {
                    val now = fVal["now"].asDoubleOrNull()
                    if (now == null) continue // Skip cores whose "now" is null!
                    val min = fVal["min"].asDoubleOrNull()
                    val base = fVal["base"].asDoubleOrNull()
                    val max = fVal["max"].asDoubleOrNull()
                    freqMap[core] = CpuFrequencyDto(now = now, min = min, base = base, max = max)
                }
            }
        }

        return CpuStatus(
            loadPercent = loadPercent,
            model = model,
            coreCount = coreCount,
            cacheInfo = cacheStr,
            tempReadings = tempReadings,
            averageTemp = avgTemp,
            maxTemp = maxTemp,
            hasTemperature = tempReadings.isNotEmpty(),
            frequencies = freqMap,
            hasFrequencies = freqMap.isNotEmpty()
        )
    }

    private fun parseMemory(obj: JsonObject): MemoryStatus {
        // Memory values are in kB (KiB): divide by 1024*1024 for GB.
        val totalKb = obj["total"].asLongOrNull() ?: 0L
        val availKb = obj["available"].asLongOrNull() ?: 0L
        val cachedKb = obj["cached"].asLongOrNull() ?: 0L
        val inUseKb = (totalKb - availKb).coerceAtLeast(0L)
        val usagePercent = if (totalKb > 0L) {
            ((inUseKb.toDouble() / totalKb.toDouble()) * 100.0).toFloat().coerceIn(0f, 100f)
        } else 0f

        val swapTotalKb = obj["swap_total"].asLongOrNull() ?: 0L
        val swapAvailKb = obj["swap_available"].asLongOrNull() ?: 0L
        val swapInUseKb = (swapTotalKb - swapAvailKb).coerceAtLeast(0L)
        val hasSwap = swapTotalKb > 0L
        val swapUsagePercent = if (hasSwap) {
            ((swapInUseKb.toDouble() / swapTotalKb.toDouble()) * 100.0).toFloat().coerceIn(0f, 100f)
        } else 0f

        val processCount = obj["processes"].asIntOrNull()

        return MemoryStatus(
            totalBytes = totalKb * 1024L,
            availableBytes = availKb * 1024L,
            inUseBytes = inUseKb * 1024L,
            cachedBytes = cachedKb * 1024L,
            usagePercent = usagePercent,
            swapTotalBytes = swapTotalKb * 1024L,
            swapAvailableBytes = swapAvailKb * 1024L,
            swapInUseBytes = swapInUseKb * 1024L,
            hasSwap = hasSwap,
            swapUsagePercent = swapUsagePercent,
            processCount = processCount,
            totalKb = totalKb,
            availableKb = availKb
        )
    }

    private fun parseStorage(obj: JsonObject): StorageStatus {
        // JSON object order must be preserved (JsonObject preserves iteration order)
        val volumeList = mutableListOf<VolumeInfo>()
        for ((volumeName, volEl) in obj) {
            if (volEl is JsonObject) {
                val totalBytes = volEl["total"].asDoubleOrNull() ?: 0.0
                val availBytes = volEl["available"].asDoubleOrNull() ?: 0.0
                val inUseBytes = (totalBytes - availBytes).coerceAtLeast(0.0)
                val usagePct = if (totalBytes > 0.0) {
                    ((inUseBytes / totalBytes) * 100.0).toFloat().coerceIn(0f, 100f)
                } else 0f
                val iconHint = volEl["icon"].asStringOrNull() ?: "storage"
                volumeList.add(
                    VolumeInfo(
                        name = volumeName,
                        totalBytes = totalBytes,
                        availableBytes = availBytes,
                        inUseBytes = inUseBytes,
                        usagePercent = usagePct,
                        iconHint = iconHint
                    )
                )
            }
        }

        val totalAcrossVolumes = volumeList.sumOf { it.totalBytes }
        val availAcrossVolumes = volumeList.sumOf { it.availableBytes }
        val inUseAcrossVolumes = (totalAcrossVolumes - availAcrossVolumes).coerceAtLeast(0.0)
        val totalUsagePct = if (totalAcrossVolumes > 0.0) {
            ((inUseAcrossVolumes / totalAcrossVolumes) * 100.0).toFloat().coerceIn(0f, 100f)
        } else 0f

        return StorageStatus(
            volumes = volumeList,
            totalBytes = totalAcrossVolumes,
            availableBytes = availAcrossVolumes,
            inUseBytes = inUseAcrossVolumes,
            usagePercent = totalUsagePct,
            hasVolumes = volumeList.isNotEmpty()
        )
    }

    private fun parseNetwork(
        obj: JsonObject,
        previousRxBytes: Long?,
        previousTxBytes: Long?,
        previousTimestamp: Long?,
        currentTimestamp: Long
    ): Pair<NetworkStatus, Pair<Double, Double>> {
        val iface = obj["interface"].asStringOrNull()?.takeIf { it.isNotBlank() } ?: "N/A"
        val speed = obj["speed"].asDoubleOrNull()
        val rx = obj["rx"].asLongOrNull()
        val tx = obj["tx"].asLongOrNull()

        var rxRateBps = 0.0
        var txRateBps = 0.0

        if (previousRxBytes != null && previousTxBytes != null && previousTimestamp != null &&
            rx != null && tx != null
        ) {
            val elapsedSec = (currentTimestamp - previousTimestamp).toDouble() / 1000.0
            if (elapsedSec > 0.3) {
                if (rx >= previousRxBytes) {
                    rxRateBps = (rx - previousRxBytes).toDouble() / elapsedSec
                }
                if (tx >= previousTxBytes) {
                    txRateBps = (tx - previousTxBytes).toDouble() / elapsedSec
                }
            }
        }

        val networkStatus = NetworkStatus(
            interfaceName = iface,
            linkSpeedMbit = speed,
            rxRateBps = rxRateBps,
            txRateBps = txRateBps,
            rxTotalBytes = rx,
            txTotalBytes = tx
        )

        return Pair(networkStatus, Pair(rxRateBps, txRateBps))
    }

    private fun parseHost(obj: JsonObject): HostStatus {
        val uptimeSec = obj["uptime"].asDoubleOrNull() ?: 0.0
        val uptimeFormatted = formatUptime(uptimeSec)
        val hostname = obj["hostname"].asStringOrNull()?.takeIf { it.isNotBlank() } ?: "N/A"
        val os = obj["os"].asStringOrNull()?.takeIf { it.isNotBlank() } ?: "N/A"

        val loadAvgList = when (val la = obj["loadavg"]) {
            is JsonArray -> la.mapNotNull { it.asDoubleOrNull() }
            else -> emptyList()
        }

        // host.app_memory is a STRING in kB ("37820"): parse with asDoubleOrNull
        val appMemKb = obj["app_memory"].asDoubleOrNull()
        val appMemFormatted = if (appMemKb != null) FormatUtils.formatBytes(appMemKb * 1024.0) else "N/A"

        return HostStatus(
            hostname = hostname,
            os = os,
            uptimeSeconds = uptimeSec,
            uptimeFormatted = uptimeFormatted,
            loadAvg = loadAvgList,
            appMemoryKb = appMemKb,
            appMemoryFormatted = appMemFormatted
        )
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
}
