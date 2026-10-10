package svenmeier.coxswain

import svenmeier.coxswain.gym.Difficulty
import svenmeier.coxswain.gym.Segment
import svenmeier.coxswain.gym.Snapshot
import svenmeier.coxswain.gym.Workout
import svenmeier.coxswain.gym.WorkoutDefinition
import kotlin.math.max
import kotlin.math.roundToInt

internal enum class ResultMeasure {
    SPLIT, POWER, RATE, PULSE;
    fun value(sample: Snapshot): Float? = when (this) {
        SPLIT -> sample.speed.get().takeIf { it > 0 }?.let { 50000f / it }
        POWER -> sample.power.get().toFloat()
        RATE -> sample.strokeRate.get().toFloat()
        PULSE -> sample.pulse.get()?.takeIf { it > 0 }?.toFloat()
    }
    fun target(segment: Segment?): Float? = when (this) {
        SPLIT -> segment?.speed?.get()?.takeIf { it > 0 }?.let { 50000f / it }
        POWER -> segment?.power?.get()?.takeIf { it > 0 }?.toFloat()
        RATE -> segment?.strokeRate?.get()?.takeIf { it > 0 }?.toFloat()
        PULSE -> segment?.pulse?.get()?.takeIf { it > 0 }?.toFloat()
    }
}

internal data class ResultPoint(val elapsed: Float, val clock: Long, val snapshot: Snapshot,
                                val interval: Int, val intervalStart: Float)
internal data class ResultPhase(val index: Int, val start: Float, val end: Float,
                                val segment: Segment?, val difficulty: Difficulty,
                                val estimated: Boolean)
internal data class ResultScale(val low: Float, val high: Float) {
    fun fraction(value: Float, fasterUp: Boolean): Float {
        val f = ((value - low) / (high - low)).coerceIn(0f, 1f)
        return if (fasterUp) f else 1f - f
    }
}
internal data class IntervalResult(val seconds: Int, val meters: Int, val split: Int?,
                                   val power: Int?, val rate: Int?, val pulse: Int?)

/** Display-only quality handling. Saved totals, race scoring and coaching references stay untouched. */
internal class WorkoutChartData(workout: Workout, val statistics: WorkoutStatistics) {
    val segments: List<Segment> = runCatching {
        WorkoutDefinition.thaw(workout.programDefinition.get())?.getSegments()?.toList() ?: emptyList()
    }.getOrDefault(emptyList())
    val start = workout.start.get().coerceAtLeast(0L)
    private val elapsedMillis = statistics.duration * 1000L
    private val recordedFinish = workout.completed.get() ?: 0L
    private val lastClock = statistics.samples.maxOfOrNull { it.second.recordedAt.get() ?: 0L } ?: 0L
    val end = max(start + elapsedMillis, max(recordedFinish, lastClock))
    val span = (end - start).coerceAtLeast(1L)
    // Never imply precision for imports, malformed clocks, or legacy evenly spaced samples.
    val estimatedClock = statistics.samples.isEmpty() || statistics.samples.any {
        val clock = it.second.recordedAt.get() ?: 0L
        clock < start || clock > end || clock == 0L || (it.second.duration.get() ?: 0) <= 0
    } || statistics.samples.zipWithNext().any { (a, b) ->
        (a.second.recordedAt.get() ?: 0L) > (b.second.recordedAt.get() ?: 0L)
    }
    private var inferred = false
    val points: List<ResultPoint> = buildPoints()
    private val groupedPoints = points.groupBy { it.interval }
    private val phaseStarts = groupedPoints.values.map { it.first().intervalStart }.sorted()
    val phases: List<ResultPhase> = groupedPoints.values.map { samples ->
        val first = samples.first()
        ResultPhase(first.interval, first.intervalStart, statistics.duration.toFloat(),
            segments.getOrNull(first.interval), segments.getOrNull(first.interval)?.difficulty?.get()
                ?: first.snapshot.difficulty.get(), inferred)
    }.sortedBy { it.start }.mapIndexed { index, phase ->
        phase.copy(end = phaseStarts.getOrNull(index + 1) ?: statistics.duration.toFloat())
    }
    // Only the initial work step is eligible for startup exclusion.
    val startupEnd = findStartupEnd()
    private val firstValid = ResultMeasure.entries.associateWith { measure ->
        points.firstOrNull { (measure.value(it.snapshot) ?: 0f) > 0f }?.elapsed
    }
    private val phaseByIndex = phases.associateBy { it.index }
    val pauseRanges: List<Pair<Long, Long>> = if (estimatedClock) emptyList() else points.zipWithNext()
        .mapNotNull { (a, b) ->
            val gap = b.clock - a.clock - ((b.elapsed - a.elapsed) * 1000).toLong()
            if (gap > 2000L) (a.clock + 1000L) to (b.clock - 1000L) else null
        }

