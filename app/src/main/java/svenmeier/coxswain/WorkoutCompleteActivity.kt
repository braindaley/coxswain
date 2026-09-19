package svenmeier.coxswain

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import propoid.db.Reference
import svenmeier.coxswain.compose.CoxswainTheme
import svenmeier.coxswain.gym.SessionType
import svenmeier.coxswain.gym.Workout

class WorkoutCompleteActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reference = Reference.from<Workout>(intent)
        val workout = reference?.let { Gym.instance(this).get(it) }
        if (workout == null) {
            finish()
            return
        }

        val gym = Gym.instance(this)
        setContent {
            CoxswainTheme {
                WorkoutCompleteScreen(workout = workout, snapshots = gym.getSnapshots(workout).list(), onDone = { finish() })
            }
        }
    }

    companion object {
        @JvmStatic
        fun start(activity: Activity, workout: Workout) {
            activity.startActivity(
                Intent(activity, WorkoutCompleteActivity::class.java)
                    .setData(Reference(workout).toUri())
            )
        }
    }
}

@Composable
private fun WorkoutCompleteScreen(workout: Workout, snapshots: List<svenmeier.coxswain.gym.Snapshot>, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("WORKOUT COMPLETE", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(12.dp))
        Text(workout.programName("Workout"), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(28.dp))
        val primary = if (workout.sessionType.get() == SessionType.DURATION) {
            "%,d m".format(workout.distance.get())
        } else {
            formatDuration(workout.duration.get())
        }
        Text(primary, fontSize = 56.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        WorkoutResults(workout, snapshots)
        RaceResultSummary(workout)
        Spacer(Modifier.weight(1f))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Done", fontWeight = FontWeight.Bold)
        }
    }
}
