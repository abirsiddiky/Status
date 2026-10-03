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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.model.ChartHistory
import com.example.data.model.MemoryStatus
import com.example.ui.components.ArcGauge
import com.example.ui.components.SmoothLineChart
import com.example.ui.components.StatusTopAppBar
import com.example.ui.theme.ChartMemoryColor
import com.example.util.FormatUtils
import java.util.Locale

@Composable
fun MemoryDetailScreen(
    memory: MemoryStatus,
    history: ChartHistory,
    settings: AppSettings,
    onNavigateBack: () -> Unit,
    onToggleTheme: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    Scaffold(
        modifier = Modifier.testTag("memory_detail_scaffold"),
        topBar = {
            StatusTopAppBar(
                title = "Memory",
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
                .testTag("memory_detail_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Memory Overview Card with Gauges
            item(key = "mem_overview_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "RAM Utilization",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArcGauge(
                                value = memory.usagePercent,
                                label = "RAM",
                                valueText = FormatUtils.formatPercent(memory.usagePercent),
                                reduceAnimations = settings.reduceAnimations,
                                size = 125.dp
                            )

                            if (memory.hasSwap) {
                                ArcGauge(
                                    value = memory.swapUsagePercent,
                                    label = "Swap",
                                    valueText = FormatUtils.formatPercent(memory.swapUsagePercent),
                                    reduceAnimations = settings.reduceAnimations,
                                    size = 125.dp
                                )
                            }
                        }
                    }
                }
            }

            // Detailed Breakdown
            item(key = "mem_breakdown_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Memory Breakdown",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        MemoryDetailRow("Total RAM", FormatUtils.formatBytes(memory.totalBytes))
                        MemoryDetailRow("In use", FormatUtils.formatBytes(memory.inUseBytes))
                        MemoryDetailRow("Available", FormatUtils.formatBytes(memory.availableBytes))
                        MemoryDetailRow("In cache", FormatUtils.formatBytes(memory.cachedBytes))

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Swap Space",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (memory.hasSwap) {
                            MemoryDetailRow("Swap total", FormatUtils.formatBytes(memory.swapTotalBytes))
                            MemoryDetailRow("Swap in use", FormatUtils.formatBytes(memory.swapInUseBytes))
                            MemoryDetailRow("Swap available", FormatUtils.formatBytes(memory.swapAvailableBytes))
                        } else {
                            Text(
                                text = "No swap configured on this system",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (memory.processCount != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            MemoryDetailRow("Active processes", memory.processCount.toString())
                        }
                    }
                }
            }

            // Usage Area Chart
            item(key = "mem_chart_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "RAM Usage History (60s)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SmoothLineChart(
                            points = history.memoryUsage,
                            lineColor = ChartMemoryColor,
                            minY = 0f,
                            maxY = 100f,
                            yLabelFormatter = { String.format(Locale.US, "%.0f%%", it) },
                            currentValueLabel = FormatUtils.formatPercent(memory.usagePercent)
                        )
                    }
                }
            }

            item(key = "mem_bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MemoryDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
