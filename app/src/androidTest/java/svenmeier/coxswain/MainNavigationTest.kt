package svenmeier.coxswain

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
}
