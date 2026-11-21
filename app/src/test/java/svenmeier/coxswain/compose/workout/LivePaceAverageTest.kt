package svenmeier.coxswain.compose.workout

import org.junit.Assert.*
import org.junit.Test
import svenmeier.coxswain.gym.Measurement

class LivePaceAverageTest {
    private fun read(t: Int, distance: Int, strokes: Int = t, monitorSpeed: Int = 300) =
        Measurement().apply { duration=t;this.distance=distance;this.strokes=strokes;speed=monitorSpeed }

    @Test fun rollingAverageRejectsDriveRecoverySwingsAndDoesNotFollowStrokeEvents() {
        val average = LivePaceAverage()
        val readings = (0..160).map { t ->
            // Distance arrives in bursts, while monitor speed swings within each stroke.
            val meters = (t/3)*9 + when (t%3) { 1 -> 8; 2 -> 9; else -> 0 }
            average.update(read(t,meters,t/2,if (t%3==1) 330 else 200))
        }
        assertTrue(readings.take(8).all { it == 0 })
        assertTrue(readings.drop(100).all { it in 297..303 })
        // Stroke events cannot refresh feedback between the current five-second publications.
        for (t in 8..12) assertEquals(readings[8],readings[t])
    }

    @Test fun averageStillRespondsToSustainedPaceChangesAndStops() {
        val average = LivePaceAverage()
        for (t in 0..30) average.update(read(t,t*3))
        var value = 0
        for (t in 31..50) value = average.update(read(t,90+(t-30)*4))
        assertTrue(value in 325..360)
        for (t in 51..70) value = average.update(read(t,90+(t-30)*4))
        assertTrue(value in 370..400)
        for (t in 71..110) value = average.update(read(t,90+(t-30)*4))
        assertTrue(value in 395..400)
        for (t in 111..140) value = average.update(read(t,410))
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
