package svenmeier.coxswain

import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import svenmeier.coxswain.compose.CoxswainTheme

@RunWith(AndroidJUnit4::class)
class HeartRateZoneSettingsTest {
    @get:Rule val compose = createComposeRule()
    @Test fun calculatedProfileCannotSaveUntilValidThenPersistsActualValues() {
        var saved: HeartRateZones? = null
        compose.setContent { CoxswainTheme { Surface { HeartRateZonesSettings(null, {}, { saved = it }) } } }
        compose.onNodeWithText("Save zones").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Resting heart rate (BPM)").performScrollTo().performTextInput("60")
        compose.onNodeWithText("Maximum heart rate (BPM)").performScrollTo().performTextInput("175")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("105–122 BPM").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Save zones").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(HeartRateZones.training(175,60), saved) }
    }
    @Test fun customValidationAndTurnOffPreserveAtomicSettings() {
        var saved: HeartRateZones? = HeartRateZones.reserve(60, 175)
        var calls = 0
        compose.setContent { CoxswainTheme(darkTheme = true) { Surface {
            HeartRateZonesSettings(saved, {}, { saved = it; calls++ })
        } } }
        compose.onNodeWithText("Custom").performClick()
        compose.onNodeWithText("Zone 2 starts at (BPM)").performTextReplacement("150")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Save zones").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Zone 2 starts at (BPM)").performScrollTo().performTextReplacement("100")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Zone 5 starts at (BPM)").performScrollTo().performTextInput("175")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Save zones").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(100, saved!!.moderate); assertNull(saved!!.resting); assertEquals(1, calls) }
        compose.onNodeWithText("Turn off zones for new workouts").performScrollTo().performClick()
        compose.runOnIdle { assertNull(saved); assertEquals(2, calls) }
    }
    @Test fun historyDoesNotAutofillMaximumAndKeepsUserOverrides() {
        var history by androidx.compose.runtime.mutableStateOf(RecordedHeartRateRange(70, 180))
        var saved: HeartRateZones? = null
        compose.setContent { CoxswainTheme { Surface {
            HeartRateZonesSettings(null, {}, { saved = it }, recordedRange = history)
        } } }
        compose.onNodeWithText("Resting heart rate (BPM)").assertTextContains("70")
        compose.onNodeWithText("Maximum heart rate (BPM)").performScrollTo().assert(hasText("180").not())
        compose.onNodeWithText("Resting heart rate (BPM)").performScrollTo().performTextReplacement("60")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Save zones").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Maximum heart rate (BPM)").performScrollTo().performTextInput("180")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.runOnIdle { history = RecordedHeartRateRange(65, 185) }
        compose.onNodeWithText("Save zones").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(HeartRateZones.training(180,60), saved) }
    }
    @Test fun unverifiedSavedMaximumNeedsExplicitReview() {
        var saved:HeartRateZones?=null
        compose.setContent { CoxswainTheme { Surface {
            HeartRateZonesSettings(HeartRateZones.reserve(76,135).asTraining(),{}, { saved=it },
                recordedRange=RecordedHeartRateRange(76,148))
        } } }
        compose.onNodeWithText("Maximum heart rate (BPM)").performScrollTo().assert(hasText("135").not())
        compose.onNodeWithText("Save zones").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Estimate maximum from age").performScrollTo().performClick()
        compose.onNodeWithText("Age").performScrollTo().performTextInput("40")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Save zones").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(180,saved!!.maximum);assertEquals(40,saved!!.age) }
    }

}
