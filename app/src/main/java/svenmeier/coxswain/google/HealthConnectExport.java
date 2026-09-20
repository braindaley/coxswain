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
import svenmeier.coxswain.DiagnosticsLog;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.io.Export;
import propoid.util.content.Preference;

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
        if (wasExported(workout)) {
            if (!automatic) toast(context.getString(R.string.ui_health_already_synced));
            return;
        }
        if (client == null) {
            toast(context.getString(R.string.ui_health_unavailable));
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
                DiagnosticsLog.record(context, "Health Connect permission check failed: " + t.getMessage());
                toast(context.getString(R.string.ui_health_permission_failed, t.getMessage()));
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
            toast(context.getString(R.string.ui_health_grant_in_settings));
        } catch (Exception e) {
            toast(context.getString(R.string.ui_health_settings_failed));
        }
    }

    private void export(Workout workout) {
        toast(context.getString(R.string.healthconnect_export_starting));

        List<Snapshot> snapshots = gym.getSnapshots(workout).list();
        List<Record> records = new Workout2HealthConnect().map(workout, snapshots);

        if (records.isEmpty()) {
            toast(context.getString(R.string.ui_health_no_data));
            return;
        }

        ListenableFuture<InsertRecordsResponse> insertFuture = HealthConnectBridge.insertRecordsAsync(client, records);
        Futures.addCallback(insertFuture, new FutureCallback<InsertRecordsResponse>() {
            @Override
            public void onSuccess(InsertRecordsResponse result) {
                markExported(workout);
                Log.d(Coxswain.TAG, "Inserted " + result.getRecordIdsList().size() + " records into Health Connect: " + result.getRecordIdsList());
                toast(context.getString(R.string.healthconnect_export_finished));
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e(Coxswain.TAG, "Health Connect export failed", t);
                DiagnosticsLog.record(context, "Health Connect export failed: " + t.getMessage());
                toast(context.getString(R.string.healthconnect_export_failed) + ": " + t.getMessage());
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void toast(final String text) {
        handler.post(() -> Toast.makeText(context, text, Toast.LENGTH_LONG).show());
    }

    private boolean wasExported(Workout workout) {
        return context.getSharedPreferences("health_connect_exports", Context.MODE_PRIVATE)
                .getBoolean(Long.toString(workout.start.get()), false);
    }

    private void markExported(Workout workout) {
        context.getSharedPreferences("health_connect_exports", Context.MODE_PRIVATE).edit()
                .putBoolean(Long.toString(workout.start.get()), true).apply();
    }

    /** Syncs every finalized workout. Stable client record IDs and local markers make retries safe. */
    public static void syncHistory(Context context) {
        List<Workout> workouts = Gym.instance(context).getWorkouts().list();
        if (workouts.isEmpty()) {
            Toast.makeText(context, R.string.ui_health_no_history, Toast.LENGTH_SHORT).show();
            return;
        }
        int pending = 0;
        for (Workout workout : workouts) {
            HealthConnectExport export = new HealthConnectExport(context);
            if (!export.wasExported(workout)) {
                pending++;
                export.start(workout, false);
            }
        }
        String message = pending == 0
                ? context.getString(R.string.ui_health_up_to_date)
                : context.getResources().getQuantityString(R.plurals.ui_health_syncing_history, pending, pending);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static void enableAutomatic(Context context) {
        Preference.getString(context, R.string.preference_export_last).set(HealthConnectExport.class.getName());
        Preference.getBoolean(context, R.string.preference_export_auto).set(true);
        Toast.makeText(context, R.string.ui_health_auto_enabled, Toast.LENGTH_LONG).show();
    }
}
