package com.workstation.netmeter

import android.app.Application
import com.workstation.netmeter.settings.SettingsRepository
import com.workstation.netmeter.settings.settingsDataStore
import com.workstation.netmeter.usage.DataUsageManager
import com.workstation.netmeter.usage.usageDataStore

/**
 * Application entry point. Owns the process-wide singletons so the service,
 * the receiver and the UI share the same DataStore-backed repositories.
 */
class NetMeterApp : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var usageManager: DataUsageManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsRepository = SettingsRepository(settingsDataStore)
        usageManager = DataUsageManager(usageDataStore)
    }

    companion object {
        lateinit var instance: NetMeterApp
            private set
    }
}
