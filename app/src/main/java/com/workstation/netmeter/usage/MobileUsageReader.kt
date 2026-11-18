package com.workstation.netmeter.usage

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import androidx.core.content.getSystemService

/**
 * Reads mobile-only data usage through [NetworkStatsManager].
 *
 * This API is guarded by the special "Usage Access" app-op, so it normally
 * returns null unless the user explicitly grants it in system settings.
 * Callers must treat a null result as "not available" and must never present
 * total device traffic as mobile traffic.
 */
class MobileUsageReader(private val context: Context) {

    fun hasUsageAccess(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        return try {
            val appOps = context.getSystemService<AppOpsManager>() ?: return false
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (t: Throwable) {
            false
        }
    }

    /** @return mobile rx/tx bytes for [startMs, endMs], or null when inaccessible. */
    fun queryMobileUsage(startMs: Long, endMs: Long): Pair<Long, Long>? {
        if (!hasUsageAccess()) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        return try {
            val manager = context.getSystemService<NetworkStatsManager>() ?: return null
            val stats = manager.querySummaryForDevice(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startMs,
                endMs,
            ) ?: return null
            var rx = 0L
            var tx = 0L
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                rx += bucket.rxBytes
                tx += bucket.txBytes
            }
            stats.close()
            rx to tx
        } catch (t: Throwable) {
            null
        }
    }
}
