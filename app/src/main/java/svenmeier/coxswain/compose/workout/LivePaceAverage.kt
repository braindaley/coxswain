package svenmeier.coxswain.compose.workout

import svenmeier.coxswain.gym.Measurement
import kotlin.math.roundToInt

/** A rolling 15-second distance average, published every 3 s independently of stroke events. */
internal class LivePaceAverage {
    private val samples = java.util.TreeMap<Int, Measurement>()
    private var lastUpdate: Int? = null
    private var smoothedSpeed: Double? = null
    private var speed = 0

    fun update(raw: Measurement): Int {
        val previous = samples.lastEntry()?.value
        if (previous != null && (raw.duration < previous.duration || raw.distance < previous.distance ||
                    raw.duration - previous.duration > 5)) {
            samples.clear(); lastUpdate = null; smoothedSpeed = null; speed = 0
        }
        samples[raw.duration] = Measurement(raw)
        val start = raw.duration - 15
        // Keep the observation before the window edge so the starting counter can be interpolated.
        while (samples.size > 1 && samples.higherKey(samples.firstKey()) <= start) samples.pollFirstEntry()
        val first = samples.firstEntry().value
        val fromTime = maxOf(start, first.duration)
        val span = raw.duration - fromTime
        if (span >= 5 && (lastUpdate == null || raw.duration - lastUpdate!! >= 3)) {
            val next = samples.higherEntry(first.duration)?.value
            val fromDistance = if (first.duration < fromTime && next != null)
                first.distance + (next.distance - first.distance) *
                    (fromTime - first.duration).toDouble() / (next.duration - first.duration)
            else first.distance.toDouble()
            val measured = (raw.distance - fromDistance).coerceAtLeast(0.0) * 100 / span
            // Ease changes in overlapping windows instead of jumping as a drive or recovery
            // enters/leaves the edge. Sustained changes still move the average toward the new pace.
            smoothedSpeed = if (measured == 0.0) 0.0 else smoothedSpeed?.takeIf { it > 0 }?.let {
                it + 0.35 * (measured - it)
            } ?: measured
            speed = smoothedSpeed!!.roundToInt()
            lastUpdate = raw.duration
        }
        return speed
    }
}
