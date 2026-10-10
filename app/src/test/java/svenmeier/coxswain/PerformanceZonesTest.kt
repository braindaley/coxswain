package svenmeier.coxswain

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.preference.PreferenceManager
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class) @Config(sdk = [34])
class PerformanceZonesTest {
    @Test fun powerAndPaceAreIndependentOfElevatedHeartRateAndUseCorrectDirection() {
        val power = OutputZones(100, 150, 200)
        val pace = OutputZones(180, 150, 120, true)
        val heart = HeartRateZones(100, 130, 160)
        assertEquals(3, heart.zone(170f)); assertEquals(0, power.zone(80f)); assertEquals(0, pace.zone(200f))
        assertEquals(1, power.zone(100f)); assertEquals(2, power.zone(150f)); assertEquals(3, power.zone(200f))
        assertEquals(1, pace.zone(180f)); assertEquals(2, pace.zone(150f)); assertEquals(3, pace.zone(120f))
        assertNull(power.zone(0f)); assertNull(pace.zone(Float.NaN))
        assertEquals(listOf(0,1,2,3), power.pieces(80f, 220f).map { it.third })
        assertEquals(listOf(3,2,1,0), pace.pieces(110f, 200f).map { it.third })
    }
    @Test fun percentileSuggestionsNeedEnoughVariationAndReversePaceRanks() {
        val values = (1..100).toList()
        assertEquals(OutputZones(25, 50, 85), OutputZones.suggest(values))
        assertEquals(OutputZones(75, 50, 15, true), OutputZones.suggest(values, true))
        assertNull(OutputZones.suggest(List(100) { 100 }))
        assertNull(OutputZones.suggest((1..10).toList()))
    }
    @Test fun profilesRejectMalformedLimitsAndPreservePartialConfiguration() {
        val onlyPower = PerformanceZones(OutputZones(100, 150, 200), null)
        assertEquals(onlyPower, PerformanceZones.decode(onlyPower.encode()))
        assertNull(PerformanceZones.decode("{\"version\":1,\"power\":false,\"pace\":{\"moderate\":180,\"vigorous\":150,\"peak\":120}}"))
        assertNull(PerformanceZones.decode("{\"version\":1}"))
        assertTrue(runCatching { OutputZones(200,150,100) }.isFailure)
        assertTrue(runCatching { OutputZones(100,150,200,true) }.isFailure)
    }
    @Test fun splitInputAcceptsMinutesOrSecondsAndRejectsInvalidSeconds() {
        assertEquals(125, parseZoneSplit("2:05")); assertEquals(125, parseZoneSplit("125"))
        assertNull(parseZoneSplit("2:65")); assertNull(parseZoneSplit("0:00"))
        assertNull(parseZoneSplit("-1:20")); assertEquals("2:05", formatZoneSplit(125))
    }
    @Test fun frozenProfileKeepsOldColorsAfterNewSettings() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val original = PerformanceZones(OutputZones(100,150,200), OutputZones(180,150,120,true))
        prefs.edit().putString(PerformanceZones.KEY, original.encode()).commit()
        val frozen = PerformanceZones.freeze(context)
        prefs.edit().putString(PerformanceZones.KEY, PerformanceZones(OutputZones(150,200,250), null).encode()).commit()
        assertEquals(original, PerformanceZones.decode(frozen))
        prefs.edit().remove(PerformanceZones.KEY).commit()
    }
    @Test fun historySuggestionsExcludePartialRowsAndRestAndNeedTimedSamples() {
        val row = svenmeier.coxswain.gym.Workout().apply {
            status.set(svenmeier.coxswain.gym.WorkoutStatus.COMPLETED); start.set(100000L)
            duration.set(300); distance.set(1200); strokes.set(120)
        }
        val samples = (1..300).map { second -> svenmeier.coxswain.gym.Snapshot().apply {
            duration.set(second); recordedAt.set(100000L + second * 1000)
            power.set(second / 5 + 100); speed.set(300 + second / 5); strokes.set(second / 2)
            distance.set(second * 4); strokeRate.set(24)
        } }
        val base = PerformanceZones.suggestRecordings(listOf(row to samples))
        assertNotNull(base)
        val partial = svenmeier.coxswain.gym.Workout().apply {
            status.set(svenmeier.coxswain.gym.WorkoutStatus.ENDED_EARLY); start.set(200000L)
            duration.set(300); distance.set(1200); strokes.set(120)
        }
        val extreme = samples.map { svenmeier.coxswain.gym.Snapshot().apply {
            duration.set(it.duration.get()); power.set(900); speed.set(900); strokeRate.set(24)
        } }
        assertEquals(base, PerformanceZones.suggestRecordings(listOf(row to samples, partial to extreme)))
        extreme.forEach { it.difficulty.set(svenmeier.coxswain.gym.Difficulty.REST) }
        assertNull(PerformanceZones.suggestRecordings(listOf(row to extreme)))
        samples.forEach { it.duration.set(0) }
        assertNull(PerformanceZones.suggestRecordings(listOf(row to samples)))
    }

}
