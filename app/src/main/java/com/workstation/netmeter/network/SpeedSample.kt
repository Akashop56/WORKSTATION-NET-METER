package com.workstation.netmeter.network

/**
 * A snapshot of the current throughput, expressed in bytes per second.
 *
 * A value of zero means "no traffic in the last interval" as well as
 * "not enough samples yet" / "counter reset". Callers do not need to
 * distinguish those cases.
 */
data class SpeedSample(
    val downloadBytesPerSecond: Long,
    val uploadBytesPerSecond: Long,
) {
    companion object {
        val ZERO = SpeedSample(0L, 0L)
    }
}
