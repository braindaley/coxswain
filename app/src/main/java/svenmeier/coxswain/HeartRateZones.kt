package svenmeier.coxswain

import android.content.Context
import androidx.preference.PreferenceManager
import org.json.JSONObject
import svenmeier.coxswain.gym.Snapshot
import kotlin.math.ceil

/** Integer BPM lower boundaries. Values exactly on a boundary enter the higher zone. */
data class HeartRateZones(val moderate: Int, val vigorous: Int, val peak: Int,
                          val resting: Int? = null, val maximum: Int? = null, val age: Int? = null) {
    init {
        require(moderate in 2..398 && vigorous > moderate && peak > vigorous && peak <= 400)
        require((resting == null) == (maximum == null))
        if (resting != null && maximum != null) {
            require(resting > 0 && maximum > resting && maximum <= 400)
            require(moderate == ceil(resting + .4 * (maximum - resting)).toInt())
            require(vigorous == ceil(resting + .6 * (maximum - resting)).toInt())
            require(peak == ceil(resting + .85 * (maximum - resting)).toInt())
        }
        if (age != null) require(age in 1..120 && maximum == 220 - age)
    }
    fun zone(bpm: Float): Int? = when {
        !bpm.isFinite() || bpm <= 0 -> null
        bpm < moderate -> 0
        bpm < vigorous -> 1
        bpm < peak -> 2
        else -> 3
    }
    fun bounds(zone: Int): String = when (zone) {
        0 -> "< $moderate BPM"
        1 -> "$moderate–${vigorous - 1} BPM"
        2 -> "$vigorous–${peak - 1} BPM"
        else -> "≥ $peak BPM"
    }
    fun encode(): String = JSONObject().put("version", 1).put("moderate", moderate)
        .put("vigorous", vigorous).put("peak", peak).put("resting", resting)
        .put("maximum", maximum).put("age", age).toString()

    /** Divide a measured line exactly at zone boundaries, without smoothing interval changes. */
    fun pieces(from: Float, to: Float): List<Triple<Float, Float, Int>> {
        if (zone(from) == null || zone(to) == null) return emptyList()
        val cuts = mutableListOf(0f, 1f)
        if (from != to) listOf(moderate, vigorous, peak).forEach {
            val fraction = (it - from) / (to - from)
            if (fraction > 0f && fraction < 1f) cuts.add(fraction)
        }
        return cuts.sorted().zipWithNext().map { (a, b) -> Triple(a, b, zone(from + (to - from) * (a + b) / 2)!!) }
    }
    companion object {
        const val KEY = "heart_rate_zone_profile"
        fun reserve(resting: Int, maximum: Int, age: Int? = null): HeartRateZones {
            require(resting > 0 && maximum > resting && maximum <= 400)
            return HeartRateZones(ceil(resting + .4 * (maximum - resting)).toInt(),
                ceil(resting + .6 * (maximum - resting)).toInt(),
                ceil(resting + .85 * (maximum - resting)).toInt(), resting, maximum, age)
        }
        @JvmStatic fun decode(raw: String?): HeartRateZones? = if (raw.isNullOrBlank()) null else runCatching {
            val json = JSONObject(raw)
            require(json.getInt("version") == 1)
            HeartRateZones(json.getInt("moderate"), json.getInt("vigorous"), json.getInt("peak"),
                if (json.has("resting") && !json.isNull("resting")) json.getInt("resting") else null,
                if (json.has("maximum") && !json.isNull("maximum")) json.getInt("maximum") else null,
                if (json.has("age") && !json.isNull("age")) json.getInt("age") else null)
        }.getOrNull()
        @JvmStatic fun freeze(context: Context): String? = runCatching {
            decode(PreferenceManager.getDefaultSharedPreferences(context).getString(KEY, null))?.encode()
        }.getOrNull()
    }
}

/** Use observed HR only, including recovery; a missing reading cannot contribute zone time. */
internal class HeartRateZoneTimes(zones: HeartRateZones, samples: List<Pair<Float, Snapshot>>) {
    val seconds = FloatArray(4)
    init {
        val ordered = samples.sortedBy { it.first }.groupBy { it.first }.values.map { it.last() }
        ordered.forEachIndexed { index, (time, sample) ->
            val value = (sample.pulse.get() ?: 0).toFloat()
            val zone = zones.zone(value) ?: return@forEachIndexed
            val previous = ordered.getOrNull(index - 1)
            val delta = (time - (previous?.first ?: 0f)).coerceAtLeast(0f)
            val previousValue = (previous?.second?.pulse?.get() ?: 0).toFloat()
            // Sparse legacy samples do not imply uninterrupted monitoring. Cap gaps at 5 seconds.
            if (previous != null && zones.zone(previousValue) != null && delta <= 5f &&
                ((sample.recordedAt.get() ?: 0L) - (previous.second.recordedAt.get() ?: 0L) <= 5000L)) {
                zones.pieces(previousValue, value).forEach { (a, b, bucket) -> seconds[bucket] += delta * (b - a) }
            } else seconds[zone] += minOf(1f, delta)
        }
    }
    val total: Float get() = seconds.sum()
    fun percent(zone: Int): Float = if (total > 0) 100f * seconds[zone] / total else 0f
}
