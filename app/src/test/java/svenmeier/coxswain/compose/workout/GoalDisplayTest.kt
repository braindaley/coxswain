package svenmeier.coxswain.compose.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import svenmeier.coxswain.gym.Measurement
import svenmeier.coxswain.gym.Segment
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
}
