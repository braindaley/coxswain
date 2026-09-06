package svenmeier.coxswain.google

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.response.InsertRecordsResponse
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.guava.future
import kotlin.reflect.KClass

object HealthConnectBridge {
    @JvmStatic
    fun insertRecordsAsync(
        client: HealthConnectClient,
        records: List<Record>
    ): ListenableFuture<InsertRecordsResponse> {
        return GlobalScope.future {
            client.insertRecords(records)
        }
    }

    @JvmStatic
    fun getGrantedPermissionsAsync(
        client: HealthConnectClient
    ): ListenableFuture<Set<String>> {
        return GlobalScope.future {
            client.permissionController.getGrantedPermissions()
        }
    }

    @JvmStatic
    fun getWritePermission(clazz: Class<out Record>): String {
        return HealthPermission.getWritePermission(clazz.kotlin)
    }

    @JvmStatic
    fun createPermissionIntent(context: Context, permissions: Set<String>): Intent {
        // We use the framework action directly for Android 14+ to avoid the "virtual" action
        // which only works with ActivityResultLauncher.
        val intent = Intent("android.health.connect.action.REQUEST_PERMISSIONS")
        intent.putExtra("android.health.connect.extra.PERMISSIONS", ArrayList(permissions))
        return intent
    }

    @JvmStatic
    fun getSettingsIntent(): Intent {
        return Intent("android.health.connect.action.HEALTH_CONNECT_SETTINGS")
    }
}
