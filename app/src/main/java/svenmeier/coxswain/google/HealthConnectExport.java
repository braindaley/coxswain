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
import androidx.health.connect.client.PermissionController;
import androidx.health.connect.client.records.DistanceRecord;
import androidx.health.connect.client.records.ExerciseSessionRecord;
import androidx.health.connect.client.records.HeartRateRecord;
import androidx.health.connect.client.records.PowerRecord;
import androidx.health.connect.client.records.Record;
import androidx.health.connect.client.records.SpeedRecord;
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord;
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
    private final HealthConnectClient client;

    private static final Set<String> PERMISSIONS;
    static {
        PERMISSIONS = new HashSet<>();
        PERMISSIONS.add(HealthConnectBridge.getWritePermission(ExerciseSessionRecord.class));
        PERMISSIONS.add(HealthConnectBridge.getWritePermission(HeartRateRecord.class));
        PERMISSIONS.add(HealthConnectBridge.getWritePermission(SpeedRecord.class));
        PERMISSIONS.add(HealthConnectBridge.getWritePermission(PowerRecord.class));
        PERMISSIONS.add(HealthConnectBridge.getWritePermission(TotalCaloriesBurnedRecord.class));
        PERMISSIONS.add(HealthConnectBridge.getWritePermission(DistanceRecord.class));
    }

    public HealthConnectExport(Context context) {
        super(context);
        this.gym = Gym.instance(context);
        this.client = HealthConnectClient.getOrCreate(context);
    }

    @Override
    public void start(Workout workout, boolean automatic) {
        checkPermissions(workout);
    }

    private void checkPermissions(Workout workout) {
        ListenableFuture<Set<String>> grantedFuture = HealthConnectBridge.getGrantedPermissionsAsync(client);
        Futures.addCallback(grantedFuture, new FutureCallback<Set<String>>() {
            @Override
            public void onSuccess(Set<String> granted) {
                if (granted.containsAll(PERMISSIONS)) {
                    export(workout);
                } else {
                    requestPermissions();
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                toast(context.getString(R.string.googlefit_export_failed));
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void requestPermissions() {
        if (context instanceof Activity) {
            try {
                Intent intent = PermissionController.createRequestPermissionResultContract().createIntent(context, PERMISSIONS);
                ((Activity) context).startActivityForResult(intent, 0);
            } catch (Exception e) {
                toast(context.getString(R.string.googlefit_export_failed));
            }
        } else {
            toast(context.getString(R.string.googlefit_export_permissions_manual));
        }
    }

    private void export(Workout workout) {
        toast(context.getString(R.string.googlefit_export_starting));

        List<Snapshot> snapshots = gym.getSnapshots(workout).list();
        List<Record> records = new Workout2HealthConnect().map(workout, snapshots);

        ListenableFuture<InsertRecordsResponse> insertFuture = HealthConnectBridge.insertRecordsAsync(client, records);
        Futures.addCallback(insertFuture, new FutureCallback<InsertRecordsResponse>() {
            @Override
            public void onSuccess(InsertRecordsResponse result) {
                toast(context.getString(R.string.googlefit_export_finished));
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e(Coxswain.TAG, "Health Connect export failed", t);
                toast(context.getString(R.string.googlefit_export_failed));
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void toast(final String text) {
        handler.post(() -> Toast.makeText(context, text, Toast.LENGTH_LONG).show());
    }
}
