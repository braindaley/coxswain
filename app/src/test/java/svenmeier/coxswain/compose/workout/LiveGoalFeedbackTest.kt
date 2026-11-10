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
    @Test fun intervalRestartUsesDistanceInsteadOfRecoveryDilutedMonitorSpeed() {
        // Recorded second effort from the connected rower's 3 x 1,000m workout.
        val smoother = LiveGoalSmoother(Measurement().apply { duration=468;distance=1020 })
        fun read(t: Int, meters: Int, monitorSpeed: Int) = smoother.update(Measurement().apply {
            duration=t;distance=meters;speed=monitorSpeed
        })
        read(469,1021,0)
        val rawSpeed14 = read(470,1024,14)
        assertEquals(200,rawSpeed14.speed) // 4 metres / 2 seconds, rather than a 59-minute split.
        read(471,1027,14)
        read(472,1032,35)
        val value = read(473,1035,44)
        assertEquals(350,value.speed) // (1035 - 1021) / (473 - 469).
        repeat(10) { assertEquals(350,read(473,1035,44).speed) }
        assertEquals(0,read(480,1035,44).speed) // No samples spanning the gap yet.
        assertEquals(0,read(481,1035,44).speed) // Stationary, despite a stale positive speed.
    }

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

    @Test fun startupAveragesAvailableReadingsForEachMetricImmediately() {
        val smoother = LiveGoalSmoother()
        fun read(t: Int, speed: Int = 0, rate: Int = 0, power: Int = 0) =
            smoother.update(Measurement().apply {
                duration = t; this.speed = speed; strokeRate = rate; this.power = power
            })
        assertEquals(0, read(0).speed)
        read(1)
        val first = read(2, speed = 250)
        assertEquals(250, first.speed)
        assertEquals(0, first.strokeRate)
        val second = read(3, speed = 270, rate = 24)
        assertEquals(260, second.speed)
        assertEquals(24, second.strokeRate)
        val third = read(4, speed = 290, rate = 26, power = 120)
        assertEquals(270, third.speed)
        assertEquals(25, third.strokeRate)
        assertEquals(120, third.power)
        assertEquals(0, read(5).speed)
        assertEquals(73, read(6, speed = 0, rate = 26, power = 100).power)
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
