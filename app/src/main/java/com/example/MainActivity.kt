package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.model.CpuStatus
import com.example.data.model.HostStatus
import com.example.data.model.MemoryStatus
import com.example.data.model.NetworkStatus
import com.example.data.model.StorageStatus
import com.example.ui.StatusViewModel
import com.example.ui.screens.CpuDetailScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MemoryDetailScreen
import com.example.ui.screens.NetworkDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StorageDetailScreen
import com.example.ui.screens.SystemDetailScreen
import com.example.ui.theme.StatusTheme
import kotlinx.coroutines.awaitCancellation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: StatusViewModel = viewModel()
            val lifecycleOwner = LocalLifecycleOwner.current

            // Polling is tied strictly to Lifecycle.State.STARTED
            // Polling stops when app enters background and restarts when visible
            LaunchedEffect(lifecycleOwner) {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.onLifecycleStarted()
                    try {
                        awaitCancellation()
                    } finally {
                        viewModel.onLifecycleStopped()
                    }
                }
            }

            val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val telemetry by viewModel.telemetryState.collectAsStateWithLifecycle()

            StatusTheme(themeMode = appSettings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StatusAppNavigation(
                        viewModel = viewModel,
                        uiState = uiState,
                        telemetry = telemetry,
                        appSettings = appSettings
                    )
                }
            }
        }
    }
}

@Composable
fun StatusAppNavigation(
    viewModel: StatusViewModel,
    uiState: com.example.ui.StatusMainUiState,
    telemetry: com.example.data.TelemetryState,
    appSettings: com.example.data.AppSettings
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            DashboardScreen(
                servers = uiState.servers,
                activeServer = uiState.activeServer,
                telemetry = telemetry,
                settings = appSettings,
                showAddServerDialog = uiState.showAddServerDialog,
                editingServer = uiState.editingServer,
                onOpenAddServer = { viewModel.openAddServerDialog() },
                onCloseServerDialog = { viewModel.closeServerDialog() },
                onSaveServer = { server -> viewModel.saveServer(server) },
                onTestConnection = { server -> viewModel.testConnection(server) },
                onSelectServer = { serverId -> viewModel.setActiveServer(serverId) },
                onToggleTheme = { viewModel.toggleTheme() },
                onOpenSettings = { navController.navigate("settings") },
                onRetry = { viewModel.refreshNow() },
                onNavigateToCpu = { navController.navigate("cpu_detail") },
                onNavigateToMemory = { navController.navigate("memory_detail") },
                onNavigateToStorage = { navController.navigate("storage_detail") },
                onNavigateToNetwork = { navController.navigate("network_detail") },
                onNavigateToSystem = { navController.navigate("system_detail") }
            )
        }

        composable("cpu_detail") {
            CpuDetailScreen(
                cpu = telemetry.status?.cpu ?: CpuStatus(),
                history = telemetry.history,
                settings = appSettings,
                onNavigateBack = { navController.popBackStack() },
                onToggleTheme = { viewModel.toggleTheme() }
            )
        }

        composable("memory_detail") {
            MemoryDetailScreen(
                memory = telemetry.status?.memory ?: MemoryStatus(),
                history = telemetry.history,
                settings = appSettings,
                onNavigateBack = { navController.popBackStack() },
                onToggleTheme = { viewModel.toggleTheme() }
            )
        }

        composable("storage_detail") {
            StorageDetailScreen(
                storage = telemetry.status?.storage ?: StorageStatus(),
                history = telemetry.history,
                settings = appSettings,
                onNavigateBack = { navController.popBackStack() },
                onToggleTheme = { viewModel.toggleTheme() }
            )
        }

        composable("network_detail") {
            NetworkDetailScreen(
                network = telemetry.status?.network ?: NetworkStatus(),
                history = telemetry.history,
                settings = appSettings,
                onNavigateBack = { navController.popBackStack() },
                onToggleTheme = { viewModel.toggleTheme() }
            )
        }

        composable("system_detail") {
            SystemDetailScreen(
                host = telemetry.status?.host ?: HostStatus(),
                processCount = telemetry.status?.memory?.processCount,
                settings = appSettings,
                onNavigateBack = { navController.popBackStack() },
                onToggleTheme = { viewModel.toggleTheme() }
            )
        }

        composable("settings") {
            SettingsScreen(
                servers = uiState.servers,
                activeServerId = uiState.activeServer?.id,
                settings = appSettings,
                showAddServerDialog = uiState.showAddServerDialog,
                editingServer = uiState.editingServer,
                onNavigateBack = { navController.popBackStack() },
                onToggleTheme = { viewModel.toggleTheme() },
                onSelectActiveServer = { serverId -> viewModel.setActiveServer(serverId) },
                onOpenAddServer = { viewModel.openAddServerDialog() },
                onOpenEditServer = { server -> viewModel.openEditServerDialog(server) },
                onCloseServerDialog = { viewModel.closeServerDialog() },
                onSaveServer = { server -> viewModel.saveServer(server) },
                onDeleteServer = { serverId -> viewModel.deleteServer(serverId) },
                onTestConnection = { server -> viewModel.testConnection(server) },
                onSetRefreshInterval = { seconds -> viewModel.setRefreshInterval(seconds) },
                onSetReduceAnimations = { reduce -> viewModel.setReduceAnimations(reduce) },
                onSetThemeMode = { mode -> viewModel.setThemeMode(mode) }
            )
        }
    }
}
