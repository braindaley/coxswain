package svenmeier.coxswain

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import propoid.db.Reference
import svenmeier.coxswain.compose.CoxswainTheme
import svenmeier.coxswain.gym.Program
import svenmeier.coxswain.gym.Workout
import svenmeier.coxswain.gym.WorkoutDefinition

class RaceYourBestActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val gym = Gym.instance(this)
        val program = Reference.from<Program>(intent)?.let { gym.getProgram(it) }
        if (program == null) { finish(); return }
        setContent { CoxswainTheme { RaceYourBestScreen(program, gym.getRaceCandidates(program), onBack = { finish() }, onStart = { workout -> gym.repeat(workout); WorkoutActivity.start(this); finish() }) } }
    }
    companion object { @JvmStatic fun start(activity: Activity, program: Program) { activity.startActivity(Intent(activity, RaceYourBestActivity::class.java).setData(Reference(program).toUri())) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RaceYourBestScreen(program: Program, candidates: List<Workout>, onBack: () -> Unit, onStart: (Workout) -> Unit) {
    var selected by remember(candidates) { mutableStateOf(candidates.firstOrNull()) }
    Scaffold(topBar = { TopAppBar(title = { Text("Race your best") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { pad ->
        Column(Modifier.padding(pad).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(program.name.get(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Choose a compatible completed result to race against.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (candidates.isEmpty()) Text("Complete this program once to create a race target.")
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(candidates) { candidate ->
                    Card(Modifier.fillMaxWidth().clickable { selected = candidate }, colors = CardDefaults.cardColors(containerColor = if (candidate == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(candidate.start.get()))); Text(if (WorkoutDefinition.typeOf(program) == svenmeier.coxswain.gym.SessionType.DURATION) "${candidate.distance.get()} m" else "%d:%02d".format(candidate.duration.get()/60, candidate.duration.get()%60), fontWeight = FontWeight.Bold) }
                    }
                }
            }
            Button(onClick = { selected?.let(onStart) }, enabled = selected != null, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Start race", fontWeight = FontWeight.Bold) }
        }
    }
}
