package svenmeier.coxswain

import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import svenmeier.coxswain.compose.CoxswainTheme

@RunWith(AndroidJUnit4::class)
class PerformanceZoneSettingsTest {
    @get:Rule val compose = createComposeRule()
    private val suggested = PerformanceZones(OutputZones(100,150,200), OutputZones(180,150,120,true))
    @Test fun suggestionsFillFieldsAndSavePaceInSeconds() {
        var saved: PerformanceZones? = null
        compose.setContent { CoxswainTheme { Surface { PerformanceZonesSettings(null, suggested, false, false, {}, { saved = it }) } } }
        compose.onNodeWithText("Moderate starts at (W)").performScrollTo().assertTextContains("100")
        compose.onNodeWithText("Peak starts at (m:ss /500 m)").performScrollTo().assertTextContains("2:00")
        compose.onNodeWithText("Save zones").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(suggested, saved) }
    }
    @Test fun descendingPowerCannotSaveAndPaceOverrideIsRetained() {
        var saved: PerformanceZones? = null
        compose.setContent { CoxswainTheme(darkTheme = true) { Surface { PerformanceZonesSettings(null, suggested, false, false, {}, { saved = it }) } } }
        compose.onNodeWithText("Moderate starts at (W)").performScrollTo().performTextReplacement("250")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Save zones").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Moderate starts at (W)").performScrollTo().performTextReplacement("110")
        compose.onNodeWithText("Peak starts at (m:ss /500 m)").performScrollTo().performTextReplacement("1:55")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Save zones").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(110, saved!!.power!!.moderate); assertEquals(115, saved!!.pace!!.peak) }
    }
}
