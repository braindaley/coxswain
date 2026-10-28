package svenmeier.coxswain

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import svenmeier.coxswain.compose.workout.SideProgressRail
import svenmeier.coxswain.gym.*

@RunWith(AndroidJUnit4::class)
class IntervalRaceRailTest {
    @get:Rule val compose = createComposeRule()
    @Test fun regularRaceRailMovesThroughRecoveryBands() = checkRail(false)
    @Test fun peteRaceRailMovesThroughRecoveryBands() = checkRail(true)

    private fun checkRail(plan: Boolean) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val gym = Gym.instance(context)
        val p = Program("Rail UI test").apply {
            getSegment(0).setDistance(500)
            addSegment(Segment(Difficulty.REST).setDuration(120))
            addSegment(Segment(Difficulty.HARD).setDistance(1000))
        }
        var tick by mutableIntStateOf(0)
        var best: Workout? = null
        fun measure(t: Int, m: Int) { gym.onMeasured(Measurement().apply { duration = t; distance = m }) }
        try {
            compose.runOnUiThread {
                gym.mergeProgram(p)
                gym.select(p)
                measure(120,500); measure(240,520); measure(540,1520)
                best = gym.complete()
                if (plan) gym.startPlanSession(p,"rail-test",1,1,1,"ROW_AGAINST",0,0L,best)
                else gym.race(p,best)
                measure(150,500); measure(180,510)
            }
            compose.setContent { SideProgressRail(gym,tick,Modifier.size(40.dp,600.dp)) }
            val rail = compose.onNodeWithContentDescription("Live row 31 percent, saved row 37 percent")
            rail.assertExists()
            val pixels = rail.captureToImage().toPixelMap()
            val liveX = (pixels.width * 6.5f / 40).toInt()
            val bestX = (pixels.width * 22.5f / 40).toInt()
            assertEquals(Color.White.toArgb(), pixels[liveX,(pixels.height*(1-155f/495)).toInt()].toArgb())
            assertEquals(Color(0xFFFFCD72).toArgb(), pixels[bestX,(pixels.height*(1-185f/495)).toInt()].toArgb())
            compose.runOnUiThread { measure(210,515);tick++ }
            compose.onNodeWithContentDescription("Live row 37 percent, saved row 43 percent").assertExists()
            compose.runOnUiThread { gym.pause();tick++ }
            compose.onNodeWithContentDescription("Live row 37 percent, saved row 43 percent").assertExists()
        } finally {
            compose.runOnUiThread { gym.discard();best?.let(gym::delete);gym.delete(p) }
        }
    }
}
