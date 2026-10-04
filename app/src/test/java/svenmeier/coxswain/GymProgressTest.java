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
    public void distanceRaceScoresAtTargetRatherThanOvershootSample() {
        Program program = Program.meters("Boundary", 100, Difficulty.HARD);
        gym.mergeProgram(program);
        gym.select(program);
        gym.onMeasured(measurement(9, 90, 24));
        gym.onMeasured(measurement(10, 110, 24));
        Workout best = gym.complete();
        assertEquals(9.5f, gym.resultSeconds(best, program), .001f);
        gym.race(program, best);
        gym.onMeasured(measurement(10, 100, 24));
        Workout result = gym.complete();
        assertEquals(svenmeier.coxswain.gym.RaceOutcome.LOST, result.raceOutcome.get());
        assertEquals(-500, result.raceMargin.get().intValue());
    }

    @Test
    public void timedRaceScoresMetersAtRequestedTime() {
        Program program = Program.minutes("Boundary time", 1, Difficulty.MEDIUM);
        gym.mergeProgram(program);
        gym.select(program);
        gym.onMeasured(measurement(58, 580, 24));
        gym.onMeasured(measurement(62, 620, 24));
        Workout best = gym.complete();
        assertEquals(600, gym.activeDistance(best));
        gym.race(program, best);
        gym.onMeasured(measurement(60, 610, 24));
        Workout result = gym.complete();
        assertEquals(svenmeier.coxswain.gym.RaceOutcome.WON, result.raceOutcome.get());
        assertEquals(10, result.raceMargin.get().intValue());
    }

    @Test
    public void healthSyncMarkersSurviveBackupRestore() {
        context.getSharedPreferences("health_connect_exports", Context.MODE_PRIVATE).edit().putBoolean("1700000000000", true).commit();
        String backup = gym.createBackup();
        context.getSharedPreferences("health_connect_exports", Context.MODE_PRIVATE).edit().clear().commit();
        gym.restoreBackup(backup);
        assertTrue(context.getSharedPreferences("health_connect_exports", Context.MODE_PRIVATE).getBoolean("1700000000000", false));
    }

    @Test
    public void interruptedWorkoutIsRecoveredIntoHistory() throws Exception {
        gym.startFreeRow();
        gym.onMeasured(measurement(30, 100, 24));
        Field field = Gym.class.getDeclaredField("repository");
        field.setAccessible(true);
        ((Repository) field.get(gym)).close();
        Constructor<Gym> constructor = Gym.class.getDeclaredConstructor(Context.class);
        constructor.setAccessible(true);
        gym = constructor.newInstance(context);
        gym.initialize();
        Workout recovered = gym.getAllWorkouts().list().get(0);
        assertEquals(WorkoutStatus.ENDED_EARLY, recovered.status.get());
        assertEquals(100, recovered.distance.get().intValue());
        assertFalse(gym.hasActiveSession());
        assertTrue(gym.createBackup().contains("ENDED_EARLY"));
    }

    @Test
    public void finishedTargetCannotAccumulateMoreRaceMeters() {
        Program program = Program.minutes("Timed", 1, Difficulty.MEDIUM);
        gym.select(program);
        gym.onMeasured(measurement(60, 1000, 24));
        gym.onMeasured(measurement(90, 2000, 24));
        Workout result = gym.endEarly();
        assertEquals(1000, result.distance.get().intValue());
        assertEquals(60, result.duration.get().intValue());
    }

    @Test
    public void timedIntervalRaceIgnoresRestMeters() {
        Program program = new Program("Rest race");
        program.getSegment(0).setDuration(10);
        program.addSegment(new Segment(Difficulty.REST).setDuration(10));
        program.addSegment(new Segment(Difficulty.MEDIUM).setDuration(10));
        gym.mergeProgram(program);
        gym.select(program);
        gym.onMeasured(measurement(10, 100, 24));
        gym.onMeasured(measurement(20, 200, 24));
        gym.onMeasured(measurement(30, 300, 24));
        Workout best = gym.complete();
        gym.race(program, best);
        gym.onMeasured(measurement(10, 100, 24));
        gym.onMeasured(measurement(20, 100, 0));
        assertEquals(100f, gym.getPaceDistanceAt(20), .001f);
        gym.onMeasured(measurement(30, 200, 24));
        Workout result = gym.complete();
        assertEquals(200, gym.activeDistance(best));
        assertEquals(svenmeier.coxswain.gym.RaceOutcome.TIED, result.raceOutcome.get());
    }

    @Test
    public void racePreferencesStayIndependentAndPortableForIdenticalCopies() throws Exception {
        Program first = Program.meters("Same", 1000, Difficulty.MEDIUM);
        gym.mergeProgram(first);
        Program second = gym.duplicateProgram(first, "Same");
        gym.setRacePreferred(first, true);
        gym.setRacePreferred(second, false);
        assertTrue(gym.isRacePreferred(first));
        assertFalse(gym.isRacePreferred(second));
        Program detached = svenmeier.coxswain.gym.WorkoutDefinition.thaw(svenmeier.coxswain.gym.WorkoutDefinition.freeze(second));
        assertFalse(gym.isRacePreferred(detached));
        String backup = gym.createBackup();
        gym.delete(first); gym.delete(second);
        gym.restoreBackup(backup);
        assertTrue(gym.isRacePreferred(gym.resolveSavedProgram(first)));
        assertFalse(gym.isRacePreferred(gym.resolveSavedProgram(detached)));
    }

    @Test
    public void failedRestoreRollsBackAndRetryRestoresSnapshots() throws Exception {
        Program program = Program.meters("Atomic", 1000, Difficulty.HARD);
        gym.mergeProgram(program);
        Workout row = finish(program, measurement(100, 1000, 24));
        String valid = gym.createBackup();
        gym.delete(row); gym.delete(program);
        long programs = gym.getPrograms().count();
        org.json.JSONObject invalid = new org.json.JSONObject(valid);
        invalid.getJSONArray("workouts").getJSONObject(0).put("goalType", "INVALID");
        try { gym.restoreBackup(invalid.toString()); org.junit.Assert.fail("Must reject corrupt backup"); }
        catch (IllegalArgumentException expected) { }
        assertEquals(programs, gym.getPrograms().count());
        assertEquals(0, gym.getAllWorkouts().count());
        gym.restoreBackup(valid);
        assertEquals(1, gym.getAllWorkouts().count());
        assertTrue(gym.getSnapshots(gym.getAllWorkouts().list().get(0)).count() > 0);
    }

    @Test
    public void structuredBenchmarkSurvivesCompaction() {
        Program program = Program.meters("Old best", 1000, Difficulty.MEDIUM);
        Workout row = finish(program, measurement(100, 1000, 24));
        row.start.set(1L); gym.mergeWorkout(row);
        gym.compact(10);
        assertTrue(gym.getSnapshots(row).count() > 0);
    }

    @Test
    public void reconnectRebasesResetCountersWithoutDiscardingWorkout() {
        gym.startFreeRow();
        gym.onMeasured(measurement(30, 100, 24));
        gym.connectionLost();
        gym.connectionRestarted();
        gym.onMeasured(measurement(0, 0, 0));
        gym.resume();
        gym.onMeasured(measurement(10, 40, 24));
        Workout result = gym.complete();
        assertEquals(40, result.duration.get().intValue());
        assertEquals(140, result.distance.get().intValue());
    }

    @Test
    public void incompleteDistanceRaceHasNoWinningOutcome() {
        Program program = Program.meters("Race", 100, Difficulty.HARD);
        gym.mergeProgram(program);
        finish(program, measurement(30, 100, 24));
        gym.setRacePreferred(program, true);
        gym.startPreferredProgram(program);
        gym.onMeasured(measurement(10, 20, 24));
        Workout result = gym.endEarly();
        assertEquals(svenmeier.coxswain.gym.RaceOutcome.NONE, result.raceOutcome.get());
        assertEquals(0, result.raceMargin.get().intValue());
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

    @Test
    public void timedIntervalRaceUsesDistanceForBestAndOutcome() {
        Program program = new Program("Timed intervals");
        program.getSegment(0).setDuration(10);
        program.addSegment(new Segment(Difficulty.REST).setDuration(5));
        program.addSegment(new Segment(Difficulty.HARD).setDuration(10));
        gym.mergeProgram(program);
        gym.select(program);
        gym.onMeasured(measurement(10, 100, 24));
        gym.onMeasured(measurement(15, 100, 0));
        gym.onMeasured(measurement(25, 200, 24));
        Workout baseline = gym.complete();
        gym.race(program, baseline);
        assertEquals(50f, gym.getPaceDistanceAt(5), .001f);
        assertEquals(100f, gym.getPaceDistanceAt(12), .001f);
        assertEquals(100f, gym.getPaceDistanceAt(15), .001f);
        gym.onMeasured(measurement(10, 250, 24));
        gym.onMeasured(measurement(15, 250, 0));
        gym.onMeasured(measurement(25, 500, 24));
        Workout result = gym.complete();
        assertEquals(svenmeier.coxswain.gym.RaceOutcome.WON, result.raceOutcome.get());
        assertEquals(300, result.raceMargin.get().intValue());
        assertEquals(result.start.get(), gym.getRaceCandidates(program).get(0).start.get());
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
