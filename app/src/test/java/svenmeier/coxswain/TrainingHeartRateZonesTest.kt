package svenmeier.coxswain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class TrainingHeartRateZonesTest {
    @Test fun exactBoundariesEnterTheHigherTrainingZone() {
        val zones=HeartRateZones.training(180,60)
        assertEquals(listOf(108,126,144,162),zones.thresholds)
        assertEquals(5,zones.count)
        assertEquals(listOf(0,1,2,3,4),listOf(100f,108f,126f,144f,162f).map(zones::zone))
        assertTrue(zones.belowTraining(89));assertFalse(zones.belowTraining(90))
        assertNull(zones.zone(0f));assertNull(zones.zone(Float.NaN))
        assertEquals("108–125 BPM",zones.bounds(1))
        assertEquals(zones,HeartRateZones.decode(zones.encode()))
    }
    @Test fun migrationUsesMaximumRatherThanReserveAndPreservesLegacyRecordings() {
        val old=HeartRateZones.reserve(60,180)
        val saved=old.encode()
        val migrated=old.asTraining()!!
        assertEquals(108,migrated.moderate);assertEquals(126,migrated.vigorous)
        assertEquals(4,HeartRateZones.decode(saved)!!.count)
        assertEquals(HeartRateZones.training(180,80).thresholds,migrated.thresholds)
        assertNull(HeartRateZones(100,130,160).asTraining())
    }
    @Test fun customFiveZonesSplitSegmentsAndRetainTotalCoverage() {
        val zones=HeartRateZones(100,120,140,zone5=160)
        assertEquals(5,zones.pieces(80f,180f).size)
        assertEquals(1f,zones.pieces(80f,180f).sumOf { (it.second-it.first).toDouble() }.toFloat(),.001f)
        assertEquals(zones,HeartRateZones.decode(zones.encode()))
        assertTrue(runCatching { HeartRateZones(100,120,140,zone5=130) }.isFailure)
    }
    @Test fun tileTextHasAccessibleContrastAndDistinctZoneBackgrounds() {
        assertEquals(5,trainingZoneTileColors.distinct().size)
        trainingZoneTileColors.forEach { color ->
            assertTrue("White text contrast on $color",(Color.White.luminance()+.05f)/(color.luminance()+.05f)>=4.5f)
        }
    }
}
