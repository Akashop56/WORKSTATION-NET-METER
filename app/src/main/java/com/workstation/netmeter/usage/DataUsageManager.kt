package com.workstation.netmeter.usage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Persists and accumulates data usage.
 *
 * The Android traffic counters ([android.net.TrafficStats]) are cumulative
 * since boot and cover every network, so this manager tracks *total device
 * traffic*. Mobile-only figures require the special Usage Access permission
 * and are read separately through [MobileUsageReader].
 *
 * Deltas are pushed in by the monitoring service (which owns the cumulative
 * counters and therefore the reboot / counter-reset detection). Batching the
 * deltas keeps the DataStore write rate low.
 */
class DataUsageManager(
    private val dataStore: DataStore<Preferences>,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    val usage: Flow<DataUsage> = dataStore.data.map { p ->
        DataUsage(
            monthRxBytes = p[Keys.MONTH_RX] ?: 0L,
            monthTxBytes = p[Keys.MONTH_TX] ?: 0L,
            dayRxBytes = p[Keys.DAY_RX] ?: 0L,
            dayTxBytes = p[Keys.DAY_TX] ?: 0L,
            periodStartMs = p[Keys.PERIOD_START] ?: 0L,
            monthStartMs = p[Keys.MONTH_START] ?: 0L,
        )
    }

    /**
     * Add traffic measured since the previous call.
     *
     * @param rxDelta received bytes (negative values are ignored)
     * @param txDelta transmitted bytes (negative values are ignored)
     */
    suspend fun addUsage(rxDelta: Long, txDelta: Long, startingHour: Int) {
        val rx = rxDelta.coerceAtLeast(0L)
        val tx = txDelta.coerceAtLeast(0L)

        val prefs = dataStore.data.first()
        val now = clock()
        val schedule = UsageSchedule(startingHour)

        var monthRx = prefs[Keys.MONTH_RX] ?: 0L
        var monthTx = prefs[Keys.MONTH_TX] ?: 0L
        var dayRx = prefs[Keys.DAY_RX] ?: 0L
        var dayTx = prefs[Keys.DAY_TX] ?: 0L
        var periodStart = prefs[Keys.PERIOD_START] ?: 0L
        var monthStart = prefs[Keys.MONTH_START] ?: 0L

        if (monthStart == 0L) {
            monthStart = schedule.currentMonthStart(now)
        } else if (schedule.monthResetDue(monthStart, now)) {
            monthRx = 0L
            monthTx = 0L
            monthStart = schedule.currentMonthStart(now)
        }

        if (periodStart == 0L) {
            periodStart = schedule.currentDayStart(now)
        } else if (schedule.dayResetDue(periodStart, now)) {
            dayRx = 0L
            dayTx = 0L
            periodStart = schedule.currentDayStart(now)
        }

        monthRx += rx
        monthTx += tx
        dayRx += rx
        dayTx += tx

        dataStore.edit { p ->
            p[Keys.MONTH_RX] = monthRx
            p[Keys.MONTH_TX] = monthTx
            p[Keys.DAY_RX] = dayRx
            p[Keys.DAY_TX] = dayTx
            p[Keys.PERIOD_START] = periodStart
            p[Keys.MONTH_START] = monthStart
        }
    }

    /** Clear every recorded usage figure (keeps the configured limit). */
    suspend fun resetUsage() {
        dataStore.edit { p ->
            p.remove(Keys.MONTH_RX)
            p.remove(Keys.MONTH_TX)
            p.remove(Keys.DAY_RX)
            p.remove(Keys.DAY_TX)
            p.remove(Keys.PERIOD_START)
            p.remove(Keys.MONTH_START)
        }
    }

    private companion object {
        object Keys {
            val MONTH_RX = longPreferencesKey("usage_month_rx")
            val MONTH_TX = longPreferencesKey("usage_month_tx")
            val DAY_RX = longPreferencesKey("usage_day_rx")
            val DAY_TX = longPreferencesKey("usage_day_tx")
            val PERIOD_START = longPreferencesKey("usage_period_start")
            val MONTH_START = longPreferencesKey("usage_month_start")
        }
    }
}
