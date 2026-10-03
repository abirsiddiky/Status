package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AppSettings
import com.example.data.TelemetryState
import com.example.data.model.ServerConfig
import com.example.ui.components.CpuDashboardCard
import com.example.ui.components.MemoryDashboardCard
import com.example.ui.components.NetworkDashboardCard
import com.example.ui.components.ServerDialog
import com.example.ui.components.StatusTopAppBar
import com.example.ui.components.StorageDashboardCard
import com.example.ui.components.SystemDashboardCard
import com.example.ui.theme.StatusOfflineRed
import com.example.ui.theme.StatusOnlineGreen
import com.example.util.FormatUtils

@Composable
fun DashboardScreen(
    servers: List<ServerConfig>,
    activeServer: ServerConfig?,
    telemetry: TelemetryState,
    settings: AppSettings,
    showAddServerDialog: Boolean,
    editingServer: ServerConfig?,
    onOpenAddServer: () -> Unit,
    onCloseServerDialog: () -> Unit,
    onSaveServer: (ServerConfig) -> Unit,
    onTestConnection: suspend (ServerConfig) -> com.example.data.ConnectionTestResult,
    onSelectServer: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onNavigateToCpu: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToSystem: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when an error occurs
    LaunchedEffect(telemetry.errorMessage) {
        val error = telemetry.errorMessage
        if (!error.isNullOrBlank()) {
            val result = snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "Retry",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                onRetry()
            }
        }
    }

    val isFirstLaunch = servers.isEmpty()
    val shouldShowDialog = isFirstLaunch || showAddServerDialog || editingServer != null

    Scaffold(
        modifier = Modifier.testTag("dashboard_scaffold"),
        topBar = {
            StatusTopAppBar(
                title = activeServer?.getDisplayName() ?: "Status",
                themeMode = settings.themeMode,
                onToggleTheme = onToggleTheme,
                onOpenSettings = onOpenSettings,
                savedServers = servers,
                activeServerId = activeServer?.id,
                onSelectServer = onSelectServer,
                isDashboard = true
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (servers.isEmpty()) {
                // First-launch empty state behind mandatory dialog
                EmptyServersView(onAddServer = onOpenAddServer)
            } else {
                val status = telemetry.status

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("dashboard_cards_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Status Header Chip (Online / Offline)
                    item(key = "status_header_chip") {
                        StatusIndicatorHeader(
                            isOnline = telemetry.isOnline,
                            isLoading = telemetry.isLoading,
                            lastUpdated = telemetry.lastSuccessfulUpdate,
                            errorMessage = telemetry.errorMessage
                        )
                    }

                    if (status != null) {
                        item(key = "cpu_card") {
                            CpuDashboardCard(
                                cpu = status.cpu,
                                onViewMore = onNavigateToCpu,
                                reduceAnimations = settings.reduceAnimations
                            )
                        }

                        item(key = "memory_card") {
                            MemoryDashboardCard(
                                memory = status.memory,
                                onViewMore = onNavigateToMemory,
                                reduceAnimations = settings.reduceAnimations
                            )
                        }

                        item(key = "storage_card") {
                            StorageDashboardCard(
                                storage = status.storage,
                                onViewMore = onNavigateToStorage,
                                reduceAnimations = settings.reduceAnimations
                            )
                        }

                        item(key = "network_card") {
                            NetworkDashboardCard(
                                network = status.network,
                                onViewMore = onNavigateToNetwork
                            )
                        }

                        item(key = "system_card") {
                            SystemDashboardCard(
                                host = status.host,
                                processCount = status.memory.processCount,
                                onViewMore = onNavigateToSystem
                            )
                        }
                    } else {
                        item(key = "awaiting_telemetry") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Connecting to server…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Server Dialog (Mandatory on first launch or when triggered)
            if (shouldShowDialog) {
                ServerDialog(
                    initialServer = editingServer,
                    isMandatory = isFirstLaunch,
                    onDismiss = onCloseServerDialog,
                    onTestConnection = onTestConnection,
                    onSaveServer = onSaveServer
                )
            }
        }
    }
}

@Composable
fun StatusIndicatorHeader(
    isOnline: Boolean,
    isLoading: Boolean,
    lastUpdated: Long,
    errorMessage: String?
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("status_indicator_header")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Glowing dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) StatusOnlineGreen else StatusOfflineRed)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isOnline) "Online" else "Offline",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isOnline) StatusOnlineGreen else StatusOfflineRed
                )
                if (isOnline && lastUpdated > 0L) {
                    Text(
                        text = " • ${FormatUtils.formatTime(lastUpdated)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (!isOnline && errorMessage != null) {
                Text(
                    text = errorMessage.take(30),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = StatusOfflineRed,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun EmptyServersView(onAddServer: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_server),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No server configured",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add your Status monitor server to start tracking system performance in real-time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddServer,
                modifier = Modifier.testTag("empty_state_add_server_button")
            ) {
                Text("Add server")
            }
        }
    }
}
