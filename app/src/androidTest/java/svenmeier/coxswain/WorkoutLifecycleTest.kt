package svenmeier.coxswain

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutLifecycleTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun pausedSessionSurvivesRotationBackgroundAndBackCancellation() {
        val gym = Gym.instance(ApplicationProvider.getApplicationContext<Context>())
        gym.startFreeRow()

        ActivityScenario.launch(WorkoutActivity::class.java).use { scenario ->
            compose.onNodeWithText("Pause").assertIsDisplayed().performClick()
            compose.onNodeWithText("Resume").assertIsDisplayed()

            scenario.recreate()
            compose.onNodeWithText("Resume").assertIsDisplayed()

            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            compose.onNodeWithText("Resume").assertIsDisplayed()

            pressBack()
            compose.onNodeWithText("End this session?").assertIsDisplayed()
            compose.onNodeWithText("Keep rowing").performClick()
            compose.onNodeWithText("Resume").assertIsDisplayed().performClick()
            compose.onNodeWithText("Pause").assertIsDisplayed()

            compose.onNodeWithText("End session").performClick()
            compose.waitUntil(5_000) { scenario.state == Lifecycle.State.DESTROYED }
        }
    }

    @Test
    fun staleWorkoutLaunchClosesInsteadOfShowingAnEmptySession() {
        val gym = Gym.instance(ApplicationProvider.getApplicationContext<Context>())
        gym.deselect()

        ActivityScenario.launch(WorkoutActivity::class.java).use { scenario ->
            compose.waitUntil(5_000) { scenario.state == Lifecycle.State.DESTROYED }
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }
}
