package com.workstation.netmeter.ui.dashboard

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workstation.netmeter.R
import com.workstation.netmeter.network.Transport
import com.workstation.netmeter.service.ServiceState
import com.workstation.netmeter.usage.DataUsage
import com.workstation.netmeter.util.Permissions
import com.workstation.netmeter.util.SpeedFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit,
    viewModel: DashboardViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasNotificationPermission by remember {
        mutableStateOf(Permissions.hasPostNotifications(context))
    }
    var pendingStart by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasNotificationPermission = granted
        if (granted && pendingStart) {
            pendingStart = false
            viewModel.setMonitoring(true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!hasNotificationPermission) {
                PermissionCard(
                    onGrant = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pendingStart = state.isMonitoring
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }

            StatusCard(state = state)
            SpeedCard(state = state)
            TotalsCard(state = state)
            NetworkCard(state = state)
            UsageCard(state = state)

            Spacer(modifier = Modifier.height(4.dp))
            if (state.isMonitoring) {
                OutlinedButton(
                    onClick = { viewModel.setMonitoring(false) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.stop_monitoring))
                }
            } else {
                Button(
                    onClick = {
                        if (Permissions.hasPostNotifications(context)) {
                            viewModel.setMonitoring(true)
                        } else {
                            pendingStart = true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.start_monitoring))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionCard(onGrant: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.permission_required),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onGrant) {
                Text(stringResource(R.string.grant_permission))
            }
        }
    }
}

@Composable
private fun StatusCard(state: DashboardUiState) {
    val statusText = when (state.serviceState) {
        ServiceState.ACTIVE -> stringResource(R.string.monitoring_on)
        ServiceState.WAITING_FOR_NETWORK -> stringResource(R.string.waiting_for_network)
        ServiceState.STOPPED -> stringResource(R.string.monitoring_off)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.monitoring_status),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SpeedCard(state: DashboardUiState) {
    val unit = state.settings.speedUnit
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.live_speed),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SpeedColumn(
                    label = stringResource(R.string.download_speed),
                    value = SpeedFormatter.format(state.downloadBps, unit),
                )
                SpeedColumn(
                    label = stringResource(R.string.upload_speed),
                    value = if (state.settings.showUpDown) {
                        SpeedFormatter.format(state.uploadBps, unit)
                    } else {
                        stringResource(R.string.not_available)
                    },
                    valueColor = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun SpeedColumn(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

@Composable
private fun TotalsCard(state: DashboardUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledValue(
                label = stringResource(R.string.total_downloaded),
                value = SpeedFormatter.formatTotal(state.totalRxBytes),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            LabeledValue(
                label = stringResource(R.string.total_uploaded),
                value = SpeedFormatter.formatTotal(state.totalTxBytes),
            )
        }
    }
}

@Composable
private fun NetworkCard(state: DashboardUiState) {
    val network = state.network
    val typeText = when (network.transport) {
        Transport.WIFI -> stringResource(R.string.network_wifi)
        Transport.MOBILE -> stringResource(R.string.network_mobile)
        Transport.ETHERNET -> stringResource(R.string.network_ethernet)
        Transport.NONE -> stringResource(R.string.network_none)
    }
    val validationText = if (network.validated) {
        stringResource(R.string.internet_available)
    } else {
        stringResource(R.string.internet_unavailable)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledValue(label = stringResource(R.string.current_network), value = typeText)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            LabeledValue(label = stringResource(R.string.status_label), value = validationText)
        }
    }
}

@Composable
private fun UsageCard(state: DashboardUiState) {
    val usage = state.usage
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledValue(
                label = stringResource(R.string.monthly_usage),
                value = SpeedFormatter.formatTotal(usage.monthTotalBytes),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            LabeledValue(
                label = stringResource(R.string.remaining_data),
                value = remainingData(state.settings.monthlyLimitMb, usage.monthTotalBytes),
            )
        }
    }
}

@Composable
private fun remainingData(limitMb: Int, usedBytes: Long): String {
    if (limitMb <= 0) return stringResource(R.string.limit_disabled)
    val remaining = (limitMb * DataUsage.BYTES_PER_MB - usedBytes).coerceAtLeast(0L)
    return SpeedFormatter.formatTotal(remaining)
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}
