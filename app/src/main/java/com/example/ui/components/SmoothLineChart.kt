package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChartRxColor
import com.example.ui.theme.ChartTxColor
import java.util.Locale

/**
 * Pure Compose Canvas chart supporting smooth cubic Bezier lines,
 * translucent fill, dashed horizontal grid lines, and Y-axis labels.
 *
 * Implemented with drawWithCache to ensure zero object allocations per draw frame.
 */
@Composable
fun SmoothLineChart(
    points: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = lineColor.copy(alpha = 0.25f),
    minY: Float? = 0f,
    maxY: Float? = null,
    yLabelFormatter: (Float) -> String = { String.format(Locale.US, "%.0f", it) },
    title: String? = null,
    currentValueLabel: String? = null
) {
    val downsampled = remember(points) {
        if (points.size > 60) points.takeLast(60) else points
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(12.dp)
    ) {
        if (title != null || currentValueLabel != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (currentValueLabel != null) {
                    Text(
                        text = currentValueLabel,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = lineColor
                    )
                }
            }
        }

        if (downsampled.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Awaiting samples…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        val effectiveMinY = minY ?: (downsampled.minOrNull() ?: 0f)
        val dataMax = downsampled.maxOrNull() ?: 100f
        val effectiveMaxY = (maxY ?: (dataMax * 1.1f)).coerceAtLeast(effectiveMinY + 1f)
        val yRange = (effectiveMaxY - effectiveMinY).coerceAtLeast(0.001f)

        val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            // Y-axis labels
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .height(height)
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = yLabelFormatter(effectiveMaxY),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = yLabelFormatter((effectiveMaxY + effectiveMinY) / 2f),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = yLabelFormatter(effectiveMinY),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Drawing area using Spacer + drawWithCache
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .drawWithCache {
                        val w = size.width
                        val h = size.height

                        val linePath = Path()
                        val fillPath = Path()

                        val stepX = w / (downsampled.size - 1).coerceAtLeast(1)

                        val coords = downsampled.mapIndexed { index, value ->
                            val norm = ((value - effectiveMinY) / yRange).coerceIn(0f, 1f)
                            Offset(
                                x = index * stepX,
                                y = h - (norm * h)
                            )
                        }

                        if (coords.isNotEmpty()) {
                            linePath.moveTo(coords.first().x, coords.first().y)
                            fillPath.moveTo(coords.first().x, h)
                            fillPath.lineTo(coords.first().x, coords.first().y)

                            for (i in 0 until coords.size - 1) {
                                val p0 = coords[i]
                                val p1 = coords[i + 1]
                                val controlX = (p0.x + p1.x) / 2f
                                linePath.cubicTo(
                                    x1 = controlX, y1 = p0.y,
                                    x2 = controlX, y2 = p1.y,
                                    x3 = p1.x, y3 = p1.y
                                )
                                fillPath.cubicTo(
                                    x1 = controlX, y1 = p0.y,
                                    x2 = controlX, y2 = p1.y,
                                    x3 = p1.x, y3 = p1.y
                                )
                            }

                            fillPath.lineTo(coords.last().x, h)
                            fillPath.close()
                        }

                        val fillBrush = Brush.verticalGradient(
                            colors = listOf(fillColor, fillColor.copy(alpha = 0f)),
                            startY = 0f,
                            endY = h
                        )

                        onDrawBehind {
                            // 3 dashed grid lines (top, middle, bottom)
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, 0f),
                                end = Offset(w, 0f),
                                pathEffect = dashEffect,
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, h / 2f),
                                end = Offset(w, h / 2f),
                                pathEffect = dashEffect,
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, h),
                                end = Offset(w, h),
                                pathEffect = dashEffect,
                                strokeWidth = 1.dp.toPx()
                            )

                            // Fill
                            drawPath(path = fillPath, brush = fillBrush)

                            // Line
                            drawPath(
                                path = linePath,
                                color = lineColor,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
            )
        }
    }
}

/**
 * Dual line chart for Network Transfer (TX / RX) with legend.
 */
