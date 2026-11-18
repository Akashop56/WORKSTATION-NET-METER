package com.workstation.netmeter.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
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
import com.workstation.netmeter.util.AppCrashReporter
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

    /**
     * Builds the ongoing notification, never throwing.
     *
     * [startForeground] crashes the whole app when the notification is
     * rejected, so every step that can fail (icon rendering, resource lookup,
     * PendingIntent creation) is isolated and has a safe fallback.
     */
    fun buildNotification(
        sample: SpeedSample,
        settings: AppSettings,
        network: NetworkState,
        localizedContext: Context,
        preferBitmapIcon: Boolean = true,
    ): Notification = runCatching {
        buildNotificationUnsafe(sample, settings, network, localizedContext, preferBitmapIcon)
    }.getOrElse { t ->
        AppCrashReporter.record(t, "buildNotification")
        Log.e(TAG, "notification build failed, using fallback", t)
        fallbackNotification(localizedContext)
    }

    private fun buildNotificationUnsafe(
        sample: SpeedSample,
        settings: AppSettings,
        network: NetworkState,
        localizedContext: Context,
        preferBitmapIcon: Boolean,
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

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon(sample, settings, preferBitmapIcon))
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

        contentIntent(settings, localizedContext)?.let { builder.setContentIntent(it) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    /**
     * The live rendered bitmap is the primary icon; the bundled vector is the
     * guaranteed-valid fallback if rendering or wrapping fails.
     */
    private fun smallIcon(
        sample: SpeedSample,
        settings: AppSettings,
        preferBitmapIcon: Boolean,
    ): IconCompat {
        if (settings.accent == NotificationAccent.CLASSIC || !preferBitmapIcon) {
            return runCatching {
                IconCompat.createWithResource(context, R.drawable.ic_stat_netmeter)
            }.getOrElse { fallbackIcon() }
        }
        return runCatching {
            IconCompat.createWithBitmap(
                iconRenderer.render(
                    sample.downloadBytesPerSecond,
                    sample.uploadBytesPerSecond,
                    settings.showUpDown,
                ),
            )
        }.getOrElse { fallbackIcon() }
    }

    private fun fallbackIcon(): IconCompat = try {
        IconCompat.createWithResource(context, R.drawable.ic_stat_netmeter)
    } catch (t: Throwable) {
        // Last resort: an empty adaptive icon is still a valid small icon.
        IconCompat.createWithResource(context, android.R.drawable.ic_menu_info_details)
    }

    /** Minimal, always-valid notification used when the full build fails. */
    private fun fallbackNotification(localizedContext: Context): Notification {
        val title = runCatching {
            localizedContext.getString(R.string.notification_title)
        }.getOrDefault("Network speed monitor")

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(fallbackIcon())
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
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
            AppCrashReporter.record(t, "notify")
            Log.e(TAG, "notify failed", t)
        }
    }

    companion object {
        const val CHANNEL_ID = "netmeter_speed_monitor"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "SpeedNotification"
        private const val REQUEST_CODE_OPEN = 10
        private const val REQUEST_CODE_STOP = 11
    }
}
