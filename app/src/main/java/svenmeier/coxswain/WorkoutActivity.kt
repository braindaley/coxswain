package svenmeier.coxswain

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import svenmeier.coxswain.compose.CoxswainTheme
import svenmeier.coxswain.compose.workout.LiveRowScreen

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
                
                DisposableEffect(Unit) {
                    listener = Gym.Listener { tick++ }
                    gym.addListener(listener)
                    onDispose {
                        gym.removeListener(listener)
                    }
                }

                LiveRowScreen(
                    gym = gym,
                    onPause = { /* TODO: GymService pause */ },
                    onResume = { /* TODO: GymService resume */ },
                    onEnd = { finish() },
                    onEditMetric = { /* TODO: Metric picker */ }
                )
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
