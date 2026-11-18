package com.workstation.netmeter.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedUnitTest {

    @Test
    fun `byte units are decimal`() {
        assertEquals(850.0, SpeedUnit.BYTES_PER_SECOND.convert(850L), 0.0001)
        assertEquals(2.0, SpeedUnit.KILOBYTES_PER_SECOND.convert(2_000L), 0.0001)
        assertEquals(1.5, SpeedUnit.MEGABYTES_PER_SECOND.convert(1_500_000L), 0.0001)
    }

    @Test
    fun `bit units convert from bytes`() {
        assertEquals(8.0, SpeedUnit.BITS_PER_SECOND.convert(1L), 0.0001)
        assertEquals(1.0, SpeedUnit.KILOBITS_PER_SECOND.convert(125L), 0.0001)
        assertEquals(1.0, SpeedUnit.MEGABITS_PER_SECOND.convert(125_000L), 0.0001)
    }

    @Test
    fun `zero and negative values convert to zero`() {
        assertEquals(0.0, SpeedUnit.KILOBYTES_PER_SECOND.convert(0L), 0.0001)
        assertEquals(0.0, SpeedUnit.KILOBYTES_PER_SECOND.convert(-1L), 0.0001)
    }

    @Test
    fun `unknown keys fall back to the default unit`() {
        assertEquals(SpeedUnit.BYTES_PER_SECOND, SpeedUnit.fromKey(null))
        assertEquals(SpeedUnit.BYTES_PER_SECOND, SpeedUnit.fromKey("does-not-exist"))
        assertEquals(SpeedUnit.MEGABYTES_PER_SECOND, SpeedUnit.fromKey("MB/s"))
        assertEquals(SpeedUnit.MEGABITS_PER_SECOND, SpeedUnit.fromKey("Mbps"))
        // The ambiguous "MBPS" spelling must not silently mean megabits.
        assertEquals(SpeedUnit.BYTES_PER_SECOND, SpeedUnit.fromKey("MBPS"))
    }
}
