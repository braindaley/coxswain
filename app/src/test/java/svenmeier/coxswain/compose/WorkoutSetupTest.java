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
}
