package svenmeier.coxswain

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import svenmeier.coxswain.compose.CoxswainTheme
import svenmeier.coxswain.compose.workout.LiveRowScreen
import svenmeier.coxswain.gym.*

@RunWith(AndroidJUnit4::class)
class PaceStrokeDisplayTest {
    @get:Rule val compose=createComposeRule()

    @Test fun allPaceTilesAndCoachingHoldTheirValueBetweenStrokeEvents() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val prefs=context.getSharedPreferences("live_row_display",Context.MODE_PRIVATE)
        val before=prefs.getString("metric_bindings",null)
        prefs.edit().putString("metric_bindings","DURATION,DISTANCE,AVERAGE_SPLIT,SPLIT,SPEED,PULSE").commit()
        val gym=Gym.instance(context)
        var tick by mutableIntStateOf(0)
        fun packet(t: Int, meters: Int, count: Int, pulse: Int = 100) {
            compose.runOnUiThread {
                gym.onMeasured(Measurement().apply {
                    duration=t;distance=meters;strokes=count;this.pulse=pulse;speed=100+(t%3)*100
                });tick++
            }
            compose.waitForIdle()
        }
        fun paceTiles() = listOf("WORK AVG SPLIT ","LIVE SPLIT ","SPEED ").map { prefix ->
            compose.onNodeWithContentDescription(prefix,substring=true).fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription].joinToString()
        }
        try {
            compose.runOnUiThread {
                val p=Program.meters("Stable pace",2000,Difficulty.MEDIUM).apply { getSegment(0).setSpeed(300) }
                gym.startPlanSession(p,"stable-pace-test",1,1,0,"SPEED",167,0L,null)
            }
            compose.setContent { CoxswainTheme { LiveRowScreen(gym,tick,{},{},{}) } }
            for(t in 1..30) packet(t,t*3,t/3+1)
            compose.onNodeWithText("On target").assertIsDisplayed()
            compose.onNodeWithContentDescription("WORK AVG SPLIT 2:47").assertExists()
            val held=paceTiles()
            for(t in 31..36) {
                packet(t,90+(t-30)*10,11)
                assertEquals(held,paceTiles())
                compose.onNodeWithText("On target").assertIsDisplayed()
            }
            packet(36,150,12) // Same timestamp, new completed stroke.
            assertNotEquals(held,paceTiles())
            compose.onNodeWithContentDescription("WORK AVG SPLIT 2:00").assertExists()
            compose.onNodeWithText("On target").assertDoesNotExist()
            val next=paceTiles()
            packet(36,151,12,105) // Extra sensor/heart packet cannot change the number again.
            assertEquals(next,paceTiles())
        } finally {
            compose.runOnUiThread { gym.discard() }
            if(before==null) prefs.edit().remove("metric_bindings").commit()
            else prefs.edit().putString("metric_bindings",before).commit()
        }
    }
}
