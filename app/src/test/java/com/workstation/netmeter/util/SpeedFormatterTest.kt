package com.workstation.netmeter.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedFormatterTest {

    @Test
    fun `bytes per second formatting`() {
        assertEquals("850 B/s", SpeedFormatter.format(850L, SpeedUnit.BYTES_PER_SECOND))
        assertEquals("0 B/s", SpeedFormatter.format(0L, SpeedUnit.BYTES_PER_SECOND))
    }

    @Test
    fun `kilobytes per second formatting`() {
        assertEquals("223 KB/s", SpeedFormatter.format(223_000L, SpeedUnit.KILOBYTES_PER_SECOND))
        assertEquals("1.2 KB/s", SpeedFormatter.format(1_240L, SpeedUnit.KILOBYTES_PER_SECOND))
    }

    @Test
    fun `megabytes per second formatting`() {
        assertEquals("1.2 MB/s", SpeedFormatter.format(1_200_000L, SpeedUnit.MEGABYTES_PER_SECOND))
    }

    @Test
    fun `bits based formatting`() {
        // 12_500_000 B/s = 100_000_000 bit/s = 100 Mbps
        assertEquals("100 Mbps", SpeedFormatter.format(12_500_000L, SpeedUnit.MEGABITS_PER_SECOND))
        assertEquals("20 Kbps", SpeedFormatter.format(2_500L, SpeedUnit.KILOBITS_PER_SECOND))
        assertEquals("64 bps", SpeedFormatter.format(8L, SpeedUnit.BITS_PER_SECOND))
    }

    @Test
    fun `compact formatting auto scales`() {
        assertEquals("0B", SpeedFormatter.formatCompact(0L))
        assertEquals("850B", SpeedFormatter.formatCompact(850L))
        assertEquals("45K", SpeedFormatter.formatCompact(45_000L))
        assertEquals("223K", SpeedFormatter.formatCompact(223_000L))
        assertEquals("1.2M", SpeedFormatter.formatCompact(1_200_000L))
        assertEquals("10M", SpeedFormatter.formatCompact(10_000_000L))
    }

    @Test
    fun `compact formatting never goes negative`() {
        assertEquals("0B", SpeedFormatter.formatCompact(-500L))
    }

    @Test
    fun `total formatting uses kb mb gb`() {
        assertEquals("0 B", SpeedFormatter.formatTotal(0L))
        assertEquals("500 KB", SpeedFormatter.formatTotal(500_000L))
        assertEquals("2.5 MB", SpeedFormatter.formatTotal(2_500_000L))
        assertEquals("1.5 GB", SpeedFormatter.formatTotal(1_500_000_000L))
    }

    @Test
    fun `decimal helper strips trailing zero`() {
        assertEquals("1", SpeedFormatter.formatDecimal(1.0))
        assertEquals("1.2", SpeedFormatter.formatDecimal(1.24))
        assertEquals("223", SpeedFormatter.formatDecimal(223.4))
        assertEquals("0", SpeedFormatter.formatDecimal(Double.NaN))
    }
}
