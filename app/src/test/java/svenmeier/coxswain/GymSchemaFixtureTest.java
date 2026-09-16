package svenmeier.coxswain;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import propoid.db.Repository;
import propoid.db.schema.Column;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.SessionType;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.gym.WorkoutDefinition;
import svenmeier.coxswain.gym.WorkoutStatus;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class GymSchemaFixtureTest {

    private static final String DATABASE = "gym-v1-fixture";

    private Context context;
    private Repository repository;

    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase(DATABASE);

        SQLiteDatabase database = context.openOrCreateDatabase(DATABASE, Context.MODE_PRIVATE, null);
        String sql;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/migrations/gym-v1.sql"),
                StandardCharsets.UTF_8))) {
            sql = reader.lines().collect(Collectors.joining("\n"));
        }
        for (String statement : sql.split(";")) {
            if (!statement.trim().isEmpty()) {
                database.execSQL(statement);
            }
        }
        database.close();

        repository = new Repository(context, DATABASE, new GymVersioning());
    }

    @After
    public void tearDown() {
        repository.close();
        context.deleteDatabase(DATABASE);
    }

    @Test
    public void currentVersionReadsExistingProgramsWorkoutsAndSnapshots() {
        assertEquals(2, repository.query(new Program()).count());
        assertEquals(3, repository.query(new Segment()).count());
        assertEquals(3, repository.query(new Workout()).count());
        assertEquals(3, repository.query(new Snapshot()).count());
        assertEquals(GymVersioning.DATABASE_VERSION, repository.getDatabase().getVersion());

        Program intervals = repository.query(new Program()).list().get(1);
        assertEquals("Legacy intervals", intervals.name.get());
        assertEquals(4, intervals.getSegmentsCount());
        assertEquals(Integer.valueOf(300), intervals.getSegment(0).duration.get());

        Workout firstWorkout = repository.query(new Workout()).list().get(0);
        assertEquals("Legacy 2K", firstWorkout.programName("Unknown"));
        assertEquals(Integer.valueOf(2000), firstWorkout.distance.get());
        assertEquals(SessionType.DISTANCE, firstWorkout.sessionType.get());
        assertEquals(WorkoutStatus.COMPLETED, firstWorkout.status.get());
        assertEquals(Long.valueOf(1700000500000L), firstWorkout.completed.get());
        Program frozen = WorkoutDefinition.thaw(firstWorkout.programDefinition.get());
        assertEquals("Legacy 2K", frozen.name.get());
        assertEquals(2000, frozen.getSegment(0).distance.get().intValue());

        Workout orphaned = repository.query(new Workout()).list().get(2);
        assertEquals("Workout", orphaned.programName("Unknown"));
        assertEquals(SessionType.DISTANCE, orphaned.sessionType.get());
        assertEquals(6200, WorkoutDefinition.thaw(orphaned.programDefinition.get())
                .getSegment(0).distance.get().intValue());
    }

    @Test
    public void fixtureMatchesCurrentSchemaExactly() {
        repository.query(new Program());
        repository.query(new Segment());
        repository.query(new Workout());
        repository.query(new Snapshot());

        assertColumns("Program", "_id", "_type", "name", "segments");
        assertColumns("Segment", "_id", "_type", "difficulty", "distance", "duration",
                "strokes", "energy", "speed", "strokeRate", "pulse", "power");
        assertColumns("Workout", "_id", "_type", "program", "location", "start", "duration",
                "distance", "strokes", "energy", "evaluate", "sessionType", "programName",
                "programDefinition", "status", "pausedDuration", "completed", "goalType",
                "goalTarget", "raceReference", "raceOutcome", "raceMargin");
        assertColumns("Snapshot", "_id", "_type", "workout", "difficulty", "distance",
                "strokes", "energy", "speed", "pulse", "strokeRate", "strokeRatio", "power");
    }

    private void assertColumns(String table, String... expected) {
        Set<String> actual = Column.get(table, repository.getDatabase()).stream()
                .map(column -> column.name)
                .collect(Collectors.toSet());
        assertEquals(new HashSet<>(Arrays.asList(expected)), actual);
    }
}
