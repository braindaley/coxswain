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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import svenmeier.coxswain.compose.CoxswainTheme
import svenmeier.coxswain.R

class HealthConnectManageActivity : ComponentActivity() {
    private var status by mutableStateOf("")
    private var available by mutableStateOf(true)
    private lateinit var client: HealthConnectClient
    private val permissionLauncher = registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { updateStatus(it) }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        status = getString(R.string.ui_health_checking)
        available = HealthConnectClient.getSdkStatus(this) == HealthConnectClient.SDK_AVAILABLE
        if (available) {
            client = HealthConnectClient.getOrCreate(this)
            refreshStatus()
        } else status = getString(R.string.ui_health_unavailable)
        setContent {
            CoxswainTheme {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = { finish() }, contentPadding = PaddingValues(0.dp)) { Text(stringResource(R.string.ui_back)) }
                    Text(stringResource(R.string.ui_health_connect), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Card(Modifier.fillMaxWidth()) { Text(status, Modifier.padding(18.dp)) }
                    Text(stringResource(R.string.ui_health_explanation))
                    Button(onClick = { permissionLauncher.launch(HealthConnectBridge.getWritePermissions()) }, enabled = available, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_health_grant)) }
                    OutlinedButton(onClick = { runCatching { startActivity(HealthConnectBridge.getSettingsIntent()) } }, enabled = available, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_health_open_settings)) }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); if (::client.isInitialized) refreshStatus() }

    private fun refreshStatus() {
        Futures.addCallback(HealthConnectBridge.getGrantedPermissionsAsync(client), object : FutureCallback<Set<String>> {
            override fun onSuccess(result: Set<String>) = updateStatus(result)
            override fun onFailure(t: Throwable) { status = getString(R.string.ui_health_check_failed, t.message ?: "") }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun updateStatus(granted: Set<String>) {
        val required = HealthConnectBridge.getWritePermissions()
        status = when {
            granted.containsAll(required) -> getString(R.string.ui_health_connected)
            granted.isEmpty() -> getString(R.string.ui_health_not_connected)
            else -> resources.getQuantityString(R.plurals.ui_health_partial, granted.intersect(required).size, granted.intersect(required).size, required.size)
        }
    }

    companion object { @JvmStatic fun start(activity: Activity) = activity.startActivity(Intent(activity, HealthConnectManageActivity::class.java)) }
}
