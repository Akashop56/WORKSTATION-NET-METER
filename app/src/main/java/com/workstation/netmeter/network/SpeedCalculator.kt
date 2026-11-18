package com.workstation.netmeter.network

/**
 * Stateful throughput calculator.
 *
 * Feed it successive cumulative byte counters together with a monotonic
 * timestamp; it returns the average speed over the interval using the real
 * elapsed time (not an assumed interval).
 *
 * Edge cases handled:
 *  - first sample after creation          -> [SpeedSample.ZERO], counters stored
 *  - [TrafficStatsReader.UNSUPPORTED]     -> reset, [SpeedSample.ZERO]
 *  - negative counter delta (reset/reboot)-> reset, [SpeedSample.ZERO]
 *  - non-positive elapsed time            -> [SpeedSample.ZERO]
 *  - zero traffic                         -> [SpeedSample.ZERO]
 *  - huge deltas                          -> overflow-safe arithmetic
 *
 * The class is pure Kotlin so it can be unit tested without Android.
 */
class SpeedCalculator(
    private val smoothingEnabled: Boolean = false,
    private val smoothingFactor: Double = DEFAULT_SMOOTHING_FACTOR,
) {

    private var lastRxBytes: Long = UNSET
    private var lastTxBytes: Long = UNSET
    private var lastElapsedMs: Long = UNSET
    private var smoothedDownload: Long = 0L
    private var smoothedUpload: Long = 0L

    /** Compute throughput for the interval ending at [elapsedMs]. */
    fun sample(rxBytes: Long, txBytes: Long, elapsedMs: Long): SpeedSample {
        if (rxBytes == UNSUPPORTED || txBytes == UNSUPPORTED) {
            reset()
            return SpeedSample.ZERO
        }

        val previousRx = lastRxBytes
        val previousTx = lastTxBytes
        val previousElapsed = lastElapsedMs

        lastRxBytes = rxBytes
        lastTxBytes = txBytes
        lastElapsedMs = elapsedMs

        if (previousRx == UNSET || previousTx == UNSET || previousElapsed == UNSET) {
            return SpeedSample.ZERO
        }

        val elapsed = elapsedMs - previousElapsed
        if (elapsed <= 0L) return SpeedSample.ZERO

        val rxDelta = rxBytes - previousRx
        val txDelta = txBytes - previousTx
        if (rxDelta < 0L || txDelta < 0L) {
            // Counter reset (reboot, unsupported device counters). Re-baseline.
            smoothedDownload = 0L
            smoothedUpload = 0L
            return SpeedSample.ZERO
        }

        val download = bytesPerSecond(rxDelta, elapsed)
        val upload = bytesPerSecond(txDelta, elapsed)
        if (!smoothingEnabled) return SpeedSample(download, upload)

        smoothedDownload = smooth(smoothedDownload, download)
        smoothedUpload = smooth(smoothedUpload, upload)
        return SpeedSample(smoothedDownload, smoothedUpload)
    }

    /** Forget all counters; the next [sample] starts a fresh interval. */
    fun reset() {
        lastRxBytes = UNSET
        lastTxBytes = UNSET
        lastElapsedMs = UNSET
        smoothedDownload = 0L
        smoothedUpload = 0L
    }

    private fun smooth(previous: Long, instant: Long): Long {
        if (previous <= 0L) return instant
        val alpha = smoothingFactor.coerceIn(0.0, 1.0)
        return (alpha * instant + (1.0 - alpha) * previous).toLong().coerceAtLeast(0L)
    }

    companion object {
        /** Value returned by TrafficStats when the counter is not available. */
        const val UNSUPPORTED = -1L

        private const val UNSET = Long.MIN_VALUE
        private const val DEFAULT_SMOOTHING_FACTOR = 0.35

        /** Overflow-safe bytes/second for [deltaBytes] over [elapsedMs]. */
        fun bytesPerSecond(deltaBytes: Long, elapsedMs: Long): Long {
            if (deltaBytes <= 0L || elapsedMs <= 0L) return 0L
            return if (deltaBytes > Long.MAX_VALUE / 1_000L) {
                deltaBytes / elapsedMs * 1_000L
            } else {
                deltaBytes * 1_000L / elapsedMs
            }
        }
    }
}
