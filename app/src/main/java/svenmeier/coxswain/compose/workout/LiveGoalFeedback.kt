package svenmeier.coxswain.compose.workout

import svenmeier.coxswain.gym.Measurement
import svenmeier.coxswain.view.ValueBinding
import kotlin.math.roundToInt

/** Five seconds of display-only goal feedback; recordings and scoring keep raw measurements. */
internal class LiveGoalSmoother {
    private val samples = java.util.TreeMap<Int, Measurement>()
    fun update(raw: Measurement): Measurement {
        if (samples.isNotEmpty() && raw.duration < samples.lastKey()) samples.clear()
        samples[raw.duration] = Measurement(raw)
        while (samples.isNotEmpty() && samples.firstKey() < raw.duration - 4) samples.pollFirstEntry()
        return Measurement(raw).apply {
            speed = average(raw.speed) { it.speed }
            strokeRate = average(raw.strokeRate) { it.strokeRate }
            power = average(raw.power) { it.power }
        }
    }

    private fun average(current: Int, metric: (Measurement) -> Int): Int {
        if (current <= 0) return 0
        // Sensors become available independently. Start with the samples we have,
        // rather than diluting the first readings with pre-row/unavailable zeros.
        // Keep zeros after a valid reading so real slowdowns still affect the window.
        return samples.values.map(metric).dropWhile { it <= 0 }.average().roundToInt()
    }
}

internal fun rawGoalValue(binding: ValueBinding, measurement: Measurement): Int = when (binding) {
    ValueBinding.SPLIT, ValueBinding.SPEED -> measurement.speed
    ValueBinding.STROKE_RATE -> measurement.strokeRate
    ValueBinding.POWER -> measurement.power
    ValueBinding.PULSE -> measurement.pulse
    else -> 0
}

/** Compact direction for coaching when the goal needs the large bottom readout. */
internal fun liveTargetFeedback(binding: ValueBinding, segment: svenmeier.coxswain.gym.Segment,
                                measurement: Measurement, goal: GoalDisplay, rateCap: Boolean): String? {
    if(binding == ValueBinding.SPLIT) {
        if(measurement.speed <= 0) return null
        val split = (50000f / measurement.speed).roundToInt()
        return "Now %d:%02d · %s".format(split/60,split%60,goal.feedback ?: "On target")
    }
    val actual: Int
    val target: Int
    val unit: String
    when(binding) {
        ValueBinding.STROKE_RATE -> { actual=measurement.strokeRate;target=segment.strokeRate.get();unit="SPM" }
        ValueBinding.POWER -> { actual=measurement.power;target=segment.power.get();unit="W" }
        ValueBinding.PULSE -> { actual=measurement.pulse;target=segment.pulse.get();unit="BPM" }
        else -> return null
    }
    if(actual <= 0) return null
    val difference=actual-target
    val direction=when {
        difference==0 -> if(rateCap) "At cap" else "On target"
        difference>0 -> "$difference $unit above ${if(rateCap) "cap" else "target"}"
        else -> "${-difference} $unit below ${if(rateCap) "cap" else "target"}"
    }
    return "Now $actual $unit · $direction"
}
