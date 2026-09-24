package svenmeier.coxswain

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import propoid.db.Reference
import svenmeier.coxswain.compose.CoxswainTheme
import svenmeier.coxswain.gym.Workout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkoutDetailsActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val workout = Reference.from<Workout>(intent)?.let { Gym.instance(this).get(it) }
        if (workout == null) { finish(); return }
        val gym = Gym.instance(this)
        val snapshots = ArrayList(gym.getSnapshots(workout).list())
        setContent { CoxswainTheme { WorkoutDetailsScreen(workout, snapshots, onBack = { finish() }, onDelete = { gym.delete(workout); finish() }) } }
    }
    companion object { @JvmStatic fun start(activity: Activity, workout: Workout) { activity.startActivity(Intent(activity, WorkoutDetailsActivity::class.java).setData(Reference(workout).toUri())) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutDetailsScreen(workout: Workout, snapshots: List<svenmeier.coxswain.gym.Snapshot>, onBack: () -> Unit, onDelete: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ui_workout_details)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.ui_back)) } },
                actions = {
                    IconButton(onClick = { confirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.ui_delete_from_history))
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp)) {
                            Text(stringResource(R.string.ui_done), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(workout.programName(stringResource(R.string.ui_free_row)), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(workout.start.get())),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(workoutPrimaryValue(workout), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.ui_workout_summary), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            WorkoutResults(workout, snapshots)
            RaceResultSummary(workout)
            Spacer(Modifier.height(12.dp))
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(stringResource(R.string.ui_delete_workout_question)) }, text = { Text(stringResource(R.string.ui_delete_workout_explanation)) }, confirmButton = { TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete)) } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.ui_cancel)) } })
}

@Composable private fun DetailRow(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value, fontWeight = FontWeight.Bold) } }
