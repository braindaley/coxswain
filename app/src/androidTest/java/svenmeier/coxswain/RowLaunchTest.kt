package svenmeier.coxswain

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import svenmeier.coxswain.gym.*
import svenmeier.coxswain.pete.PetePlanStore

@RunWith(AndroidJUnit4::class)
class RowLaunchTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun petesPlanCardOpensBriefAndStartsRow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val gym = Gym.instance(context)
        gym.deselect()
        val store = PetePlanStore(context, gym)
        store.stop()
        store.enroll()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onAllNodesWithText("5,000m")[0].performClick()
            compose.onNodeWithText("Start row").assertIsDisplayed().performClick()
            compose.onNodeWithText("PETE'S PLAN").assertIsDisplayed()
            compose.onNodeWithText("End session").performClick()
        }
        store.stop()
    }

    @Test fun distanceDurationIntervalsAndRaceOpenLiveScreen() {
        val gym = Gym.instance(ApplicationProvider.getApplicationContext<Context>())
        val interval = Program.minutes("Launch intervals", 1, Difficulty.MEDIUM).apply {
            addSegment(Segment(Difficulty.REST).setDuration(30))
            addSegment(Segment(Difficulty.MEDIUM).setDuration(60))
        }
        val programs = listOf(Program.meters("Launch distance", 5000, Difficulty.MEDIUM),
            Program.minutes("Launch duration", 30, Difficulty.MEDIUM), interval)
        programs.forEach { program ->
            gym.select(program)
            ActivityScenario.launch(WorkoutActivity::class.java).use {
                compose.onNodeWithText("Pause").assertIsDisplayed()
                compose.onNodeWithText("End session").performClick()
            }
        }
        val race = Program.meters("Launch race", 100, Difficulty.MEDIUM)
        gym.select(race)
        gym.onMeasured(Measurement().apply { duration = 30; distance = 100; strokes = 12 })
        val source = gym.complete()
        // Reproduce the NULL fields present in migrated workouts from the phone crash.
        source.planActiveSeconds.set(null)
        source.planActiveDistance.set(null)
        source.planActiveStrokes.set(null)
        gym.mergeWorkout(source)
        gym.race(race, source)
        ActivityScenario.launch(WorkoutActivity::class.java).use {
            compose.onNodeWithText("RACE YOUR BEST").assertIsDisplayed()
            compose.onNodeWithText("End session").performClick()
        }
        gym.delete(source)
    }
}
