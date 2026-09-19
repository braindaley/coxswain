package svenmeier.coxswain.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import svenmeier.coxswain.Gym

private enum class MoreDestination { ROOT, CONNECT, DATA, DIAGNOSTICS, HELP }

@Composable
fun MoreScreen(
    gym: Gym,
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
        item { Text("MORE", style = MaterialTheme.typography.labelLarge) }
        item { MoreRow("Connect rower", "Pair or manage a rowing machine", onConnect) }
        item { MoreRow("Settings", "Units, display, heart rate, and audio", onSettings) }
        item { MoreRow("Data & Export", "Health Connect, automatic export, and storage", onData) }
        item { MoreRow("Diagnostics", "Live connection and measurement checks", onDiagnostics) }
        item { MoreRow("Help", "Connection and workout guidance", onHelp) }
    }
}

@Composable
private fun ConnectRowerScreen(gym: Gym, onBack: () -> Unit, onConnect: () -> Unit, onDisconnect: () -> Unit) {
    MorePage("Connect rower", onBack) {
        StatusCard(if (gym.connected) "Connected" else if (gym.connecting) "Connecting…" else "Disconnected", gym.connectedRowerName ?: if (gym.connecting) "Waiting for a rowing machine" else "No rower connected")
        Text("Bluetooth FTMS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Scan for a supported Bluetooth rowing machine. You can remember the selected device for faster connection next time.")
        Button(onClick = onConnect, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(if (gym.connected) "Connect another rower" else "Scan for rower") }
        if (gym.connected) OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) { Text("Disconnect") }
        HorizontalDivider()
        Text("USB", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Connect a supported WaterRower USB cable. Android will ask for device access and Coxswain will connect automatically.")
    }
}

@Composable
private fun DataExportScreen(onBack: () -> Unit, onSettings: () -> Unit, onHealthSettings: () -> Unit, onEnableAutomaticHealthExport: () -> Unit, onSyncHealthHistory: () -> Unit, onBackup: () -> Unit, onRestore: () -> Unit) {
    MorePage("Data & Export", onBack) {
        Text("Health Connect", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Grant Health Connect access, export completed rows automatically, or sync existing workout history. Repeated syncs reuse stable workout IDs.")
        Button(onClick = onHealthSettings, modifier = Modifier.fillMaxWidth()) { Text("Health Connect permissions") }
        OutlinedButton(onClick = onEnableAutomaticHealthExport, modifier = Modifier.fillMaxWidth()) { Text("Use Health Connect automatically") }
        OutlinedButton(onClick = onSyncHealthHistory, modifier = Modifier.fillMaxWidth()) { Text("Sync existing history") }
        HorizontalDivider()
        Text("Automatic export & storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Choose the automatic export destination, include track data, or change where Coxswain stores its database.")
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Open data settings") }
        HorizontalDivider()
        Text("Backup & restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Save or restore programs, workout history, recorded snapshots, race references, and app preferences.")
        Button(onClick = onBackup, modifier = Modifier.fillMaxWidth()) { Text("Create backup") }
        OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text("Restore backup") }
    }
}

@Composable
private fun DiagnosticsScreen(gym: Gym, onBack: () -> Unit) {
    val measurement = gym.measurement
    MorePage("Diagnostics", onBack) {
        StatusCard(if (gym.connected) "Rower connected" else "Rower disconnected", gym.connectedRowerName ?: "No device")
        Text("Live measurement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        DiagnosticRow("Distance", "${measurement.distance} m")
        DiagnosticRow("Elapsed time", "%d:%02d".format(measurement.duration / 60, measurement.duration % 60))
        DiagnosticRow("Stroke rate", "${measurement.strokeRate}")
        DiagnosticRow("Power", "${measurement.power} W")
        DiagnosticRow("Heart rate", if (measurement.pulse > 0) "${measurement.pulse} bpm" else "No signal")
        DiagnosticRow("Heart source", gym.heartSourceName ?: "Not active")
        Text("If values stop changing, disconnect and reconnect the rower. Watches work when they can broadcast standard BLE Heart Rate or ANT+; ordinary watch-to-phone syncing does not provide live heart rate.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HelpScreen(onBack: () -> Unit) {
    MorePage("Help", onBack) {
        HelpItem("Start rowing", "Use Free Row for an open session, or Quick Start to choose duration, distance, or intervals.")
        HelpItem("Programs", "Programs are saved workout definitions. A used program is kept read-only so its history and best result remain comparable; duplicate it to make changes.")
        HelpItem("Race Your Best", "Open a program and choose Race Your Best. Coxswain compares the live row with a compatible completed result.")
        HelpItem("Heart rate", "Choose a BLE or ANT+ sensor in Settings. A supported watch must expose a live heart-rate broadcast mode.")
        HelpItem("Connection problems", "Open Diagnostics to confirm that the rower and measurements are active.")
    }
}

@Composable private fun MorePage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("Back") }
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable private fun StatusCard(title: String, detail: String) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun DiagnosticRow(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value, fontWeight = FontWeight.Bold) } }
@Composable private fun HelpItem(title: String, body: String) { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun MoreRow(title: String, subtitle: String, onClick: () -> Unit) { Card(Modifier.fillMaxWidth().clickable { onClick() }) { Column(Modifier.padding(20.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); Text(subtitle, style = MaterialTheme.typography.bodyMedium) } } }
