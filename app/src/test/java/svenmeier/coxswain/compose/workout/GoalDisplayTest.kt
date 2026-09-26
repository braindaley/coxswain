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
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoalDisplayTest {
    @Test fun liveRowMetricLayoutPersistsBetweenSessions() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("live_row_display", Context.MODE_PRIVATE).edit().clear().commit()

        val defaults = listOf(
            ValueBinding.DURATION,
            ValueBinding.DISTANCE,
            ValueBinding.SPLIT,
            ValueBinding.STROKE_RATE,
            ValueBinding.POWER,
            ValueBinding.PULSE
        )
        assertEquals(defaults, loadLiveRowMetrics(context))

        val custom = listOf(
            ValueBinding.DISTANCE,
            ValueBinding.DURATION,
            ValueBinding.POWER,
            ValueBinding.SPLIT,
            ValueBinding.STROKE_RATE,
            ValueBinding.ENERGY
        )
        saveLiveRowMetrics(context, custom)
        assertEquals(custom, loadLiveRowMetrics(context))
    }

    @Test fun strokeRateShowsTargetWhenMatchedAndSignedDifferenceOtherwise() {
        val segment = Segment().setStrokeRate(24)
        val onTarget = goalDisplay(ValueBinding.STROKE_RATE, segment, Measurement().apply { strokeRate = 24 })!!
        assertEquals("24", onTarget.variance)
        assertEquals("24", onTarget.target)
        assertEquals(0, onTarget.state)

        val below = goalDisplay(ValueBinding.STROKE_RATE, segment, Measurement().apply { strokeRate = 23 })!!
        assertEquals("-1", below.variance)
        assertEquals(-1, below.state)

        val above = goalDisplay(ValueBinding.STROKE_RATE, segment, Measurement().apply { strokeRate = 25 })!!
        assertEquals("+1", above.variance)
        assertEquals(1, above.state)
    }

    @Test fun powerUsesSameTargetAndSignedDifferenceBehavior() {
        val segment = Segment().setPower(180)
        assertEquals("180", goalDisplay(ValueBinding.POWER, segment, Measurement().apply { power = 180 })!!.variance)
        assertEquals("-5", goalDisplay(ValueBinding.POWER, segment, Measurement().apply { power = 175 })!!.variance)
        assertEquals("+5", goalDisplay(ValueBinding.POWER, segment, Measurement().apply { power = 185 })!!.variance)
    }

    @Test fun speedUsesTargetSpeedWhenMatchedAndSignedDifferenceOtherwise() {
        val segment = Segment().setSpeed(400)
        assertEquals("4.0", goalDisplay(ValueBinding.SPEED, segment, Measurement().apply { speed = 400 })!!.variance)
        assertEquals("-0.1", goalDisplay(ValueBinding.SPEED, segment, Measurement().apply { speed = 390 })!!.variance)
        assertEquals("+0.1", goalDisplay(ValueBinding.SPEED, segment, Measurement().apply { speed = 410 })!!.variance)
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

    @Test fun liveMetricFormattingPreservesRealisticLargeValues() {
        val context = RuntimeEnvironment.getApplication()
        val tenThousand = "%,d".format(Locale.getDefault(), 10_000)
        assertEquals(tenThousand, formatMetricValue(ValueBinding.DISTANCE, 10_000, context))
        assertEquals(tenThousand, formatMetricValue(ValueBinding.STROKES, 10_000, context))
        assertEquals(tenThousand, formatMetricValue(ValueBinding.ENERGY, 10_000, context))
        assertEquals("120:00", formatMetricValue(ValueBinding.DURATION, 7_200, context))
        assertEquals("+${tenThousand}", formatMetricValue(ValueBinding.DELTA_DISTANCE, 10_000, context))
        assertEquals("-${tenThousand}", formatMetricValue(ValueBinding.DELTA_DISTANCE, -10_000, context))
        assertEquals("-120:00", formatMetricValue(ValueBinding.DELTA_DURATION, -7_200, context))
    }
}
