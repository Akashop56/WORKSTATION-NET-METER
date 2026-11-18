package com.workstation.netmeter.util

import java.util.Locale

/**
 * Pure formatting helpers for throughput and data-volume values.
 *
 * Everything here is JVM-only so it can be covered by plain unit tests.
 */
object SpeedFormatter {

    private const val KILO = 1_000L
    private const val MEGA = 1_000_000L
    private const val GIGA = 1_000_000_000L

    /** Full text used in the dashboard and the expanded notification, e.g. "223 KB/s". */
    fun format(bytesPerSecond: Long, unit: SpeedUnit): String {
        val value = unit.convert(bytesPerSecond)
        return when (unit) {
            SpeedUnit.BYTES_PER_SECOND,
            SpeedUnit.BITS_PER_SECOND,
            -> "${value.toLong()} ${unit.displayLabel}"
            else -> "${formatDecimal(value)} ${unit.displayLabel}"
        }
    }

    /**
     * Compact text used inside the status-bar icon, e.g. "850B", "45K", "1.2M", "10M".
     * The unit is chosen automatically so the text always stays short.
     */
    fun formatCompact(bytesPerSecond: Long): String {
        val bytes = bytesPerSecond.coerceAtLeast(0L)
        return when {
            bytes < KILO -> "${bytes}B"
            bytes < MEGA -> "${formatDecimal(bytes.toDouble() / KILO)}K"
            bytes < GIGA -> "${formatDecimal(bytes.toDouble() / MEGA)}M"
            else -> "${formatDecimal(bytes.toDouble() / GIGA)}G"
        }
    }

    /** Human readable volume, e.g. "1.2 GB". */
    fun formatTotal(bytes: Long): String {
        val value = bytes.coerceAtLeast(0L)
        return when {
            value < KILO -> "$value B"
            value < MEGA -> "${formatDecimal(value.toDouble() / KILO)} KB"
            value < GIGA -> "${formatDecimal(value.toDouble() / MEGA)} MB"
            else -> "${formatDecimal(value.toDouble() / GIGA)} GB"
        }
    }

    /** 0-9.99 -> one decimal, >= 10 -> integer. A trailing ".0" is stripped. */
    fun formatDecimal(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        val text = if (value < 10.0) {
            String.format(Locale.US, "%.1f", value)
        } else {
            String.format(Locale.US, "%.0f", value)
        }
        return if (text.endsWith(".0")) text.dropLast(2) else text
    }
}
