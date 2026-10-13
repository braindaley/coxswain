package svenmeier.coxswain.compose.workout

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import svenmeier.coxswain.gym.*
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RaceReferenceLabelTest {
    @Test fun distanceRaceKeepsTheLossVisibleAfterTheBestReachesTheFinish() {
        val program=Program.meters("5K",5000,Difficulty.EASY)
        assertNull(raceFinishReadout(program,1479,1479.8f))
        assertEquals("Best finished",raceFinishReadout(program,1480,1479.8f))
        assertEquals("1:03 behind",raceFinishReadout(program,1543,1479.8f))
        // The final distance difference is zero, but the finish-time difference is not.
        val row=Measurement().apply { distance=5000; duration=1543 }
        val best=Workout().apply { distance.set(5000); duration.set(1480) }
        assertEquals(0,raceDisplay(row,best,program,5000f,5000).leadMeters)
        assertEquals("1:03 behind",raceFinishReadout(program,row.duration,1479.8f))
    }
    @Test fun timedRacesAndPaceTargetsDoNotSwitchToFinishTime() {
        assertNull(raceFinishReadout(Program.minutes("Timed",32,Difficulty.EASY),1920,1920f))
        assertNull(raceFinishReadout(null,1543,1480f))
        assertNull(raceFinishReadout(Program.meters("5K",5000,Difficulty.EASY),1500,Float.NaN))
    }
    @Test fun timedIntervalsShowScoringDistanceInsteadOfTheirFixedDuration() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            val program = Program.minutes("Timed intervals",4,Difficulty.EASY).apply {
                addSegment(Segment(Difficulty.REST).setDuration(120))
                addSegment(Segment(Difficulty.HARD).setDuration(240))
            }
            assertTrue(WorkoutDefinition.ranksByDistance(program))
            assertEquals("5,954 m",formatRaceReferenceValue(program,5954,1920))
            assertEquals("5,000 m",formatRaceReferenceValue(Program.minutes("Timed",32,Difficulty.MEDIUM),5000,1920))
        } finally { Locale.setDefault(original) }
    }
    @Test fun distanceIntervalsAndMixedProgramsShowFinishTime() {
        val distance=Program.meters("Distance",5000,Difficulty.MEDIUM)
        assertEquals("20:42",formatRaceReferenceValue(distance,5000,1242))
        distance.addSegment(Segment(Difficulty.REST).setDuration(120))
        distance.addSegment(Segment(Difficulty.HARD).setDistance(1000))
        assertFalse(WorkoutDefinition.ranksByDistance(distance))
        assertEquals("28:15",formatRaceReferenceValue(distance,6000,1695))
        val mixed=Program.minutes("Mixed",4,Difficulty.EASY).apply {
            addSegment(Segment(Difficulty.HARD).setDistance(1000))
        }
        assertFalse(WorkoutDefinition.ranksByDistance(mixed))
        assertEquals("8:12",formatRaceReferenceValue(mixed,1900,492))
    }
}
