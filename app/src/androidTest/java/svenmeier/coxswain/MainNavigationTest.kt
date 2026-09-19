package svenmeier.coxswain

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainNavigationTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun primaryTabsOpenTheirDestinations() {
        compose.onNodeWithText("Ready to row?").assertIsDisplayed()

        compose.onNodeWithContentDescription("Programs").performClick()
        compose.onNodeWithText("MY PROGRAMS").assertIsDisplayed()

        compose.onNodeWithContentDescription("History").performClick()
        compose.onNodeWithText("WORKOUT HISTORY").assertIsDisplayed()

        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("MORE").assertIsDisplayed()
    }

    @Test
    fun quickStartOpensSharedWorkoutSetup() {
        compose.onNodeWithText("Quick Start").performClick()
        compose.onNodeWithText("Duration workout").assertIsDisplayed()
        compose.onNodeWithText("Distance").performClick()
        compose.onNodeWithText("Distance workout").assertIsDisplayed()
        compose.onNodeWithText("Intervals").performClick()
        compose.onNodeWithText("Intervals workout").assertIsDisplayed()
        compose.onNodeWithText("Add segment").assertIsDisplayed()
    }

    @Test
    fun intervalBuilderChangesTypesValuesAddsDeletesAndRejectsZero() {
        compose.onNodeWithText("Quick Start").performClick()
        compose.onNodeWithText("Intervals").performClick()

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

    @Test
    fun everyMoreDestinationIsReachable() {
        compose.onNodeWithContentDescription("More").performClick()

        compose.onNodeWithText("Connect rower").performClick()
        compose.onNodeWithText("Bluetooth FTMS").assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()

        compose.onNodeWithText("Data & Export").performClick()
        compose.onNodeWithText("Sync existing history").assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()

        compose.onNodeWithText("Diagnostics").performClick()
        compose.onNodeWithText("Live measurement").assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()

        compose.onNodeWithText("Help").performClick()
        compose.onNodeWithText("Race Your Best").assertIsDisplayed()
    }
}
