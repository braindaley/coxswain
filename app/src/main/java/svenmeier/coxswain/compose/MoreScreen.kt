package svenmeier.coxswain.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.hardware.usb.UsbDevice
import propoid.util.content.Preference
import svenmeier.coxswain.Gym
import svenmeier.coxswain.GymService
import svenmeier.coxswain.DiagnosticsLog
import svenmeier.coxswain.R
import svenmeier.coxswain.rower.wired.usb.UsbConnector

private enum class MoreDestination { ROOT, CONNECT, DATA, DIAGNOSTICS, HELP }

@Composable
fun MoreScreen(
    gym: Gym,
    connectRequestKey: Int = 0,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSettings: () -> Unit,
    onHealthSettings: () -> Unit,
    onEnableAutomaticHealthExport: () -> Unit,
    onSyncHealthHistory: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    var destination by remember { mutableStateOf(MoreDestination.ROOT) }
    LaunchedEffect(connectRequestKey) {
        if (connectRequestKey > 0) destination = MoreDestination.CONNECT
    }
    var update by remember { mutableIntStateOf(0) }
    DisposableEffect(gym) {
        val listener = Gym.Listener { update++ }
        gym.addListener(listener)
        onDispose { gym.removeListener(listener) }
    }
    @Suppress("UNUSED_VARIABLE") val refresh = update
    when (destination) {
        MoreDestination.ROOT -> MoreRoot(onConnect = { destination = MoreDestination.CONNECT }, onSettings, onData = { destination = MoreDestination.DATA }, onDiagnostics = { destination = MoreDestination.DIAGNOSTICS }, onHelp = { destination = MoreDestination.HELP })
        MoreDestination.CONNECT -> ConnectRowerScreen(gym, onBack = { destination = MoreDestination.ROOT }, onConnect, onDisconnect)
        MoreDestination.DATA -> DataExportScreen(onBack = { destination = MoreDestination.ROOT }, onSettings, onHealthSettings, onEnableAutomaticHealthExport, onSyncHealthHistory, onBackup, onRestore)
        MoreDestination.DIAGNOSTICS -> DiagnosticsScreen(gym, onBack = { destination = MoreDestination.ROOT })
        MoreDestination.HELP -> HelpScreen(onBack = { destination = MoreDestination.ROOT })
    }
}

@Composable
private fun MoreRoot(onConnect: () -> Unit, onSettings: () -> Unit, onData: () -> Unit, onDiagnostics: () -> Unit, onHelp: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(stringResource(R.string.ui_more).uppercase(), style = MaterialTheme.typography.labelLarge) }
        item { MoreRow(stringResource(R.string.ui_connect_rower), stringResource(R.string.ui_connect_rower_subtitle), onConnect) }
        item { MoreRow(stringResource(R.string.action_settings), stringResource(R.string.ui_settings_subtitle), onSettings) }
        item { MoreRow(stringResource(R.string.ui_data_export), stringResource(R.string.ui_data_export_subtitle), onData) }
        item { MoreRow(stringResource(R.string.ui_diagnostics), stringResource(R.string.ui_diagnostics_subtitle), onDiagnostics) }
        item { MoreRow(stringResource(R.string.action_help), stringResource(R.string.ui_help_subtitle), onHelp) }
    }
}

