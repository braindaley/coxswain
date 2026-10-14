package svenmeier.coxswain.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Looper
import androidx.preference.PreferenceManager
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import svenmeier.coxswain.R
import svenmeier.coxswain.gym.Measurement
import java.time.Duration
import java.util.ArrayDeque

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class BluetoothHeartRememberTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val prefs get() = PreferenceManager.getDefaultSharedPreferences(context)
    private val key get() = context.getString(R.string.preference_bluetooth_heart_device)
    private val address = "AA:BB:CC:DD:EE:FF"
    private var heart: BluetoothHeart? = null

    @Before fun prepare() {
        shadowOf(context).grantPermissions(Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT)
        BluetoothAdapter.getDefaultAdapter().enable()
        prefs.edit().putString(key,address).commit()
    }
    @After fun cleanup() { heart?.destroy(); prefs.edit().remove(key).commit() }
    private fun start(): BluetoothGatt {
        heart=BluetoothHeart(context,Measurement()) {}
        val field=BluetoothHeart::class.java.getDeclaredField("connections").apply { isAccessible=true }
        val connection=(field.get(heart) as ArrayDeque<*>).peek()
        val connected=connection.javaClass.getDeclaredField("connected").apply { isAccessible=true }
        return connected.get(connection) as BluetoothGatt
    }
    @Test fun reopenedPickerShowsRememberChecked() {
        val intent=Intent(context,BluetoothActivity::class.java)
            .putExtra("name","Heart rate").putExtra("filter",BlueWriter.SERVICE_HEART_RATE.toString())
            .putExtra(BluetoothActivity.DEVICE_REMEMBER,true)
        val controller=Robolectric.buildActivity(BluetoothActivity::class.java,intent).setup()
        try {
            assertTrue(controller.get().findViewById<android.widget.CheckBox>(R.id.bluetooth_remember).isChecked)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun rememberedDeviceSurvivesConnectionTimeoutAndPickerCancellation() {
        val gatt=start()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(61))
        assertEquals(address,prefs.getString(key,null))
        assertTrue(shadowOf(gatt).isClosed)
        val picker=shadowOf(context).nextStartedActivity
        assertEquals(BluetoothActivity::class.java.name,picker.component!!.className)
        assertTrue(picker.getBooleanExtra(BluetoothActivity.DEVICE_REMEMBER,false))
        context.sendBroadcast(Intent("svenmeier.coxswain.bluetooth.SELECTED").setPackage(context.packageName))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(address,prefs.getString(key,null))
    }
    @Test fun temporaryDisconnectDoesNotReopenPickerOrForgetWatch() {
        val gatt=start()
        shadowOf(gatt).gattCallback.onConnectionStateChange(gatt,0,BluetoothProfile.STATE_DISCONNECTED)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(address,prefs.getString(key,null))
        assertFalse(shadowOf(gatt).isClosed)
        assertNull(shadowOf(context).nextStartedActivity)
    }
    @Test fun destroyedConnectionCannotOpenPickerFromLateCallbacks() {
        val gatt=start()
        heart!!.destroy()
        shadowOf(gatt).gattCallback.onConnectionStateChange(gatt,0,BluetoothProfile.STATE_DISCONNECTED)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(61))
        assertEquals(address,prefs.getString(key,null))
        assertNull(shadowOf(context).nextStartedActivity)
    }
    @Test fun explicitUncheckedSelectionForgetsAndCheckedSelectionReplacesWatch() {
        start()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(61))
        val next="11:22:33:44:55:66"
        context.sendBroadcast(Intent("svenmeier.coxswain.bluetooth.SELECTED").setPackage(context.packageName)
            .putExtra(BluetoothActivity.DEVICE_ADDRESS,next).putExtra(BluetoothActivity.DEVICE_REMEMBER,false))
        shadowOf(Looper.getMainLooper()).idle()
        assertNull(prefs.getString(key,null))
        // That manually selected connection falls back after ten seconds.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(11))
        context.sendBroadcast(Intent("svenmeier.coxswain.bluetooth.SELECTED").setPackage(context.packageName)
            .putExtra(BluetoothActivity.DEVICE_ADDRESS,next).putExtra(BluetoothActivity.DEVICE_REMEMBER,true))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(next,prefs.getString(key,null))
    }
    @Test fun nextHeartInstanceReusesRememberedDeviceAfterShutdown() {
        start();heart!!.destroy()
        val gatt=start()
        assertEquals(address,gatt.device.address)
        assertNull(shadowOf(context).nextStartedActivity)
    }
}
