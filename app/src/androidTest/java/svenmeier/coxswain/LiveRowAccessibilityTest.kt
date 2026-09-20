package svenmeier.coxswain

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import svenmeier.coxswain.compose.CoxswainTheme
import svenmeier.coxswain.compose.workout.GoalDisplay
import svenmeier.coxswain.compose.workout.MetricCell
import svenmeier.coxswain.gym.Measurement
import svenmeier.coxswain.view.ValueBinding

@RunWith(AndroidJUnit4::class)
class LiveRowAccessibilityTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun metricIsReadOnlyUntilDisplayEditingIsEnabled() {
        compose.setContent {
            CoxswainTheme {
                MetricCell(ValueBinding.DURATION, Measurement(), editable = false, onClick = {})
            }
        }

        compose.onNodeWithContentDescription("DURATION metric, 00:00")
            .assertHasNoClickAction()
    }

    @Test
    fun goalMetricHasOneLocalizedSummaryAndEditAction() {
        val measurement = Measurement().apply { strokeRate = 24 }
        var edits = 0
        compose.setContent {
            CoxswainTheme {
                MetricCell(
                    ValueBinding.STROKE_RATE,
                    measurement,
                    GoalDisplay("-2", "26", -1),
                    editable = true,
                    onClick = { edits++ }
                )
            }
        }

        compose.onNodeWithContentDescription(
            "STROKERATE goal variance -2, current 24, target 26"
        ).assertHasClickAction().performClick()
        assertEquals(1, edits)
    }
}
