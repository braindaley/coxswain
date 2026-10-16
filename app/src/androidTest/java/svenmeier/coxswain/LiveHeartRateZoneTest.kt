package svenmeier.coxswain

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asAndroidBitmap
import org.junit.Assert.assertEquals
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import svenmeier.coxswain.compose.CoxswainTheme
import svenmeier.coxswain.compose.workout.MetricCell
import svenmeier.coxswain.gym.*
import svenmeier.coxswain.view.ValueBinding

@RunWith(AndroidJUnit4::class)
class LiveHeartRateZoneTest {
    @get:Rule val compose=createComposeRule()
    @Test fun liveZoneLabelUpdatesAndMissingPulseHasNoZone() = checkTile(false)
    @Test fun darkTileKeepsActualBpmEvenWhenAGoalVarianceWasProvided() = checkTile(true)
    private fun checkTile(dark:Boolean) {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs=PreferenceManager.getDefaultSharedPreferences(context)
        val before=prefs.getString(HeartRateZones.KEY,null)
        prefs.edit().putString(HeartRateZones.KEY,HeartRateZones.training(180,60).encode()).commit()
        val gym=Gym.instance(context)
        var tick by mutableIntStateOf(0)
        try {
            compose.runOnUiThread {
                gym.select(Program.minutes("HR tile test",30,Difficulty.EASY))
                gym.onMeasured(Measurement().apply { pulse=120 })
            }
            compose.setContent {
                @Suppress("UNUSED_VARIABLE") val refresh=tick
                CoxswainTheme(darkTheme=dark) { Surface {
                    MetricCell(ValueBinding.PULSE,gym,onClick={},displayValue="-5",goalState=-1,modifier=Modifier.size(190.dp))
                } }
            }
            compose.onNodeWithText("120").assertIsDisplayed()
            compose.onNodeWithText("-5").assertDoesNotExist()
            compose.onNodeWithText("Zone 2 · BPM").assertIsDisplayed()
            val tile=compose.onNodeWithContentDescription("Zone 2",substring=true).captureToImage()
            assertEquals(trainingZoneTileColors[1].toArgb(),tile.toPixelMap()[2,2].toArgb())
            FileOutputStream(File(context.cacheDir,if(dark) "live-zone-dark.png" else "live-zone-light.png")).use {
                tile.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
            }
            compose.runOnUiThread { gym.onMeasured(Measurement().apply { pulse=150 });tick++ }
            compose.onNodeWithText("Zone 4 · BPM").assertIsDisplayed()
            compose.runOnUiThread { gym.onMeasured(Measurement().apply { pulse=165 });tick++ }
            compose.onNodeWithText("Zone 5 · BPM").assertIsDisplayed()
            compose.runOnUiThread { gym.onMeasured(Measurement().apply { pulse=80 });tick++ }
            compose.onNodeWithText("Below Zone 1 · BPM").assertIsDisplayed()
            compose.runOnUiThread { gym.onMeasured(Measurement().apply { pulse=0 });tick++ }
            compose.onNodeWithText("Zone 5 · BPM").assertDoesNotExist()
            compose.onNodeWithText("Below Zone 1 · BPM").assertDoesNotExist()
        } finally {
            if(before==null) prefs.edit().remove(HeartRateZones.KEY).commit()
            else prefs.edit().putString(HeartRateZones.KEY,before).commit()
        }
    }
}
