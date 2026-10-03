package com.example.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Configuration representing a remote Linux server running the Status monitor agent.
 *
 * @property id Unique identifier for this saved server.
 * @property name User-friendly label (defaults to host if blank).
 * @property scheme Protocol scheme ("http" or "https"). Default is "http".
 * @property host Domain name or IPv4/IPv6 address of the server.
 * @property port Port number the Status agent is listening on (default 9090).
 * @property path Optional URL subpath prefix (e.g. "status" or empty).
 */
@Serializable
data class ServerConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val scheme: String = "http",
    val host: String = "",
    val port: Int = 9090,
    val path: String = ""
) {
    /**
     * Resolves the full base URL formatted as:
     * `{scheme}://{host}:{port}[/{path}]`
     */
    fun getBaseUrl(): String {
        val cleanHost = host.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        val cleanPath = path.trim().trim('/')
        val pathSuffix = if (cleanPath.isNotEmpty()) "/$cleanPath" else ""
        return "$scheme://$cleanHost:$port$pathSuffix"
    }

    /**
     * Returns the human-readable display name, falling back to host:port if name is blank.
     */
    fun getDisplayName(): String {
        return if (name.isNotBlank()) name.trim() else host.ifBlank { "Server" }
    }
}
