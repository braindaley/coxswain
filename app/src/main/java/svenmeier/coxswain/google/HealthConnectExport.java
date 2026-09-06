package svenmeier.coxswain.google;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.records.Record;
import androidx.health.connect.client.response.InsertRecordsResponse;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.io.Export;

public class HealthConnectExport extends Export<Workout> {

    private final Handler handler = new Handler();
    private final Gym gym;
    private HealthConnectClient client;

    private static final Set<String> PERMISSIONS;
    static {
        PERMISSIONS = new HashSet<>();
        PERMISSIONS.add("android.permission.health.WRITE_EXERCISE");
        PERMISSIONS.add("android.permission.health.WRITE_HEART_RATE");
        PERMISSIONS.add("android.permission.health.WRITE_SPEED");
        PERMISSIONS.add("android.permission.health.WRITE_POWER");
        PERMISSIONS.add("android.permission.health.WRITE_TOTAL_CALORIES_BURNED");
        PERMISSIONS.add("android.permission.health.WRITE_DISTANCE");
    }

    public HealthConnectExport(Context context) {
        super(context);
        this.gym = Gym.instance(context);
        try {
            this.client = HealthConnectClient.getOrCreate(context);
        } catch (Exception e) {
            Log.e(Coxswain.TAG, "Health Connect not available", e);
        }
    }

    @Override
    public void start(Workout workout, boolean automatic) {
        if (client == null) {
            toast("Health Connect not available on this device");
            return;
        }
        checkPermissions(workout);
    }

    private void checkPermissions(Workout workout) {
        ListenableFuture<Set<String>> grantedFuture = HealthConnectBridge.getGrantedPermissionsAsync(client);
        Futures.addCallback(grantedFuture, new FutureCallback<Set<String>>() {
            @Override
            public void onSuccess(Set<String> granted) {
                // We require at least Exercise permission to do anything useful
                if (granted.contains("android.permission.health.WRITE_EXERCISE")) {
                    export(workout);
                } else {
                    requestPermissions();
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                toast("Permission check failed: " + t.getMessage());
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void requestPermissions() {
        if (context instanceof Activity) {
            try {
                Intent intent = HealthConnectBridge.createPermissionIntent(context, PERMISSIONS);
                Log.d(Coxswain.TAG, "Starting Health Connect permission request: " + intent.getAction());
                ((Activity) context).startActivityForResult(intent, 0);
            } catch (Exception e) {
                Log.e(Coxswain.TAG, "Permission request failed, trying settings", e);
                openSettings();
            }
        } else {
            toast(context.getString(R.string.healthconnect_export_permissions_manual));
        }
    }

    private void openSettings() {
        try {
            Intent intent = HealthConnectBridge.getSettingsIntent();
            context.startActivity(intent);
            toast("Please grant Coxswain permissions in Health Connect settings");
        } catch (Exception e) {
            toast("Could not open Health Connect settings");
        }
    }

    private void export(Workout workout) {
        toast(context.getString(R.string.healthconnect_export_starting));

        List<Snapshot> snapshots = gym.getSnapshots(workout).list();
        List<Record> records = new Workout2HealthConnect().map(workout, snapshots);

        if (records.isEmpty()) {
            toast("No data to export");
            return;
        }

        ListenableFuture<InsertRecordsResponse> insertFuture = HealthConnectBridge.insertRecordsAsync(client, records);
        Futures.addCallback(insertFuture, new FutureCallback<InsertRecordsResponse>() {
            @Override
            public void onSuccess(InsertRecordsResponse result) {
                Log.d(Coxswain.TAG, "Inserted " + result.getRecordIdsList().size() + " records into Health Connect: " + result.getRecordIdsList());
                toast(context.getString(R.string.healthconnect_export_finished));
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e(Coxswain.TAG, "Health Connect export failed", t);
                toast(context.getString(R.string.healthconnect_export_failed) + ": " + t.getMessage());
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void toast(final String text) {
        handler.post(() -> Toast.makeText(context, text, Toast.LENGTH_LONG).show());
    }
}
