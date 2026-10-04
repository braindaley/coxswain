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
    public void exportUsesRecordedTimesDeduplicatesAndPreservesPauseGaps() {
        Workout workout = new Workout();
        long start = 1_700_000_000_000L;
        workout.start.set(start); workout.duration.set(60); workout.pausedDuration.set(30);
        workout.completed.set(start + 90_000);
        Snapshot first = new Snapshot();
        first.duration.set(10); first.power.set(100); first.recordedAt.set(start + 10_000);
        Snapshot duplicate = new Snapshot();
        duplicate.duration.set(10); duplicate.power.set(120); duplicate.recordedAt.set(start + 10_000);
        Snapshot afterPause = new Snapshot();
        afterPause.duration.set(40); afterPause.power.set(200); afterPause.recordedAt.set(start + 70_000);
        List<Record> records = new Workout2HealthConnect().map(workout, java.util.Arrays.asList(first, duplicate, afterPause));
        PowerRecord power = (PowerRecord) findRecord(records, PowerRecord.class);
        assertEquals(2, power.getSamples().size());
        assertEquals(start + 10_000, power.getSamples().get(0).getTime().toEpochMilli());
        assertEquals(start + 70_000, power.getSamples().get(1).getTime().toEpochMilli());
        assertEquals(start + 90_000, power.getEndTime().toEpochMilli());
    }

    @Test
    public void exportUsesElapsedSecondsWhenLegacyWallTimesAreAbsent() {
        Workout workout = new Workout();
        workout.start.set(1_700_000_000_000L); workout.duration.set(100);
        Snapshot first = new Snapshot(); first.duration.set(10); first.power.set(100);
        Snapshot last = new Snapshot(); last.duration.set(80); last.power.set(200);
        PowerRecord power = (PowerRecord) findRecord(new Workout2HealthConnect().map(workout, java.util.Arrays.asList(first, last)), PowerRecord.class);
        assertEquals(workout.start.get() + 80_000, power.getSamples().get(1).getTime().toEpochMilli());
    }

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
        assertEquals("coxswain_workout_1700000000000", records.get(0).getMetadata().getClientRecordId());
        assertEquals("coxswain_workout_1700000000000_heart", heartRate.getMetadata().getClientRecordId());
    }

    @Test
    public void exerciseOnlyPermissionExportsSessionWithoutOptionalRecords() {
        Workout workout = new Workout();
        workout.start.set(1_700_000_000_000L);
        workout.duration.set(60);
        workout.distance.set(250);
        List<Record> records = new Workout2HealthConnect().map(workout, new ArrayList<>(),
                java.util.Collections.singleton("android.permission.health.WRITE_EXERCISE"));
        assertEquals(1, records.size());
        assertTrue(records.get(0) instanceof ExerciseSessionRecord);
    }

    @Test
    public void emptyWorkoutDoesNotProduceInvalidHealthRecords() {
        assertTrue(new Workout2HealthConnect().map(new Workout(), new ArrayList<>()).isEmpty());
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
