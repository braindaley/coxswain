package svenmeier.coxswain.compose.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import svenmeier.coxswain.gym.Measurement
import svenmeier.coxswain.gym.Segment
import svenmeier.coxswain.gym.Difficulty
import svenmeier.coxswain.gym.Program
import svenmeier.coxswain.gym.Workout
import svenmeier.coxswain.view.ValueBinding

class GoalDisplayTest {
    @Test fun strokeRateVarianceHasNeutralBandAndTarget() {
        val segment = Segment().setStrokeRate(24)
        val measurement = Measurement().apply { strokeRate = 25 }
        val goal = goalDisplay(ValueBinding.STROKE_RATE, segment, measurement)!!
        assertEquals("+1", goal.variance)
        assertEquals("24", goal.target)
        assertEquals(0, goal.state)
    }

    @Test fun splitVarianceUsesSecondsPerFiveHundredMeters() {
        val segment = Segment().setSpeed(400)
        val measurement = Measurement().apply { speed = 500 }
        val goal = goalDisplay(ValueBinding.SPLIT, segment, measurement)!!
        assertEquals("+25 s", goal.variance)
        assertEquals("2:05 /500 m", goal.target)
        assertEquals(1, goal.state)
    }

    @Test fun unrelatedMetricHasNoGoalTreatment() {
        assertNull(goalDisplay(ValueBinding.POWER, Segment().setStrokeRate(24), Measurement()))
    }

    @Test fun restDisplayShowsPositionCountdownAndRealNextTarget() {
        val row = Segment(Difficulty.EASY).setDuration(300)
        val rest = Segment(Difficulty.REST).setDuration(60)
        val next = Segment(Difficulty.HARD).setDistance(1000)
        val display = restDisplay(listOf(row, rest, next), rest, .25f)
        assertEquals(2, display.position)
        assertEquals(3, display.total)
        assertEquals("0:45", display.remaining)
        assertEquals("Next: 1000 m row", display.next)
    }

    @Test fun raceDisplayComparesBothBoatsAtCurrentElapsedTime() {
        val program = Program.meters("2K", 2000, Difficulty.HARD)
        val best = Workout().apply { duration.set(500); distance.set(2000) }
        val current = Measurement().apply { duration = 250; distance = 1100 }
        val display = raceDisplay(current, best, program)
        assertEquals(.55f, display.currentProgress, .001f)
        assertEquals(.5f, display.bestProgress, .001f)
        assertEquals(100, display.leadMeters)
    }
}
