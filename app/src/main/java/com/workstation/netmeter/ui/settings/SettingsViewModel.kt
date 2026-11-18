package com.workstation.netmeter.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.workstation.netmeter.NetMeterApp
import com.workstation.netmeter.network.NetworkState
import com.workstation.netmeter.service.SpeedStateHolder
import com.workstation.netmeter.settings.AppLanguage
import com.workstation.netmeter.settings.AppSettings
import com.workstation.netmeter.settings.NotificationAccent
import com.workstation.netmeter.settings.TapAction
import com.workstation.netmeter.settings.ThemeMode
import com.workstation.netmeter.usage.DataUsage
import com.workstation.netmeter.util.SpeedUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NetMeterApp

    val settings: StateFlow<AppSettings> = app.settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings(),
    )

    val network: StateFlow<NetworkState> = SpeedStateHolder.network

    val usage: StateFlow<DataUsage> = app.usageManager.usage.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DataUsage(),
    )

    fun setHideOnLockScreen(value: Boolean) = edit { app.settingsRepository.setHideOnLockScreen(value) }
    fun setNotifyOnlyWhenConnected(value: Boolean) =
        edit { app.settingsRepository.setNotifyOnlyWhenConnected(value) }
    fun setShowUpDown(value: Boolean) = edit { app.settingsRepository.setShowUpDown(value) }
    fun setTapAction(value: TapAction) = edit { app.settingsRepository.setTapAction(value) }
    fun setSpeedUnit(value: SpeedUnit) = edit { app.settingsRepository.setSpeedUnit(value) }
    fun setAccent(value: NotificationAccent) = edit { app.settingsRepository.setAccent(value) }
    fun setTheme(value: ThemeMode) = edit { app.settingsRepository.setTheme(value) }
    fun setLanguage(value: AppLanguage) = edit { app.settingsRepository.setLanguage(value) }
    fun setMonthlyLimitMb(value: Int) = edit { app.settingsRepository.setMonthlyLimitMb(value) }
    fun setStartingHour(value: Int) = edit { app.settingsRepository.setStartingHour(value) }
    fun setStartOnBoot(value: Boolean) = edit { app.settingsRepository.setStartOnBoot(value) }
    fun setSamplingIntervalMs(value: Long) = edit { app.settingsRepository.setSamplingIntervalMs(value) }

    fun resetUsage() = edit { app.usageManager.resetUsage() }

    private fun edit(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
        }
    }
}
