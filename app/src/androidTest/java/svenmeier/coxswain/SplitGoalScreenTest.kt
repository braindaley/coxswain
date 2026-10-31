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
    @Test fun missingSplitStaysMissingAndSelectedTilesStayNeutral() = checkLayout(false)
    @Test fun chosenSplitShowsActualPaceAndTargetColor() = checkLayout(true)
    @Test fun recordedRaceKeepsLargeGapAndColorsOnlyChosenRateTile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("live_row_display",Context.MODE_PRIVATE)
        val before = prefs.getString("metric_bindings",null)
        val layout = "DURATION,DISTANCE,POWER,STROKE_RATE,AVERAGE_SPLIT,PULSE"
        prefs.edit().putString("metric_bindings",layout).commit()
        val gym = Gym.instance(context)
        val program = Program("24 SPM regression").apply { getSegment(0).setDistance(5000).setStrokeRate(24) }
        var best: Workout? = null
        var tick by mutableIntStateOf(0)
        try {
            compose.runOnUiThread {
                gym.mergeProgram(program); gym.select(program)
                gym.onMeasured(Measurement().apply { duration=1500;distance=5000;strokeRate=24;speed=333 })
                best=gym.complete()
                gym.race(program,best)
                gym.onMeasured(Measurement().apply { duration=60;distance=180;strokeRate=24;speed=300 })
            }
            compose.setContent { CoxswainTheme { LiveRowScreen(gym,tick,{},{},{}) } }
            fun checkRate(value: Int, color: Color) {
                val tile=compose.onNodeWithContentDescription("STROKE RATE $value, target 24 SPM")
                tile.assertIsDisplayed()
                assertEquals(color.toArgb(),tile.captureToImage().toPixelMap()[2,2].toArgb())
                compose.onNodeWithText("Target 24 SPM",substring=true).assertIsDisplayed()
                compose.onNodeWithText("Now ",substring=true).assertDoesNotExist()
                compose.onNodeWithText("━ Saved best").assertIsDisplayed()
                compose.onNodeWithText("20 m behind").assertIsDisplayed()
            }
            checkRate(24,Color(0xFF126B4D))
            compose.runOnUiThread {
                gym.onMeasured(Measurement().apply { duration=66;distance=200;strokeRate=22;speed=300 });tick++
            }
            checkRate(22,Color(0xFF7A2836))
            assertEquals(layout,prefs.getString("metric_bindings",null))
        } finally {
            compose.runOnUiThread { gym.discard();best?.let(gym::delete);gym.delete(program) }
            if(before==null) prefs.edit().remove("metric_bindings").commit() else prefs.edit().putString("metric_bindings",before).commit()
        }
    }
    private fun checkLayout(splitChosen: Boolean) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("live_row_display",Context.MODE_PRIVATE)
        val before = prefs.getString("metric_bindings",null)
        val layout = "DURATION,DISTANCE,AVERAGE_SPLIT,${if(splitChosen) "SPLIT" else "SPEED"},POWER,STROKE_RATE"
        prefs.edit().putString("metric_bindings",layout).commit()
        val gym=Gym.instance(context)
        var tick by mutableIntStateOf(0)
        try {
            compose.runOnUiThread {
                val program=Program.minutes("20 min",20,Difficulty.MEDIUM)
                program.getSegment(0).setSpeed(233)
                gym.startPlanSession(program,"layout-test",2,1,3,"SPEED",215,0L,null)
                gym.onMeasured(Measurement().apply { duration=1;distance=4;speed=250;strokeRate=24;power=75 })
            }
            compose.setContent { CoxswainTheme { LiveRowScreen(gym,tick,{},{},{}) } }
            compose.onNodeWithText("Hold 3:35 /500 m").assertIsDisplayed()
            compose.onNodeWithText("15 s faster").assertIsDisplayed()
            compose.onNodeWithText("Now ",substring=true).assertDoesNotExist()
            if(splitChosen) {
                val tile=compose.onNodeWithContentDescription("LIVE SPLIT 3:20, target 3:35 /500 m")
                tile.assertIsDisplayed()
                assertEquals(Color(0xFF126B4D).toArgb(),tile.captureToImage().toPixelMap()[2,2].toArgb())
            } else {
                compose.onNodeWithContentDescription("LIVE SPLIT",substring=true).assertDoesNotExist()
                compose.onNodeWithContentDescription("SPEED",substring=true).assertIsDisplayed()
            }
            val rate=compose.onNodeWithContentDescription("STROKE RATE 24")
            assertEquals(Color(0xFF042C3D).toArgb(),rate.captureToImage().toPixelMap()[2,2].toArgb())
            compose.runOnUiThread {
                gym.onMeasured(Measurement().apply { duration=6;distance=20;speed=220;strokeRate=36;power=75 });tick++
            }
            compose.onNodeWithText("12 s slower").assertIsDisplayed()
            if(splitChosen) {
                val tile=compose.onNodeWithContentDescription("LIVE SPLIT 3:47, target 3:35 /500 m")
                assertEquals(Color(0xFF7A2836).toArgb(),tile.captureToImage().toPixelMap()[2,2].toArgb())
            }
            assertEquals(layout,prefs.getString("metric_bindings",null))
        } finally {
            compose.runOnUiThread { gym.discard() }
            if(before==null) prefs.edit().remove("metric_bindings").commit() else prefs.edit().putString("metric_bindings",before).commit()
        }
    }
}
