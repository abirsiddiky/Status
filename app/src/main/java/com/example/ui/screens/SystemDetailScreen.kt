package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.model.HostStatus
import com.example.ui.components.StatusTopAppBar
import java.util.Locale

@Composable
fun SystemDetailScreen(
    host: HostStatus,
    processCount: Int?,
    settings: AppSettings,
    onNavigateBack: () -> Unit,
    onToggleTheme: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    Scaffold(
        modifier = Modifier.testTag("system_detail_scaffold"),
        topBar = {
            StatusTopAppBar(
                title = "System",
                themeMode = settings.themeMode,
                onToggleTheme = onToggleTheme,
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("system_detail_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "sys_host_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Host Information",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        SystemDetailRow("Hostname", host.hostname)
                        SystemDetailRow("Operating System", host.os)
                        SystemDetailRow("System Uptime", host.uptimeFormatted)
                        SystemDetailRow("Monitor App Memory", host.appMemoryFormatted)
                        SystemDetailRow("Active Processes", processCount?.toString() ?: "N/A")
                    }
                }
            }

            item(key = "sys_load_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Load Averages",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val load1 = host.loadAvg.getOrNull(0)?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A"
                        val load5 = host.loadAvg.getOrNull(1)?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A"
                        val load15 = host.loadAvg.getOrNull(2)?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A"

                        SystemDetailRow("1 minute load", load1)
                        SystemDetailRow("5 minute load", load5)
                        SystemDetailRow("15 minute load", load15)
                    }
                }
            }

            item(key = "sys_bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SystemDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
