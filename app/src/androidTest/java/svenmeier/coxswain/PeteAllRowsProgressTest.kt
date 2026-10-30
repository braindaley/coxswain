package svenmeier.coxswain

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import svenmeier.coxswain.compose.*
import svenmeier.coxswain.gym.*
import svenmeier.coxswain.pete.*

@RunWith(AndroidJUnit4::class)
class PeteAllRowsProgressTest {
    @get:Rule val compose = createComposeRule()
    @Test fun homeOverviewAndRecapGiveCreditForAllFiveRows() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val before = prefs.getString("petes_plan_state_v1",null)
        val gym = Gym.instance(context)
        val store = PetePlanStore(context,gym)
        val rows = mutableListOf<Workout>()
        var screen by mutableIntStateOf(0)
        try {
            compose.runOnUiThread {
                store.stop()
                val state=store.enroll()
                for(index in 0..4) {
                    val row=Workout(null).apply {
                        status.set(WorkoutStatus.COMPLETED);planEnrollment.set(state.enrollmentId)
                        planWeek.set(state.activeWeek);planAttempt.set(state.activeAttempt);planSession.set(index)
                        duration.set(1200);distance.set(4000)
                    }
                    gym.mergeWorkout(row);rows.add(row)
                }
            }
            compose.setContent { CoxswainTheme(darkTheme=true) {
                when(screen) {
                    0 -> Column(Modifier.verticalScroll(rememberScrollState())) { PetePlanHomeCard(store,{},{}) }
                    1 -> PetePlanScreen(gym,store,0,null,{}, {}, {_,_,_->})
                    else -> PeteWorkoutProgress(rows.last())
                }
            } }
            compose.onNodeWithText("5/5 rows").assertIsDisplayed()
            compose.onNodeWithText("3/3 required · 2/2 optional").assertIsDisplayed()
            compose.onNodeWithText("OPTIONAL").performScrollTo().assertIsDisplayed()
            compose.onAllNodesWithText("Completed",substring=true).fetchSemanticsNodes().let { org.junit.Assert.assertTrue(it.size>=5) }
            compose.runOnUiThread { screen=1 }
            compose.onNodeWithText("5/5 rows").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("3/3 required · 2/2 optional").assertIsDisplayed()
            compose.runOnUiThread { screen=2 }
            compose.onNodeWithText("5 of 5 rows complete").assertIsDisplayed()
            compose.onNodeWithText("3/3 required · 2/2 optional").assertIsDisplayed()
        } finally {
            compose.runOnUiThread {
                rows.forEach(gym::delete)
                if(before==null) prefs.edit().remove("petes_plan_state_v1").commit()
                else prefs.edit().putString("petes_plan_state_v1",before).commit()
            }
        }
    }
}
