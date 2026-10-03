package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {

    fun formatBytes(bytes: Double): String {
        if (bytes <= 0.0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        var value = bytes
        var unitIndex = 0
        while (value >= 1024.0 && unitIndex < units.size - 1) {
            value /= 1024.0
            unitIndex++
        }
        return if (value >= 100 || unitIndex == 0) {
            String.format(Locale.US, "%.0f %s", value, units[unitIndex])
        } else {
            String.format(Locale.US, "%.1f %s", value, units[unitIndex])
        }
    }

    fun formatBytes(bytes: Long): String = formatBytes(bytes.toDouble())

    /**
     * Formats bit rates (e.g. from network bytes per second * 8).
     */
    fun formatBitsPerSec(bytesPerSec: Double): String {
        if (bytesPerSec <= 0.0) return "0.0 Kbit/s"
        val bitsPerSec = bytesPerSec * 8.0
        return when {
            bitsPerSec >= 1_000_000_000.0 -> String.format(Locale.US, "%.2f Gbit/s", bitsPerSec / 1_000_000_000.0)
            bitsPerSec >= 1_000_000.0 -> String.format(Locale.US, "%.1f Mbit/s", bitsPerSec / 1_000_000.0)
            bitsPerSec >= 1_000.0 -> String.format(Locale.US, "%.1f Kbit/s", bitsPerSec / 1_000.0)
            else -> String.format(Locale.US, "%.0f bit/s", bitsPerSec)
        }
    }

    fun formatFrequency(mhzOrGhz: Double?): String {
        if (mhzOrGhz == null || mhzOrGhz <= 0.0) return "N/A"
        return if (mhzOrGhz >= 1000.0) {
            String.format(Locale.US, "%.2f GHz", mhzOrGhz / 1000.0)
        } else {
            String.format(Locale.US, "%.0f MHz", mhzOrGhz)
        }
    }

    fun formatTemperature(tempC: Double?): String {
        if (tempC == null) return "N/A"
        return String.format(Locale.US, "%.1f°C", tempC)
    }

    fun formatPercent(value: Float): String {
        return String.format(Locale.US, "%.1f%%", value.coerceIn(0f, 100f))
    }

    fun formatTime(timestamp: Long): String {
        if (timestamp <= 0L) return "Never"
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
