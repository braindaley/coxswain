package svenmeier.coxswain.gym;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class WorkoutDefinitionTest {

    @Test
    public void frozenDefinitionIsDetachedFromEditableProgram() {
        Program original = new Program("Intervals");
        original.getSegments().clear();
        original.addSegment(new Segment(Difficulty.HARD).setDuration(300).setPower(190));
        original.addSegment(new Segment(Difficulty.REST).setDuration(60));

        Workout workout = original.newWorkout();
        original.name.set("Changed");
        original.getSegment(0).setDuration(600);

        Program frozen = WorkoutDefinition.thaw(workout.programDefinition.get());
        assertEquals("Intervals", workout.programName.get());
        assertEquals("Intervals", frozen.name.get());
        assertEquals(300, frozen.getSegment(0).duration.get().intValue());
        assertEquals(190, frozen.getSegment(0).power.get().intValue());
        assertEquals(SessionType.INTERVAL, workout.sessionType.get());
        assertEquals(PerformanceGoal.POWER, workout.goalType.get());
        assertEquals(190, workout.goalTarget.get().intValue());
    }
}
