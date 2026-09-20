package svenmeier.coxswain.google

import androidx.health.connect.client.HealthConnectClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class HealthConnectDeviceTest {

    @Test
    fun sdkAndPermissionStateCanBeQueried() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val status = HealthConnectClient.getSdkStatus(context)
        assertTrue(
            status == HealthConnectClient.SDK_AVAILABLE ||
                status == HealthConnectClient.SDK_UNAVAILABLE ||
                status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
        )

        if (status == HealthConnectClient.SDK_AVAILABLE) {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = HealthConnectBridge.getGrantedPermissionsAsync(client)
                .get(10, TimeUnit.SECONDS)
            assertTrue(HealthConnectBridge.getWritePermissions().containsAll(granted))
        }
    }
}
