package com.workstation.netmeter.settings

import com.workstation.netmeter.util.SpeedUnit

/** Immutable snapshot of every user preference. */
data class AppSettings(
    val monitoringEnabled: Boolean = false,
    val hideOnLockScreen: Boolean = false,
    val notifyOnlyWhenConnected: Boolean = false,
    val showUpDown: Boolean = true,
    val tapAction: TapAction = TapAction.OPEN_APP,
    val speedUnit: SpeedUnit = SpeedUnit.DEFAULT,
    val monthlyLimitMb: Int = 0,
    val startingHour: Int = 0,
    val language: AppLanguage = AppLanguage.EN,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val accent: NotificationAccent = NotificationAccent.SYSTEM,
    val startOnBoot: Boolean = false,
    val samplingIntervalMs: Long = DEFAULT_SAMPLING_INTERVAL_MS,
) {
    companion object {
        const val DEFAULT_SAMPLING_INTERVAL_MS = 1_000L
    }
}

enum class TapAction(val key: String) {
    OPEN_APP("OPEN_APP"),
    STOP("STOP"),
    NONE("NONE"),
    ;

    companion object {
        fun fromKey(key: String?): TapAction =
            entries.firstOrNull { it.key == key } ?: OPEN_APP
    }
}

enum class AppLanguage(val key: String, val localeTag: String) {
    EN("en", "en"),
    HI("hi", "hi"),
    HINGLISH("en-IN", "en-IN"),
    ;

    companion object {
        fun fromKey(key: String?): AppLanguage =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: EN
    }
}

enum class ThemeMode(val key: String) {
    SYSTEM("SYSTEM"),
    DARK("DARK"),
    LIGHT("LIGHT"),
    ;

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class NotificationAccent(val key: String) {
    SYSTEM("SYSTEM"),
    CLASSIC("CLASSIC"),
    ;

    companion object {
        fun fromKey(key: String?): NotificationAccent =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}
