package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.model.ChartHistory
import com.example.data.model.CpuStatus
import com.example.ui.components.ArcGauge
import com.example.ui.components.SmoothLineChart
import com.example.ui.components.StatusTopAppBar
import com.example.ui.theme.ChartCpuColor
import com.example.ui.theme.ChartTempColor
import com.example.util.FormatUtils
import java.util.Locale

@Composable
fun CpuDetailScreen(
    cpu: CpuStatus,
    history: ChartHistory,
    settings: AppSettings,
    onNavigateBack: () -> Unit,
    onToggleTheme: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    Scaffold(
        modifier = Modifier.testTag("cpu_detail_scaffold"),
        topBar = {
            StatusTopAppBar(
                title = "Processor",
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
                .testTag("cpu_detail_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Status Card (Load and Average Temperature + Max)
            item(key = "cpu_status_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Status",
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
                                value = cpu.loadPercent,
                                label = "Load",
                                valueText = FormatUtils.formatPercent(cpu.loadPercent),
                                reduceAnimations = settings.reduceAnimations,
                                size = 120.dp
                            )

                            if (cpu.hasTemperature && cpu.averageTemp != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    ArcGauge(
                                        value = cpu.averageTemp.toFloat(),
                                        maxValue = 105f,
                                        label = "Average Temp",
                                        valueText = FormatUtils.formatTemperature(cpu.averageTemp),
                                        customColor = ChartTempColor,
                                        reduceAnimations = settings.reduceAnimations,
                                        size = 120.dp
                                    )
                                    if (cpu.maxTemp != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Max: ${FormatUtils.formatTemperature(cpu.maxTemp)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    }
                }
            }

            // Temperatures Sensor List Card (One row per sensor with limit)
            item(key = "cpu_temperatures_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Temperatures",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (cpu.tempReadings.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                cpu.tempReadings.forEach { reading ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = reading.label,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val currentStr = FormatUtils.formatTemperature(reading.current)
                                        val limitStr = if (reading.limit != null) " (Limit ${FormatUtils.formatTemperature(reading.limit)})" else ""
                                        Text(
                                            text = "$currentStr$limitStr",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = ChartTempColor
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No sensor on this device",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Information Card (Model, Cores, Cache)
            item(key = "cpu_info_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Information",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CpuDetailInfoRow(label = "Model", value = cpu.model)
                        Spacer(modifier = Modifier.height(8.dp))
                        CpuDetailInfoRow(label = "Cores", value = cpu.coreCount?.toString() ?: "N/A")
                        Spacer(modifier = Modifier.height(8.dp))
                        CpuDetailInfoRow(
                            label = "Cache",
                            value = cpu.cacheInfo ?: "Not available on this device"
                        )
                    }
                }
            }

            // Load History Chart
            item(key = "cpu_load_chart") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Load History (60s)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SmoothLineChart(
                            points = history.cpuLoad,
                            lineColor = ChartCpuColor,
                            minY = 0f,
                            maxY = 100f,
                            yLabelFormatter = { String.format(Locale.US, "%.0f%%", it) },
                            currentValueLabel = FormatUtils.formatPercent(cpu.loadPercent)
                        )
                    }
                }
            }

            // Temperature History Chart of average temperature (only if sensor exists)
            if (cpu.hasTemperature && history.cpuTemp.isNotEmpty()) {
                item(key = "cpu_temp_chart") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Temperature History",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SmoothLineChart(
                                points = history.cpuTemp,
                                lineColor = ChartTempColor,
                                minY = 20f,
                                maxY = 100f,
                                yLabelFormatter = { String.format(Locale.US, "%.0f°C", it) },
                                currentValueLabel = FormatUtils.formatTemperature(cpu.averageTemp)
                            )
                        }
                    }
                }
            }

            // Per-Core Frequencies (only if available)
            item(key = "cpu_frequencies") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Core Frequencies",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (cpu.hasFrequencies) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                cpu.frequencies.forEach { (coreName, freq) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = coreName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val nowStr = FormatUtils.formatFrequency(freq.now)
                                        val maxStr = if (freq.max != null) " (Max ${FormatUtils.formatFrequency(freq.max)})" else ""
                                        Text(
                                            text = "$nowStr$maxStr",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Not available on this device",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "cpu_bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CpuDetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
