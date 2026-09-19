package svenmeier.coxswain.google

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import svenmeier.coxswain.compose.CoxswainTheme

class HealthConnectManageActivity : ComponentActivity() {
    private var status by mutableStateOf("Checking access…")
    private var available by mutableStateOf(true)
    private lateinit var client: HealthConnectClient
    private val permissionLauncher = registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { updateStatus(it) }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        available = HealthConnectClient.getSdkStatus(this) == HealthConnectClient.SDK_AVAILABLE
        if (available) {
            client = HealthConnectClient.getOrCreate(this)
            refreshStatus()
        } else status = "Health Connect is not available on this device."
        setContent {
            CoxswainTheme {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = { finish() }, contentPadding = PaddingValues(0.dp)) { Text("Back") }
                    Text("Health Connect", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Card(Modifier.fillMaxWidth()) { Text(status, Modifier.padding(18.dp)) }
                    Text("Coxswain writes rowing sessions, distance, calories, heart rate, speed, and power. It does not request read access.")
                    Button(onClick = { permissionLauncher.launch(HealthConnectBridge.getWritePermissions()) }, enabled = available, modifier = Modifier.fillMaxWidth()) { Text("Grant access") }
                    OutlinedButton(onClick = { runCatching { startActivity(HealthConnectBridge.getSettingsIntent()) } }, enabled = available, modifier = Modifier.fillMaxWidth()) { Text("Open Health Connect settings") }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); if (::client.isInitialized) refreshStatus() }

    private fun refreshStatus() {
        Futures.addCallback(HealthConnectBridge.getGrantedPermissionsAsync(client), object : FutureCallback<Set<String>> {
            override fun onSuccess(result: Set<String>) = updateStatus(result)
            override fun onFailure(t: Throwable) { status = "Could not check access: ${t.message}" }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun updateStatus(granted: Set<String>) {
        val required = HealthConnectBridge.getWritePermissions()
        status = when {
            granted.containsAll(required) -> "Connected · all rowing data permissions granted"
            granted.isEmpty() -> "Not connected · access has not been granted"
            else -> "Partially connected · ${granted.intersect(required).size} of ${required.size} permissions granted"
        }
    }

    companion object { @JvmStatic fun start(activity: Activity) = activity.startActivity(Intent(activity, HealthConnectManageActivity::class.java)) }
}