    private fun buildPoints(): List<ResultPoint> {
        var interval = 0
        var baseline = Counter(0f, 0f, 0f, 0f)
        var previous = baseline
        return statistics.samples.map { (elapsed, sample) ->
            val current = Counter(elapsed, sample.distance.get().toFloat(), sample.strokes.get().toFloat(), sample.energy.get().toFloat())
            val recordedIndex = sample.intervalIndex.get() ?: -1
            val recordedStart = sample.intervalStart.get() ?: 0
            var stepStart: Float
            if (recordedIndex in segments.indices && recordedStart in 0..elapsed.toInt()) {
                interval = recordedIndex
                stepStart = recordedStart.toFloat()
            } else {
                if (segments.size > 1) inferred = true
                // For old recordings, reconstruct the prescribed targets, including identical efforts.
                while (interval < segments.lastIndex && reached(segments[interval], current, baseline)) {
                    baseline = boundary(segments[interval], baseline, previous, current)
                    interval++
                }
                stepStart = baseline.time
            }
            previous = current
            val clock = if (estimatedClock) start + (elapsed / statistics.duration.coerceAtLeast(1) * span).toLong()
                else sample.recordedAt.get()
            ResultPoint(elapsed, clock.coerceIn(start, end), sample,
                if (segments.isEmpty()) -1 else interval, stepStart)
        }
    }

    private data class Counter(val time: Float, val distance: Float, val strokes: Float, val energy: Float) {
        fun targetValue(segment: Segment): Float = when {
            segment.distance.get() > 0 -> distance
            segment.strokes.get() > 0 -> strokes
            segment.energy.get() > 0 -> energy
            else -> time
        }
        fun interpolate(other: Counter, f: Float) = Counter(time + (other.time - time) * f,
            distance + (other.distance - distance) * f, strokes + (other.strokes - strokes) * f,
            energy + (other.energy - energy) * f)
    }
    private fun reached(segment: Segment, current: Counter, baseline: Counter): Boolean =
        segment.target > 0 && current.targetValue(segment) - baseline.targetValue(segment) > segment.target

    private fun boundary(segment: Segment, baseline: Counter, previous: Counter, current: Counter): Counter {
        if (segment.duration.get() > 0 && segment.distance.get() == 0 && segment.strokes.get() == 0 && segment.energy.get() == 0) {
            val time = baseline.time + segment.duration.get()
            val f = ((time - previous.time) / (current.time - previous.time).coerceAtLeast(1f)).coerceIn(0f, 1f)
            return previous.interpolate(current, f).copy(time = time)
        }
        val target = baseline.targetValue(segment) + segment.target
        val f = ((target - previous.targetValue(segment)) /
            (current.targetValue(segment) - previous.targetValue(segment)).coerceAtLeast(1f)).coerceIn(0f, 1f)
        return previous.interpolate(current, f)
    }

    fun clockAt(elapsed: Float): Long {
        if (elapsed <= 0f) return start
        if (elapsed >= statistics.duration) return end
        val next = points.indexOfFirst { it.elapsed >= elapsed }
        val b = points.getOrNull(next) ?: return end
        val a = points.getOrNull(next - 1)
        val beforeElapsed = a?.elapsed ?: 0f
        val beforeClock = a?.clock ?: start
        return beforeClock + ((b.clock - beforeClock) *
            ((elapsed - beforeElapsed) / (b.elapsed - beforeElapsed).coerceAtLeast(.001f))).toLong()
    }
    fun clockFraction(clock: Long): Float = ((clock - start).toDouble() / span).toFloat().coerceIn(0f, 1f)
    fun phase(point: ResultPoint): ResultPhase? = phaseByIndex[point.interval]
    fun isRest(point: ResultPoint) = (phase(point)?.difficulty ?: point.snapshot.difficulty.get()) == Difficulty.REST

    fun value(point: ResultPoint, measure: ResultMeasure): Float? {
        val raw = measure.value(point.snapshot) ?: return null
        if (raw < 0f) return null
        // Startup zeros mean the monitor hasn't produced this measure yet. Later true zeros remain.
        if (raw <= 0f && point.elapsed < (firstValid[measure] ?: Float.MAX_VALUE)) return null
        return raw
    }
    fun plottedValue(point: ResultPoint, measure: ResultMeasure): Float? =
        if (measure != ResultMeasure.PULSE && point.elapsed < startupEnd) null else value(point, measure)

    /** Three-reading median for presentation only; never cross steps, zeros or sensor gaps. */
    fun displayValues(measure: ResultMeasure): List<Float?> {
        val values = points.map { plottedValue(it, measure) }
        return values.mapIndexed { index, value ->
            val before = points.getOrNull(index - 1)
            val after = points.getOrNull(index + 1)
            val point = points[index]
            val previous = values.getOrNull(index - 1)
            val next = values.getOrNull(index + 1)
            if (value == null || value <= 0f || previous == null || previous <= 0f || next == null || next <= 0f ||
                before == null || after == null || before.interval != point.interval || after.interval != point.interval ||
                isRest(before) != isRest(point) || isRest(after) != isRest(point) ||
                point.elapsed - before.elapsed !in 0.001f..2.5f || after.elapsed - point.elapsed !in 0.001f..2.5f ||
                point.clock - before.clock !in 1L..3000L || after.clock - point.clock !in 1L..3000L) value
            else listOf(previous, value, next).sorted()[1]
        }
    }

