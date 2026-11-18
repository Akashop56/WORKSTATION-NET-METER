package com.workstation.netmeter.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.workstation.netmeter.R
import com.workstation.netmeter.network.NetworkState
import com.workstation.netmeter.network.SpeedSample
import com.workstation.netmeter.service.SpeedMonitorService
import com.workstation.netmeter.settings.AppSettings
import com.workstation.netmeter.settings.NotificationAccent
import com.workstation.netmeter.settings.TapAction
import com.workstation.netmeter.MainActivity
import com.workstation.netmeter.util.SpeedFormatter

/**
 * Creates and refreshes the ongoing foreground notification whose small icon
 * is the live status-bar indicator.
 */
class SpeedNotificationManager(
    private val context: Context,
    private val iconRenderer: SpeedIconRenderer = SpeedIconRenderer(),
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    /** Creates the silent, low-importance channel. Safe to call repeatedly. */
    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun buildNotification(
        sample: SpeedSample,
        settings: AppSettings,
        network: NetworkState,
        localizedContext: Context,
    ): Notification {
        val showSpeed = !settings.notifyOnlyWhenConnected || network.validated
        val text = when {
            !showSpeed -> localizedContext.getString(R.string.notification_waiting)
            settings.showUpDown -> localizedContext.getString(
                R.string.notification_text_both,
                SpeedFormatter.format(sample.downloadBytesPerSecond, settings.speedUnit),
                SpeedFormatter.format(sample.uploadBytesPerSecond, settings.speedUnit),
            )
            else -> localizedContext.getString(
                R.string.notification_text_download,
                SpeedFormatter.format(sample.downloadBytesPerSecond, settings.speedUnit),
            )
        }

        val smallIcon = if (settings.accent == NotificationAccent.CLASSIC) {
            IconCompat.createWithResource(context, R.drawable.ic_stat_netmeter)
        } else {
            IconCompat.createWithBitmap(
                iconRenderer.render(
                    sample.downloadBytesPerSecond,
                    sample.uploadBytesPerSecond,
                    settings.showUpDown,
                ),
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(localizedContext.getString(R.string.notification_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(
                if (settings.hideOnLockScreen) {
                    NotificationCompat.VISIBILITY_PRIVATE
                } else {
                    NotificationCompat.VISIBILITY_PUBLIC
                },
            )
            .setContentIntent(contentIntent(settings, localizedContext))
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            notification.foregroundServiceBehavior = Notification.FOREGROUND_SERVICE_IMMEDIATE
            notification
        } else {
            notification
        }
    }

    private fun contentIntent(settings: AppSettings, localizedContext: Context): PendingIntent? {
        return when (settings.tapAction) {
            TapAction.NONE -> null
            TapAction.STOP -> PendingIntent.getService(
                localizedContext,
                REQUEST_CODE_STOP,
                Intent(localizedContext, SpeedMonitorService::class.java)
                    .setAction(SpeedMonitorService.ACTION_STOP),
                pendingIntentFlags(),
            )
            TapAction.OPEN_APP -> PendingIntent.getActivity(
                localizedContext,
                REQUEST_CODE_OPEN,
                Intent(localizedContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                pendingIntentFlags(),
            )
        }
    }

    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    /** Updates the notification if notifications are enabled for the app. */
    fun notify(notification: Notification) {
        if (!notificationManager.areNotificationsEnabled()) return
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (t: Throwable) {
            // POST_NOTIFICATIONS can be revoked between the check and the call.
        }
    }

    companion object {
        const val CHANNEL_ID = "netmeter_speed_monitor"
        const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE_OPEN = 10
        private const val REQUEST_CODE_STOP = 11
    }
}
