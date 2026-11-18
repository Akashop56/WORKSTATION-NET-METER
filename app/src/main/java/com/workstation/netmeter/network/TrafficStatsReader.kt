package com.workstation.netmeter.network

import android.net.TrafficStats
import android.os.SystemClock

/**
 * Abstraction over the cumulative traffic counters so the calculator can be
 * tested without Android and so alternative data sources can be plugged in.
 */
interface TrafficStatsReader {
    /** Cumulative received bytes since boot, or [SpeedCalculator.UNSUPPORTED]. */
    fun rxBytes(): Long

    /** Cumulative transmitted bytes since boot, or [SpeedCalculator.UNSUPPORTED]. */
    fun txBytes(): Long

    /** Monotonic clock used to measure the sampling interval. */
    fun elapsedMs(): Long
}

/** Production implementation backed by [TrafficStats]. */
class SystemTrafficStatsReader : TrafficStatsReader {

    override fun rxBytes(): Long = TrafficStats.getTotalRxBytes()

    override fun txBytes(): Long = TrafficStats.getTotalTxBytes()

    override fun elapsedMs(): Long = SystemClock.elapsedRealtime()
}
