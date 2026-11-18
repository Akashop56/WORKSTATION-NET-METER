package com.workstation.netmeter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.workstation.netmeter.NetMeterApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restarts the monitor after a reboot when the user enabled "Start on Boot"
 * and monitoring was on.
 *
 * The receiver is deliberately not direct-boot aware: the DataStore files live
 * in credential-protected storage and cannot be read before the first unlock.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? NetMeterApp
                val repository = app?.settingsRepository
                if (repository != null) {
                    val settings = repository.settings.first()
                    if (settings.startOnBoot && settings.monitoringEnabled) {
                        SpeedServiceController.start(context)
                    }
                }
            } catch (t: Throwable) {
                // Never crash the boot process.
            } finally {
                pendingResult.finish()
            }
        }
    }
}
