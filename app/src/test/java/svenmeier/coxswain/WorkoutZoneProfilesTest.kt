package svenmeier.coxswain

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import svenmeier.coxswain.gym.Workout

@RunWith(RobolectricTestRunner::class) @Config(sdk = [34])
class WorkoutZoneProfilesTest {
    private val heart = HeartRateZones(100,130,160)
    private val output = PerformanceZones(OutputZones(100,150,200),OutputZones(180,150,120,true))
    @Test fun oldRowsUseCurrentSettingsWithoutChangingTheirRecordedMetadata() {
        val row = Workout()
        val resolved = WorkoutZoneProfiles.resolve(row, heart, output)
        assertEquals(heart,resolved.heart); assertEquals(output,resolved.output)
        assertTrue(resolved.currentHeart); assertTrue(resolved.currentOutput)
        assertEquals(3,resolved.heart!!.zone(170f)); assertEquals(2,resolved.output!!.power!!.zone(175f))
        assertNull(row.heartRateZones.get()); assertNull(row.performanceZones.get())
    }
    @Test fun recordedProfilesTakePrecedenceAndDeliberatelyDisabledMetricStaysDisabled() {
        val partial = PerformanceZones(OutputZones(120,170,220),null)
        val recordedHeart = HeartRateZones(110,140,170)
        val row = Workout().apply { heartRateZones.set(recordedHeart.encode()); performanceZones.set(partial.encode()) }
        val resolved = WorkoutZoneProfiles.resolve(row,heart,output)
        assertEquals(recordedHeart,resolved.heart); assertEquals(partial,resolved.output)
        assertFalse(resolved.currentHeart); assertFalse(resolved.currentOutput)
        assertNull(resolved.output!!.pace)
    }
    @Test fun absentSettingsDoNotInventThresholdsAndCorruptRecordedProfilesAreNotReplaced() {
        val row = Workout()
        assertNull(WorkoutZoneProfiles.resolve(row,null,null).output)
        row.heartRateZones.set("{}"); row.performanceZones.set("{}");
        val resolved = WorkoutZoneProfiles.resolve(row,heart,output)
        assertNull(resolved.heart); assertNull(resolved.output)
        assertFalse(resolved.currentHeart); assertFalse(resolved.currentOutput)
    }
}
