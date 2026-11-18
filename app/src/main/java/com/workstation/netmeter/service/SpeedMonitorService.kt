package com.workstation.netmeter.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.workstation.netmeter.NetMeterApp
import com.workstation.netmeter.network.NetworkState
import com.workstation.netmeter.network.NetworkStateMonitor
import com.workstation.netmeter.network.SpeedCalculator
import com.workstation.netmeter.network.SpeedSample
import com.workstation.netmeter.network.SystemTrafficStatsReader
import com.workstation.netmeter.network.TrafficStatsReader
import com.workstation.netmeter.notification.SpeedNotificationManager
import com.workstation.netmeter.settings.AppSettings
import com.workstation.netmeter.settings.LocaleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Foreground service that samples the cumulative traffic counters once per
 * second and publishes the result to:
 *  1. [SpeedStateHolder], observed by the dashboard, and
 *  2. the ongoing notification, whose small icon is rendered live and shown
 *     by Android in the status bar.
 *
 * The service is safe to start repeatedly: [onStartCommand] is idempotent and
 * the tick loop is only started when it is not already running.
 */
class SpeedMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null
    private var settingsJob: Job? = null

    private val trafficReader: TrafficStatsReader = SystemTrafficStatsReader()
    private val calculator = SpeedCalculator()
    private val networkMonitor: NetworkStateMonitor by lazy { NetworkStateMonitor(this) }
    private val notificationManager: SpeedNotificationManager by lazy {
        SpeedNotificationManager(this)
    }

    @Volatile
    private var settings: AppSettings = AppSettings()

    @Volatile
    private var localizedContext: Context = LocaleManager.wrapWithStored(this)

    @Volatile
    private var networkState: NetworkState = NetworkState()

    // Cumulative counter bookkeeping (reboot / counter-reset detection + batching).
    private var lastRx: Long = UNSET
    private var lastTx: Long = UNSET
    private var lastElapsed: Long = UNSET
    private var lastFlushElapsed: Long = UNSET
    private var pendingRx: Long = 0L
    private var pendingTx: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        localizedContext = LocaleManager.wrapWithStored(this)
        notificationManager.createChannel()
        networkMonitor.start()

        settingsJob = scope.launch {
            NetMeterApp.instance.settingsRepository.settings.collect { updated ->
                val languageChanged = updated.language != settings.language
                settings = updated
                if (languageChanged) {
                    localizedContext = LocaleManager.wrap(this@SpeedMonitorService, updated.language)
                }
            }
        }

        scope.launch {
            networkMonitor.state.collect { state ->
                networkState = state
                SpeedStateHolder.updateNetwork(state)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopMonitoring()
            return START_NOT_STICKY
        }

        notificationManager.createChannel()
        startForegroundWith(sample = SpeedSample.ZERO)
        updateServiceState()
        startTicking()
        return START_STICKY
    }

    override fun onDestroy() {
        tickJob?.cancel()
        settingsJob?.cancel()
        runCatching { flushUsage() }
        scope.cancel()
        networkMonitor.stop()
        calculator.reset()
        resetCounters()
        SpeedStateHolder.updateServiceState(ServiceState.STOPPED)
        super.onDestroy()
    }

    private fun startTicking() {
        if (tickJob?.isActive == true) return
        tickJob = scope.launch {
            while (true) {
                tickOnce()
                delay(settings.samplingIntervalMs)
            }
        }
    }

    private suspend fun tickOnce() {
        val rx = trafficReader.rxBytes()
        val tx = trafficReader.txBytes()
        val elapsed = trafficReader.elapsedMs()

        val sample = calculator.sample(rx, tx, elapsed)
        SpeedStateHolder.updateSpeed(sample)
        SpeedStateHolder.updateTotals(rx, tx)

        accumulateUsage(rx, tx, elapsed)
        if (lastFlushElapsed == UNSET || elapsed - lastFlushElapsed >= USAGE_FLUSH_INTERVAL_MS) {
            flushUsage()
            lastFlushElapsed = elapsed
        }

        val notification = notificationManager.buildNotification(
            sample = sample,
            settings = settings,
            network = networkState,
            localizedContext = localizedContext,
        )
        notificationManager.notify(notification)
    }

    private fun accumulateUsage(rx: Long, tx: Long, elapsed: Long) {
        val reboot = lastElapsed != UNSET && elapsed < lastElapsed
        if (lastRx != UNSET && lastTx != UNSET && !reboot) {
            if (rx >= lastRx && tx >= lastTx) {
                pendingRx += rx - lastRx
                pendingTx += tx - lastTx
            }
        }
        lastRx = rx
        lastTx = tx
        lastElapsed = elapsed
    }

    private fun flushUsage() {
        if (pendingRx == 0L && pendingTx == 0L) return
        runBlocking {
            runCatching {
                NetMeterApp.instance.usageManager.addUsage(
                    pendingRx,
                    pendingTx,
                    settings.startingHour,
                )
            }
        }
        pendingRx = 0L
        pendingTx = 0L
    }

    private fun startForegroundWith(sample: SpeedSample) {
        val notification = notificationManager.buildNotification(
            sample = sample,
            settings = settings,
            network = networkState,
            localizedContext = localizedContext,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                SpeedNotificationManager.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(SpeedNotificationManager.NOTIFICATION_ID, notification)
        }
    }

    private fun updateServiceState() {
        val state = if (settings.notifyOnlyWhenConnected && !networkState.validated) {
            ServiceState.WAITING_FOR_NETWORK
        } else {
            ServiceState.ACTIVE
        }
        SpeedStateHolder.updateServiceState(state)
    }

    private fun stopMonitoring() {
        runCatching { flushUsage() }
        stopSelf()
    }

    private fun resetCounters() {
        lastRx = UNSET
        lastTx = UNSET
        lastElapsed = UNSET
        lastFlushElapsed = UNSET
        pendingRx = 0L
        pendingTx = 0L
    }

    companion object {
        const val ACTION_STOP = "com.workstation.netmeter.action.STOP"

        private const val USAGE_FLUSH_INTERVAL_MS = 10_000L
        private const val UNSET = Long.MIN_VALUE
    }
}
