package svenmeier.coxswain

import androidx.preference.PreferenceManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import svenmeier.coxswain.gym.Snapshot
import svenmeier.coxswain.gym.Difficulty

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HeartRateZonesTest {
    @Test fun reserveUsesPersonalRestingMaximumAndCeilingAtBoundaries() {
        val zones = HeartRateZones.reserve(60, 175)
        assertEquals(106, zones.moderate); assertEquals(129, zones.vigorous); assertEquals(158, zones.peak)
        assertEquals(0, zones.zone(105f)); assertEquals(1, zones.zone(106f))
        assertEquals(2, zones.zone(129f)); assertEquals(3, zones.zone(158f))
        assertNull(zones.zone(0f)); assertNull(zones.zone(Float.NaN))
        assertEquals(zones, HeartRateZones.decode(zones.encode()))
        val estimated = HeartRateZones.reserve(60, 180, 40)
        assertEquals(40, HeartRateZones.decode(estimated.encode())!!.age)
    }
    @Test fun rejectsInvalidOrInconsistentProfiles() {
        listOf("{}", "null", "{\"version\":2,\"moderate\":100,\"vigorous\":130,\"peak\":160}",
            "{\"version\":1,\"moderate\":130,\"vigorous\":100,\"peak\":160}").forEach { assertNull(HeartRateZones.decode(it)) }
        assertTrue(runCatching { HeartRateZones.reserve(180, 175) }.isFailure)
        assertTrue(runCatching { HeartRateZones.reserve(60, 180, 50) }.isFailure)
        assertTrue(runCatching { HeartRateZones(100, 100, 160) }.isFailure)
    }
    @Test fun savedRecordingThresholdsAreIndependentOfLaterSettings() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(HeartRateZones.KEY, HeartRateZones.reserve(60, 175).encode()).commit()
        val frozen = HeartRateZones.freeze(context)
        prefs.edit().putString(HeartRateZones.KEY, HeartRateZones(120, 150, 180).encode()).commit()
        assertEquals(106, HeartRateZones.decode(frozen)!!.moderate)
        assertEquals(120, HeartRateZones.decode(HeartRateZones.freeze(context))!!.moderate)
        prefs.edit().remove(HeartRateZones.KEY).commit()
        assertNull(HeartRateZones.freeze(context)); assertEquals(106, HeartRateZones.decode(frozen)!!.moderate)
    }
    private fun sample(time: Int, pulse: Int, rest: Boolean = false) = time.toFloat() to Snapshot().apply {
        this.pulse.set(pulse); recordedAt.set(100000L + time * 1000)
        difficulty.set(if (rest) Difficulty.REST else Difficulty.HARD)
    }
    @Test fun missingAndSparseReadingsAreNotLightOrBackfilledAndRecoveryCounts() {
        val zones = HeartRateZones(100, 130, 160)
        val times = HeartRateZoneTimes(zones, listOf(sample(1, 90), sample(2, 90), sample(3, 0),
            sample(10, 140, true), sample(11, 140, true), sample(30, 170)))
        assertEquals(2f, times.seconds[0], .001f)
        assertEquals(2f, times.seconds[2], .001f)
        assertEquals(1f, times.seconds[3], .001f)
        assertEquals(5f, times.total, .001f)
        assertEquals(40f, times.percent(2), .001f)
    }
    @Test fun crossingsSplitBothLineAndTimeAtEachThreshold() {
        val zones = HeartRateZones(100, 130, 160)
        val pieces = zones.pieces(90f, 170f)
        assertEquals(listOf(0, 1, 2, 3), pieces.map { it.third })
        val times = HeartRateZoneTimes(zones, listOf(sample(0, 90), sample(4, 170)))
        assertEquals(.5f, times.seconds[0], .001f); assertEquals(1.5f, times.seconds[1], .001f)
        assertEquals(1.5f, times.seconds[2], .001f); assertEquals(.5f, times.seconds[3], .001f)
        assertEquals(listOf(3, 2, 1, 0), zones.pieces(170f, 90f).map { it.third })
    }
    @Test fun duplicateTimestampsUseLatestReadingAndPauseTimeIsExcluded() {
        val zones = HeartRateZones(100, 130, 160)
        val paused = sample(2, 170).also { it.second.recordedAt.set(200000L) }
        val times = HeartRateZoneTimes(zones, listOf(sample(1, 90), sample(1, 140), paused))
        assertEquals(0f, times.seconds[0], .001f); assertEquals(1f, times.seconds[2], .001f)
        assertEquals(1f, times.seconds[3], .001f); assertEquals(2f, times.total, .001f)
    }
}
