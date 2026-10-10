package svenmeier.coxswain

import svenmeier.coxswain.gym.Snapshot
import svenmeier.coxswain.gym.Workout
import svenmeier.coxswain.gym.WorkoutStatus

internal data class RecordedHeartRateRange(val startingLow: Int? = null, val peak: Int? = null)

/** A conservative observed peak: highest BPM sustained across at least five recorded seconds. */
internal fun recordedHeartRateRange(workout: Workout, recordings: List<Snapshot>): RecordedHeartRateRange {
    if (workout.status.get() != WorkoutStatus.COMPLETED) return RecordedHeartRateRange()
    val samples = ArrayList(recordings).filter { (it.duration.get() ?: 0) > 0 }
        .sortedBy { it.duration.get() }.groupBy { it.duration.get() }.values.map { it.last() }
    val window = ArrayDeque<Snapshot>()
    var previous: Snapshot? = null
    var peak: Int? = null
    var startingLow: Int? = null
    for (sample in samples) {
        val time = sample.duration.get()
        val pulse = sample.pulse.get() ?: 0
        val before = previous
        val delta = if (before != null) time - before.duration.get() else 0
        val clockDelta = if (before != null) (sample.recordedAt.get() ?: 0L) - (before.recordedAt.get() ?: 0L) else 0L
        if (pulse !in 1..400 || delta > 5 || clockDelta - delta * 1000L > 2000L) window.clear()
        if (pulse in 1..400) {
            window.addLast(sample)
            // Keep the observation just before the five-second boundary to establish coverage.
            while (window.size > 1 && window.elementAt(1).duration.get() <= time - 5) window.removeFirst()
            if (window.size >= 3 && time - window.first().duration.get() >= 5) {
                val sustained = window.minOf { it.pulse.get() }
                peak = maxOf(peak ?: 0, sustained)
                if (time <= 30) {
                    // A single erroneous low reading must not become the resting estimate.
                    val sustainedLow = window.maxOf { it.pulse.get() }
                    startingLow = minOf(startingLow ?: sustainedLow, sustainedLow)
                }
            }
        }
        previous = sample
    }
    return RecordedHeartRateRange(startingLow, peak)
}
