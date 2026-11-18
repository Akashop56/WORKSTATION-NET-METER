package com.workstation.netmeter.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class UsageScheduleTest {

    private val zone = ZoneId.of("UTC")

    private fun schedule(hour: Int = 6) = UsageSchedule(startingHour = hour, zone = zone)

    private fun at(date: String, time: String): Long =
        ZonedDateTime.of(LocalDate.parse(date), LocalTime.parse(time), zone)
            .toInstant().toEpochMilli()

    @Test
    fun `daily boundary respects the configured starting hour`() {
        assertEquals(at("2026-11-18", "06:00"), schedule().currentDayStart(at("2026-11-18", "10:00")))
        assertEquals(at("2026-11-17", "06:00"), schedule().currentDayStart(at("2026-11-18", "05:59")))
    }

    @Test
    fun `midnight starting hour uses the calendar day`() {
        assertEquals(at("2026-11-18", "00:00"), schedule(0).currentDayStart(at("2026-11-18", "00:00")))
        assertEquals(at("2026-11-17", "00:00"), schedule(0).currentDayStart(at("2026-11-18", "00:00") - 1))
    }

    @Test
    fun `monthly boundary is the first of the month at the starting hour`() {
        assertEquals(at("2026-11-01", "06:00"), schedule().currentMonthStart(at("2026-11-18", "10:00")))
        // Before the first boundary of the month the previous month is still current.
        assertEquals(at("2026-10-01", "06:00"), schedule().currentMonthStart(at("2026-11-01", "05:00")))
        assertEquals(at("2026-11-01", "06:00"), schedule().currentMonthStart(at("2026-11-01", "06:00")))
    }

    @Test
    fun `daily reset triggers only after the boundary passes`() {
        val previous = at("2026-11-17", "06:00")
        assertFalse(schedule().dayResetDue(previous, at("2026-11-18", "05:59")))
        assertTrue(schedule().dayResetDue(previous, at("2026-11-18", "06:00")))
    }

    @Test
    fun `monthly reset triggers only after the boundary passes`() {
        val previous = at("2026-10-01", "06:00")
        assertFalse(schedule().monthResetDue(previous, at("2026-11-01", "05:59")))
        assertTrue(schedule().monthResetDue(previous, at("2026-11-01", "06:00")))
    }

    @Test
    fun `invalid starting hour is rejected`() {
        try {
            UsageSchedule(startingHour = 24, zone = zone)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // Expected.
        }
    }
}
