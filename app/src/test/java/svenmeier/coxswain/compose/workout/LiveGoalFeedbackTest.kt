package svenmeier.coxswain.compose.workout

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import svenmeier.coxswain.gym.*
import svenmeier.coxswain.view.ValueBinding

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiveGoalFeedbackTest {
    @Test fun splitTargetsUsePaceForBottomFeedback() {
        val work = Segment(Difficulty.MEDIUM).setSpeed(233)
        assertEquals(ValueBinding.SPLIT,goalBinding(work,"SPEED"))
        assertEquals(ValueBinding.SPLIT,goalBinding(work))
        work.strokeRate.set(24)
        assertEquals(ValueBinding.SPLIT,goalBinding(work,"SPEED"))
        assertEquals(ValueBinding.STROKE_RATE,goalBinding(work,"RATE_CAP"))
        assertNull(goalBinding(Segment(Difficulty.REST).setSpeed(233),"SPEED"))
    }
    @Test fun splitFeedbackUsesExactPrescribedSecondsAndExplicitDirection() {
        val work = Segment(Difficulty.MEDIUM).setSpeed(233) // Encoded m/s rounds away from 3:35.
        fun feedback(speed: Int) = goalDisplay(ValueBinding.SPLIT,work,Measurement().apply { this.speed=speed },false,215)!!
        assertEquals("3:35 /500 m",feedback(233).target)
        assertEquals("On target",feedback(233).feedback)
        assertEquals("15 s faster",feedback(250).feedback)
        assertEquals(1,feedback(250).state)
        assertEquals("12 s slower",feedback(220).feedback)
        assertEquals(-1,feedback(220).state)
        assertNull(goalDisplay(ValueBinding.STROKE_RATE,work,Measurement().apply { strokeRate=24 }))
        assertNull(goalDisplay(ValueBinding.SPLIT,work,Measurement(),false,215))
    }
    @Test fun directionIsExpressedInTheBottomCoachingReadout() {
        val work=Segment(Difficulty.MEDIUM).setSpeed(233)
        val m=Measurement().apply { speed=250 }
        val goal=goalDisplay(ValueBinding.SPLIT,work,m,false,215)!!
        assertEquals("Now 3:20 · 15 s faster",liveTargetFeedback(ValueBinding.SPLIT,work,m,goal,false))
        work.setStrokeRate(24)
        m.strokeRate=26
        val cap=goalDisplay(ValueBinding.STROKE_RATE,work,m,true)!!
        assertEquals("Now 26 SPM · 2 SPM above cap",liveTargetFeedback(ValueBinding.STROKE_RATE,work,m,cap,true))
    }

    @Test fun fiveSecondFeedbackAverageDoesNotRewriteOrCountRepeatedHeartPackets() {
        val smoother = LiveGoalSmoother()
        fun read(t:Int,s:Int,r:Int=24) = smoother.update(Measurement().apply { duration=t;speed=s;strokeRate=r;power=100 })
        for(t in 1..4) read(t,233)
        val raw = Measurement().apply { duration=5;speed=333;strokeRate=29;power=200 }
        val value = smoother.update(raw)
        assertEquals(253,value.speed)
        assertEquals(25,value.strokeRate)
        assertEquals(333,raw.speed)
        repeat(10) { assertEquals(253,read(5,333,29).speed) }
        assertEquals(333,read(10,333).speed) // Old values expire.
        assertEquals(0,read(11,0,0).speed)
        assertEquals(200,read(1,200).speed) // Counter reset/new row.
    }
}
