package svenmeier.coxswain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import propoid.db.Repository;
import svenmeier.coxswain.gym.Difficulty;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;

@RunWith(AndroidJUnit4.class)
public class DeviceDataValidationTest {

    @Test
    public void v1FixtureMigratesWithAndroidSqlite() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String databaseName = "gym-v1-device-fixture";
        context.deleteDatabase(databaseName);

        SQLiteDatabase database = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null);
        String sql;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("gym-v1.sql"),
                StandardCharsets.UTF_8))) {
            sql = reader.lines().collect(Collectors.joining("\n"));
        }
        for (String statement : sql.split(";")) {
            if (!statement.trim().isEmpty()) database.execSQL(statement);
        }
        database.close();

        Repository repository = new Repository(context, databaseName, new GymVersioning());
        try {
            // Propoid opens and versions each mapped type lazily on first use.
            assertEquals(2, repository.query(new Program()).count());
            assertEquals(3, repository.query(new Workout()).count());
            assertEquals(3, repository.query(new Snapshot()).count());
            assertEquals(GymVersioning.DATABASE_VERSION, repository.getDatabase().getVersion());
            Workout migrated = repository.query(new Workout()).list().get(0);
            assertEquals("Legacy 2K", migrated.programName("Unknown"));
            assertEquals(Integer.valueOf(2000), migrated.distance.get());
            assertNotNull(migrated.programDefinition.get());
        } finally {
            repository.close();
            context.deleteDatabase(databaseName);
        }
    }

    @Test
    public void backupRestoreRoundTripsThroughAndroidStorage() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Gym gym = Gym.instance(context);
        gym.deselect();
        String name = "Device backup " + System.currentTimeMillis();
        Program program = Program.meters(name, 300, Difficulty.EASY);
        gym.mergeProgram(program);
        gym.select(program);
        Measurement measurement = new Measurement();
        measurement.setDuration(3);
        measurement.setDistance(300);
        measurement.setStrokeRate(24);
        gym.onMeasured(measurement);
        Workout workout = gym.complete();
        long start = workout.start.get();
        String backup = gym.createBackup();

        gym.delete(workout);
        gym.delete(program);
        gym.restoreBackup(backup);

        Workout restored = gym.getWorkouts().list().stream()
                .filter(value -> value.start.get() == start).findFirst().orElse(null);
        Program restoredProgram = gym.getPrograms().list().stream()
                .filter(value -> name.equals(value.name.get())).findFirst().orElse(null);
        assertNotNull(restored);
        assertNotNull(restoredProgram);
        assertEquals(Integer.valueOf(300), restored.distance.get());
        assertTrue(gym.getSnapshots(restored).count() > 0);

        gym.delete(restored);
        gym.delete(restoredProgram);
    }
}
