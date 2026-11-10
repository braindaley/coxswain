package svenmeier.coxswain

import org.junit.Assert.*
import org.junit.Test
import svenmeier.coxswain.gym.*

class WorkoutStatisticsTest {
    @Test fun unavailablePowerIsExcludedAtEveryIntervalButTrueZerosRemain() {
        val rows = listOf(
            sample(1,3,0,0), sample(2,6,0,0), sample(3,10,1,100),
            sample(4,14,2,0), sample(5,14,2,0,true),
            sample(6,17,2,0), sample(7,21,3,200), sample(8,25,4,0))
        val result = WorkoutStatistics(Workout().apply { duration.set(8);distance.set(25) },rows)
        assertEquals(75,result.averagePower) // (100 + 0 + 200 + 0) / 4 available seconds.
        assertFalse(1f in result.power.availableTimes)
        assertFalse(6f in result.power.availableTimes)
        assertTrue(8f in result.power.availableTimes)
        assertEquals(100,RecordedPowerStatistics(result.samples,5f,8f).average)
    }
    @Test fun powerThatNeverBecomesAvailableHasNoAverage() {
        val result = WorkoutStatistics(Workout().apply { duration.set(2) },
            listOf(sample(1,3,0,0),sample(2,6,0,0)))
        assertNull(result.averagePower)
    }
    private fun sample(time: Int, distance: Int, strokes: Int, power: Int, rest: Boolean = false) = Snapshot().apply {
        duration.set(time); this.distance.set(distance); this.strokes.set(strokes)
        this.power.set(power); strokeRate.set(24); speed.set(400)
        difficulty.set(if (rest) Difficulty.REST else Difficulty.HARD)
    }
    @Test fun averagesExcludeRestAndUseWorkTotalsAndElapsedWeights() {
        val workout = Workout().apply { duration.set(150); distance.set(520); strokes.set(50) }
        val result = WorkoutStatistics(workout, listOf(sample(60, 200, 24, 100), sample(90, 220, 26, 0, true), sample(150, 520, 50, 200)))
        assertEquals(120, result.workSeconds)
        assertEquals(500, result.workMeters)
        assertEquals(48, result.workStrokes)
        assertEquals(120, result.averageSplit)
        assertEquals(150, result.averagePower)
        assertEquals(24, result.averageRate)
        assertEquals(listOf(60f, 90f, 150f), result.samples.map { it.first })
        assertEquals(listOf(60f, 90f), result.boundaries)
    }
    @Test fun zeroPowerDuringWorkCountsAndDuplicateTimestampsDoNotAddWeight() {
        val workout = Workout().apply { duration.set(90); distance.set(300) }
        val result = WorkoutStatistics(workout, listOf(sample(30, 100, 12, 150), sample(30, 100, 12, 150), sample(90, 300, 36, 0)))
        assertEquals(50, result.averagePower)
        assertEquals(2, result.samples.size)
    }
    @Test fun migratedWorkoutsWithNullActiveTotalsStillShowStatistics() {
        val workout = Workout().apply {
            duration.set(150); distance.set(520); strokes.set(50)
            planActiveSeconds.set(null); planActiveDistance.set(null); planActiveStrokes.set(null)
        }
        val result = WorkoutStatistics(workout, listOf(sample(60, 200, 24, 100),
            sample(90, 220, 26, 0, true), sample(150, 520, 50, 200)))
        assertEquals(120, result.workSeconds)
        assertEquals(500, result.workMeters)
        assertEquals(48, result.workStrokes)
    }

    @Test fun missingLegacySampleTimesUseEstimatedSpacing() {
        val workout = Workout().apply { duration.set(60); distance.set(200) }
        val first = sample(30, 100, 12, 100).apply { duration.set(null) }
        val last = sample(60, 200, 24, 100)
        val result = WorkoutStatistics(workout, listOf(first, last))
        assertEquals(listOf(30f, 60f), result.samples.map { it.first })
        assertEquals(60, result.workSeconds)
    }
    @Test fun heartRateIncludesRecoveryAndDeduplicatesReadings() {
        val workout = Workout().apply { duration.set(11) }
        val result = WorkoutStatistics(workout, listOf(
            sample(1, 1, 1, 100).apply { pulse.set(180) },
            sample(1, 1, 1, 100).apply { pulse.set(100) },
            sample(11, 20, 4, 0, true).apply { pulse.set(120) }))
        assertEquals(118, result.pulse.average)
        assertEquals(100, result.pulse.minimum)
        assertEquals(120, result.pulse.maximum)
    }
    @Test fun missingHeartRateIsNotAZeroOrBackfilledAcrossSensorGaps() {
        val result = WorkoutStatistics(Workout().apply { duration.set(100) }, listOf(
            sample(1, 1, 1, 100).apply { pulse.set(120) },
            sample(50, 100, 20, 100).apply { pulse.set(0) },
            sample(100, 200, 40, 100).apply { pulse.set(160) }))
        assertEquals(140, result.pulse.average)
        assertEquals(120, result.pulse.minimum)
        assertEquals(160, result.pulse.maximum)
    }
    @Test fun absentHeartRateHasNoStatistics() {
        val result = WorkoutStatistics(Workout().apply { duration.set(60) }, listOf(
            sample(10, 10, 4, 100).apply { pulse.set(null) }, sample(60, 200, 24, 100)))
        assertNull(result.pulse.average)
        assertNull(result.pulse.minimum)
        assertNull(result.pulse.maximum)
    }
}
