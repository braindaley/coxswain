package svenmeier.coxswain

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import svenmeier.coxswain.compose.CoxswainTheme
import svenmeier.coxswain.compose.workout.LiveRowScreen
import svenmeier.coxswain.gym.Workout
import svenmeier.coxswain.gym.WorkoutStatus

class WorkoutActivity : ComponentActivity() {

    private lateinit var gym: Gym
    private var listener: Gym.Listener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gym = Gym.instance(this)

        setContent {
            CoxswainTheme {
                // To keep the UI reactive, we'll need a state that updates
                // whenever the gym measurement changes
                var tick by remember { mutableIntStateOf(0) }
                var confirmEnd by remember { mutableStateOf(false) }
                
                DisposableEffect(Unit) {
                    listener = Gym.Listener { scope ->
                        tick++
                        if (scope is Workout && (scope.status.get() == WorkoutStatus.COMPLETED ||
                                    scope.status.get() == WorkoutStatus.ENDED_EARLY)) {
                            WorkoutCompleteActivity.start(this@WorkoutActivity, scope)
                            finish()
                        } else if (scope is Workout && scope.status.get() == WorkoutStatus.DISCARDED) {
                            finish()
                        }
                    }
                    gym.addListener(listener)
                    onDispose {
                        gym.removeListener(listener)
                    }
                }

                LiveRowScreen(
                    gym = gym,
                    refreshTick = tick,
                    onPause = { gym.pause() },
                    onResume = { gym.resume() },
                    onEnd = {
                        val workout = gym.endEarly()
                        if (workout == null) finish()
                    },
                    onEditMetric = { /* TODO: Metric picker */ }
                )
                BackHandler { confirmEnd = true }
                if (confirmEnd) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { confirmEnd = false },
                        title = { androidx.compose.material3.Text("End this session?") },
                        text = { androidx.compose.material3.Text("Choose End session to save the workout in History, or keep rowing.") },
                        confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmEnd = false; if (gym.endEarly() == null) finish() }) { androidx.compose.material3.Text("End session") } },
                        dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmEnd = false }) { androidx.compose.material3.Text("Keep rowing") } }
                    )
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun start(activity: Activity) {
            activity.startActivity(Intent(activity, WorkoutActivity::class.java))
        }

        @JvmStatic
        fun restart(activity: Activity) {
            start(activity)
        }
    }
}
