package com.example.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Data Transfer Object for `GET /api/status`.
 * Every section is parsed defensively as a [JsonElement] so unexpected shapes
 * in one section never cause the entire request to fail.
 */
@Serializable
data class StatusDto(
    val cpu: JsonElement? = null,
    val memory: JsonElement? = null,
    val storage: JsonElement? = null,
    val network: JsonElement? = null,
    val host: JsonElement? = null
)

@Serializable
data class CpuFrequencyDto(
    val now: Double? = null,
    val min: Double? = null,
    val base: Double? = null,
    val max: Double? = null
)
