package svenmeier.coxswain

import svenmeier.coxswain.gym.Difficulty
import svenmeier.coxswain.gym.Snapshot
import svenmeier.coxswain.gym.Workout
import kotlin.math.roundToInt

/** Recorded timestamps are authoritative; legacy recordings have only evenly spaced samples. */
internal class WorkoutStatistics(workout: Workout, recordings: List<Snapshot>) {
    // Propoid query lists close their cursor when an iteration finishes.
    // Statistics need several passes, so materialize once at the boundary.
    private val snapshots = ArrayList(recordings)
    val duration = workout.duration.get().coerceAtLeast(0)
    private val timed = snapshots.isNotEmpty() && snapshots.all { (it.duration.get() ?: 0) > 0 }
    val samples: List<Pair<Float, Snapshot>> = if (timed) snapshots
        .map { (it.duration.get() ?: 0).coerceIn(0, duration).toFloat() to it }
        .sortedBy { it.first }.distinctByLastTime()
    else snapshots.mapIndexed { index, sample ->
        (duration.toFloat() * (index + 1) / snapshots.size) to sample
    }
    val boundaries = samples.zipWithNext().filter { it.first.second.difficulty.get() != it.second.second.difficulty.get() }.map { it.first.first }
    val restRanges = samples.mapIndexedNotNull { index, sample ->
        if (sample.second.difficulty.get() == Difficulty.REST)
            (if (index == 0) 0f else samples[index - 1].first) to sample.first else null
    }
    private val work = samples.filter { it.second.difficulty.get() != Difficulty.REST }
    private var seconds = 0f
    private var meters = 0
    private var strokes = 0
    private var weightedPower = 0f
    init {
        var previousTime = 0f
        var previousDistance = 0
        var previousStrokes = 0
        samples.forEach { (time, sample) ->
            val delta = (time - previousTime).coerceAtLeast(0f)
            if (sample.difficulty.get() != Difficulty.REST) {
                seconds += delta
                meters += (sample.distance.get() - previousDistance).coerceAtLeast(0)
                strokes += (sample.strokes.get() - previousStrokes).coerceAtLeast(0)
                weightedPower += sample.power.get() * delta
            }
            previousTime = time
            previousDistance = sample.distance.get()
            previousStrokes = sample.strokes.get()
        }
    }
    private val hasRest = restRanges.isNotEmpty() && timed
    private val hasActiveTotals = (workout.planActiveSeconds.get() ?: 0) > 0 &&
        workout.planActiveDistance.get() != null && workout.planActiveStrokes.get() != null
    val workSeconds = if (hasActiveTotals) workout.planActiveSeconds.get() else if (hasRest) seconds.roundToInt() else duration
    val workMeters = if (hasActiveTotals) workout.planActiveDistance.get() else if (hasRest) meters else workout.distance.get()
    val workStrokes = if (hasActiveTotals) workout.planActiveStrokes.get() else if (hasRest) strokes else workout.strokes.get()
    val averageSplit = if (workMeters > 0) (workSeconds * 500.0 / workMeters).roundToInt() else null
    val averagePower = if (seconds > 0) (weightedPower / seconds).roundToInt() else null
    val averageRate = if (workSeconds > 0) (workStrokes * 60.0 / workSeconds).roundToInt() else null
    val bestSplit = work.mapNotNull { if (it.second.speed.get() > 0) 50000 / it.second.speed.get() else null }.minOrNull()
    val minimumRate = work.minOfOrNull { it.second.strokeRate.get() }
    val maximumRate = work.maxOfOrNull { it.second.strokeRate.get() }
    val maximumPower = work.maxOfOrNull { it.second.power.get() }
    // Heart rate includes recovery, and unavailable readings never count as zero BPM.
    val pulse = RecordedPulseStatistics(samples)
}

internal class RecordedPulseStatistics(samples: List<Pair<Float, Snapshot>>, start: Float = 0f,
                                       end: Float = Float.MAX_VALUE) {
    private val valid = samples.filter {
        (it.first > start || start == 0f && it.first == 0f) && it.first <= end && (it.second.pulse.get() ?: 0) > 0
    }
    val minimum = valid.minOfOrNull { it.second.pulse.get() }
    val maximum = valid.maxOfOrNull { it.second.pulse.get() }
    val average: Int?
    init {
        var weighted = 0.0
        var seconds = 0f
        samples.forEachIndexed { index, (time, sample) ->
            val pulse = sample.pulse.get() ?: 0
            val previous = samples.getOrNull(index - 1)
            val from = maxOf(start, previous?.first ?: 0f)
            val to = minOf(end, time)
            if (pulse > 0 && time in start..end && to > from) {
                // A first reading or reconnection must not backfill an unobserved sensor gap.
                val weight = if ((previous?.second?.pulse?.get() ?: 0) > 0) to - from else minOf(1f, to - from)
                weighted += pulse * weight
                seconds += weight
            }
        }
        average = if (seconds > 0) (weighted / seconds).roundToInt()
            else valid.map { it.second.pulse.get() }.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
    }
}

private fun List<Pair<Float, Snapshot>>.distinctByLastTime() = groupBy { it.first }.values.map { it.last() }
