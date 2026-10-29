package svenmeier.coxswain

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import svenmeier.coxswain.compose.CoxswainTheme
import svenmeier.coxswain.compose.workout.LiveRowScreen
import svenmeier.coxswain.gym.*

@RunWith(AndroidJUnit4::class)
class SplitGoalScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun customizedLayoutAlwaysShowsActualSplitWithDirectionAndNeutralStrokeRate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("live_row_display",Context.MODE_PRIVATE)
        val before = prefs.getString("metric_bindings",null)
        prefs.edit().putString("metric_bindings","DURATION,DISTANCE,AVERAGE_SPLIT,SPEED,POWER,STROKE_RATE").commit()
        val gym = Gym.instance(context)
        var tick by mutableIntStateOf(0)
        try {
            compose.runOnUiThread {
                val program = Program.minutes("Week 2 · 20 min",20,Difficulty.MEDIUM)
                program.getSegment(0).setSpeed(233)
                gym.startPlanSession(program,"split-goal-test",2,1,3,"SPEED",215,0L,null)
                gym.onMeasured(Measurement().apply { duration=1;distance=4;speed=250;strokeRate=24;power=75 })
            }
            compose.setContent { CoxswainTheme { LiveRowScreen(gym,tick,{},{},{}) } }
            compose.onNodeWithText("3:20").assertIsDisplayed()
            compose.onNodeWithText("SPLIT · /500 M\n15 s faster · 3:35 target").assertIsDisplayed()
            val split = compose.onNodeWithContentDescription("SPLIT 3:20, target 3:35 /500 m")
            assertEquals(Color(0xFF126B4D).toArgb(),split.captureToImage().toPixelMap()[2,2].toArgb())
            val rate = compose.onNodeWithContentDescription("STROKE RATE 24")
            assertEquals(Color(0xFF042C3D).toArgb(),rate.captureToImage().toPixelMap()[2,2].toArgb())
            compose.runOnUiThread {
                gym.onMeasured(Measurement().apply { duration=6;distance=20;speed=220;strokeRate=36;power=75 });tick++
            }
            compose.onNodeWithText("3:47").assertIsDisplayed()
            compose.onNodeWithText("SPLIT · /500 M\n12 s slower · 3:35 target").assertIsDisplayed()
            val slower = compose.onNodeWithContentDescription("SPLIT 3:47, target 3:35 /500 m")
            assertEquals(Color(0xFF7A2836).toArgb(),slower.captureToImage().toPixelMap()[2,2].toArgb())
            val newRate = compose.onNodeWithContentDescription("STROKE RATE 36")
            assertEquals(Color(0xFF042C3D).toArgb(),newRate.captureToImage().toPixelMap()[2,2].toArgb())
        } finally {
            compose.runOnUiThread { gym.discard() }
            if(before == null) prefs.edit().remove("metric_bindings").commit() else prefs.edit().putString("metric_bindings",before).commit()
        }
    }
}
