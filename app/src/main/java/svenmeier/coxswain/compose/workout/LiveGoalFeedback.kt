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
        val values = samples.values
        return Measurement(raw).apply {
            speed = if(raw.speed > 0) values.map { it.speed }.average().roundToInt() else 0
            strokeRate = if(raw.strokeRate > 0) values.map { it.strokeRate }.average().roundToInt() else 0
            power = if(raw.power > 0) values.map { it.power }.average().roundToInt() else 0
        }
    }
}

/** Target direction belongs below the user-selected metric grid. */
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
