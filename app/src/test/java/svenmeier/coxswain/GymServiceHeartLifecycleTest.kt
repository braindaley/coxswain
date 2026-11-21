package svenmeier.coxswain

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.content.Intent
import android.os.Looper
import androidx.preference.PreferenceManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import svenmeier.coxswain.bluetooth.BluetoothHeart
import java.time.Duration
import java.util.ArrayDeque

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class GymServiceHeartLifecycleTest {
    @Test fun transportReconnectReplacesWatchClientAndServiceShutdownCancelsRetries() {
        val context=RuntimeEnvironment.getApplication()
        shadowOf(context).grantPermissions(Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT)
        BluetoothAdapter.getDefaultAdapter().enable()
        val prefs=PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(context.getString(R.string.preference_hardware_heart_sensor),BluetoothHeart::class.java.name)
            .putString(context.getString(R.string.preference_bluetooth_heart_device),"AA:BB:CC:DD:EE:FF").commit()
        val controller=Robolectric.buildService(GymService::class.java).create()
        val service=controller.get()
        fun gatt(): BluetoothGatt {
            val heart=GymService::class.java.getDeclaredField("heart").apply { isAccessible=true }.get(service)
            val stack=BluetoothHeart::class.java.getDeclaredField("connections").apply { isAccessible=true }.get(heart) as ArrayDeque<*>
            val current=stack.peek()!!
            return current.javaClass.getDeclaredField("connected").apply { isAccessible=true }.get(current) as BluetoothGatt
        }
        var destroyed=false
        try {
            service.onStartCommand(Intent().putExtra(GymService.CONNECTOR_MOCK,true),0,1)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))
            val original=gatt()
            service.onConnected() // The rower transport reconnects while the service stays alive.
            val replacement=gatt()
            assertNotSame(original,replacement)
            assertTrue(shadowOf(original).isClosed)
            controller.destroy();destroyed=true
            assertTrue(shadowOf(replacement).isClosed)
            val device=shadowOf(replacement.device)
            val attempts=device.bluetoothGatts.size
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(61))
            assertEquals(attempts,device.bluetoothGatts.size)
        } finally {
            if (!destroyed) controller.destroy()
            prefs.edit().clear().commit()
        }
    }
}
