package svenmeier.coxswain.compose.workout

import org.junit.Assert.assertEquals
import org.junit.Test
import svenmeier.coxswain.gym.Difficulty
import svenmeier.coxswain.gym.Segment

class RestCountdownTest {
    @Test fun remainingSecondsDoNotLoseASecondToFloatingPointRounding() {
        val rest = Segment(Difficulty.REST).setDuration(180)
        for (elapsed in 0..180) {
            val seconds = 180-elapsed
            assertEquals("%d:%02d".format(seconds/60,seconds%60),
                restDisplay(listOf(rest),rest,elapsed/180f).remaining)
        }
    }
    @Test fun irregularPacketsDoNotRestartSecondsOrRewindCountdown() {
        val clock = RestCountdown()
        assertEquals(180,clock.update(180,false,0))
        assertEquals(179,clock.update(180,false,1000))
        assertEquals(179,clock.update(179,false,1300))
        assertEquals(178,clock.update(179,false,2000))
        assertEquals(178,clock.update(180,false,2100)) // Stale report cannot add time.
        assertEquals(177,clock.update(178,false,3000))
        assertEquals(0,clock.update(1,false,180000))
    }
    @Test fun pauseFreezesDisplayAndResumeDoesNotConsumePausedTime() {
        val clock = RestCountdown()
        clock.update(180,false,0)
        assertEquals(170,clock.update(170,true,10000))
        assertEquals(170,clock.update(170,true,73000))
        assertEquals(170,clock.update(170,false,74000))
        assertEquals(169,clock.update(169,false,75000))
    }
}
