package svenmeier.coxswain.compose;

import android.content.Context;
import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.gym.WorkoutDefinition;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class WorkoutSetupTest {

    @Test
    public void distanceShortcutCarriesItsInitialSetupType() {
        Context context = RuntimeEnvironment.getApplication();
        Intent intent = WorkoutSetupActivity.createIntent(context, "Distance");

        assertEquals("Distance", intent.getStringExtra("workoutType"));
    }

    @Test
    public void distanceQuickStartHasExactlyOneRequestedSegment() {
        Program program = WorkoutSetupActivityKt.buildProgram("Distance", 5000, "None", 0);

        assertEquals(1, program.getSegmentsCount());
        assertEquals(5000, program.getSegment(0).distance.get().intValue());
        assertEquals(0, program.getSegment(0).duration.get().intValue());
    }

    @Test
    public void durationQuickStartUsesMinutes() {
        Program program = WorkoutSetupActivityKt.buildProgram("Duration", 30, "None", 0);

        assertEquals(1, program.getSegmentsCount());
        assertEquals(1800, program.getSegment(0).duration.get().intValue());
        assertEquals(0, program.getSegment(0).distance.get().intValue());
    }

    @Test
    public void paceGoalConvertsToEngineSpeed() {
        Program program = WorkoutSetupActivityKt.buildProgram("Distance", 2000, "Speed", 125);
        Segment segment = program.getSegment(0);

        assertEquals(400, segment.speed.get().intValue());
    }

    @Test
    public void immediateAndSavedDurationDefinitionsAreIdentical() {
        Program immediate = WorkoutSetupActivityKt.buildProgram("Duration", 30, "Power", 180);
        Program saved = WorkoutSetupActivityKt.buildProgram("Duration", 30, "Power", 180);
        assertEquals(WorkoutDefinition.compatibilityKey(immediate), WorkoutDefinition.compatibilityKey(saved));
    }

    @Test
    public void immediateAndSavedDistanceDefinitionsAreIdentical() {
        Program immediate = WorkoutSetupActivityKt.buildProgram("Distance", 5000, "Stroke rate", 24);
        Program saved = WorkoutSetupActivityKt.buildProgram("Distance", 5000, "Stroke rate", 24);
        assertEquals(WorkoutDefinition.compatibilityKey(immediate), WorkoutDefinition.compatibilityKey(saved));
    }

    @Test
    public void mixedIntervalDefinitionPreservesEverySegmentTypeAndGoal() {
        java.util.List<DraftSegment> segments = Arrays.asList(
                new DraftSegment(SegmentType.DURATION, 5),
                new DraftSegment(SegmentType.REST, 1),
                new DraftSegment(SegmentType.DISTANCE, 1000));
        Program immediate = WorkoutSetupActivityKt.buildProgram("Intervals", 0, "Power", 190, segments);
        Program saved = WorkoutSetupActivityKt.buildProgram("Intervals", 0, "Power", 190, segments);

        assertEquals(WorkoutDefinition.compatibilityKey(immediate), WorkoutDefinition.compatibilityKey(saved));
        assertEquals(3, immediate.getSegmentsCount());
        assertEquals(300, immediate.getSegment(0).duration.get().intValue());
        assertEquals(60, immediate.getSegment(1).duration.get().intValue());
        assertEquals(0, immediate.getSegment(1).power.get().intValue());
        assertEquals(1000, immediate.getSegment(2).distance.get().intValue());
        assertEquals(190, immediate.getSegment(2).power.get().intValue());
    }
}
