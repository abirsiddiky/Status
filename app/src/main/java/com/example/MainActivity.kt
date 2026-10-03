package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.TelemetryState
import com.example.data.model.CpuStatus
import com.example.data.model.HostStatus
import com.example.data.model.MemoryStatus
import com.example.data.model.NetworkStatus
import com.example.data.model.ServerConfig
import com.example.data.model.StorageStatus
import com.example.ui.AppState
import com.example.ui.StatusViewModel
import com.example.ui.screens.CpuDetailScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MemoryDetailScreen
import com.example.ui.screens.NetworkDetailScreen
import com.example.ui.screens.NoServerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StorageDetailScreen
import com.example.ui.screens.SystemDetailScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.StatusTheme
import kotlinx.coroutines.awaitCancellation

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel: StatusViewModel by viewModels()

        // Keep splash screen on screen until the first DataStore read finishes
        splashScreen.setKeepOnScreenCondition {
            viewModel.appState.value is AppState.Loading
        }

        setContent {
            val appState by viewModel.appState.collectAsStateWithLifecycle()
            val telemetry by viewModel.telemetryState.collectAsStateWithLifecycle()
            val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()

            val lifecycleOwner = LocalLifecycleOwner.current

            // Lifecycle-aware background polling
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

            // Theme is read on startup: first frame already uses the correct light/dark theme
            StatusTheme(themeMode = appState.settings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (appState !is AppState.Loading) {
                        StatusAppNavigation(
                            viewModel = viewModel,
                            appState = appState,
                            telemetry = telemetry,
                            showAddServerDialog = dialogState.first,
                            editingServer = dialogState.second
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusAppNavigation(
    viewModel: StatusViewModel,
    appState: AppState,
    telemetry: TelemetryState,
    showAddServerDialog: Boolean,
    editingServer: ServerConfig?
) {
    val navController = rememberNavController()

    // Start destination chosen strictly from initial AppState (never navigate Dashboard -> NoServer -> Dashboard)
    val startDestination = rememberSaveable {
        if (appState is AppState.NoServer) "no_server" else "dashboard"
    }

    val isSystemDark = isSystemInDarkTheme()
    val isCurrentlyDark = when (appState.settings.themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("no_server") {
            // When a server is added, navigate to dashboard and pop no_server
            LaunchedEffect(appState) {
                if (appState is AppState.Ready) {
                    navController.navigate("dashboard") {
                        popUpTo("no_server") { inclusive = true }
                    }
                }
            }

            NoServerScreen(
                settings = appState.settings,
                onToggleTheme = { viewModel.toggleTheme(isCurrentlyDark) },
                onOpenAddServer = { viewModel.openAddServerDialog() },
                onCloseServerDialog = { viewModel.closeServerDialog() },
                onSaveServer = { server -> viewModel.saveServer(server) },
                onTestConnection = { server -> viewModel.testConnection(server) },
                showAddServerDialog = showAddServerDialog,
                editingServer = editingServer
            )
        }

        composable("dashboard") {
            // If all servers are deleted, return to no_server
            LaunchedEffect(appState) {
                if (appState is AppState.NoServer) {
                    navController.navigate("no_server") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            }

            val readyState = appState as? AppState.Ready
            val servers = readyState?.servers ?: emptyList()
            val activeServer = readyState?.activeServer

            DashboardScreen(
                servers = servers,
                activeServer = activeServer,
                telemetry = telemetry,
                settings = appState.settings,
                showAddServerDialog = showAddServerDialog,
                editingServer = editingServer,
                onOpenAddServer = { viewModel.openAddServerDialog() },
                onCloseServerDialog = { viewModel.closeServerDialog() },
                onSaveServer = { server -> viewModel.saveServer(server) },
                onTestConnection = { server -> viewModel.testConnection(server) },
                onSelectServer = { serverId -> viewModel.setActiveServer(serverId) },
                onToggleTheme = { viewModel.toggleTheme(isCurrentlyDark) },
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
                settings = appState.settings,
                onNavigateBack = { navController.popBackStack() },
                onToggleTheme = { viewModel.toggleTheme(isCurrentlyDark) }
            )
        }

        composable("memory_detail") {
            MemoryDetailScreen(
                memory = telemetry.status?.memory ?: MemoryStatus(),
                history = telemetry.history,
                settings = appState.settings,
                onNavigateBack = { navController.popBackStack() },
                onToggleTheme = { viewModel.toggleTheme(isCurrentlyDark) }
            )
        }

        composable("storage_detail") {
            StorageDetailScreen(
                storage = telemetry.status?.storage ?: StorageStatus(),
                history = telemetry.history,
                settings = appState.settings,
                onNavigateBack = { navController.popBackStack() },
                onToggleTheme = { viewModel.toggleTheme(isCurrentlyDark) }
            )
        }

        composable("network_detail") {
            NetworkDetailScreen(
                network = telemetry.status?.network ?: NetworkStatus(),
                history = telemetry.history,
                settings = appState.settings,
                onNavigateBack = { navController.popBackStack() },
                onToggleTheme = { viewModel.toggleTheme(isCurrentlyDark) }
            )
        }

        composable("system_detail") {
            SystemDetailScreen(
                host = telemetry.status?.host ?: HostStatus(),
                processCount = telemetry.status?.memory?.processCount,
                settings = appState.settings,
                onNavigateBack = { navController.popBackStack() },
                onToggleTheme = { viewModel.toggleTheme(isCurrentlyDark) }
            )
        }

        composable("settings") {
            val readyState = appState as? AppState.Ready
            val servers = readyState?.servers ?: emptyList()
            val activeServerId = readyState?.activeServer?.id

            SettingsScreen(
                servers = servers,
                activeServerId = activeServerId,
                settings = appState.settings,
                showAddServerDialog = showAddServerDialog,
                editingServer = editingServer,
                onNavigateBack = { navController.popBackStack() },
                onToggleTheme = { viewModel.toggleTheme(isCurrentlyDark) },
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
