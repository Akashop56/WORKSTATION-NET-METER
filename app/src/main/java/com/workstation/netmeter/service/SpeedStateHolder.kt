package com.workstation.netmeter.service

import com.workstation.netmeter.network.NetworkState
import com.workstation.netmeter.network.SpeedSample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ServiceState { STOPPED, ACTIVE, WAITING_FOR_NETWORK }

/**
 * Process-wide live state shared between the foreground service and the UI.
 *
 * The service writes, the dashboard collects. A plain object keeps the app
 * lightweight (no DI framework) while guaranteeing both sides see the same
 * instance inside the single app process.
 */
object SpeedStateHolder {

    private val _speed = MutableStateFlow(SpeedSample.ZERO)
    val speed: StateFlow<SpeedSample> = _speed.asStateFlow()

    private val _serviceState = MutableStateFlow(ServiceState.STOPPED)
    val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

    private val _network = MutableStateFlow(NetworkState())
    val network: StateFlow<NetworkState> = _network.asStateFlow()

    private val _totals = MutableStateFlow(TotalTraffic(0L, 0L))
    val totals: StateFlow<TotalTraffic> = _totals.asStateFlow()

    fun updateSpeed(sample: SpeedSample) {
        _speed.value = sample
    }

    fun updateNetwork(state: NetworkState) {
        _network.value = state
    }

    fun updateTotals(rxBytes: Long, txBytes: Long) {
        _totals.value = TotalTraffic(rxBytes, txBytes)
    }

    /** Cumulative traffic counters since the device booted. */
    data class TotalTraffic(val rxBytes: Long, val txBytes: Long) {
        val total: Long get() = rxBytes + txBytes
    }

    fun updateServiceState(state: ServiceState) {
        _serviceState.value = state
        if (state == ServiceState.STOPPED) {
            _speed.value = SpeedSample.ZERO
        }
    }
}
