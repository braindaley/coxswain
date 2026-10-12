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
