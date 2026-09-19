package svenmeier.coxswain.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MoreScreen(onConnect: () -> Unit, onSettings: () -> Unit) {
    var info by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    info?.let { title -> AlertDialog(onDismissRequest = { info = null }, title = { Text(title) }, text = { Text("This destination is connected to the existing Coxswain tools and will be expanded as the new settings flow is completed.") }, confirmButton = { TextButton(onClick = { info = null }) { Text("Done") } }) }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("MORE", style = MaterialTheme.typography.labelLarge) }
        item { MoreRow("Connect rower", "Pair or manage a rowing machine", onConnect) }
        item { MoreRow("Settings", "Units, display, heart rate, and data", onSettings) }
        item { MoreRow("Data & Export", "Export or manage workout data", { info = "Data & Export" }) }
        item { MoreRow("Diagnostics", "Connection and measurement checks", { info = "Diagnostics" }) }
        item { MoreRow("Help", "Learn how Coxswain works", { info = "Help" }) }
    }
}

@Composable private fun MoreRow(title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }) { Column(Modifier.padding(20.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); Text(subtitle, style = MaterialTheme.typography.bodyMedium) } }
}
