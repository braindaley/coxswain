package svenmeier.coxswain

import android.content.Context
import androidx.preference.PreferenceManager
import org.json.JSONObject
import svenmeier.coxswain.gym.WorkoutStatus
import svenmeier.coxswain.gym.Workout
import svenmeier.coxswain.gym.Snapshot
import svenmeier.coxswain.gym.Difficulty
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Relative output categories, independent of physiological HR zones. */
data class OutputZones(val moderate: Int, val vigorous: Int, val peak: Int, val fasterIsLower: Boolean = false) {
    init {
        require(listOf(moderate, vigorous, peak).all { it in 1..100000 })
        require(if (fasterIsLower) moderate > vigorous && vigorous > peak else moderate < vigorous && vigorous < peak)
    }
    fun zone(value: Float): Int? = when {
        !value.isFinite() || value <= 0f -> null
        fasterIsLower && value <= peak || !fasterIsLower && value >= peak -> 3
        fasterIsLower && value <= vigorous || !fasterIsLower && value >= vigorous -> 2
        fasterIsLower && value <= moderate || !fasterIsLower && value >= moderate -> 1
        else -> 0
    }
    fun pieces(from: Float, to: Float): List<Triple<Float, Float, Int>> {
        if (zone(from) == null || zone(to) == null) return emptyList()
        val cuts = mutableListOf(0f, 1f)
        if (from != to) listOf(moderate, vigorous, peak).forEach { boundary ->
            val fraction = (boundary - from) / (to - from)
            if (fraction > 0 && fraction < 1) cuts.add(fraction)
        }
        return cuts.sorted().zipWithNext().map { (a, b) -> Triple(a, b, zone(from + (to - from) * (a + b) / 2)!!) }
    }
    fun encode() = JSONObject().put("moderate", moderate).put("vigorous", vigorous).put("peak", peak)
    companion object {
        fun decode(json: JSONObject?, fasterIsLower: Boolean) = json?.let {
            OutputZones(it.getInt("moderate"), it.getInt("vigorous"), it.getInt("peak"), fasterIsLower)
        }
        /** Each input represents the median output in one five-second bin, avoiding sample-rate bias. */
        fun suggest(values: List<Int>, fasterIsLower: Boolean = false): OutputZones? {
            val sorted = values.filter { it in 1..100000 }.sorted()
            if (sorted.size < 12) return null
            fun percentile(fraction: Double) = sorted[(ceil(sorted.size * fraction).toInt() - 1).coerceIn(sorted.indices)]
            return runCatching {
                if (fasterIsLower) OutputZones(percentile(.75), percentile(.5), percentile(.15), true)
                else OutputZones(percentile(.25), percentile(.5), percentile(.85))
            }.getOrNull() // Flat history cannot establish three distinct thresholds.
        }
    }
}

data class PerformanceZones(val power: OutputZones?, val pace: OutputZones?) {
    init { require(power != null || pace != null); require(power?.fasterIsLower != true && pace?.fasterIsLower != false) }
    fun encode(): String = JSONObject().put("version", 1).put("power", power?.encode()).put("pace", pace?.encode()).toString()
    companion object {
        const val KEY = "performance_zone_profile"
        @JvmStatic fun decode(raw: String?): PerformanceZones? = if (raw.isNullOrBlank()) null else runCatching {
            val json = JSONObject(raw); require(json.getInt("version") == 1)
            for (key in listOf("power", "pace")) require(!json.has(key) || json.isNull(key) || json.get(key) is JSONObject)
            PerformanceZones(OutputZones.decode(json.optJSONObject("power"), false), OutputZones.decode(json.optJSONObject("pace"), true))
        }.getOrNull()
        @JvmStatic fun freeze(context: Context): String? = runCatching {
            decode(PreferenceManager.getDefaultSharedPreferences(context).getString(KEY, null))?.encode()
        }.getOrNull()
        internal fun suggest(gym: Gym): PerformanceZones? {
            val rows = ArrayList(gym.allWorkouts.list()).filter { it.status.get() == WorkoutStatus.COMPLETED }
                .sortedByDescending { it.start.get() }.take(20)
            return suggestRecordings(rows.map { it to ArrayList(gym.getSnapshots(it).list()) })
        }
        internal fun suggestRecordings(recordings: List<Pair<Workout, List<Snapshot>>>): PerformanceZones? {
            val watts = mutableListOf<Int>(); val splits = mutableListOf<Int>()
            val rows = recordings.filter { it.first.status.get() == WorkoutStatus.COMPLETED }
                .sortedByDescending { it.first.start.get() }.take(20)
            for ((workout, snapshots) in rows) {
                val statistics = WorkoutStatistics(workout, snapshots)
                if (statistics.samples.any { (it.second.duration.get() ?: 0) <= 0 }) continue
                val charts = WorkoutChartData(workout, statistics)
                for ((measure, output) in listOf(ResultMeasure.POWER to watts, ResultMeasure.SPLIT to splits)) {
                    charts.points.filter { !charts.isRest(it) && it.snapshot.difficulty.get() != Difficulty.REST && (it.snapshot.duration.get() ?: 0) > 0 }
                        .mapNotNull { point -> charts.plottedValue(point, measure)?.takeIf { it > 0 }?.let { point.elapsed to it } }
                        .groupBy { (it.first / 5).toInt() }.values.filter { it.size >= 2 }.forEach { bin ->
                            val sorted = bin.map { it.second }.sorted()
                            output.add(sorted[sorted.size / 2].roundToInt())
                        }
                }
            }
            val power = OutputZones.suggest(watts); val pace = OutputZones.suggest(splits, true)
            return if (power == null && pace == null) null else PerformanceZones(power, pace)
        }
    }
}

internal fun parseZoneSplit(value: String): Int? {
    val parts = value.trim().split(':')
    return if (parts.size == 1) parts[0].toIntOrNull()?.takeIf { it in 1..100000 }
    else if (parts.size == 2) {
        val minutes = parts[0].toIntOrNull(); val seconds = parts[1].toIntOrNull()
        if (minutes != null && minutes in 0..1666 && seconds != null && seconds in 0..59)
            (minutes * 60 + seconds).takeIf { it in 1..100000 } else null
    } else null
}
internal fun formatZoneSplit(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)
