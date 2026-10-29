package svenmeier.coxswain.pete

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import svenmeier.coxswain.gym.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PeteGoalPresentationTest {
    private val source = Workout(Program.meters("Week 1 · 5,000m",5000,Difficulty.MEDIUM))
    private fun show(goal: PeteGoal) = presentPeteGoal(goal,{ "Week 1 · 5,000m (Oct 20 · 2:25 /500 m)" },{145})!!

    @Test fun matchingAndAdjustedSplitsExplainValueAndSourceWithoutCallingThemRaces() {
        val same = show(PeteGoal.Speed(145,source))
        assertEquals("Hold 2:25 /500 m",same.value)
        assertTrue(same.detail.contains("Match its average split"))
        assertTrue(same.detail.contains("Oct 20"))
        assertEquals("Start with pace target",same.startLabel)
        assertTrue(show(PeteGoal.Speed(143,source)).detail.contains("2 seconds faster per 500 m"))
        assertTrue(show(PeteGoal.Speed(148,source)).detail.contains("3 seconds slower per 500 m"))
    }
    @Test fun phasedPaceExplainsScopeAndStopsPrescribingItAfterTheTargetedPieces() {
        assertTrue(show(PeteGoal.Speed(145,source,3)).detail.contains("First 3 work intervals only"))
        assertEquals("Hold 2:25 /500 m",peteLiveInstruction("SPEED_PHASE",145,345,false,false))
        assertEquals("Pace target finished · Follow the coaching note",peteLiveInstruction("SPEED_PHASE",145,0,false,false))
        assertNull(peteLiveInstruction("SPEED_PHASE",145,345,true,false))
    }
    @Test fun strokeRateCapAndExactRateAreDifferentFromStrokeCounts() {
        val cap = show(PeteGoal.StrokeRateCap(24,source,"Relax your recovery."))
        assertEquals("Stay at or below 24 SPM",cap.value)
        assertTrue(cap.detail.contains("Relax your recovery."))
        assertTrue(cap.detail.contains("Oct 20"))
        assertEquals("Start with stroke-rate target",cap.startLabel)
        assertEquals("Hold 20 SPM",show(PeteGoal.StrokeRate(20)).value)
        assertEquals("Stay at or below 24 SPM",peteLiveInstruction("RATE_CAP",24,0,false,false))
        assertEquals("Hold 20 SPM",peteLiveInstruction("RATE",20,0,false,false))
    }
    @Test fun pacingReferencesAreNotAdvertisedAsEnforcedTargetsOrRaces() {
        val reference = show(PeteGoal.Reference(145,source,"Start gently, then build."))
        assertEquals("Reference: 2:25 /500 m",reference.value)
        assertTrue(reference.detail.contains("Start gently, then build."))
        assertEquals("Start with pacing reference",reference.startLabel)
        assertEquals("Pace reference · 2:25 /500 m",peteLiveInstruction("REFERENCE",145,0,false,false))
        assertEquals("Start without target",show(PeteGoal.Unavailable("Complete Week 1 first.")).startLabel)
        assertNull(presentPeteGoal(PeteGoal.None,{""},{null}))
    }
    @Test fun onlyRecordedRacesUseRaceButtonAndNameTheCorrectWinningMetric() {
        val race = show(PeteGoal.RowAgainst(source))
        assertEquals("Race previous row",race.startLabel)
        assertEquals("Beat your previous time",race.value)
        assertTrue(race.detail.contains("actual pacing"))
        val timed = Workout(Program.minutes("20min",20,Difficulty.MEDIUM))
        assertEquals("Row farther in the same time",show(PeteGoal.RowAgainst(timed)).value)
        assertEquals("Beat your previous distance",peteLiveInstruction("ROW_AGAINST",0,0,false,true))
        assertEquals("Beat your previous time",peteLiveInstruction("ROW_AGAINST",0,0,false,false))
    }
}
