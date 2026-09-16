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
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("MORE", style = MaterialTheme.typography.labelLarge) }
        item { MoreRow("Connect rower", "Pair or manage a rowing machine", onConnect) }
        item { MoreRow("Settings", "Units, display, heart rate, and data", onSettings) }
    }
}

@Composable private fun MoreRow(title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }) { Column(Modifier.padding(20.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); Text(subtitle, style = MaterialTheme.typography.bodyMedium) } }
}
