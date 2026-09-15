package svenmeier.coxswain.gym;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WorkoutTest {

    @Test
    public void startsWithZeroTotalsAndEvaluationEnabled() {
        Workout workout = new Workout();

        assertEquals(Integer.valueOf(0), workout.duration.get());
        assertEquals(Integer.valueOf(0), workout.distance.get());
        assertEquals(Integer.valueOf(0), workout.strokes.get());
        assertEquals(Integer.valueOf(0), workout.energy.get());
        assertTrue(workout.evaluate.get());
    }

    @Test
    public void measurementUpdatesWorkoutTotals() {
        Workout workout = new Workout();
        Measurement measurement = measurement(120, 500, 58, 32);

        workout.onMeasured(measurement);

        assertEquals(Integer.valueOf(120), workout.duration.get());
        assertEquals(Integer.valueOf(500), workout.distance.get());
        assertEquals(Integer.valueOf(58), workout.strokes.get());
        assertEquals(Integer.valueOf(32), workout.energy.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMeasurementThatMovesTotalsBackward() {
        Workout workout = new Workout();
        workout.onMeasured(measurement(120, 500, 58, 32));

        workout.onMeasured(measurement(121, 499, 59, 33));
    }

    private Measurement measurement(int duration, int distance, int strokes, int energy) {
        Measurement measurement = new Measurement();
        measurement.setDuration(duration);
        measurement.setDistance(distance);
        measurement.setStrokes(strokes);
        measurement.setEnergy(energy);
        return measurement;
    }
}
