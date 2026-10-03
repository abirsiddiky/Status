package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ConnectionTestResult
import com.example.data.model.ServerConfig
import com.example.ui.theme.StatusOfflineRed
import com.example.ui.theme.StatusOnlineGreen
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ServerDialog(
    initialServer: ServerConfig? = null,
    isMandatory: Boolean = false,
    onDismiss: () -> Unit,
    onTestConnection: suspend (ServerConfig) -> ConnectionTestResult,
    onSaveServer: (ServerConfig) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initialServer?.name ?: "") }
    var scheme by remember { mutableStateOf(initialServer?.scheme ?: "http") }
    var host by remember { mutableStateOf(initialServer?.host ?: "") }
    var portText by remember { mutableStateOf(initialServer?.port?.toString() ?: "9090") }
    var path by remember { mutableStateOf(initialServer?.path ?: "") }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<ConnectionTestResult?>(null) }

    fun buildCurrentConfig(): ServerConfig {
        val parsedPort = portText.toIntOrNull() ?: 9090
        val effectiveName = name.trim().ifBlank { host.trim() }
        return ServerConfig(
            id = initialServer?.id ?: UUID.randomUUID().toString(),
            name = effectiveName,
            scheme = scheme,
            host = host.trim().removePrefix("http://").removePrefix("https://").trimEnd('/'),
            port = parsedPort,
            path = path.trim().trim('/')
        )
    }

    Dialog(
        onDismissRequest = {
            if (!isMandatory) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isMandatory,
            dismissOnClickOutside = !isMandatory,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .testTag("server_dialog_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = if (initialServer == null) "Add Server" else "Edit Server",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isMandatory) {
                    Text(
                        text = "Configure a Linux server running the Status monitor agent to continue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Scheme Segmented Control
                Text(
                    text = "Scheme",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    listOf("http", "https").forEach { opt ->
                        val selected = scheme.equals(opt, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    scheme = opt
                                    testResult = null
                                }
                                .padding(vertical = 8.dp)
                                .testTag("scheme_${opt}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opt.uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Host Field
                OutlinedTextField(
                    value = host,
                    onValueChange = {
                        host = it
                        testResult = null
                    },
                    label = { Text("IP address or domain *") },
                    placeholder = { Text("192.168.1.100 or status.local") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("server_host_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Port & Path Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = portText,
                        onValueChange = {
                            portText = it
                            testResult = null
                        },
                        label = { Text("Port") },
                        placeholder = { Text("9090") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("server_port_input")
                    )

                    OutlinedTextField(
                        value = path,
                        onValueChange = {
                            path = it
                            testResult = null
                        },
                        label = { Text("Path (optional)") },
                        placeholder = { Text("api or empty") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("server_path_input")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Name Field (Optional)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (optional, defaults to host)") },
                    placeholder = { Text("Home Server / Debian VPS") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("server_name_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Test Connection Button
                OutlinedButton(
                    onClick = {
                        val config = buildCurrentConfig()
                        isTesting = true
                        testResult = null
                        coroutineScope.launch {
                            val res = onTestConnection(config)
                            testResult = res
                            isTesting = false
                        }
                    },
                    enabled = host.isNotBlank() && !isTesting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_connection_button")
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Testing connection…")
                    } else {
                        Text("Test connection")
                    }
                }

                // Test Result Card
                testResult?.let { result ->
                    Spacer(modifier = Modifier.height(12.dp))
                    when (result) {
                        is ConnectionTestResult.Success -> {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = StatusOnlineGreen.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("test_success_banner")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "✓ Connected successfully",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = StatusOnlineGreen
                                    )
                                    val details = buildString {
                                        append("Latency: ${result.latencyMs} ms")
                                        if (!result.hostname.isNullOrBlank()) append(" • Host: ${result.hostname}")
                                        if (!result.os.isNullOrBlank()) append(" • ${result.os}")
                                    }
                                    Text(
                                        text = details,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        is ConnectionTestResult.Failure -> {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = StatusOfflineRed.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("test_error_banner")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "✕ Connection failed",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = StatusOfflineRed
                                    )
                                    Text(
                                        text = result.errorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    // Save anyway button
                                    TextButton(
                                        onClick = {
                                            val config = buildCurrentConfig()
                                            onSaveServer(config)
                                        },
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .testTag("save_anyway_button")
                                    ) {
                                        Text("Save anyway", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isMandatory) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("cancel_server_button")
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    val isSuccess = testResult is ConnectionTestResult.Success
                    Button(
                        onClick = {
                            val config = buildCurrentConfig()
                            onSaveServer(config)
                        },
                        enabled = isSuccess,
                        modifier = Modifier.testTag("save_server_button")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
