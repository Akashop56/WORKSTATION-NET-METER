package com.workstation.netmeter.service

import android.content.Context
import android.content.Intent
import android.os.Build

/** Thin helper that starts/stops [SpeedMonitorService] from the UI and the boot receiver. */
object SpeedServiceController {

    fun start(context: Context) {
        val intent = Intent(context, SpeedMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, SpeedMonitorService::class.java))
    }
}
