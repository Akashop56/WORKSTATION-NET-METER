package com.workstation.netmeter.usage

/** Aggregated data usage for the current day and month. */
data class DataUsage(
    val monthRxBytes: Long = 0L,
    val monthTxBytes: Long = 0L,
    val dayRxBytes: Long = 0L,
    val dayTxBytes: Long = 0L,
    val periodStartMs: Long = 0L,
    val monthStartMs: Long = 0L,
    val mobileRxBytes: Long? = null,
    val mobileTxBytes: Long? = null,
) {
    val monthTotalBytes: Long get() = monthRxBytes + monthTxBytes
    val dayTotalBytes: Long get() = dayRxBytes + dayTxBytes

    val hasMobileBreakdown: Boolean get() = mobileRxBytes != null && mobileTxBytes != null

    companion object {
        const val BYTES_PER_MB = 1_000_000L
    }
}
