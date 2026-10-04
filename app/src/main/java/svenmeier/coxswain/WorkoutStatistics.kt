package svenmeier.coxswain

import svenmeier.coxswain.gym.Difficulty
import svenmeier.coxswain.gym.Snapshot
import svenmeier.coxswain.gym.Workout
import kotlin.math.roundToInt

/** Recorded timestamps are authoritative; legacy recordings have only evenly spaced samples. */
internal class WorkoutStatistics(workout: Workout, snapshots: List<Snapshot>) {
    val duration = workout.duration.get().coerceAtLeast(0)
    private val timed = snapshots.any { it.duration.get() > 0 }
    val samples: List<Pair<Float, Snapshot>> = if (timed) snapshots
        .map { it.duration.get().coerceIn(0, duration).toFloat() to it }
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
    val workSeconds = if (workout.planActiveSeconds.get() > 0) workout.planActiveSeconds.get() else if (hasRest) seconds.roundToInt() else duration
    val workMeters = if (workout.planActiveSeconds.get() > 0) workout.planActiveDistance.get() else if (hasRest) meters else workout.distance.get()
    val workStrokes = if (workout.planActiveSeconds.get() > 0) workout.planActiveStrokes.get() else if (hasRest) strokes else workout.strokes.get()
    val averageSplit = if (workMeters > 0) (workSeconds * 500.0 / workMeters).roundToInt() else null
    val averagePower = if (seconds > 0) (weightedPower / seconds).roundToInt() else null
    val averageRate = if (workSeconds > 0) (workStrokes * 60.0 / workSeconds).roundToInt() else null
    val bestSplit = work.mapNotNull { if (it.second.speed.get() > 0) 50000 / it.second.speed.get() else null }.minOrNull()
    val minimumRate = work.minOfOrNull { it.second.strokeRate.get() }
    val maximumRate = work.maxOfOrNull { it.second.strokeRate.get() }
    val maximumPower = work.maxOfOrNull { it.second.power.get() }
}

private fun List<Pair<Float, Snapshot>>.distinctByLastTime() = groupBy { it.first }.values.map { it.last() }
