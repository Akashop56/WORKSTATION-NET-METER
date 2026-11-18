package com.workstation.netmeter.network

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedCalculatorTest {

    @Test
    fun `first sample only stores counters`() {
        val calculator = SpeedCalculator()
        val sample = calculator.sample(rxBytes = 0L, txBytes = 0L, elapsedMs = 0L)
        assertEquals(SpeedSample.ZERO, sample)
    }

    @Test
    fun `download and upload speed use the real elapsed time`() {
        val calculator = SpeedCalculator()
        calculator.sample(rxBytes = 0L, txBytes = 0L, elapsedMs = 0L)
        val sample = calculator.sample(rxBytes = 5_000L, txBytes = 2_500L, elapsedMs = 500L)
        assertEquals(10_000L, sample.downloadBytesPerSecond)
        assertEquals(5_000L, sample.uploadBytesPerSecond)
    }

    @Test
    fun `one second interval produces bytes per second`() {
        val calculator = SpeedCalculator()
        calculator.sample(rxBytes = 1_000L, txBytes = 500L, elapsedMs = 1_000L)
        val sample = calculator.sample(rxBytes = 2_000L, txBytes = 1_000L, elapsedMs = 2_000L)
        assertEquals(1_000L, sample.downloadBytesPerSecond)
        assertEquals(500L, sample.uploadBytesPerSecond)
    }

    @Test
    fun `zero traffic reports zero`() {
        val calculator = SpeedCalculator()
        calculator.sample(rxBytes = 9_000L, txBytes = 8_000L, elapsedMs = 0L)
        val sample = calculator.sample(rxBytes = 9_000L, txBytes = 8_000L, elapsedMs = 1_000L)
        assertEquals(SpeedSample.ZERO, sample)
    }

    @Test
    fun `non positive elapsed time reports zero`() {
        val calculator = SpeedCalculator()
        calculator.sample(rxBytes = 0L, txBytes = 0L, elapsedMs = 5_000L)
        val sample = calculator.sample(rxBytes = 1_000L, txBytes = 0L, elapsedMs = 5_000L)
        assertEquals(0L, sample.downloadBytesPerSecond)
    }

    @Test
    fun `negative counter delta is treated as a counter reset`() {
        val calculator = SpeedCalculator()
        calculator.sample(rxBytes = 1_000L, txBytes = 0L, elapsedMs = 0L)
        val reset = calculator.sample(rxBytes = 500L, txBytes = 0L, elapsedMs = 1_000L)
        assertEquals(SpeedSample.ZERO, reset)

        // The calculator re-baselines and measures normally afterwards.
        val after = calculator.sample(rxBytes = 1_500L, txBytes = 0L, elapsedMs = 2_000L)
        assertEquals(1_000L, after.downloadBytesPerSecond)
    }

    @Test
    fun `unsupported traffic stats values reset the session`() {
        val calculator = SpeedCalculator()
        calculator.sample(rxBytes = 1_000L, txBytes = 1_000L, elapsedMs = 0L)
        val unsupported = calculator.sample(
            rxBytes = SpeedCalculator.UNSUPPORTED,
            txBytes = SpeedCalculator.UNSUPPORTED,
            elapsedMs = 1_000L,
        )
        assertEquals(SpeedSample.ZERO, unsupported)

        // Next reading is treated as a fresh baseline.
        calculator.sample(rxBytes = 200L, txBytes = 100L, elapsedMs = 2_000L)
        val sample = calculator.sample(rxBytes = 400L, txBytes = 200L, elapsedMs = 3_000L)
        assertEquals(200L, sample.downloadBytesPerSecond)
        assertEquals(100L, sample.uploadBytesPerSecond)
    }

    @Test
    fun `smoothing blends successive readings`() {
        val calculator = SpeedCalculator(smoothingEnabled = true, smoothingFactor = 0.5)
        calculator.sample(rxBytes = 0L, txBytes = 0L, elapsedMs = 0L)
        val first = calculator.sample(rxBytes = 1_000L, txBytes = 0L, elapsedMs = 1_000L)
        assertEquals(1_000L, first.downloadBytesPerSecond)

        val second = calculator.sample(rxBytes = 3_000L, txBytes = 0L, elapsedMs = 2_000L)
        // 0.5 * 2000 (instant) + 0.5 * 1000 (previous) = 1500
        assertEquals(1_500L, second.downloadBytesPerSecond)
    }

    @Test
    fun `large deltas do not overflow`() {
        val delta = Long.MAX_VALUE / 2_000
        assertEquals(delta, SpeedCalculator.bytesPerSecond(delta, 1_000L))
    }

    @Test
    fun `very large deltas fall back to a safe division order`() {
        val delta = Long.MAX_VALUE / 100
        val result = SpeedCalculator.bytesPerSecond(delta, 10_000L)
        assertEquals(9_223_372_036_854_000L, result)
    }

    @Test
    fun `reset clears the stored counters`() {
        val calculator = SpeedCalculator()
        calculator.sample(rxBytes = 1_000L, txBytes = 1_000L, elapsedMs = 0L)
        calculator.reset()
        val sample = calculator.sample(rxBytes = 2_000L, txBytes = 2_000L, elapsedMs = 1_000L)
        assertEquals(SpeedSample.ZERO, sample)
    }
}
