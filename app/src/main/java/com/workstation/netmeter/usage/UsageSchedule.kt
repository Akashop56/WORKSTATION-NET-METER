package com.workstation.netmeter.usage

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pure (JVM only) calculation of the daily / monthly usage reset boundaries.
 *
 * [startingHour] is the hour of day at which a new usage period starts. The
 * current period therefore starts at [startingHour] on the current day, or on
 * the previous day if the current time is still before that hour.
 */
class UsageSchedule(
    private val startingHour: Int = DEFAULT_STARTING_HOUR,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {

    init {
        require(startingHour in 0..23) { "startingHour must be 0..23" }
    }

    /** Epoch millis of the most recent daily boundary at or before [nowMs]. */
    fun currentDayStart(nowMs: Long): Long {
        val now = Instant.ofEpochMilli(nowMs).atZone(zone)
        val boundary = now.toLocalDate().atTime(startingHour, 0).atZone(zone)
        val effective = if (now.toLocalTime() < LocalTime.of(startingHour, 0)) {
            boundary.minusDays(1)
        } else {
            boundary
        }
        return effective.toInstant().toEpochMilli()
    }

    /** Epoch millis of the start of the current monthly period at or before [nowMs]. */
    fun currentMonthStart(nowMs: Long): Long {
        val now = Instant.ofEpochMilli(nowMs).atZone(zone)
        val firstOfMonth = now.toLocalDate().withDayOfMonth(1)
        val boundary = firstOfMonth.atTime(startingHour, 0).atZone(zone)
        val effective = if (now.toLocalDate().dayOfMonth == 1 &&
            now.toLocalTime() < LocalTime.of(startingHour, 0)
        ) {
            boundary.minusMonths(1)
        } else {
            boundary
        }
        return effective.toInstant().toEpochMilli()
    }

    /** True when the daily period that started at [periodStartMs] has been superseded. */
    fun dayResetDue(periodStartMs: Long, nowMs: Long): Boolean =
        currentDayStart(nowMs) > periodStartMs

    /** True when the monthly period that started at [monthStartMs] has been superseded. */
    fun monthResetDue(monthStartMs: Long, nowMs: Long): Boolean =
        currentMonthStart(nowMs) > monthStartMs

    companion object {
        const val DEFAULT_STARTING_HOUR = 0
    }
}
