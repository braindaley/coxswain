package svenmeier.coxswain

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import svenmeier.coxswain.compose.CoxswainTheme
import svenmeier.coxswain.compose.WorkoutSetupScreen

@RunWith(AndroidJUnit4::class)
class WorkoutSetupScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun intervalBuilderChangesTypesValuesAddsDeletesAndRejectsZero() {
        compose.setContent {
            CoxswainTheme {
                WorkoutSetupScreen(
                    initialType = "Intervals",
                    onBack = {},
                    onStart = {},
                    onSaveAsProgram = {}
                )
            }
        }

        // The first Distance node is the program-type selector; the second is
        // the first segment's type selector.
        compose.onAllNodesWithText("Distance")[1].performClick()
        compose.onNodeWithText("500").performTextReplacement("750")

        compose.onNodeWithText("Add segment").performClick()
        compose.onAllNodesWithContentDescription("Delete segment").assertCountEquals(4)
        compose.onAllNodesWithContentDescription("Delete segment")[3].performClick()
        compose.onAllNodesWithContentDescription("Delete segment").assertCountEquals(3)

        compose.onNodeWithText("750").performTextReplacement("0")
        compose.onNodeWithText("Start workout").assertIsNotEnabled()
        compose.onNodeWithText("Save as program").assertIsNotEnabled()
    }
}
