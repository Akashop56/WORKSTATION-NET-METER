package com.workstation.netmeter.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.workstation.netmeter.util.AppCrashReporter

/**
 * Thin helper that starts/stops [SpeedMonitorService] from the UI and the boot
 * receiver.
 *
 * Both start calls are guarded: a platform rejection (background-start
 * restriction, missing foreground-service type permission) must degrade to a
 * stopped monitor instead of killing the app.
 */
object SpeedServiceController {

    fun start(context: Context) {
        val intent = Intent(context, SpeedMonitorService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (t: Throwable) {
            AppCrashReporter.record(t, "startForegroundService")
            // The app is foregrounded when the user taps Start, so a plain
            // start is legal and the service can still promote itself.
            runCatching { context.startService(intent) }
                .onFailure { AppCrashReporter.record(it, "startService fallback") }
        }
    }

    fun stop(context: Context) {
        runCatching { context.stopService(Intent(context, SpeedMonitorService::class.java)) }
            .onFailure { AppCrashReporter.record(it, "stopService") }
    }
}
