package svenmeier.coxswain.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import svenmeier.coxswain.GymService
import svenmeier.coxswain.R

/** Obtain runtime consent while visible, before starting the connected-device service. */
class BluetoothConnectActivity : ComponentActivity() {
    private val permissions = arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (permissions.all { result[it] == true }) connectOrEnable()
        else stopWithMessage(R.string.ui_connection_permission_needed)
    }
    private val enableBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) connectOrEnable(false)
        else stopWithMessage(R.string.ui_bluetooth_enable_needed)
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        if (state != null) return // A pending system dialog will deliver its result.
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) connectOrEnable()
        else requestPermissions.launch(permissions)
    }

    private fun connectOrEnable(allowPrompt: Boolean = true) {
        try {
            val adapter = getSystemService(BluetoothManager::class.java)?.adapter
            if (adapter == null) { stopWithMessage(R.string.bluetooth_rower_no_bluetooth); return }
            if (!adapter.isEnabled) {
                if (allowPrompt) enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                else stopWithMessage(R.string.ui_bluetooth_enable_needed)
                return
            }
            GymService.startBluetoothReady(this)
            finish()
        } catch (exception: SecurityException) {
            stopWithMessage(R.string.ui_connection_permission_needed)
        }
    }

    private fun stopWithMessage(message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}