    private fun findStartupEnd(): Float {
        val first = phases.firstOrNull { it.difficulty != Difficulty.REST } ?: return 0f
        // Bound the exclusion to one minute and at most a quarter of the first step, so short
        // efforts still have a visible trace. Sparse historical samples are not thrown away.
        val limit = first.start + minOf(60f, (first.end - first.start) / 4f)
        val initial = points.filter { it.interval == first.index && it.elapsed < limit }
        if (initial.isEmpty() || initial.none { it.snapshot.speed.get() > 0 }) return 0f
        val hasStrokeCounts = initial.any { it.snapshot.strokes.get() > 0 }
        initial.forEach { point ->
            val window = initial.filter { it.elapsed in (point.elapsed - 10f)..point.elapsed }
            if (window.first().elapsed > point.elapsed - 8f) return@forEach
            if (hasStrokeCounts && point.snapshot.strokes.get() < 6) return@forEach
            if (window.zipWithNext().any { (a, b) -> b.elapsed - a.elapsed > 2f }) return@forEach
            val pace = window.mapNotNull { ResultMeasure.SPLIT.value(it.snapshot) }
            if (pace.size != window.size) return@forEach
            val median = pace.sorted()[pace.size / 2]
            if (pace.maxOrNull()!! - pace.minOrNull()!! <= median * .03f) return point.elapsed
        }
        return limit
    }

    fun scale(measure: ResultMeasure): ResultScale {
        val work = points.filter { measure == ResultMeasure.PULSE || !isRest(it) }
        val average = when (measure) {
            ResultMeasure.SPLIT -> statistics.averageSplit
            ResultMeasure.POWER -> statistics.averagePower
            ResultMeasure.RATE -> statistics.averageRate
            ResultMeasure.PULSE -> statistics.pulse.average
        }?.toFloat()
        val values = (work.mapNotNull { plottedValue(it, measure) } +
            phases.filter { measure == ResultMeasure.PULSE || it.difficulty != Difficulty.REST }
                .mapNotNull { measure.target(it.segment) } + listOfNotNull(average))
        val low = values.minOrNull() ?: 0f
        val high = values.maxOrNull() ?: 1f
        val padding = max((high - low) * .2f, if (measure == ResultMeasure.SPLIT) 4f else 3f)
        return ResultScale((low - padding).coerceAtLeast(0f), high + padding)
    }
    fun breaksBefore(index: Int, measure: ResultMeasure = ResultMeasure.SPLIT): Boolean {
        val before = points.getOrNull(index - 1) ?: return true
        val current = points[index]
        return (measure != ResultMeasure.PULSE && before.interval != current.interval) ||
            current.clock - before.clock - ((current.elapsed - before.elapsed) * 1000).toLong() > 2000L
    }
    fun intervalResult(phase: ResultPhase): IntervalResult {
        // Interpolate cumulative counters at boundaries; do not attribute rests to the work beside them.
        fun counterAt(time: Float, read: (Snapshot) -> Int): Float {
            if (time <= 0f) return 0f
            val next = points.indexOfFirst { it.elapsed >= time }
            val b = points.getOrNull(next) ?: return points.lastOrNull()?.let { read(it.snapshot).toFloat() } ?: 0f
            val a = points.getOrNull(next - 1)
            val fromTime = a?.elapsed ?: 0f
            val from = a?.snapshot?.let(read)?.toFloat() ?: 0f
            return from + (read(b.snapshot) - from) * ((time - fromTime) / (b.elapsed - fromTime).coerceAtLeast(.001f))
        }
        val seconds = (phase.end - phase.start).roundToInt().coerceAtLeast(0)
        val meters = (counterAt(phase.end) { it.distance.get() } - counterAt(phase.start) { it.distance.get() }).roundToInt().coerceAtLeast(0)
        val strokes = (counterAt(phase.end) { it.strokes.get() } - counterAt(phase.start) { it.strokes.get() }).roundToInt().coerceAtLeast(0)
        var power = 0f
        var weight = 0f
        points.forEachIndexed { index, point ->
            val from = max(phase.start, points.getOrNull(index - 1)?.elapsed ?: 0f)
            val until = minOf(phase.end, point.elapsed)
            if (until > from && point.interval == phase.index) {
                power += point.snapshot.power.get() * (until - from)
                weight += until - from
            }
        }
        return IntervalResult(seconds, meters, if (meters > 0) (seconds * 500f / meters).roundToInt() else null,
            if (weight > 0f) (power / weight).roundToInt() else null,
            if (seconds > 0) (strokes * 60f / seconds).roundToInt() else null,
            RecordedPulseStatistics(statistics.samples, phase.start, phase.end).average)
    }
}
