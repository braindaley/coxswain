package svenmeier.coxswain.google;

import androidx.health.connect.client.records.DistanceRecord;
import androidx.health.connect.client.records.ExerciseSessionRecord;
import androidx.health.connect.client.records.HeartRateRecord;
import androidx.health.connect.client.records.PowerRecord;
import androidx.health.connect.client.records.Record;
import androidx.health.connect.client.records.SpeedRecord;
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord;
import androidx.health.connect.client.records.metadata.Device;
import androidx.health.connect.client.records.metadata.Metadata;
import androidx.health.connect.client.units.Energy;
import androidx.health.connect.client.units.Length;
import androidx.health.connect.client.units.Power;
import androidx.health.connect.client.units.Velocity;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;

public class Workout2HealthConnect {

    public static final int MAX_SAMPLES = 1000;

    public List<Record> map(Workout workout, List<Snapshot> snapshots) {
        List<Record> records = new ArrayList<>();

        Instant start = Instant.ofEpochMilli(workout.start.get());
        Instant end = start.plusMillis(workout.duration.get() * 1000L);
        ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(start);

        // 1. Exercise Session
        String workoutId = "coxswain_workout_" + workout.start.get();
        Metadata sessionMetadata = Metadata.manualEntry(workoutId, 0, (Device) null);
        records.add(new ExerciseSessionRecord(
                start,
                zoneOffset,
                end,
                zoneOffset,
                sessionMetadata,
                ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE,
                workout.programName("Rowing"),
                "Recorded with Coxswain on WaterRower", // Notes
                Collections.emptyList(), // Segments
                Collections.emptyList(), // Laps
                null // Route
        ));

        // 2. Heart Rate
        List<HeartRateRecord.Sample> hrSamples = new ArrayList<>();
        // 3. Speed
        List<SpeedRecord.Sample> speedSamples = new ArrayList<>();
        // 4. Power
        List<PowerRecord.Sample> powerSamples = new ArrayList<>();

        int size = snapshots.size();
        int step = Math.max(1, size / MAX_SAMPLES);

        for (int i = 0; i < size; i += step) {
            Snapshot snapshot = snapshots.get(i);
            Instant time = start.plusMillis(i * 1000L);
            
            // Health Connect requires heart rate > 0
            int pulse = snapshot.pulse.get();
            if (pulse > 0) {
                hrSamples.add(new HeartRateRecord.Sample(time, (long) pulse));
            }

            // Health Connect requires speed >= 0
            int speed = snapshot.speed.get();
            if (speed >= 0) {
                speedSamples.add(new SpeedRecord.Sample(time, Velocity.metersPerSecond((double) speed / 100.0)));
            }

            // Health Connect requires power >= 0
            int power = snapshot.power.get();
            if (power >= 0) {
                powerSamples.add(new PowerRecord.Sample(time, Power.watts((double) power)));
            }
        }

        if (!hrSamples.isEmpty()) {
            records.add(new HeartRateRecord(start, zoneOffset, end, zoneOffset, hrSamples, Metadata.manualEntry((Device) null)));
        }
        if (!speedSamples.isEmpty()) {
            records.add(new SpeedRecord(start, zoneOffset, end, zoneOffset, speedSamples, Metadata.manualEntry((Device) null)));
        }
        if (!powerSamples.isEmpty()) {
            records.add(new PowerRecord(start, zoneOffset, end, zoneOffset, powerSamples, Metadata.manualEntry((Device) null)));
        }

        // 5. Total Calories
        int energy = workout.energy.get();
        if (energy >= 0) {
            records.add(new TotalCaloriesBurnedRecord(
                    start,
                    zoneOffset,
                    end,
                    zoneOffset,
                    Energy.kilocalories((double) energy),
                    Metadata.manualEntry((Device) null)
            ));
        }

        // 6. Distance
        int distance = workout.distance.get();
        if (distance >= 0) {
            records.add(new DistanceRecord(
                    start,
                    zoneOffset,
                    end,
                    zoneOffset,
                    Length.meters((double) distance),
                    Metadata.manualEntry((Device) null)
            ));
        }

        return records;
    }
}
