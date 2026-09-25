package svenmeier.coxswain

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click as espressoClick
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ProgramIntervalEditingTest {
    @get:Rule
    val compose = createAndroidComposeRule<ProgramActivity>()

    @Test
    fun changingIntervalDurationRefreshesBuilderCard() {
        compose.onNodeWithText("Intervals").performClick()
        compose.onAllNodesWithText("60:00").get(0).performClick()
        onView(withId(R.id.btn_duration_plus)).perform(espressoClick())
        onView(withText("OK")).perform(espressoClick())
        compose.waitForIdle()
        compose.onNodeWithText("60:30").assertIsDisplayed()
    }
}
