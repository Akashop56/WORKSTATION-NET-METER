package com.workstation.netmeter.util

/**
 * The speed units offered by the app.
 *
 * All conversions are decimal (SI) based: 1 KB = 1000 B, 1 Kb = 1000 b.
 * [bytesPerUnit] is the number of bytes that a single unit represents, which
 * makes converting a bytes-per-second value a single division.
 */
enum class SpeedUnit(
    val key: String,
    val displayLabel: String,
    val bytesPerUnit: Double,
) {
    BYTES_PER_SECOND("B/s", "B/s", 1.0),
    KILOBYTES_PER_SECOND("KB/s", "KB/s", 1_000.0),
    MEGABYTES_PER_SECOND("MB/s", "MB/s", 1_000_000.0),
    BITS_PER_SECOND("bps", "bps", 1.0 / 8.0),
    KILOBITS_PER_SECOND("Kbps", "Kbps", 1_000.0 / 8.0),
    MEGABITS_PER_SECOND("Mbps", "Mbps", 1_000_000.0 / 8.0);

    /** Value of [bytesPerSecond] expressed in this unit. */
    fun convert(bytesPerSecond: Long): Double =
        if (bytesPerSecond <= 0L) 0.0 else bytesPerSecond / bytesPerUnit

    companion object {
        val DEFAULT = BYTES_PER_SECOND

        /** Keys are unambiguous ("MB/s" vs "Mbps"), so matching is case-sensitive. */
        fun fromKey(key: String?): SpeedUnit =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
