package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.AppSettings
import com.example.data.ConnectionTestResult
import com.example.data.model.ServerConfig
import com.example.ui.components.ServerDialog
import com.example.ui.components.StatusTopAppBar

@Composable
fun NoServerScreen(
    settings: AppSettings,
    onToggleTheme: () -> Unit,
    onOpenAddServer: () -> Unit,
    onCloseServerDialog: () -> Unit,
    onSaveServer: (ServerConfig) -> Unit,
    onTestConnection: suspend (ServerConfig) -> ConnectionTestResult,
    showAddServerDialog: Boolean,
    editingServer: ServerConfig?
) {
    Scaffold(
        modifier = Modifier.testTag("no_server_scaffold"),
        topBar = {
            StatusTopAppBar(
                title = "Status",
                themeMode = settings.themeMode,
                onToggleTheme = onToggleTheme,
                isDashboard = false
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            EmptyServersView(onAddServer = onOpenAddServer)

            // Mandatory dialog on NoServer: cannot be dismissed without adding a server
            ServerDialog(
                initialServer = editingServer,
                isMandatory = true,
                onDismiss = onCloseServerDialog,
                onTestConnection = onTestConnection,
                onSaveServer = onSaveServer
            )
        }
    }
}
