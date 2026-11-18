package com.workstation.netmeter.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.workstation.netmeter.util.SpeedUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reads and writes the persisted user preferences backed by Jetpack DataStore.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { p ->
        AppSettings(
            monitoringEnabled = p[Keys.MONITORING_ENABLED] ?: false,
            hideOnLockScreen = p[Keys.HIDE_ON_LOCK_SCREEN] ?: false,
            notifyOnlyWhenConnected = p[Keys.NOTIFY_ONLY_CONNECTED] ?: false,
            showUpDown = p[Keys.SHOW_UP_DOWN] ?: true,
            tapAction = TapAction.fromKey(p[Keys.TAP_ACTION]),
            speedUnit = SpeedUnit.fromKey(p[Keys.SPEED_UNIT]),
            monthlyLimitMb = (p[Keys.MONTHLY_LIMIT_MB] ?: 0).coerceAtLeast(0),
            startingHour = (p[Keys.STARTING_HOUR] ?: 0).coerceIn(0, 23),
            language = AppLanguage.fromKey(p[Keys.LANGUAGE]),
            theme = ThemeMode.fromKey(p[Keys.THEME]),
            accent = NotificationAccent.fromKey(p[Keys.ACCENT]),
            startOnBoot = p[Keys.START_ON_BOOT] ?: false,
            samplingIntervalMs = (p[Keys.SAMPLING_INTERVAL_MS]
                ?: AppSettings.DEFAULT_SAMPLING_INTERVAL_MS).coerceIn(500L, 5_000L),
        )
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) =
        dataStore.edit { it[Keys.MONITORING_ENABLED] = enabled }

    suspend fun setHideOnLockScreen(enabled: Boolean) =
        dataStore.edit { it[Keys.HIDE_ON_LOCK_SCREEN] = enabled }

    suspend fun setNotifyOnlyWhenConnected(enabled: Boolean) =
        dataStore.edit { it[Keys.NOTIFY_ONLY_CONNECTED] = enabled }

    suspend fun setShowUpDown(enabled: Boolean) =
        dataStore.edit { it[Keys.SHOW_UP_DOWN] = enabled }

    suspend fun setTapAction(action: TapAction) =
        dataStore.edit { it[Keys.TAP_ACTION] = action.key }

    suspend fun setSpeedUnit(unit: SpeedUnit) =
        dataStore.edit { it[Keys.SPEED_UNIT] = unit.key }

    suspend fun setMonthlyLimitMb(limitMb: Int) =
        dataStore.edit { it[Keys.MONTHLY_LIMIT_MB] = limitMb.coerceAtLeast(0) }

    suspend fun setStartingHour(hour: Int) =
        dataStore.edit { it[Keys.STARTING_HOUR] = hour.coerceIn(0, 23) }

    suspend fun setLanguage(language: AppLanguage) =
        dataStore.edit { it[Keys.LANGUAGE] = language.key }

    suspend fun setTheme(theme: ThemeMode) =
        dataStore.edit { it[Keys.THEME] = theme.key }

    suspend fun setAccent(accent: NotificationAccent) =
        dataStore.edit { it[Keys.ACCENT] = accent.key }

    suspend fun setStartOnBoot(enabled: Boolean) =
        dataStore.edit { it[Keys.START_ON_BOOT] = enabled }

    suspend fun setSamplingIntervalMs(intervalMs: Long) =
        dataStore.edit { it[Keys.SAMPLING_INTERVAL_MS] = intervalMs.coerceIn(500L, 5_000L) }

    private object Keys {
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val HIDE_ON_LOCK_SCREEN = booleanPreferencesKey("hide_on_lock_screen")
        val NOTIFY_ONLY_CONNECTED = booleanPreferencesKey("notify_only_connected")
        val SHOW_UP_DOWN = booleanPreferencesKey("show_up_down")
        val TAP_ACTION = stringPreferencesKey("tap_action")
        val SPEED_UNIT = stringPreferencesKey("speed_unit")
        val MONTHLY_LIMIT_MB = intPreferencesKey("monthly_limit_mb")
        val STARTING_HOUR = intPreferencesKey("starting_hour")
        val LANGUAGE = LANGUAGE_KEY
        val THEME = stringPreferencesKey("theme")
        val ACCENT = stringPreferencesKey("notification_accent")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val SAMPLING_INTERVAL_MS = longPreferencesKey("sampling_interval_ms")
    }
}
