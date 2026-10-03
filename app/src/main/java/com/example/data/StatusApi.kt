package com.example.data

import com.example.data.model.ServerConfig
import com.example.data.model.StatusDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

sealed class ConnectionTestResult {
    data class Success(val latencyMs: Long, val hostname: String?, val os: String?) : ConnectionTestResult()
    data class Failure(val errorMessage: String, val httpCode: Int? = null) : ConnectionTestResult()
}

class StatusApi(
    private val client: OkHttpClient = defaultClient
) {
    companion object {
        val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .callTimeout(7, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }

        val json: Json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
            explicitNulls = false
        }
    }

    /**
     * Tests connectivity to the given server configuration.
     * Invokes GET {baseUrl}/api/status with exact diagnostic reporting.
     */
    suspend fun testConnection(server: ServerConfig): ConnectionTestResult = withContext(Dispatchers.IO) {
        val url = "${server.getBaseUrl()}/api/status"
        val request = try {
            Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .build()
        } catch (e: Exception) {
            return@withContext ConnectionTestResult.Failure("Invalid URL: ${e.message}")
        }

        val startTime = System.currentTimeMillis()
        try {
            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val bodyString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext ConnectionTestResult.Failure(
                        errorMessage = "HTTP ${response.code} ${response.message}: ${bodyString.take(100)}",
                        httpCode = response.code
                    )
                }

                if (bodyString.isBlank()) {
                    return@withContext ConnectionTestResult.Failure("Empty response received from server")
                }

                try {
                    val dto = json.decodeFromString<StatusDto>(bodyString)
                    ConnectionTestResult.Success(
                        latencyMs = latency,
                        hostname = dto.host?.hostname,
                        os = dto.host?.os
                    )
                } catch (e: SerializationException) {
                    ConnectionTestResult.Failure(
                        "Server returned an unexpected response: ${e.message}"
                    )
                } catch (e: Exception) {
                    ConnectionTestResult.Failure(
                        "Failed to parse server response: ${e.message}"
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            ConnectionTestResult.Failure("Connection timed out after 5s ($url)")
        } catch (e: ConnectException) {
            ConnectionTestResult.Failure("Connection refused: ${e.message ?: "Server not reachable"}")
        } catch (e: UnknownHostException) {
            ConnectionTestResult.Failure("Unknown host: Cannot resolve '${server.host}'")
        } catch (e: IOException) {
            ConnectionTestResult.Failure("Network I/O error: ${e.message ?: "Connection interrupted"}")
        } catch (e: Exception) {
            ConnectionTestResult.Failure("${e.javaClass.simpleName}: ${e.message ?: "Unexpected error"}")
        }
    }

    /**
     * Fetches real telemetry from GET {baseUrl}/api/status on Dispatchers.IO.
     * Decodes JSON defensively off the main thread.
     */
    suspend fun fetchStatus(server: ServerConfig): Result<StatusDto> = withContext(Dispatchers.IO) {
        val url = "${server.getBaseUrl()}/api/status"
        val request = try {
            Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .build()
        } catch (e: Exception) {
            return@withContext Result.failure(IllegalArgumentException("Invalid URL: ${e.message}", e))
        }

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code} ${response.message}: ${body.take(120)}")
                    )
                }

                if (body.isBlank()) {
                    return@withContext Result.failure(IOException("Server returned an empty body"))
                }

                try {
                    val dto = json.decodeFromString<StatusDto>(body)
                    Result.success(dto)
                } catch (e: SerializationException) {
                    Result.failure(
                        IllegalStateException("Server returned an unexpected response: ${e.message}", e)
                    )
                } catch (e: Exception) {
                    Result.failure(
                        IllegalStateException("Failed to parse status JSON: ${e.message}", e)
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(IOException("Connection timed out after 5s", e))
        } catch (e: ConnectException) {
            Result.failure(IOException("Connection refused: ${e.message ?: "Host unreachable"}", e))
        } catch (e: UnknownHostException) {
            Result.failure(IOException("Cannot resolve host '${server.host}'", e))
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(IOException("${e.javaClass.simpleName}: ${e.message}", e))
        }
    }
}
