package svenmeier.coxswain

import android.content.Context
import androidx.preference.PreferenceManager
import org.json.JSONObject
import svenmeier.coxswain.gym.Snapshot
import kotlin.math.ceil

/** Integer BPM lower boundaries. Values exactly on a boundary enter the higher zone. */
data class HeartRateZones @JvmOverloads constructor(val moderate: Int, val vigorous: Int, val peak: Int,
                          val resting: Int? = null, val maximum: Int? = null, val age: Int? = null, val zone5: Int? = null) {
    init {
        require(moderate in 2..398 && vigorous > moderate && peak > vigorous && peak <= 400)
        require(zone5 == null || zone5 > peak && zone5 <= 400)
        if (zone5 == null) require((resting == null) == (maximum == null))
        if (resting != null && maximum != null) {
            require(resting > 0 && maximum > resting && maximum <= 400)
            if (zone5 == null) {
            require(moderate == ceil(resting + .4 * (maximum - resting)).toInt())
            require(vigorous == ceil(resting + .6 * (maximum - resting)).toInt())
            require(peak == ceil(resting + .85 * (maximum - resting)).toInt())
            }
        }
        if (zone5 != null) {
            require(resting == null || maximum != null)
            maximum?.let { max ->
                require(max in 10..400)
                require(listOf(moderate,vigorous,peak,zone5) == listOf(.6,.7,.8,.9).map { ceil(max*it).toInt() })
            }
        }
        if (age != null) require(age in 1..120 && maximum == 220 - age)
    }
    val count: Int get() = if (zone5 == null) 4 else 5
    val thresholds: List<Int> get() = listOfNotNull(moderate, vigorous, peak, zone5)
    fun zone(bpm: Float): Int? = if (!bpm.isFinite() || bpm <= 0) null
        else thresholds.count { bpm >= it }
    fun bounds(zone: Int): String = when (zone) {
        0 -> "< $moderate BPM"
        count-1 -> "≥ ${thresholds.last()} BPM"
        else -> "${thresholds[zone-1]}–${thresholds[zone]-1} BPM"
    }
    /** Legacy recorded profiles remain intact; current maximum-based settings can upgrade. */
    fun asTraining(): HeartRateZones? = if (zone5 != null) this
        else maximum?.let { training(it, resting, age) }
    fun belowTraining(bpm: Int): Boolean = zone5 != null && maximum != null && bpm < ceil(maximum*.5).toInt()
    fun encode(): String = JSONObject().put("version", if (zone5 == null) 1 else 2).put("zone5", zone5).put("moderate", moderate)
        .put("vigorous", vigorous).put("peak", peak).put("resting", resting)
        .put("maximum", maximum).put("age", age).toString()

    /** Divide a measured line exactly at zone boundaries, without smoothing interval changes. */
    fun pieces(from: Float, to: Float): List<Triple<Float, Float, Int>> {
        if (zone(from) == null || zone(to) == null) return emptyList()
        val cuts = mutableListOf(0f, 1f)
        if (from != to) thresholds.forEach {
            val fraction = (it - from) / (to - from)
            if (fraction > 0f && fraction < 1f) cuts.add(fraction)
        }
        return cuts.sorted().zipWithNext().map { (a, b) -> Triple(a, b, zone(from + (to - from) * (a + b) / 2)!!) }
    }
    companion object {
        fun training(maximum: Int, resting: Int? = null, age: Int? = null): HeartRateZones {
            require(maximum in 10..400)
            return HeartRateZones(ceil(maximum*.6).toInt(),ceil(maximum*.7).toInt(),
                ceil(maximum*.8).toInt(),resting,maximum,age,ceil(maximum*.9).toInt())
        }
        const val KEY = "heart_rate_zone_profile"
        fun reserve(resting: Int, maximum: Int, age: Int? = null): HeartRateZones {
            require(resting > 0 && maximum > resting && maximum <= 400)
            return HeartRateZones(ceil(resting + .4 * (maximum - resting)).toInt(),
                ceil(resting + .6 * (maximum - resting)).toInt(),
                ceil(resting + .85 * (maximum - resting)).toInt(), resting, maximum, age)
        }
        @JvmStatic fun decode(raw: String?): HeartRateZones? = if (raw.isNullOrBlank()) null else runCatching {
            val json = JSONObject(raw)
            val version = json.getInt("version"); require(version in 1..2)
            HeartRateZones(json.getInt("moderate"), json.getInt("vigorous"), json.getInt("peak"),
                if (json.has("resting") && !json.isNull("resting")) json.getInt("resting") else null,
                if (json.has("maximum") && !json.isNull("maximum")) json.getInt("maximum") else null,
                if (json.has("age") && !json.isNull("age")) json.getInt("age") else null,
                if (version == 2) json.getInt("zone5") else null)
        }.getOrNull()
        @JvmStatic fun freeze(context: Context): String? = runCatching {
            decode(PreferenceManager.getDefaultSharedPreferences(context).getString(KEY, null))?.let { (it.asTraining() ?: it).encode() }
        }.getOrNull()
    }
}

/** Use observed HR only, including recovery; a missing reading cannot contribute zone time. */
internal class HeartRateZoneTimes(zones: HeartRateZones, samples: List<Pair<Float, Snapshot>>) {
    val seconds = FloatArray(zones.count)
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
