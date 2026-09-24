package svenmeier.coxswain

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import svenmeier.coxswain.compose.CoxswainTheme
import svenmeier.coxswain.compose.workout.MetricCell
import svenmeier.coxswain.gym.Measurement
import svenmeier.coxswain.view.ValueBinding

@RunWith(AndroidJUnit4::class)
class LiveRowAccessibilityTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun metricIsReadOnlyUntilDisplayEditingIsEnabled() {
        val gym = Gym.instance(ApplicationProvider.getApplicationContext())
        compose.runOnUiThread { gym.onMeasured(Measurement()) }
        compose.setContent {
            CoxswainTheme {
                MetricCell(ValueBinding.DURATION, gym, isEditing = false, onClick = {})
            }
        }

        compose.onNodeWithContentDescription("DURATION 00:00")
            .assertIsNotEnabled()
    }

    @Test
    fun goalMetricHasOneSummaryAndEditAction() {
        val measurement = Measurement().apply { strokeRate = 24 }
        val gym = Gym.instance(ApplicationProvider.getApplicationContext())
        compose.runOnUiThread { gym.onMeasured(measurement) }
        var edits = 0
        compose.setContent {
            CoxswainTheme {
                MetricCell(
                    ValueBinding.STROKE_RATE,
                    gym,
                    isEditing = true,
                    onClick = { edits++ },
                    displayValue = "-2",
                    goalState = -1,
                    targetDescription = "26"
                )
            }
        }

        compose.onNodeWithContentDescription(
            "STROKE RATE -2, target 26"
        ).assertHasClickAction().performClick()
        assertEquals(1, edits)
    }
}
