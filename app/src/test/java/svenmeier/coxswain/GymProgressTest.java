package svenmeier.coxswain;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

import propoid.db.Repository;
import svenmeier.coxswain.gym.Difficulty;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.gym.WorkoutStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class GymProgressTest {

    private Context context;
    private Gym gym;

    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase("gym");

        Constructor<Gym> constructor = Gym.class.getDeclaredConstructor(Context.class);
        constructor.setAccessible(true);
        gym = constructor.newInstance(context);
        gym.initialize();
    }

    @After
    public void tearDown() throws Exception {
        if (gym != null) {
            Field repositoryField = Gym.class.getDeclaredField("repository");
            repositoryField.setAccessible(true);
            ((Repository) repositoryField.get(gym)).close();
        }
        context.deleteDatabase("gym");
    }

    @Test
    public void progressUsesValuesRelativeToSegmentStart() {
        Segment segment = new Segment(Difficulty.HARD).setDistance(1000).setStrokeRate(24);
        Measurement start = measurement(20, 100, 20);
        gym.onMeasured(start);
        Gym.Progress progress = gym.new Progress(segment, start);
        gym.progress = progress;

        gym.onMeasured(measurement(80, 350, 23));
        assertEquals(250, progress.achieved());
        assertEquals(0.25f, progress.completion(), 0.001f);
        assertFalse(progress.inLimit());

        gym.onMeasured(measurement(260, 1200, 24));
        assertEquals(1.0f, progress.completion(), 0.001f);
        assertTrue(progress.inLimit());
    }

    @Test
    public void transitionsAcrossDistanceAndDurationSegments() {
        Program program = new Program("Mixed");
        program.getSegments().clear();
        program.addSegment(new Segment(Difficulty.HARD).setDistance(100));
        program.addSegment(new Segment(Difficulty.REST).setDuration(10));
        gym.select(program);

        assertEquals(Event.PROGRAM_START, gym.onMeasured(measurement(1, 10, 24)));
        assertEquals(Event.SEGMENT_CHANGED, gym.onMeasured(measurement(5, 100, 24)));
        assertEquals(Event.PROGRAM_FINISHED, gym.onMeasured(measurement(15, 110, 24)));
        assertEquals(15, gym.current.duration.get().intValue());
        assertEquals(110, gym.current.distance.get().intValue());
    }

    @Test
    public void freeRowFinalizesIntoHistory() {
        gym.startFreeRow();
        gym.onMeasured(measurement(30, 125, 24));

        Workout finished = gym.endEarly();

        assertEquals(WorkoutStatus.COMPLETED, finished.status.get());
        assertTrue(finished.completed.get() > 0);
        assertFalse(gym.hasActiveSession());
        assertEquals(1, gym.getWorkouts().count());
    }

    @Test
    public void pausedMeasurementsDoNotAdvanceWorkoutOrSnapshots() {
        gym.startFreeRow();
        gym.onMeasured(measurement(10, 100, 24));
        Workout workout = gym.current;

        gym.pause();
        gym.onMeasured(measurement(20, 200, 24));
        assertEquals(10, workout.duration.get().intValue());
        assertEquals(100, workout.distance.get().intValue());

        gym.resume();
        gym.onMeasured(measurement(25, 250, 24));
        assertEquals(15, workout.duration.get().intValue());
        assertEquals(150, workout.distance.get().intValue());
        assertEquals(10, workout.pausedDuration.get().intValue());
        assertEquals(15, gym.getSnapshots(workout).count());
    }

    @Test
    public void pausedMeasurementsDoNotAdvanceTargetProgress() {
        gym.select(Program.meters("200 m", 200, Difficulty.EASY));
        gym.onMeasured(measurement(5, 50, 24));
        assertEquals(50, gym.progress.achieved());

        gym.pause();
        gym.onMeasured(measurement(10, 150, 24));
        assertEquals(50, gym.progress.achieved());

        gym.resume();
        gym.onMeasured(measurement(15, 200, 24));
        assertEquals(100, gym.progress.achieved());
        assertEquals(0.5f, gym.progress.completion(), 0.001f);
    }

    @Test
    public void connectionLossPausesAndPreservesActiveWorkout() {
        gym.startFreeRow();
        gym.connected = true;
        gym.connectedRowerName = "Test rower";
        gym.onMeasured(measurement(10, 100, 24));
        Workout workout = gym.current;

        gym.connectionLost();

        assertTrue(gym.hasActiveSession());
        assertTrue(gym.isPaused());
        assertFalse(gym.connected);
        assertEquals(null, gym.connectedRowerName);
        assertEquals(WorkoutStatus.ACTIVE, workout.status.get());
        assertEquals(0, gym.getWorkouts().count());

        gym.onMeasured(measurement(20, 200, 24));
        assertEquals(10, workout.duration.get().intValue());
        assertEquals(100, workout.distance.get().intValue());
    }

    @Test
    public void connectionLossKeepsPausedWorkoutPaused() {
        gym.startFreeRow();
        gym.onMeasured(measurement(10, 100, 24));
        Workout workout = gym.current;
        gym.pause();

        gym.connectionLost();

        assertTrue(gym.hasActiveSession());
        assertTrue(gym.isPaused());
        assertEquals(WorkoutStatus.ACTIVE, workout.status.get());
        assertEquals(0, gym.getWorkouts().count());
    }

    @Test
    public void backupRestorePreservesProgramsResultsSnapshotsAndPreferences() {
        Program program = Program.meters("Backup 1K", 1000, Difficulty.HARD);
        gym.mergeProgram(program);
        gym.select(program);
        gym.onMeasured(measurement(12, 1000, 26));
        Workout workout = gym.complete();
        long start = workout.start.get();
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("backup_test", true).commit();

        String backup = gym.createBackup();
        gym.delete(workout);
        gym.delete(program);
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("backup_test", false).commit();

        gym.restoreBackup(backup);

        Workout restored = gym.getWorkouts().list().stream().filter(value -> value.start.get() == start).findFirst().orElse(null);
        assertNotNull(restored);
        assertEquals("Backup 1K", restored.programName("Missing"));
        assertEquals(1000, restored.distance.get().intValue());
        assertTrue(gym.getSnapshots(restored).count() > 0);
        assertTrue(androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean("backup_test", false));
        Program restoredProgram = gym.getPrograms().list().stream().filter(value -> "Backup 1K".equals(value.name.get())).findFirst().orElse(null);
        assertNotNull(restoredProgram);
        assertTrue(gym.hasWorkoutHistory(restoredProgram));
    }

    @Test
    public void everyFinalizationPathClearsTheActiveSession() {
        gym.startFreeRow();
        gym.onMeasured(measurement(5, 20, 20));
        assertNotNull(gym.endEarly());
        assertFalse(gym.hasActiveSession());

        gym.select(Program.meters("Finish", 20, Difficulty.EASY));
        gym.onMeasured(measurement(5, 20, 20));
        assertNotNull(gym.complete());
        assertFalse(gym.hasActiveSession());

        gym.startFreeRow();
        gym.onMeasured(measurement(5, 20, 20));
        assertNotNull(gym.discard());
        assertFalse(gym.hasActiveSession());
        assertEquals(2, gym.getWorkouts().count());
    }

    @Test
    public void deletingWorkoutDeletesSnapshotsButPreservesProgram() {
        Program program = Program.meters("Keep source", 100, Difficulty.EASY);
        gym.mergeProgram(program);
        gym.select(program);
        gym.onMeasured(measurement(10, 100, 22));
        Workout workout = gym.complete();
        assertTrue(gym.getSnapshots(workout).count() > 0);

        gym.delete(workout);

        assertEquals(0, gym.getSnapshots(workout).count());
        assertTrue(gym.getPrograms().list().stream().anyMatch(value -> "Keep source".equals(value.name.get())));
    }

    @Test
    public void duplicateDeleteAndFinalProgramFallbackRefreshRepository() {
        Program source = gym.getPrograms().list().get(0);
        long originalCount = gym.getPrograms().count();
        Program duplicate = gym.duplicateProgram(source, "A copy");
        assertEquals(originalCount + 1, gym.getPrograms().count());
        gym.delete(duplicate);
        assertEquals(originalCount, gym.getPrograms().count());

        for (Program value : new java.util.ArrayList<>(gym.getPrograms().list())) gym.delete(value);
        assertEquals(1, gym.getPrograms().count());
    }

    @Test
    public void raceCandidatesSelectBestDistanceDurationAndCompletedIntervals() {
        Program distance = Program.meters("Race distance", 100, Difficulty.HARD);
        gym.mergeProgram(distance);
        finish(distance, measurement(30, 100, 24));
        finish(distance, measurement(25, 100, 24));
        assertEquals(25, gym.getRaceCandidates(distance).get(0).duration.get().intValue());

        Program duration = Program.minutes("Race duration", 1, Difficulty.HARD);
        gym.mergeProgram(duration);
        finish(duration, measurement(60, 1000, 24));
        finish(duration, measurement(60, 1100, 24));
        assertEquals(1100, gym.getRaceCandidates(duration).get(0).distance.get().intValue());

        Program intervals = new Program("Race intervals");
        intervals.getSegments().clear();
        intervals.addSegment(new Segment(Difficulty.HARD).setDistance(10));
        intervals.addSegment(new Segment(Difficulty.REST).setDuration(5));
        intervals.addSegment(new Segment(Difficulty.HARD).setDistance(10));
        gym.mergeProgram(intervals);
        gym.select(intervals);
        gym.onMeasured(measurement(1, 10, 24));
        gym.onMeasured(measurement(6, 10, 20));
        gym.onMeasured(measurement(7, 20, 24));
        Workout completed = gym.complete();
        gym.select(intervals);
        gym.onMeasured(measurement(1, 5, 24));
        gym.endEarly();
        List<Workout> candidates = gym.getRaceCandidates(intervals);
        assertEquals(1, candidates.size());
        assertEquals(completed.start.get(), candidates.get(0).start.get());
    }

    @Test
    public void preferredRaceStartHonorsSavedToggleAndRequiresCompletedBenchmark() {
        Program program = Program.meters("Saved race", 100, Difficulty.HARD);
        gym.mergeProgram(program);
        gym.setRacePreferred(program, true);
        gym.startPreferredProgram(program);
        assertTrue(gym.pace == null);
        gym.onMeasured(measurement(20, 50, 24));
        gym.endEarly();
        gym.startPreferredProgram(program);
        assertTrue(gym.pace == null);
        Workout best = finish(program, measurement(30, 100, 24));
        gym.startPreferredProgram(program);
        assertNotNull(gym.pace);
        assertEquals(best.start.get(), gym.pace.start.get());
        gym.setRacePreferred(program, false);
        gym.startPreferredProgram(program);
        assertTrue(gym.pace == null);
        assertFalse(gym.isRacePreferred(program));
    }

    private Workout finish(Program program, Measurement measurement) {
        gym.select(program);
        gym.onMeasured(measurement);
        return gym.complete();
    }

    private Measurement measurement(int duration, int distance, int strokeRate) {
        Measurement measurement = new Measurement();
        measurement.setDuration(duration);
        measurement.setDistance(distance);
        measurement.setStrokeRate(strokeRate);
        return measurement;
    }
}
