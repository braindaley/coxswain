package svenmeier.coxswain

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import svenmeier.coxswain.gym.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutChartDataTest {
    private val start = 1_791_459_000_000L
    private fun row(duration: Int = 60, program: Program? = null) = Workout().apply {
        this.duration.set(duration); this.start.set(this@WorkoutChartDataTest.start)
        completed.set(start.get() + duration * 1000L)
        distance.set(200); strokes.set(24)
        programDefinition.set(WorkoutDefinition.freeze(program))
    }
    private fun sample(time: Int, speed: Int = 400, power: Int = 100, strokes: Int = time / 2,
                       index: Int = -1, stepStart: Int = 0, distance: Int = time * 4) = Snapshot().apply {
        duration.set(time); recordedAt.set(start + time * 1000L)
        this.speed.set(speed); this.power.set(power); strokeRate.set(24)
        this.strokes.set(strokes); this.distance.set(distance)
        intervalIndex.set(index); intervalStart.set(stepStart)
    }
    private fun chart(workout: Workout, vararg samples: Snapshot) =
        WorkoutChartData(workout, WorkoutStatistics(workout, samples.toList()))

    @Test fun startupOutlierDoesNotCompressMainPaceAndRawScaleRetainsIt() {
        val result = chart(row(), sample(4, speed = 175, strokes = 2), sample(16, speed = 330, strokes = 6), sample(60, speed = 335))
        assertTrue(result.scale(ResultMeasure.SPLIT, false).high < 200f)
        assertTrue(result.scale(ResultMeasure.SPLIT, true).high > 285f)
        assertEquals(50000f / 175f, result.value(result.points.first(), ResultMeasure.SPLIT)!!, .001f)
        assertEquals(60, result.statistics.duration)
        assertEquals(200, result.statistics.workMeters)
    }
    @Test fun startupMissingValuesAreGapsButLaterZeroPowerIsReal() {
        val result = chart(row(), sample(1, speed = 0, power = 0, strokes = 1), sample(16), sample(60, power = 0))
        assertNull(result.value(result.points.first(), ResultMeasure.SPLIT))
        assertNull(result.value(result.points.first(), ResultMeasure.POWER))
        assertEquals(0f, result.value(result.points.last(), ResultMeasure.POWER)!!, .001f)
        assertEquals(0f, result.scale(ResultMeasure.POWER, false).low, .001f)
    }
    @Test fun highEffortLaterInPyramidRemainsOnScale() {
        val result = chart(row(), sample(4, power = 10, strokes = 2), sample(16), sample(30, power = 600), sample(60))
        assertTrue(result.scale(ResultMeasure.POWER, false).high > 600)
        assertEquals(600f, result.value(result.points[2], ResultMeasure.POWER)!!, .001f)
    }
    @Test fun clockAxisIncludesPauseAndSeparatesTheTrace() {
        val workout = row().apply { completed.set(start.get() + 90_000); pausedDuration.set(30) }
        val result = chart(workout, sample(10), sample(20).apply { recordedAt.set(start + 50_000) },
            sample(60).apply { recordedAt.set(start + 90_000) })
        assertFalse(result.estimatedClock)
        assertEquals(start + 90_000, result.end)
        assertEquals(start + 50_000, result.clockAt(20f))
        assertEquals(1, result.pauseRanges.size)
        assertTrue(result.breaksBefore(1))
        assertEquals(20f, result.nearest(50f / 90f)!!.elapsed, .001f)
    }
    @Test fun legacyClockTimelineIsLabelledEstimatedAndReachesFinish() {
        val workout = row().apply { completed.set(start.get() + 90_000) }
        val result = chart(workout, sample(30).apply { recordedAt.set(null) }, sample(60).apply { recordedAt.set(0) })
        assertTrue(result.estimatedClock)
        assertEquals(start + 45_000, result.points.first().clock)
        assertEquals(start + 90_000, result.points.last().clock)
    }
    @Test fun nonMonotonicClockFallsBackWithoutDrawingBackwards() {
        val result = chart(row(), sample(30).apply { recordedAt.set(start + 50_000) }, sample(60).apply { recordedAt.set(start + 40_000) })
        assertTrue(result.estimatedClock)
        assertTrue(result.points.last().clock > result.points.first().clock)
    }
    @Test fun sameDifficultyStepsHaveRecordedBoundariesAndIndependentTargets() {
        val program = Program.minutes("Pyramid", 1, Difficulty.HARD).apply {
            getSegment(0).setDuration(30).setPower(100); getSegment(0).name.set("Build")
            addSegment(Segment(Difficulty.HARD).setDuration(30).setPower(200).apply { name.set("Peak") })
        }
        val result = chart(row(program = program), sample(30, index = 0), sample(31, index = 1, stepStart = 30), sample(60, index = 1, stepStart = 30))
        assertEquals(2, result.phases.size)
        assertEquals(30f, result.phases.first().end, .001f)
        assertEquals("Peak", result.phases.last().segment!!.name.get())
        assertFalse(result.phases.any { it.estimated })
        assertTrue(result.breaksBefore(1))
        assertTrue(result.scale(ResultMeasure.POWER, false).high > 200)
    }
    @Test fun legacyTimedStepsReconstructEvenWhenDifficultyNeverChanges() {
        val program = Program.minutes("Pyramid", 1, Difficulty.HARD).apply {
            getSegment(0).setDuration(20); addSegment(Segment(Difficulty.HARD).setDuration(40))
        }
        val result = chart(row(program = program), sample(10), sample(20), sample(30), sample(60))
        assertEquals(listOf(0, 0, 1, 1), result.points.map { it.interval })
        assertEquals(20f, result.phases.first().end, .001f)
        assertTrue(result.phases.all { it.estimated })
    }
    @Test fun distancePyramidCanReconstructBoundaryBetweenSamples() {
        val program = Program.meters("Pyramid", 100, Difficulty.MEDIUM).apply { addSegment(Segment(Difficulty.MEDIUM).setDistance(100)) }
        val result = chart(row(program = program), sample(20, distance = 80), sample(30, distance = 120), sample(60, distance = 200))
        assertEquals(25f, result.phases.first().end, .001f)
        assertEquals(100, result.intervalResult(result.phases.first()).meters)
        assertEquals(100, result.intervalResult(result.phases.last()).meters)
    }
    @Test fun workAndRestTotalsAreSeparateInIntervalBreakdown() {
        val program = Program.minutes("Intervals", 1, Difficulty.MEDIUM).apply {
            getSegment(0).setDuration(20); addSegment(Segment(Difficulty.REST).setDuration(10))
            addSegment(Segment(Difficulty.HARD).setDuration(30))
        }
        val result = chart(row(program = program), sample(20, index = 0, distance = 80),
            sample(30, power = 0, index = 1, stepStart = 20, distance = 90).apply { difficulty.set(Difficulty.REST) },
            sample(60, power = 200, index = 2, stepStart = 30, distance = 200))
        assertEquals(3, result.phases.size)
        assertTrue(result.isRest(result.points[1]))
        assertEquals(80, result.intervalResult(result.phases[0]).meters)
        assertEquals(10, result.intervalResult(result.phases[1]).meters)
        assertEquals(110, result.intervalResult(result.phases[2]).meters)
        assertEquals(200, result.intervalResult(result.phases[2]).power)
    }
    @Test fun missingDefinitionAndEmptyRecordingsAreSafe() {
        val workout = row().apply { programDefinition.set("broken") }
        val result = chart(workout)
        assertTrue(result.points.isEmpty())
        assertTrue(result.phases.isEmpty())
        assertNull(result.nearest(.5f))
        assertTrue(result.scale(ResultMeasure.SPLIT, false).high > result.scale(ResultMeasure.SPLIT, false).low)
    }
}
