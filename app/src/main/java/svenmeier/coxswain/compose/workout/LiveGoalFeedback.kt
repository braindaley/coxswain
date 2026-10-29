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

internal fun metricsWithGoal(metrics: List<ValueBinding>, goal: ValueBinding?): List<ValueBinding> {
    if(goal == null || goal in metrics) return metrics
    val result = metrics.toMutableList()
    if(result.isEmpty()) return listOf(goal)
    val replacement = if(goal == ValueBinding.SPLIT) result.indexOf(ValueBinding.SPEED).takeIf { it >= 0 } else null
    result[replacement ?: 2.coerceAtMost(result.lastIndex)] = goal
    return result
}
