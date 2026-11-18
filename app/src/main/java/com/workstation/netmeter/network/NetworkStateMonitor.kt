package com.workstation.netmeter.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Transport { WIFI, MOBILE, ETHERNET, NONE }

/** Everything the UI needs to know about the current network. */
data class NetworkState(
    val transport: Transport = Transport.NONE,
    val validated: Boolean = false,
    val ssid: String? = null,
    val signalLevel: Int? = null,
    val linkSpeedMbps: Int? = null,
) {
    val isConnected: Boolean get() = transport != Transport.NONE
}

/**
 * Observes connectivity changes with [ConnectivityManager.NetworkCallback] and
 * exposes an immutable [NetworkState].
 *
 * "Internet available" is derived from NET_CAPABILITY_VALIDATED, i.e. the
 * network actually passed Android's captive-portal / connectivity probe. A
 * connected Wi-Fi without working internet is therefore reported as
 * not validated instead of being assumed to work.
 */
class NetworkStateMonitor(private val context: Context) {

    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(ConnectivityManager::class.java)

    private val _state = MutableStateFlow(readCurrentState())
    val state: StateFlow<NetworkState> = _state.asStateFlow()

    private var registered = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refresh()
        override fun onLost(network: Network) = refresh()
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) = refresh()
    }

    fun start() {
        val manager = connectivityManager ?: return
        if (registered) return
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            manager.registerNetworkCallback(request, callback)
            registered = true
            refresh()
        } catch (t: Throwable) {
            registered = false
        }
    }

    fun stop() {
        val manager = connectivityManager ?: return
        if (!registered) return
        try {
            manager.unregisterNetworkCallback(callback)
        } catch (t: Throwable) {
            // Already unregistered.
        }
        registered = false
    }

    /** Re-read the active network; safe to call at any time. */
    fun refresh() {
        _state.value = readCurrentState()
    }

    fun readCurrentState(): NetworkState {
        val manager = connectivityManager ?: return NetworkState()
        return try {
            val network = manager.activeNetwork ?: return NetworkState()
            val caps = manager.getNetworkCapabilities(network) ?: return NetworkState()
            val transport = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Transport.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Transport.MOBILE
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Transport.ETHERNET
                else -> Transport.NONE
            }
            val wifiInfo = if (transport == Transport.WIFI) wifiInfo(caps) else null
            NetworkState(
                transport = transport,
                validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                ssid = wifiInfo?.let { readableSsid(it) },
                signalLevel = wifiInfo?.let { signalLevel(it.rssi) },
                linkSpeedMbps = wifiInfo?.linkSpeed,
            )
        } catch (t: Throwable) {
            NetworkState()
        }
    }

    private fun wifiInfo(caps: NetworkCapabilities): WifiInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            caps.transportInfo as? WifiInfo
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.connectionInfo
        }
    } catch (t: Throwable) {
        null
    }

    private fun readableSsid(info: WifiInfo): String? {
        if (!hasLocationPermission()) return null
        return try {
            val raw = info.ssid ?: return null
            val cleaned = raw.removeSurrounding("\"")
            if (cleaned.isEmpty() || cleaned.equals(UNKNOWN_SSID, ignoreCase = true)) null else cleaned
        } catch (t: Throwable) {
            null
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun signalLevel(rssi: Int): Int = when {
        rssi >= -55 -> 4
        rssi >= -65 -> 3
        rssi >= -75 -> 2
        rssi >= -85 -> 1
        else -> 0
    }

    private companion object {
        const val UNKNOWN_SSID = "<unknown ssid>"
    }
}
