package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CpuStatus
import com.example.data.model.HostStatus
import com.example.data.model.MemoryStatus
import com.example.data.model.NetworkStatus
import com.example.data.model.StorageStatus
import com.example.ui.theme.ChartTempColor
import com.example.util.FormatUtils
import java.util.Locale

@Composable
fun CardHeader(
    title: String,
    iconRes: Int,
    onViewMore: () -> Unit,
    testTagPrefix: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        FilledTonalButton(
            onClick = onViewMore,
            shape = CircleShape,
            modifier = Modifier.testTag("${testTagPrefix}_view_more")
        ) {
            Text(
                text = "View more",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
fun CpuDashboardCard(
    cpu: CpuStatus,
    onViewMore: () -> Unit,
    reduceAnimations: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cpu_dashboard_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            CardHeader(
                title = "Processor",
                iconRes = R.drawable.ic_cpu,
                onViewMore = onViewMore,
                testTagPrefix = "cpu"
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // CPU Load Gauge
                ArcGauge(
                    value = cpu.loadPercent,
                    label = "Load",
                    valueText = FormatUtils.formatPercent(cpu.loadPercent),
                    reduceAnimations = reduceAnimations,
                    size = 120.dp
                )

                // Temperature Gauge (or No sensor fallback)
                if (cpu.hasTemperature && cpu.averageTemp != null) {
                    ArcGauge(
                        value = cpu.averageTemp.toFloat(),
                        maxValue = 105f,
                        label = "Temperature",
                        valueText = FormatUtils.formatTemperature(cpu.averageTemp),
                        customColor = ChartTempColor,
                        reduceAnimations = reduceAnimations,
                        size = 120.dp
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "No sensor",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "on this device",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // CPU Model and Core info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Model",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = cpu.model,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Cores",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = cpu.coreCount?.toString() ?: "N/A",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun MemoryDashboardCard(
    memory: MemoryStatus,
    onViewMore: () -> Unit,
    reduceAnimations: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("memory_dashboard_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            CardHeader(
                title = "Memory",
                iconRes = R.drawable.ic_memory,
                onViewMore = onViewMore,
                testTagPrefix = "memory"
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArcGauge(
                    value = memory.usagePercent,
                    label = "RAM in use",
                    valueText = FormatUtils.formatPercent(memory.usagePercent),
                    reduceAnimations = reduceAnimations,
                    size = 120.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatRow(label = "In use", value = FormatUtils.formatBytes(memory.inUseBytes))
                    StatRow(label = "Available", value = FormatUtils.formatBytes(memory.availableBytes))
                    StatRow(label = "Cached", value = FormatUtils.formatBytes(memory.cachedBytes))
                    StatRow(
                        label = "Swap",
                        value = if (memory.hasSwap) {
                            FormatUtils.formatBytes(memory.swapInUseBytes)
                        } else {
                            "No swap"
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StorageDashboardCard(
    storage: StorageStatus,
    onViewMore: () -> Unit,
    reduceAnimations: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("storage_dashboard_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            CardHeader(
                title = "Storage",
                iconRes = R.drawable.ic_storage,
                onViewMore = onViewMore,
                testTagPrefix = "storage"
            )

            Spacer(modifier = Modifier.height(14.dp))

            val primary = storage.primaryVolume
            if (primary != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArcGauge(
                        value = primary.usagePercent,
                        label = primary.name,
                        valueText = FormatUtils.formatPercent(primary.usagePercent),
                        reduceAnimations = reduceAnimations,
                        size = 120.dp
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatRow(label = "Volume", value = primary.name)
                        StatRow(label = "In use", value = FormatUtils.formatBytes(primary.inUseBytes))
                        StatRow(label = "Total", value = FormatUtils.formatBytes(primary.totalBytes))
                        StatRow(label = "Volumes", value = "${storage.volumes.size} mounted")
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No storage volumes detected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun NetworkDashboardCard(
    network: NetworkStatus,
    onViewMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("network_dashboard_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            CardHeader(
                title = "Network",
                iconRes = R.drawable.ic_network,
                onViewMore = onViewMore,
                testTagPrefix = "network"
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Download (RX) Rate
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Download (RX)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "↓ ${FormatUtils.formatBitsPerSec(network.rxRateBps)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Upload (TX) Rate
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Upload (TX)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "↑ ${FormatUtils.formatBitsPerSec(network.txRateBps)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatRow(label = "Interface", value = network.interfaceName)
                StatRow(
                    label = "Link speed",
                    value = network.linkSpeedMbit?.let { "${it.toInt()} Mbit/s" } ?: "N/A"
                )
            }
        }
    }
}

@Composable
fun SystemDashboardCard(
    host: HostStatus,
    processCount: Int?,
    onViewMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("system_dashboard_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            CardHeader(
                title = "System",
                iconRes = R.drawable.ic_server,
                onViewMore = onViewMore,
                testTagPrefix = "system"
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = "Hostname",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = host.hostname,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Uptime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = host.uptimeFormatted,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            StatRow(label = "Operating System", value = host.os)
            Spacer(modifier = Modifier.height(6.dp))

            val loadavgStr = if (host.loadAvg.isNotEmpty()) {
                host.loadAvg.joinToString(" • ") { String.format(Locale.US, "%.2f", it) }
            } else {
                "N/A"
            }
            StatRow(label = "Load avg (1, 5, 15m)", value = loadavgStr)
            Spacer(modifier = Modifier.height(6.dp))
            StatRow(label = "Active processes", value = processCount?.toString() ?: "N/A")
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
