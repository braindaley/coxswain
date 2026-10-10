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

    @Test fun allFourLevelGuidesFitEvenWhenRecordedValuesOccupyOneLevel() {
        val result = chart(row(), sample(20), sample(30), sample(60))
        val power = resultLevelGuides(output = OutputZones(100, 150, 200))
        assertEquals(listOf(50f, 100f, 150f, 200f), power)
        val powerScale = result.scale(ResultMeasure.POWER, power)
        assertTrue(power.all { it > powerScale.low && it < powerScale.high })
        val pace = resultLevelGuides(output = OutputZones(180, 150, 120, true))
        assertEquals(listOf(210f, 180f, 150f, 120f), pace)
        val paceScale = result.scale(ResultMeasure.SPLIT, pace)
        assertTrue(pace.all { it > paceScale.low && it < paceScale.high })
        assertTrue(paceScale.fraction(pace[0], true) > paceScale.fraction(pace[3], true))
    }

    @Test fun heartLightGuideUsesRestingReferenceAndUnconfiguredMetricsHaveNoZoneGuides() {
        val heart = HeartRateZones.reserve(60, 180)
        assertEquals(listOf(60f, 108f, 132f, 162f), resultLevelGuides(heart = heart))
        assertEquals(listOf(77f, 100f, 123f, 150f), resultLevelGuides(heart = HeartRateZones(100, 123, 150)))
        assertTrue(resultLevelGuides().isEmpty())
    }

    @Test fun displayMedianReducesIsolatedNoiseWithoutChangingMeasurementsOrSustainedEffort() {
        val samples = listOf(100, 100, 180, 100, 100, 200, 200, 200).mapIndexed { index, watts ->
            sample(20 + index, power = watts)
        }
        val result = chart(row(), *samples.toTypedArray())
        val trace = result.displayValues(ResultMeasure.POWER)
        assertEquals(100f, trace[2]!!, .001f)
        assertEquals(180f, result.plottedValue(result.points[2], ResultMeasure.POWER)!!, .001f)
        assertEquals(listOf(200f, 200f, 200f), trace.takeLast(3))
        assertTrue(result.scale(ResultMeasure.POWER).high > 200f)
    }

    @Test fun displayMedianPreservesIntervalEdgesZerosAndMissingHeartReadings() {
        val program = Program.minutes("Steps", 1, Difficulty.MEDIUM).apply {
            getSegment(0).setDuration(30)
            addSegment(Segment(Difficulty.HARD).setDuration(30))
        }
        val result = chart(row(program = program), sample(29, power = 100, index = 0),
            sample(30, power = 200, index = 0), sample(31, power = 100, index = 1, stepStart = 30),
            sample(32, power = 0, index = 1, stepStart = 30), sample(33, power = 100, index = 1, stepStart = 30))
        assertEquals(listOf(100f, 200f, 100f, 0f, 100f), result.displayValues(ResultMeasure.POWER))
        val heart = chart(row(), sample(20).apply { pulse.set(120) }, sample(21).apply { pulse.set(null) },
            sample(22).apply { pulse.set(160) })
        assertEquals(listOf(120f, null, 160f), heart.displayValues(ResultMeasure.PULSE))
    }

    @Test fun displayMedianDoesNotBridgeSparseSamplesOrPauseGaps() {
        val result = chart(row(), sample(20, power = 100), sample(21, power = 180),
            sample(22, power = 100).apply { recordedAt.set(start + 32_000) })
        assertEquals(listOf(100f, 180f, 100f), result.displayValues(ResultMeasure.POWER))
        val sparse = chart(row(), sample(20, power = 100), sample(30, power = 180), sample(40, power = 100))
        assertEquals(listOf(100f, 180f, 100f), sparse.displayValues(ResultMeasure.POWER))
    }

    @Test fun startupOutlierIsNotPlottedOrUsedToScaleTheChart() {
        val result = chart(row(), sample(4, speed = 175, strokes = 2), sample(16, speed = 330, strokes = 6), sample(60, speed = 335))
        assertTrue(result.scale(ResultMeasure.SPLIT).high < 200f)
        assertNull(result.plottedValue(result.points.first(), ResultMeasure.SPLIT))
        assertEquals(50000f / 175f, result.value(result.points.first(), ResultMeasure.SPLIT)!!, .001f)
        assertEquals(60, result.statistics.duration)
        assertEquals(200, result.statistics.workMeters)
    }
    @Test fun startupMissingValuesAreGapsButLaterZeroPowerIsReal() {
        val result = chart(row(), sample(1, speed = 0, power = 0, strokes = 1), sample(16), sample(60, power = 0))
        assertNull(result.value(result.points.first(), ResultMeasure.SPLIT))
        assertNull(result.value(result.points.first(), ResultMeasure.POWER))
        assertEquals(0f, result.value(result.points.last(), ResultMeasure.POWER)!!, .001f)
        assertEquals(0f, result.scale(ResultMeasure.POWER).low, .001f)
    }
    @Test fun highEffortLaterInPyramidRemainsOnScale() {
        val result = chart(row(), sample(4, power = 10, strokes = 2), sample(16), sample(30, power = 600), sample(60))
        assertTrue(result.scale(ResultMeasure.POWER).high > 600)
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
        assertEquals(50f / 90f, result.clockFraction(start + 50_000), .001f)
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
        assertTrue(result.scale(ResultMeasure.POWER).high > 200)
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

    @Test fun monitorRampAfterSixStrokesIsHiddenUntilPaceSettles() {
        val workout = row(duration = 600).apply { distance.set(2000) }
        val samples = (1..120).map { t ->
            // Reproduce the saved row's prolonged acceleration: still slow well after stroke six.
            sample(t, speed = if (t < 40) 175 + t * 4 else 335, strokes = t / 2)
        }
        val result = WorkoutChartData(workout, WorkoutStatistics(workout, samples))
        assertTrue(result.startupEnd >= 40f)
        assertTrue(result.startupEnd <= 60f)
        result.points.filter { it.elapsed < result.startupEnd }.forEach {
            ResultMeasure.entries.forEach { measure -> assertNull(result.plottedValue(it, measure)) }
        }
        assertTrue(result.scale(ResultMeasure.SPLIT).high < 160f)
        assertEquals(samples.size, result.points.size)
        assertEquals(600, result.statistics.duration)
    }
    @Test fun laterSlowIntervalAndPeakArePreserved() {
        val program = Program.minutes("Pyramid", 2, Difficulty.MEDIUM).apply {
            addSegment(Segment(Difficulty.HARD).setDuration(120))
        }
        val samples = (1..240).map { t -> sample(t,
            speed = if (t < 15 || t in 121..135) 175 else 335,
            power = if (t == 130) 600 else 100,
            index = if (t <= 120) 0 else 1, stepStart = if (t <= 120) 0 else 120) }
        val result = WorkoutChartData(row(240, program), WorkoutStatistics(row(240, program), samples))
        assertEquals(50000f / 175, result.plottedValue(result.points[124], ResultMeasure.SPLIT)!!, .001f)
        assertEquals(600f, result.plottedValue(result.points[129], ResultMeasure.POWER)!!, .001f)
        assertTrue(result.scale(ResultMeasure.SPLIT).high > 285f)
    }
    @Test fun veryShortFirstIntervalIsNotEntirelyFiltered() {
        val program = Program.minutes("Short intervals", 1, Difficulty.HARD).apply {
            getSegment(0).setDuration(8); addSegment(Segment(Difficulty.HARD).setDuration(8))
        }
        val samples = (1..16).map { t -> sample(t, index = if (t <= 8) 0 else 1, stepStart = if (t <= 8) 0 else 8) }
        val result = WorkoutChartData(row(16, program), WorkoutStatistics(row(16, program), samples))
        assertTrue(result.startupEnd <= 2f)
        assertNotNull(result.plottedValue(result.points[7], ResultMeasure.SPLIT))
        assertNotNull(result.plottedValue(result.points[8], ResultMeasure.SPLIT))
    }
    @Test fun heartRateRetainsInitialReadingsRecoveryAndMissingSensorGaps() {
        val program = Program.minutes("Recovery", 1, Difficulty.HARD).apply {
            getSegment(0).setDuration(30); addSegment(Segment(Difficulty.REST).setDuration(30))
        }
        val result = chart(row(program = program),
            sample(4, strokes = 2, index = 0).apply { pulse.set(110) },
            sample(30, index = 0).apply { pulse.set(150) },
            sample(40, index = 1, stepStart = 30).apply { pulse.set(140) },
            sample(50, index = 1, stepStart = 30).apply { pulse.set(0) },
            sample(60, index = 1, stepStart = 30).apply { pulse.set(120) })
        assertNull(result.plottedValue(result.points[0], ResultMeasure.SPLIT))
        assertEquals(110f, result.plottedValue(result.points[0], ResultMeasure.PULSE)!!, .001f)
        assertEquals(140f, result.plottedValue(result.points[2], ResultMeasure.PULSE)!!, .001f)
        assertNull(result.plottedValue(result.points[3], ResultMeasure.PULSE))
        assertFalse(result.breaksBefore(2, ResultMeasure.PULSE))
        assertTrue(result.scale(ResultMeasure.PULSE).low < 110f)
        assertTrue(result.scale(ResultMeasure.PULSE).high > 150f)
        assertNotNull(result.intervalResult(result.phases.last()).pulse)
    }
    @Test fun intervalWithoutPulseDoesNotBorrowThePreviousStepReading() {
        val program = Program.minutes("Disconnected", 1, Difficulty.HARD).apply {
            getSegment(0).setDuration(30); addSegment(Segment(Difficulty.REST).setDuration(30))
        }
        val result = chart(row(program = program), sample(30, index = 0).apply { pulse.set(150) },
            sample(60, index = 1, stepStart = 30).apply { pulse.set(0) })
        assertNull(result.intervalResult(result.phases.last()).pulse)
    }
    @Test fun missingDefinitionAndEmptyRecordingsAreSafe() {
        val workout = row().apply { programDefinition.set("broken") }
        val result = chart(workout)
        assertTrue(result.points.isEmpty())
        assertTrue(result.phases.isEmpty())
        assertTrue(result.scale(ResultMeasure.SPLIT).high > result.scale(ResultMeasure.SPLIT).low)
    }
}
