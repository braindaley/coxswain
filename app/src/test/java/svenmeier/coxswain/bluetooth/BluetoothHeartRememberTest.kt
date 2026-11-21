package svenmeier.coxswain.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
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
import svenmeier.coxswain.DiagnosticsLog
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
    private fun connection(): Any {
        val field=BluetoothHeart::class.java.getDeclaredField("connections").apply { isAccessible=true }
        return (field.get(heart) as ArrayDeque<*>).peek()!!
    }
    private fun currentGatt(): BluetoothGatt {
        val current=connection()
        return current.javaClass.getDeclaredField("connected").apply { isAccessible=true }.get(current) as BluetoothGatt
    }
    private fun start(measurement: Measurement = Measurement()): BluetoothGatt {
        heart=BluetoothHeart(context,measurement) {}
        return currentGatt()
    }
    private fun selectManually() {
        val current=connection()
        current.javaClass.getDeclaredMethod("select").apply { isAccessible=true }.invoke(current)
        shadowOf(Looper.getMainLooper()).idle()
    }
    private fun services(gatt: BluetoothGatt, allowNotifications: Boolean = true): BluetoothGattCharacteristic {
        val service=BluetoothGattService(BlueWriter.SERVICE_HEART_RATE,BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristic=BluetoothGattCharacteristic(BlueWriter.CHARACTERISTIC_HEART_RATE_MEASUREMENT,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,BluetoothGattCharacteristic.PERMISSION_READ)
        characteristic.addDescriptor(BluetoothGattDescriptor(BlueWriter.CLIENT_CHARACTERISTIC_DESCRIPTOR,
            BluetoothGattDescriptor.PERMISSION_WRITE))
        service.addCharacteristic(characteristic)
        shadowOf(gatt).addDiscoverableService(service)
        if (allowNotifications) shadowOf(gatt).allowCharacteristicNotification(characteristic)
        shadowOf(gatt).gattCallback.onConnectionStateChange(gatt,BluetoothGatt.GATT_SUCCESS,BluetoothProfile.STATE_CONNECTED)
        return characteristic
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
        assertNull(shadowOf(context).nextStartedActivity)
        assertNotSame(gatt,currentGatt())
        selectManually()
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
        assertTrue(shadowOf(gatt).isClosed)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))
        assertNotSame(gatt,currentGatt())
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
        selectManually()
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

    @Test fun serviceDiscoveryWithoutReadingsStillTimesOutAndRetriesFreshConnection() {
        val gatt=start()
        services(gatt)
        assertFalse(DiagnosticsLog.latest(context).contains("Heart-rate readings received"))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(21))
        assertTrue(shadowOf(gatt).isClosed)
        assertNotSame(gatt,currentGatt())
        assertNull(shadowOf(context).nextStartedActivity)
        assertTrue(DiagnosticsLog.latest(context).contains("No heart-rate reading arrived"))
        assertEquals(address,prefs.getString(key,null))
    }

    @Test fun modernNotificationConfirmsReadingAndMissingPacketsClearStalePulse() {
        val measurement=Measurement()
        val gatt=start(measurement)
        val characteristic=services(gatt)
        val callback=shadowOf(gatt).gattCallback
        callback.onCharacteristicChanged(gatt,characteristic,byteArrayOf(0,120))
        assertEquals(120,measurement.pulse)
        assertTrue(DiagnosticsLog.latest(context).contains("Heart-rate readings received"))
        // Identical BPM is still fresh when new packets continue to arrive.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))
        callback.onCharacteristicChanged(gatt,characteristic,byteArrayOf(0,120))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))
        assertEquals(120,measurement.pulse)
        assertFalse(shadowOf(gatt).isClosed)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6))
        assertEquals(0,measurement.pulse)
        assertTrue(shadowOf(gatt).isClosed)
        assertTrue(DiagnosticsLog.latest(context).contains("readings stopped arriving"))
        callback.onCharacteristicChanged(gatt,characteristic,byteArrayOf(0,121))
        assertEquals(0,measurement.pulse) // Late packets cannot resurrect the closed connection.
    }

    @Test fun savedWatchCanRecoverAfterBroadcastBecomesAvailableLater() {
        val measurement=Measurement()
        val old=start(measurement)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(21))
        val next=currentGatt()
        assertNotSame(old,next)
        val characteristic=services(next)
        characteristic.value=byteArrayOf(0,95)
        shadowOf(next).gattCallback.onCharacteristicChanged(next,characteristic)
        assertEquals(95,measurement.pulse)
        assertNull(shadowOf(context).nextStartedActivity)
    }

    @Test fun failedNotificationSubscriptionAndGattErrorsDoNotClaimConnectionSuccess() {
        val first=start()
        services(first,false)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))
        assertTrue(shadowOf(first).isClosed)
        val next=currentGatt()
        shadowOf(next).gattCallback.onConnectionStateChange(next,133,BluetoothProfile.STATE_CONNECTED)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))
        assertTrue(shadowOf(next).isClosed)
        assertNotSame(next,currentGatt())
        assertTrue(DiagnosticsLog.latest(context).contains("status 133"))
    }

    @Test fun malformedAndZeroReadingsNeverCancelTimeout() {
        val measurement=Measurement()
        val gatt=start(measurement)
        val characteristic=services(gatt)
        val callback=shadowOf(gatt).gattCallback
        callback.onCharacteristicChanged(gatt,characteristic,byteArrayOf())
        callback.onCharacteristicChanged(gatt,characteristic,byteArrayOf(1,44))
        callback.onCharacteristicChanged(gatt,characteristic,byteArrayOf(0,0))
        assertEquals(0,measurement.pulse)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(16))
        assertTrue(shadowOf(gatt).isClosed)
    }

    @Test fun descriptorWriteFailureIsRetriedAndFreshConnectionCanDeliverReadings() {
        val measurement=Measurement()
        val gatt=start(measurement)
        val characteristic=services(gatt)
        shadowOf(gatt).gattCallback.onDescriptorWrite(gatt,
            characteristic.getDescriptor(BlueWriter.CLIENT_CHARACTERISTIC_DESCRIPTOR),5)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))
        assertTrue(shadowOf(gatt).isClosed)
        assertTrue(DiagnosticsLog.latest(context).contains("subscription failed"))
        val next=currentGatt()
        val reading=services(next)
        shadowOf(next).gattCallback.onCharacteristicChanged(next,reading,byteArrayOf(1,44,1))
        assertEquals(300,measurement.pulse) // The standard 16-bit heart-rate packet is also supported.
    }

    @Test fun destructionCancelsScheduledReconnectAndRowDisconnectCannotOverwriteHeartFailure() {
        val gatt=start()
        shadowOf(gatt).gattCallback.onConnectionStateChange(gatt,133,BluetoothProfile.STATE_DISCONNECTED)
        shadowOf(Looper.getMainLooper()).idle()
        val device=shadowOf(gatt.device)
        val attempts=device.bluetoothGatts.size
        heart!!.destroy()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(61))
        assertEquals(attempts,device.bluetoothGatts.size)
        DiagnosticsLog.record(context,"Rower connection was lost")
        assertTrue(DiagnosticsLog.latest(context).contains("status 133"))
        assertTrue(DiagnosticsLog.latest(context).contains("Rower connection was lost"))
    }
}
