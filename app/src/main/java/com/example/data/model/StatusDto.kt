package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Data Transfer Objects (DTO) corresponding to the `GET {baseUrl}/api/status` endpoint
 * of the Status Linux server monitor (https://github.com/dani3l0/Status).
 *
 * CRITICAL DEFENSIVE PARSING RULES:
 * 1. Every field in every data class is nullable with a default value.
 * 2. `temperatures` is parsed defensively as `List<JsonElement>?` because servers return
 *    empty arrays, arrays of numbers, or objects with sensor names.
 * 3. `frequencies` map values can contain nulls or missing fields.
 * 4. `storage` map keys represent dynamic mount points / volume names (e.g. "OS", "/home").
 *    Storage total may be a floating point number representing raw bytes.
 * 5. `memory` values are in kilobyte (kB) units.
 * 6. `network.rx` and `network.tx` are cumulative 64-bit byte counters.
 * 7. `host.uptime` is in seconds.
 * 8. `host.app_memory` can be either a numeric string or a number.
 */

@Serializable
data class StatusDto(
    val cpu: CpuDto? = null,
    val memory: MemoryDto? = null,
    val storage: Map<String, StorageVolumeDto>? = null,
    val network: NetworkDto? = null,
    val host: HostDto? = null
)

@Serializable
data class CpuDto(
    val model: String? = null,
    val utilisation: Double? = null,
    val temperatures: List<JsonElement>? = null,
    val frequencies: Map<String, CpuFrequencyDto?>? = null,
    val count: Int? = null,
    val cache: JsonElement? = null,
    val cores: Int? = null
)

@Serializable
data class CpuFrequencyDto(
    val now: Double? = null,
    val min: Double? = null,
    val base: Double? = null,
    val max: Double? = null
)

@Serializable
data class MemoryDto(
    val total: Long? = null,          // in kB
    val available: Long? = null,      // in kB
    val cached: Long? = null,         // in kB
    @SerialName("swap_total")
    val swapTotal: Long? = null,      // in kB
    @SerialName("swap_available")
    val swapAvailable: Long? = null,  // in kB
    val processes: Int? = null
)

@Serializable
data class StorageVolumeDto(
    val icon: String? = null,
    val total: Double? = null,        // in bytes (may be float)
    val available: Double? = null     // in bytes
)

@Serializable
data class NetworkDto(
    @SerialName("interface")
    val interfaceName: String? = null,
    val speed: Double? = null,        // link speed in Mbit/s
    val rx: Long? = null,             // cumulative received bytes
    val tx: Long? = null              // cumulative transmitted bytes
)

@Serializable
data class HostDto(
    val uptime: Double? = null,       // in seconds
    val os: String? = null,
    val hostname: String? = null,
    @SerialName("app_memory")
    val appMemory: JsonElement? = null,
    val loadavg: List<Double>? = null  // [1m, 5m, 15m]
)
