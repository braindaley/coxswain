package svenmeier.coxswain.google;

import androidx.health.connect.client.records.DistanceRecord;
import androidx.health.connect.client.records.ExerciseSessionRecord;
import androidx.health.connect.client.records.HeartRateRecord;
import androidx.health.connect.client.records.PowerRecord;
import androidx.health.connect.client.records.Record;
import androidx.health.connect.client.records.SpeedRecord;
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class Workout2HealthConnectTest {

    @Test
    public void mapsWorkoutSummaryAndSamples() {
        Workout workout = new Workout();
        workout.start.set(1_700_000_000_000L);
        workout.duration.set(1001);
        workout.distance.set(4000);
        workout.energy.set(250);

        List<Snapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            Snapshot snapshot = new Snapshot();
            snapshot.pulse.set(140);
            snapshot.speed.set(400);
            snapshot.power.set(180);
            snapshots.add(snapshot);
        }

        List<Record> records = new Workout2HealthConnect().map(workout, snapshots);

        assertEquals(6, records.size());
        assertTrue(hasRecord(records, ExerciseSessionRecord.class));
        assertTrue(hasRecord(records, HeartRateRecord.class));
        assertTrue(hasRecord(records, SpeedRecord.class));
        assertTrue(hasRecord(records, PowerRecord.class));
        assertTrue(hasRecord(records, TotalCaloriesBurnedRecord.class));
        assertTrue(hasRecord(records, DistanceRecord.class));

        HeartRateRecord heartRate = (HeartRateRecord) findRecord(records, HeartRateRecord.class);
        assertTrue(heartRate.getSamples().size() <= Workout2HealthConnect.MAX_SAMPLES);
    }

    private boolean hasRecord(List<Record> records, Class<? extends Record> type) {
        return findRecord(records, type) != null;
    }

    private Record findRecord(List<Record> records, Class<? extends Record> type) {
        for (Record record : records) {
            if (type.isInstance(record)) {
                return record;
            }
        }
        return null;
    }
}
