package com.workstation.netmeter.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.workstation.netmeter.NetMeterApp
import com.workstation.netmeter.network.NetworkState
import com.workstation.netmeter.network.SpeedSample
import com.workstation.netmeter.service.ServiceState
import com.workstation.netmeter.service.SpeedServiceController
import com.workstation.netmeter.service.SpeedStateHolder
import com.workstation.netmeter.settings.AppSettings
import com.workstation.netmeter.usage.DataUsage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val downloadBps: Long = 0L,
    val uploadBps: Long = 0L,
    val totalRxBytes: Long = 0L,
    val totalTxBytes: Long = 0L,
    val serviceState: ServiceState = ServiceState.STOPPED,
    val network: NetworkState = NetworkState(),
    val settings: AppSettings = AppSettings(),
    val usage: DataUsage = DataUsage(),
) {
    val isMonitoring: Boolean get() = serviceState == ServiceState.ACTIVE ||
        serviceState == ServiceState.WAITING_FOR_NETWORK
}

private data class CoreState(
    val speed: SpeedSample,
    val serviceState: ServiceState,
    val network: NetworkState,
    val totals: SpeedStateHolder.TotalTraffic,
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NetMeterApp

    val uiState: StateFlow<DashboardUiState> = combine(
        combine(
            SpeedStateHolder.speed,
            SpeedStateHolder.serviceState,
            SpeedStateHolder.network,
            SpeedStateHolder.totals,
        ) { speed, serviceState, network, totals ->
            CoreState(speed, serviceState, network, totals)
        },
        app.settingsRepository.settings,
        app.usageManager.usage,
    ) { core, settings, usage ->
        DashboardUiState(
            downloadBps = core.speed.downloadBytesPerSecond,
            uploadBps = core.speed.uploadBytesPerSecond,
            totalRxBytes = core.totals.rxBytes,
            totalTxBytes = core.totals.txBytes,
            serviceState = core.serviceState,
            network = core.network,
            settings = settings,
            usage = usage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    /** Starts or stops the foreground monitor and persists the user's choice. */
    fun setMonitoring(enabled: Boolean) {
        viewModelScope.launch {
            app.settingsRepository.setMonitoringEnabled(enabled)
            if (enabled) {
                SpeedServiceController.start(getApplication())
            } else {
                SpeedServiceController.stop(getApplication())
            }
        }
    }
}
