package svenmeier.coxswain

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import svenmeier.coxswain.gym.Difficulty
import svenmeier.coxswain.gym.Measurement
import svenmeier.coxswain.gym.Program
import svenmeier.coxswain.gym.Workout
import svenmeier.coxswain.gym.WorkoutStatus

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
        compose.onNodeWithTag("history-title").assertIsDisplayed()

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
        compose.waitUntil(5_000) {
            runCatching { compose.onAllNodesWithText("Distance workout").fetchSemanticsNodes().any { it.layoutInfo.isPlaced } }.getOrDefault(false)
        }
        compose.onNodeWithText("Intervals").performClick()
        compose.waitUntil(5_000) {
            runCatching { compose.onAllNodesWithText("Intervals workout").fetchSemanticsNodes().any { it.layoutInfo.isPlaced } }.getOrDefault(false)
        }
        compose.onNodeWithText("Add segment").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
    }

    @Test
    fun freeRowAndQuickStartReachLiveRow() {
        compose.onNodeWithText("Free Row").performClick()
        compose.activityRule.scenario.onActivity { activity ->
            Gym.instance(activity).onMeasured(Measurement().apply { duration = 5; distance = 20; strokeRate = 24 })
        }
        compose.onNodeWithText("End session").assertIsDisplayed().performClick()
        compose.onNodeWithText("WORKOUT COMPLETE").assertIsDisplayed()
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Ready to row?").assertIsDisplayed()

        compose.onNodeWithText("Quick Start").performClick()
        compose.onNodeWithText("Start workout").performClick()
        compose.activityRule.scenario.onActivity { activity ->
            Gym.instance(activity).onMeasured(Measurement().apply { duration = 5; distance = 20; strokeRate = 24 })
        }
        compose.onNodeWithText("End session").assertIsDisplayed().performClick()
        compose.onNodeWithText("WORKOUT COMPLETE").assertIsDisplayed()
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Ready to row?").assertIsDisplayed()
    }

    @Test
    fun programCreationAndRaceRoutesAreReachable() {
        var raceName = ""
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        run {
            val gym = Gym.instance(appContext)
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
        compose.onNodeWithTag("program-$raceName").performClick()
        compose.onNodeWithText("Program details").assertIsDisplayed()
        compose.onNodeWithText("Start race").assertIsDisplayed()
        compose.onNodeWithText("Start race").performClick()
        compose.onNodeWithText("RACE PROGRESS").assertIsDisplayed()
        run {
            Gym.instance(appContext).onMeasured(Measurement().apply {
                duration = 10
                distance = 100
                strokeRate = 24
            })
        }
        Gym.instance(appContext).discard()
        compose.waitUntil(5_000) {
            runCatching { compose.onAllNodesWithText("MY PROGRAMS").fetchSemanticsNodes().any { it.layoutInfo.isPlaced } }.getOrDefault(false)
        }
        compose.onNodeWithText("MY PROGRAMS").assertIsDisplayed()
        compose.onNodeWithTag("program-$raceName").performClick()
        compose.onNodeWithText("Start race").assertIsDisplayed()
        compose.onNodeWithContentDescription("Race your best").performClick()
        compose.onNodeWithText("Start workout").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Programs").performClick()
        compose.onNodeWithText("MY PROGRAMS").assertIsDisplayed()

        compose.onNodeWithText("Create program").performClick()
        compose.onNodeWithText("Intervals").performClick()
        compose.onAllNodesWithText("Name this interval").get(0).performTextInput("Warm up")
        compose.onNodeWithText("Warm up").assertIsDisplayed()
        compose.onNodeWithText("Add rest").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("01:00").assertIsDisplayed()
        compose.onNodeWithText("Save Program").performClick()
        compose.waitUntil(5_000) {
            runCatching { compose.onAllNodesWithText("MY PROGRAMS").fetchSemanticsNodes().any { it.layoutInfo.isPlaced } }.getOrDefault(false)
        }
        compose.onNodeWithText("MY PROGRAMS").assertIsDisplayed()
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
        compose.onNodeWithText("RACE PROGRESS").assertIsDisplayed()
        compose.onNodeWithText("End session").assertIsDisplayed().performClick()

        compose.onNodeWithText("History").performClick()
        compose.onNodeWithText("E2E 100 m").performClick()
        compose.onNodeWithText("Workout details").assertIsDisplayed()
        compose.onNodeWithContentDescription("Delete from History").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithTag("history-title").assertIsDisplayed()
    }

    @Test
    fun historyShowsAllProgramsWhenAnotherProgramIsSelected() {
        val suffix = System.nanoTime().toString()
        val firstName = "Audit first $suffix"
        val selectedName = "Audit selected $suffix"
        compose.activityRule.scenario.onActivity { activity ->
            val gym = Gym.instance(activity)
            val first = Program.meters(firstName, 100, Difficulty.EASY)
            gym.mergeProgram(first)
            gym.start(first, svenmeier.coxswain.gym.SessionType.DISTANCE)
            gym.onMeasured(Measurement().apply { duration = 30; distance = 100; strokeRate = 24 })
            gym.complete()

            val selected = Program.meters(selectedName, 200, Difficulty.EASY)
            gym.mergeProgram(selected)
            gym.start(selected, svenmeier.coxswain.gym.SessionType.DISTANCE)
            gym.onMeasured(Measurement().apply { duration = 45; distance = 200; strokeRate = 24 })
            gym.complete()
        }

        compose.onNodeWithText("History").performClick()
        compose.onNodeWithText(firstName).assertIsDisplayed()
        compose.onNodeWithText(selectedName).assertIsDisplayed()
    }

    @Test
    fun raceTargetsExcludeEarlyEndedWorkouts() {
        var candidateCount = -1
        compose.activityRule.scenario.onActivity { activity ->
            val gym = Gym.instance(activity)
            val program = Program.meters("Audit unfinished target ${System.nanoTime()}", 987654, Difficulty.EASY)
            gym.mergeProgram(program)
            val unfinished = Workout(program).apply {
                status.set(WorkoutStatus.ENDED_EARLY)
                distance.set(100)
                duration.set(10)
            }
            gym.mergeWorkout(unfinished)
            candidateCount = gym.getRaceCandidates(program).size
        }
        org.junit.Assert.assertEquals(0, candidateCount)
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
