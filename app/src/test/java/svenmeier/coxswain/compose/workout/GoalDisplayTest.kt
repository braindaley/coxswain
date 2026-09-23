package svenmeier.coxswain.compose.workout

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import svenmeier.coxswain.Gym
import svenmeier.coxswain.gym.Difficulty
import svenmeier.coxswain.gym.Measurement
import svenmeier.coxswain.gym.Program
import svenmeier.coxswain.gym.Segment
import svenmeier.coxswain.gym.Workout
import svenmeier.coxswain.view.ValueBinding

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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

    @Test fun getValueForBindingCountsDownForTargetAndCountsUpForFreeRow() {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("gym")
        val constructor = Gym::class.java.getDeclaredConstructor(Context::class.java)
        constructor.isAccessible = true
        val gym = constructor.newInstance(context)
        val initMethod = Gym::class.java.getDeclaredMethod("initialize")
        initMethod.isAccessible = true
        initMethod.invoke(gym)

        // 1. Distance target program (e.g. 5,000m):
        val distanceProgram = Program.meters("5K", 5000, Difficulty.EASY)
        gym.select(distanceProgram)
        gym.onMeasured(Measurement().apply { duration = 1; distance = 1 })
        assertEquals(4999, getValueForBinding(ValueBinding.DISTANCE, gym))

        gym.onMeasured(Measurement().apply { duration = 60; distance = 250 })
        assertEquals(4750, getValueForBinding(ValueBinding.DISTANCE, gym))

        // 2. Duration target program (e.g. 20 min = 1200s):
        val durationProgram = Program.minutes("20min", 20, Difficulty.EASY)
        gym.select(durationProgram)
        gym.onMeasured(Measurement().apply { duration = 1; distance = 1 })
        assertEquals(1199, getValueForBinding(ValueBinding.DURATION, gym))

        gym.onMeasured(Measurement().apply { duration = 100; distance = 400 })
        assertEquals(1100, getValueForBinding(ValueBinding.DURATION, gym))

        // 3. Free Row (no target):
        gym.startFreeRow()
        gym.onMeasured(Measurement().apply { duration = 120; distance = 500 })
        assertEquals(500, getValueForBinding(ValueBinding.DISTANCE, gym))
        assertEquals(120, getValueForBinding(ValueBinding.DURATION, gym))
    }
}
