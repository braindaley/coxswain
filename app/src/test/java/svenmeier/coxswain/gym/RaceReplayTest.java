package svenmeier.coxswain.gym;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;

public class RaceReplayTest {
    private Workout workout() {
        Workout workout = new Workout();
        workout.duration.set(100); workout.distance.set(1000);
        return workout;
    }
    private Snapshot sample(int time, int meters) {
        Snapshot snapshot = new Snapshot();
        snapshot.duration.set(time); snapshot.distance.set(meters);
        return snapshot;
    }
    @Test public void followsSlowStartRestAndFastFinish() {
        RaceReplay replay = new RaceReplay(workout(), Arrays.asList(
                sample(20, 100), sample(40, 100), sample(60, 500), sample(100, 1000)));
        assertEquals(50f, replay.distanceAt(10), .001f);
        assertEquals(100f, replay.distanceAt(30), .001f);
        assertEquals(300f, replay.distanceAt(50), .001f);
        assertEquals(750f, replay.distanceAt(80), .001f);
        assertEquals(1000f, replay.distanceAt(150), .001f);
        assertEquals(20f, replay.timeAtDistance(100), .001f);
        assertEquals(50f, replay.timeAtDistance(300), .001f);
    }
    @Test public void legacySamplesKeepPaceChangesWithEstimatedTiming() {
        RaceReplay replay = new RaceReplay(workout(), Arrays.asList(
                sample(0, 100), sample(0, 100), sample(0, 500), sample(0, 1000)));
        assertEquals(100f, replay.distanceAt(50), .001f);
        assertEquals(500f, replay.distanceAt(75), .001f);
    }
    @Test public void duplicateHardwareTimesAreConsolidated() {
        RaceReplay replay = new RaceReplay(workout(), Arrays.asList(
                sample(20, 100), sample(20, 100), sample(100, 1000)));
        assertEquals(100f, replay.distanceAt(20), .001f);
        assertEquals(550f, replay.distanceAt(60), .001f);
    }
    @Test public void noSamplesFallsBackToOverallPace() {
        RaceReplay replay = new RaceReplay(workout(), Collections.emptyList());
        assertEquals(500f, replay.distanceAt(50), .001f);
    }
    @Test public void legacyNullActiveTotalsDoNotCrashRowingReplay() {
        Workout legacy = workout();
        legacy.planActiveSeconds.set(null); legacy.planActiveDistance.set(null); legacy.planActiveStrokes.set(null);
        RaceReplay replay = new RaceReplay(legacy, Arrays.asList(sample(20, 100), sample(100, 1000)), true);
        assertEquals(550f, replay.distanceAt(60), .001f);
        RaceReplay estimated = new RaceReplay(legacy, Collections.emptyList(), true);
        assertEquals(500f, estimated.distanceAt(50), .001f);
    }
}
