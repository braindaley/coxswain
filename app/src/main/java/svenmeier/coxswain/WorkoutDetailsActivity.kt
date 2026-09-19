package svenmeier.coxswain

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import propoid.db.Reference
import svenmeier.coxswain.compose.CoxswainTheme
import svenmeier.coxswain.gym.Workout

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
    Scaffold(topBar = { TopAppBar(title = { Text("Workout details") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(workout.programName("Free Row"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(workoutPrimaryValue(workout), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            WorkoutResults(workout, snapshots)
            RaceResultSummary(workout)
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete workout") }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Delete workout?") }, text = { Text("This removes the workout and its recorded snapshots from history.") }, confirmButton = { TextButton(onClick = onDelete) { Text("Delete") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
}

@Composable private fun DetailRow(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value, fontWeight = FontWeight.Bold) } }