@Composable
private fun ConnectRowerScreen(gym: Gym, onBack: () -> Unit, onConnect: () -> Unit, onDisconnect: () -> Unit) {
    val context = LocalContext.current
    var usbDevices by remember { mutableStateOf<List<UsbDevice>>(emptyList()) }
    var isS3Mode by remember {
        mutableStateOf(Preference.getBoolean(context, R.string.preference_hardware_legacy).get() ?: false)
    }
    var connector by remember { mutableStateOf<UsbConnector?>(null) }

    DisposableEffect(context) {
        val conn = object : UsbConnector(context) {
            override fun onConnected(device: UsbDevice?) {
                if (device != null) {
                    GymService.start(context, device)
                }
            }
        }
        connector = conn
        usbDevices = conn.list().toList()
        onDispose {
            conn.destroy()
        }
    }

    MorePage(stringResource(R.string.ui_connect_rower), onBack) {
        StatusCard(
            if (gym.connected) stringResource(R.string.ui_connected) else if (gym.connecting) stringResource(R.string.ui_connecting) else stringResource(R.string.ui_disconnected),
            gym.connectedRowerName ?: if (gym.connecting) stringResource(R.string.ui_waiting_for_rower) else stringResource(R.string.ui_no_rower_connected)
        )
        if (gym.connected) {
            OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ui_disconnect))
            }
        }

        Text(stringResource(R.string.ui_bluetooth_ftms), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.ui_bluetooth_ftms_explanation))
        Button(onClick = onConnect, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Text(stringResource(if (gym.connected) R.string.ui_connect_another_rower else R.string.ui_scan_for_rower))
        }

        HorizontalDivider()

        Text(stringResource(R.string.ui_usb), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.ui_usb_explanation))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("S3 Monitor Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Enable for WaterRower S3 monitor (1200 baud). Leave off for S4 monitor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isS3Mode,
                onCheckedChange = { checked ->
                    isS3Mode = checked
                    Preference.getBoolean(context, R.string.preference_hardware_legacy).set(checked)
                }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedButton(
            onClick = {
                connector?.let { usbDevices = it.list().toList() }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh USB Devices")
        }

        if (usbDevices.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("No USB devices detected", fontWeight = FontWeight.Bold)
                    Text(
                        "Plug in your WaterRower USB cable with a USB-OTG adapter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            usbDevices.forEach { device ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val deviceName = device.productName ?: device.deviceName
                        Text(deviceName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Vendor: ${device.vendorId} · Product: ${device.productId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { connector?.connect(device) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Connect to $deviceName")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DataExportScreen(onBack: () -> Unit, onSettings: () -> Unit, onHealthSettings: () -> Unit, onEnableAutomaticHealthExport: () -> Unit, onSyncHealthHistory: () -> Unit, onBackup: () -> Unit, onRestore: () -> Unit) {
    MorePage(stringResource(R.string.ui_data_export), onBack) {
        Text(stringResource(R.string.ui_health_connect), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.ui_health_data_explanation))
        Button(onClick = onHealthSettings, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_health_permissions)) }
        OutlinedButton(onClick = onEnableAutomaticHealthExport, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_health_automatic)) }
        OutlinedButton(onClick = onSyncHealthHistory, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_sync_history)) }
        HorizontalDivider()
        Text(stringResource(R.string.ui_automatic_export_storage), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.ui_automatic_export_storage_explanation))
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_open_data_settings)) }
        HorizontalDivider()
        Text(stringResource(R.string.ui_backup_restore), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.ui_backup_restore_explanation))
        Button(onClick = onBackup, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_create_backup)) }
        OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_restore_backup)) }
    }
}

@Composable
private fun DiagnosticsScreen(gym: Gym, onBack: () -> Unit) {
    val measurement = gym.measurement
    val context = LocalContext.current
    MorePage(stringResource(R.string.ui_diagnostics), onBack) {
        StatusCard(if (gym.connected) stringResource(R.string.ui_rower_connected) else stringResource(R.string.ui_rower_disconnected), gym.connectedRowerName ?: stringResource(R.string.ui_no_device))
        Text(stringResource(R.string.ui_live_measurement), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        DiagnosticRow(stringResource(R.string.ui_distance), "${measurement.distance} m")
        DiagnosticRow(stringResource(R.string.ui_elapsed_time), "%d:%02d".format(measurement.duration / 60, measurement.duration % 60))
        DiagnosticRow(stringResource(R.string.ui_stroke_rate), "${measurement.strokeRate}")
        DiagnosticRow(stringResource(R.string.ui_power), "${measurement.power} W")
        DiagnosticRow(stringResource(R.string.ui_heart_rate), if (measurement.pulse > 0) "${measurement.pulse} bpm" else stringResource(R.string.ui_no_signal))
        DiagnosticRow(stringResource(R.string.ui_heart_source), gym.heartSourceName ?: stringResource(R.string.ui_not_active))
        Text(stringResource(R.string.ui_last_recorded_issue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(DiagnosticsLog.latest(context), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.ui_diagnostics_guidance), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HelpScreen(onBack: () -> Unit) {
    MorePage(stringResource(R.string.action_help), onBack) {
        HelpItem(stringResource(R.string.ui_help_start_rowing), stringResource(R.string.ui_help_start_rowing_body))
        HelpItem(stringResource(R.string.ui_programs), stringResource(R.string.ui_help_programs_body))
        HelpItem(stringResource(R.string.ui_help_race_title), stringResource(R.string.ui_help_race_body))
        HelpItem(stringResource(R.string.ui_heart_rate), stringResource(R.string.ui_help_heart_rate_body))
        HelpItem(stringResource(R.string.ui_connection_problems), stringResource(R.string.ui_help_connection_body))
    }
}

@Composable private fun MorePage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text(stringResource(R.string.ui_back)) }
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable private fun StatusCard(title: String, detail: String) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun DiagnosticRow(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value, fontWeight = FontWeight.Bold) } }
@Composable private fun HelpItem(title: String, body: String) { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun MoreRow(title: String, subtitle: String, onClick: () -> Unit) { Card(Modifier.fillMaxWidth().clickable { onClick() }) { Column(Modifier.padding(20.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); Text(subtitle, style = MaterialTheme.typography.bodyMedium) } } }
