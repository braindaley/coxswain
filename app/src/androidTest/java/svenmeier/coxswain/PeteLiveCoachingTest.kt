package svenmeier.coxswain

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import svenmeier.coxswain.compose.CoxswainTheme
import svenmeier.coxswain.compose.workout.LiveRowStatus
import svenmeier.coxswain.compose.workout.restDisplay
import svenmeier.coxswain.gym.*

@RunWith(AndroidJUnit4::class)
class PeteLiveCoachingTest {
    @get:Rule val compose = createComposeRule()
    @Test fun liveCoachingNamesMetricAndUpdatesWhenLimitedPaceTargetEnds() {
        val gym = Gym.instance(ApplicationProvider.getApplicationContext<Context>())
        val p = Program("Coaching test").apply {
            getSegment(0).setDistance(500).setSpeed(345)
            addSegment(Segment(Difficulty.REST).setDuration(60))
            addSegment(Segment(Difficulty.MEDIUM).setDistance(500))
        }
        var tick by mutableIntStateOf(0)
        try {
            compose.runOnUiThread {
                gym.startPlanSession(p,"coaching-test",1,1,0,"RATE_CAP",24,0L,null)
            }
            compose.setContent { CoxswainTheme {
                @Suppress("UNUSED_VARIABLE") val refresh = tick
                val progress = gym.progress
                val rest = if(progress?.segment?.difficulty?.get() == Difficulty.REST)
                    restDisplay(p.getSegments(),progress.segment,progress.completion()) else null
                LiveRowStatus(gym,rest,tick)
            } }
            compose.onNodeWithText("Stay at or below 24 SPM").assertIsDisplayed()
            compose.runOnUiThread {
                gym.startPlanSession(p,"coaching-test",1,1,0,"SPEED_PHASE",145,0L,null)
                gym.onMeasured(Measurement().apply { duration=1;distance=1 });tick++
            }
            compose.onNodeWithText("Hold 2:25 /500 m").assertIsDisplayed()
            compose.runOnUiThread {
                gym.onMeasured(Measurement().apply { duration=145;distance=500 });tick++
            }
            compose.onNodeWithText("Hold 2:25 /500 m").assertDoesNotExist()
            compose.runOnUiThread {
                gym.onMeasured(Measurement().apply { duration=205;distance=500 });tick++
            }
            compose.onNodeWithText("Pace target finished · Follow the coaching note").assertIsDisplayed()
        } finally { compose.runOnUiThread { gym.discard() } }
    }
}
