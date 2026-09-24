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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import propoid.db.Reference
import svenmeier.coxswain.compose.CoxswainTheme
import svenmeier.coxswain.gym.Workout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        val snapshots = ArrayList(gym.getSnapshots(workout).list())
        setContent {
            CoxswainTheme {
                WorkoutCompleteScreen(workout = workout, snapshots = snapshots, onDone = { finish() })
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
    Scaffold(
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Button(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp).height(56.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text(stringResource(R.string.ui_done), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { insets ->
        Column(
            modifier = Modifier.padding(insets).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.ui_workout_complete), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(workout.programName(stringResource(R.string.ui_workout)), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            val finished = workout.completed.get().takeIf { it > 0L } ?: workout.start.get()
            Text(
                SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()).format(Date(finished)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Text(workoutPrimaryValue(workout), fontSize = 56.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(22.dp))
            Text(stringResource(R.string.ui_workout_summary), modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            WorkoutResults(workout, snapshots)
            RaceResultSummary(workout)
            Spacer(Modifier.height(20.dp))
        }
    }
}
