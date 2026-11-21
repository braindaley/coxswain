package svenmeier.coxswain.compose.workout

import org.junit.Assert.*
import org.junit.Test
import svenmeier.coxswain.gym.Measurement

class StrokePaceValueTest {
    private fun reading(t: Int, meters: Int, count: Int) = Measurement().apply {
        duration=t;distance=meters;strokes=count
    }

    @Test fun changingDistanceTimeAndRepeatedPacketsCannotChangeTheValueWithinAStroke() {
        val display=StrokePaceValue()
        assertEquals(160,display.update(reading(10,30,4),160))
        assertEquals(160,display.update(reading(11,34,4),158))
        assertEquals(160,display.update(reading(12,37,4),157))
        repeat(10) { assertEquals(160,display.update(reading(12,38,4),156)) }
        assertEquals(156,display.update(reading(12,38,5),156))
        assertEquals(156,display.update(reading(13,41,5),155))
    }

    @Test fun aTimedAveragePublicationMidStrokeWaitsForTheNextStroke() {
        val average=LivePaceAverage()
        val display=StrokePaceValue()
        for (t in 0..30) {
            val raw=reading(t,t*3,t/3+1)
            display.update(raw,average.update(raw))
        }
        val raw=reading(33,102,12)
        val before=display.update(raw,average.update(raw))
        // The averaging clock next publishes at 38, in the middle of stroke 12.
        var candidate=0
        for (t in 34..38) {
            val packet=reading(t,102+(t-33)*5,12)
            candidate=average.update(packet)
            assertEquals(before,display.update(packet,candidate))
        }
        assertNotEquals(before,candidate)
        assertEquals(candidate,display.update(reading(38,127,13),candidate))
    }

    @Test fun newRowAndCounterlessDevicesDoNotKeepAnOldDisplayValue() {
        val display=StrokePaceValue()
        display.update(reading(100,300,40),160)
        assertEquals(0,display.update(reading(0,0,0),0))
        assertEquals(0,display.update(reading(4,12,0),170))
        assertEquals(170,display.update(reading(5,15,0),170))
    }
}
