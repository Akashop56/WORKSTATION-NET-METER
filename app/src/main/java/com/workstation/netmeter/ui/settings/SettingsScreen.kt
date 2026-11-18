package com.workstation.netmeter.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workstation.netmeter.R
import com.workstation.netmeter.network.Transport
import com.workstation.netmeter.settings.AppLanguage
import com.workstation.netmeter.settings.NotificationAccent
import com.workstation.netmeter.settings.TapAction
import com.workstation.netmeter.settings.ThemeMode
import com.workstation.netmeter.ui.components.SectionHeader
import com.workstation.netmeter.ui.components.SettingAction
import com.workstation.netmeter.ui.components.SettingRadioGroup
import com.workstation.netmeter.ui.components.SettingSwitch
import com.workstation.netmeter.usage.MobileUsageReader
import com.workstation.netmeter.util.Permissions
import com.workstation.netmeter.util.SpeedFormatter
import com.workstation.netmeter.util.SpeedUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLanguageChanged: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val network by viewModel.network.collectAsStateWithLifecycle()
    val usage by viewModel.usage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showResetDialog by remember { mutableStateOf(false) }
    val mobileUsageReader = remember { MobileUsageReader(context) }
    var usageAccessGranted by remember { mutableStateOf(mobileUsageReader.hasUsageAccess()) }
    var batteryExempt by remember { mutableStateOf(Permissions.isIgnoringBatteryOptimizations(context)) }

    val tapLabels = mapOf(
        TapAction.OPEN_APP to stringResource(R.string.tap_open_app),
        TapAction.STOP to stringResource(R.string.tap_stop),
        TapAction.NONE to stringResource(R.string.tap_none),
    )
    val unitLabels = mapOf(
        SpeedUnit.BYTES_PER_SECOND to stringResource(R.string.unit_bps),
        SpeedUnit.KILOBYTES_PER_SECOND to stringResource(R.string.unit_kbps),
        SpeedUnit.MEGABYTES_PER_SECOND to stringResource(R.string.unit_mbps),
        SpeedUnit.BITS_PER_SECOND to stringResource(R.string.unit_bits),
        SpeedUnit.KILOBITS_PER_SECOND to stringResource(R.string.unit_kbits),
        SpeedUnit.MEGABITS_PER_SECOND to stringResource(R.string.unit_mbits),
    )
    val themeLabels = mapOf(
        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
        ThemeMode.DARK to stringResource(R.string.theme_dark),
        ThemeMode.LIGHT to stringResource(R.string.theme_light),
    )
    val languageLabels = mapOf(
        AppLanguage.EN to stringResource(R.string.lang_en),
        AppLanguage.HI to stringResource(R.string.lang_hi),
        AppLanguage.HINGLISH to stringResource(R.string.lang_hinglish),
    )
    val accentLabels = mapOf(
        NotificationAccent.SYSTEM to stringResource(R.string.accent_system),
        NotificationAccent.CLASSIC to stringResource(R.string.accent_classic),
    )
    val intervalLabels = mapOf(
        1_000L to stringResource(R.string.interval_1s),
        2_000L to stringResource(R.string.interval_2s),
        3_000L to stringResource(R.string.interval_3s),
    )

    val mobileUsage by produceState<Pair<Long, Long>?>(initialValue = null, usageAccessGranted) {
        value = withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            mobileUsageReader.queryMobileUsage(now - 30L * 24 * 60 * 60 * 1000, now)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ------------------------------------------------------------------
            SectionHeader(stringResource(R.string.section_notifications))
            SettingSwitch(
                title = stringResource(R.string.hide_lock_screen),
                subtitle = stringResource(R.string.hide_lock_screen_desc),
                checked = settings.hideOnLockScreen,
                onCheckedChange = viewModel::setHideOnLockScreen,
            )
            SettingSwitch(
                title = stringResource(R.string.notify_only_connected),
                subtitle = stringResource(R.string.notify_only_connected_desc),
                checked = settings.notifyOnlyWhenConnected,
                onCheckedChange = viewModel::setNotifyOnlyWhenConnected,
            )
            SettingSwitch(
                title = stringResource(R.string.show_up_down),
                subtitle = stringResource(R.string.show_up_down_desc),
                checked = settings.showUpDown,
                onCheckedChange = viewModel::setShowUpDown,
            )
            SettingRadioGroup(
                title = stringResource(R.string.tap_action),
                options = TapAction.entries.toList(),
                selected = settings.tapAction,
                label = { tapLabels[it].orEmpty() },
                onSelect = viewModel::setTapAction,
            )
            SettingRadioGroup(
                title = stringResource(R.string.speed_unit),
                options = SpeedUnit.entries.toList(),
                selected = settings.speedUnit,
                label = { unitLabels[it].orEmpty() },
                onSelect = viewModel::setSpeedUnit,
            )
            SettingRadioGroup(
                title = stringResource(R.string.notification_accent),
                options = NotificationAccent.entries.toList(),
                selected = settings.accent,
                label = { accentLabels[it].orEmpty() },
                onSelect = viewModel::setAccent,
            )

            // ------------------------------------------------------------------
            SectionHeader(stringResource(R.string.section_appearance))
            SettingRadioGroup(
                title = stringResource(R.string.theme),
                options = ThemeMode.entries.toList(),
                selected = settings.theme,
                label = { themeLabels[it].orEmpty() },
                onSelect = viewModel::setTheme,
            )
            SettingRadioGroup(
                title = stringResource(R.string.language),
                options = AppLanguage.entries.toList(),
                selected = settings.language,
                label = { languageLabels[it].orEmpty() },
                onSelect = { language ->
                    viewModel.setLanguage(language)
                    onLanguageChanged()
                },
            )

            // ------------------------------------------------------------------
            SectionHeader(stringResource(R.string.section_data))
            NumberSetting(
                title = stringResource(R.string.monthly_limit),
                subtitle = stringResource(R.string.monthly_limit_desc),
                initial = settings.monthlyLimitMb,
                onCommit = viewModel::setMonthlyLimitMb,
            )
            NumberSetting(
                title = stringResource(R.string.starting_hour),
                subtitle = stringResource(R.string.starting_hour_desc),
                initial = settings.startingHour,
                validRange = 0..23,
                onCommit = viewModel::setStartingHour,
            )
            InfoRow(
                label = stringResource(R.string.monthly_usage_label),
                value = SpeedFormatter.formatTotal(usage.monthTotalBytes),
            )
            InfoRow(
                label = stringResource(R.string.remaining_label),
                value = if (settings.monthlyLimitMb <= 0) {
                    stringResource(R.string.limit_disabled)
                } else {
                    SpeedFormatter.formatTotal(
                        (settings.monthlyLimitMb * com.workstation.netmeter.usage.DataUsage.BYTES_PER_MB -
                            usage.monthTotalBytes).coerceAtLeast(0L),
                    )
                },
            )
            if (usageAccessGranted) {
                InfoRow(
                    label = stringResource(R.string.mobile_data),
                    value = mobileUsage?.let {
                        SpeedFormatter.formatTotal(it.first + it.second)
                    } ?: stringResource(R.string.not_available),
                )
            } else {
                InfoRow(
                    label = stringResource(R.string.total_device_traffic),
                    value = SpeedFormatter.formatTotal(usage.monthTotalBytes),
                )
                Text(
                    text = stringResource(R.string.usage_access_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            SettingAction(
                title = stringResource(R.string.reset_usage),
                subtitle = null,
                onClick = { showResetDialog = true },
            )

            // ------------------------------------------------------------------
            SectionHeader(stringResource(R.string.section_advanced))
            InfoRow(
                label = stringResource(R.string.network_type),
                value = when (network.transport) {
                    Transport.WIFI -> stringResource(R.string.network_wifi)
                    Transport.MOBILE -> stringResource(R.string.network_mobile)
                    Transport.ETHERNET -> stringResource(R.string.network_ethernet)
                    Transport.NONE -> stringResource(R.string.network_none)
                },
            )
            InfoRow(
                label = stringResource(R.string.status_label),
                value = if (network.validated) {
                    stringResource(R.string.validated)
                } else {
                    stringResource(R.string.not_validated)
                },
            )
            InfoRow(
                label = stringResource(R.string.ssid),
                value = network.ssid ?: stringResource(R.string.not_available),
            )
            InfoRow(
                label = stringResource(R.string.signal),
                value = network.signalLevel?.let { "$it/4" }
                    ?: stringResource(R.string.not_available),
            )
            InfoRow(
                label = stringResource(R.string.link_speed),
                value = network.linkSpeedMbps?.let { "$it Mbps" }
                    ?: stringResource(R.string.not_available),
            )
            SettingRadioGroup(
                title = stringResource(R.string.sampling_interval),
                options = listOf(1_000L, 2_000L, 3_000L),
                selected = settings.samplingIntervalMs,
                label = { intervalLabels[it].orEmpty() },
                onSelect = viewModel::setSamplingIntervalMs,
            )
            SettingSwitch(
                title = stringResource(R.string.start_on_boot),
                subtitle = stringResource(R.string.start_on_boot_desc),
                checked = settings.startOnBoot,
                onCheckedChange = viewModel::setStartOnBoot,
            )
            SettingAction(
                title = stringResource(R.string.battery_optimization),
                subtitle = if (batteryExempt) {
                    stringResource(R.string.battery_optimized)
                } else {
                    stringResource(R.string.battery_not_optimized)
                },
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                    batteryExempt = Permissions.isIgnoringBatteryOptimizations(context)
                },
            )
            SettingAction(
                title = stringResource(R.string.usage_access),
                subtitle = if (usageAccessGranted) {
                    stringResource(R.string.usage_access_granted)
                } else {
                    stringResource(R.string.usage_access_not_granted)
                },
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                    usageAccessGranted = mobileUsageReader.hasUsageAccess()
                },
            )

            // ------------------------------------------------------------------
            SectionHeader(stringResource(R.string.section_about))
            Text(
                text = stringResource(R.string.about_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_usage)) },
            text = { Text(stringResource(R.string.reset_usage_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetUsage()
                        showResetDialog = false
                    },
                ) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun NumberSetting(
    title: String,
    subtitle: String,
    initial: Int,
    validRange: IntRange = 0..Int.MAX_VALUE,
    onCommit: (Int) -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial.toString()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = text,
            onValueChange = { value ->
                text = value.filter { it.isDigit() }.take(9)
                val parsed = text.toIntOrNull()
                if (parsed != null && parsed in validRange) {
                    onCommit(parsed)
                } else if (text.isEmpty()) {
                    onCommit(validRange.first)
                }
            },
            label = { Text(title) },
            supportingText = { Text(subtitle) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
}
