package svenmeier.coxswain.compose.workout

import org.junit.Assert.*
import org.junit.Test
import svenmeier.coxswain.gym.Measurement

class LivePaceAverageTest {
    private fun read(t: Int, distance: Int, strokes: Int = t, monitorSpeed: Int = 300) =
        Measurement().apply { duration=t;this.distance=distance;this.strokes=strokes;speed=monitorSpeed }

    @Test fun rollingAverageRejectsDriveRecoverySwingsAndDoesNotFollowStrokeEvents() {
        val average = LivePaceAverage()
        val readings = (0..60).map { t ->
            // Distance arrives in bursts, while monitor speed swings within each stroke.
            val meters = (t/3)*9 + when (t%3) { 1 -> 8; 2 -> 9; else -> 0 }
            average.update(read(t,meters,t/2,if (t%3==1) 330 else 200))
        }
        assertTrue(readings.take(5).all { it == 0 })
        assertTrue(readings.drop(40).all { it in 299..301 })
        // Even changing stroke counts and distance cannot refresh the message within 3 seconds.
        assertEquals(readings[5],readings[6]); assertEquals(readings[5],readings[7])
    }

    @Test fun averageStillRespondsToSustainedPaceChangesAndStops() {
        val average = LivePaceAverage()
        for (t in 0..30) average.update(read(t,t*3))
        var value = 0
        for (t in 31..50) value = average.update(read(t,90+(t-30)*4))
        assertTrue(value in 360..400)
        for (t in 51..70) value = average.update(read(t,90+(t-30)*4))
        assertTrue(value in 395..400)
        for (t in 71..90) value = average.update(read(t,250))
        assertEquals(0,value)
    }

    @Test fun resetGapAndRepeatedPacketsCannotReuseOldPace() {
        val average = LivePaceAverage()
        for (t in 0..30) average.update(read(t,t*3))
        repeat(10) { assertEquals(300,average.update(read(30,90))) }
        assertEquals(0,average.update(read(0,0)))
        assertEquals(0,average.update(read(10,30))) // Unobserved gap starts a fresh window.
    }
}
