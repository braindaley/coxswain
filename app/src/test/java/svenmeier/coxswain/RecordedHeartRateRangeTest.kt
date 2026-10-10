package svenmeier.coxswain

import org.junit.Assert.*
import org.junit.Test
import svenmeier.coxswain.gym.*

class RecordedHeartRateRangeTest {
    private fun row(status: WorkoutStatus = WorkoutStatus.COMPLETED) = Workout().apply { this.status.set(status) }
    private fun samples(count: Int = 60, pulse: (Int) -> Int) = (1..count).map { second -> Snapshot().apply {
        duration.set(second); recordedAt.set(100000L + second * 1000); this.pulse.set(pulse(second))
    } }
    @Test fun usesEarlyLowAndSustainedPeakWithoutSingleReadingSpikes() {
        val history = samples { if (it == 2) 30 else if (it <= 15) 90 else if (it == 40) 220 else 170 }
        assertEquals(RecordedHeartRateRange(90, 170), recordedHeartRateRange(row(), history))
    }
    @Test fun lowAfterFirstThirtySecondsDoesNotBecomeRestingEstimate() {
        assertEquals(RecordedHeartRateRange(110, 110), recordedHeartRateRange(row(), samples { if (it <= 30) 110 else 80 }))
    }
    @Test fun incompleteAndUntimedRowsCannotSupplyDefaults() {
        assertEquals(RecordedHeartRateRange(), recordedHeartRateRange(row(WorkoutStatus.ENDED_EARLY), samples { 190 }))
        assertEquals(RecordedHeartRateRange(), recordedHeartRateRange(row(WorkoutStatus.ACTIVE), samples { 190 }))
        assertEquals(RecordedHeartRateRange(), recordedHeartRateRange(row(), samples { 190 }.onEach { it.duration.set(0) }))
    }
    @Test fun missingSensorAndPauseGapsDoNotBecomeSustainedValues() {
        val history = samples(8) { if (it in 3..4) 0 else 190 }
        assertEquals(RecordedHeartRateRange(), recordedHeartRateRange(row(), history))
        val paused = samples(6) { 190 }.onEach { if (it.duration.get() >= 4) it.recordedAt.set(it.recordedAt.get() + 30000L) }
        assertEquals(RecordedHeartRateRange(), recordedHeartRateRange(row(), paused))
        assertEquals(RecordedHeartRateRange(), recordedHeartRateRange(row(), samples(60) { 0 }))
    }
}