@Composable
fun DualSmoothLineChart(
    rxPoints: List<Float>,
    txPoints: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp,
    rxLabel: String = "Download (RX)",
    txLabel: String = "Upload (TX)",
    yLabelFormatter: (Float) -> String
) {
    val downsampledRx = remember(rxPoints) {
        if (rxPoints.size > 60) rxPoints.takeLast(60) else rxPoints
    }
    val downsampledTx = remember(txPoints) {
        if (txPoints.size > 60) txPoints.takeLast(60) else txPoints
    }

    val maxCount = maxOf(downsampledRx.size, downsampledTx.size)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(12.dp)
    ) {
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(ChartRxColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = rxLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(ChartTxColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = txLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (maxCount < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Awaiting network samples…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        val rxMax = downsampledRx.maxOrNull() ?: 0f
        val txMax = downsampledTx.maxOrNull() ?: 0f
        val effectiveMaxY = (maxOf(rxMax, txMax) * 1.15f).coerceAtLeast(10f)
        val yRange = effectiveMaxY.coerceAtLeast(0.001f)

        val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            // Y-axis labels
            Column(
                modifier = Modifier
                    .width(54.dp)
                    .height(height)
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = yLabelFormatter(effectiveMaxY),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = yLabelFormatter(effectiveMaxY / 2f),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = yLabelFormatter(0f),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Dual Canvas area with Spacer + drawWithCache
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .drawWithCache {
                        val w = size.width
                        val h = size.height

                        fun buildPaths(data: List<Float>): Pair<Path, Path> {
                            val line = Path()
                            val fill = Path()
                            if (data.size < 2) return Pair(line, fill)

                            val stepX = w / (data.size - 1).coerceAtLeast(1)
                            val coords = data.mapIndexed { index, v ->
                                val norm = (v / yRange).coerceIn(0f, 1f)
                                Offset(x = index * stepX, y = h - (norm * h))
                            }

                            line.moveTo(coords.first().x, coords.first().y)
                            fillPathLine(line, fill, coords, h)

                            return Pair(line, fill)
                        }

                        val (rxLine, rxFill) = buildPaths(downsampledRx)
                        val (txLine, txFill) = buildPaths(downsampledTx)

                        val rxBrush = Brush.verticalGradient(
                            colors = listOf(ChartRxColor.copy(alpha = 0.25f), Color.Transparent),
                            startY = 0f,
                            endY = h
                        )
                        val txBrush = Brush.verticalGradient(
                            colors = listOf(ChartTxColor.copy(alpha = 0.25f), Color.Transparent),
                            startY = 0f,
                            endY = h
                        )

                        onDrawBehind {
                            // Grid lines
                            drawLine(gridColor, Offset(0f, 0f), Offset(w, 0f), pathEffect = dashEffect, strokeWidth = 1.dp.toPx())
                            drawLine(gridColor, Offset(0f, h / 2f), Offset(w, h / 2f), pathEffect = dashEffect, strokeWidth = 1.dp.toPx())
                            drawLine(gridColor, Offset(0f, h), Offset(w, h), pathEffect = dashEffect, strokeWidth = 1.dp.toPx())

                            // RX (Download)
                            drawPath(rxFill, rxBrush)
                            drawPath(rxLine, ChartRxColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                            // TX (Upload)
                            drawPath(txFill, txBrush)
                            drawPath(txLine, ChartTxColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }
            )
        }
    }
}

private fun fillPathLine(line: Path, fill: Path, coords: List<Offset>, h: Float) {
    line.moveTo(coords.first().x, coords.first().y)
    fill.moveTo(coords.first().x, h)
    fill.lineTo(coords.first().x, coords.first().y)

    for (i in 0 until coords.size - 1) {
        val p0 = coords[i]
        val p1 = coords[i + 1]
        val cx = (p0.x + p1.x) / 2f
        line.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
        fill.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
    }
    fill.lineTo(coords.last().x, h)
    fill.close()
}
