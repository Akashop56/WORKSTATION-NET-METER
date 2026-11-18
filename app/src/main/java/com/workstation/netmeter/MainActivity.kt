package com.workstation.netmeter

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workstation.netmeter.settings.AppSettings
import com.workstation.netmeter.settings.LocaleManager
import com.workstation.netmeter.ui.dashboard.DashboardScreen
import com.workstation.netmeter.ui.settings.SettingsScreen
import com.workstation.netmeter.ui.theme.NetMeterTheme

/**
 * Single-activity Compose host. Also applies the persisted language before the
 * Compose tree is created, so both resources and `stringResource` are localized.
 */
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrapWithStored(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by NetMeterApp.instance.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            NetMeterTheme(themeMode = settings.theme) {
                NetMeterRoot(
                    onLanguageChanged = { recreate() },
                )
            }
        }
    }
}

private enum class Screen { Dashboard, Settings }

@Composable
private fun NetMeterRoot(onLanguageChanged: () -> Unit) {
    var screen by remember { mutableStateOf(Screen.Dashboard) }

    BackHandler(enabled = screen == Screen.Settings) {
        screen = Screen.Dashboard
    }

    when (screen) {
        Screen.Dashboard -> DashboardScreen(
            onOpenSettings = { screen = Screen.Settings },
        )

        Screen.Settings -> SettingsScreen(
            onBack = { screen = Screen.Dashboard },
            onLanguageChanged = onLanguageChanged,
        )
    }
}
