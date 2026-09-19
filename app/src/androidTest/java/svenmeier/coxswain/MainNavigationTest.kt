package svenmeier.coxswain

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import svenmeier.coxswain.gym.Difficulty
import svenmeier.coxswain.gym.Measurement
import svenmeier.coxswain.gym.Program

@RunWith(AndroidJUnit4::class)
class MainNavigationTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun primaryTabsOpenTheirDestinations() {
        compose.onNodeWithText("Ready to row?").assertIsDisplayed()

        compose.onNodeWithText("Programs").performClick()
        compose.onNodeWithText("MY PROGRAMS").assertIsDisplayed()

        compose.onNodeWithText("History").performClick()
        compose.onNodeWithText("WORKOUT HISTORY").assertIsDisplayed()

        compose.onNodeWithText("More").performClick()
        compose.onNodeWithText("MORE").assertIsDisplayed()
    }

    @Test
    fun quickStartOpensSharedWorkoutSetup() {
        compose.onNodeWithText("Quick Start").performClick()
        compose.waitUntil(5_000) {
            runCatching {
                compose.onAllNodesWithText("Duration workout").fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        compose.onNodeWithText("Distance").performClick()
        compose.onNodeWithText("Distance workout").assertIsDisplayed()
        compose.onNodeWithText("Intervals").performClick()
        compose.onNodeWithText("Intervals workout").assertIsDisplayed()
        compose.onNodeWithText("Add segment").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
    }

    @Test
    fun freeRowAndQuickStartReachLiveRow() {
        compose.onNodeWithText("Free Row").performClick()
        compose.onNodeWithText("End session").assertIsDisplayed().performClick()
        compose.onNodeWithText("Ready to row?").assertIsDisplayed()

        compose.onNodeWithText("Quick Start").performClick()
        compose.onNodeWithText("Start workout").performClick()
        compose.onNodeWithText("End session").assertIsDisplayed().performClick()
        compose.onNodeWithText("Ready to row?").assertIsDisplayed()
    }

    @Test
    fun programCreationAndRaceRoutesAreReachable() {
        var raceName = ""
        compose.activityRule.scenario.onActivity { activity ->
            val gym = Gym.instance(activity)
            val raceProgram = gym.programs.list().first()
            raceName = raceProgram.name.get()
            gym.select(raceProgram)
            val segment = raceProgram.getSegment(0)
            gym.onMeasured(Measurement().apply {
                duration = if (segment.duration.get() > 0) segment.duration.get() else 120
                distance = if (segment.distance.get() > 0) segment.distance.get() else 1000
                strokeRate = 24
            })
            gym.complete()
        }

        compose.onNodeWithText("Programs").performClick()
        compose.onNode(hasText("Race your best") and hasAnyAncestor(hasTestTag("program-$raceName"))).performClick()
        compose.onNodeWithText("Choose a compatible completed result to race against.").assertIsDisplayed()
        compose.onNodeWithText("Start race").performClick()
        compose.onNodeWithText("End session").assertIsDisplayed()
        compose.activityRule.scenario.onActivity { activity ->
            Gym.instance(activity).onMeasured(Measurement().apply {
                duration = 10
                distance = 100
                strokeRate = 24
            })
        }
        compose.onNodeWithText("End session").performClick()
        compose.onNodeWithText("WORKOUT COMPLETE").assertIsDisplayed()
        compose.onNodeWithText("Done").performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Programs").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Programs").performClick()
        compose.onNodeWithText("MY PROGRAMS").assertIsDisplayed()

        compose.onNodeWithText("Create program").performClick()
        compose.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()
    }

    @Test
    fun rowAgainAndHistoryDeletionCompleteEndToEnd() {
        compose.activityRule.scenario.onActivity { activity ->
            val gym = Gym.instance(activity)
            val program = Program.meters("E2E 100 m", 100, Difficulty.EASY)
            gym.mergeProgram(program)
            gym.select(program)
            gym.onMeasured(Measurement().apply {
                duration = 20
                distance = 100
                strokeRate = 24
            })
            gym.complete()
        }

        // Change tabs to reconstruct Home from the newly persisted result.
        compose.onNodeWithText("Programs").performClick()
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText("Row again").performScrollTo().performClick()
        compose.onNodeWithText("End session").assertIsDisplayed().performClick()

        compose.onNodeWithText("History").performClick()
        compose.onNodeWithText("E2E 100 m").performClick()
        compose.onNodeWithText("Delete workout").performScrollTo().performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("WORKOUT HISTORY").assertIsDisplayed()
    }

    @Test
    fun everyMoreDestinationIsReachable() {
        compose.onNodeWithText("More").performClick()

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
        compose.onNodeWithText("Back").performClick()
    }
}
