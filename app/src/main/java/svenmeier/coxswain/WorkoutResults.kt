package svenmeier.coxswain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import svenmeier.coxswain.gym.Snapshot
import svenmeier.coxswain.gym.Workout
import svenmeier.coxswain.gym.RaceOutcome
import svenmeier.coxswain.gym.SessionType
import svenmeier.coxswain.gym.WorkoutDefinition

@Composable
fun WorkoutResults(workout: Workout, snapshots: List<Snapshot>) {
    val avgPower = snapshots.map { it.power.get() }.filter { it > 0 }.average().toInt()
    val avgRate = snapshots.map { it.strokeRate.get() }.filter { it > 0 }.average().toInt()
    val maxPower = snapshots.maxOfOrNull { it.power.get() } ?: 0
    val maxRate = snapshots.maxOfOrNull { it.strokeRate.get() } ?: 0
    val rates = snapshots.map { it.strokeRate.get() }.filter { it > 0 }
    val splits = snapshots.map { it.speed.get() }.filter { it > 0 }.map { 50000 / it }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ResultMetricRow("Distance", "%,d m".format(workout.distance.get()))
        ResultMetricRow("Time", "%d:%02d".format(workout.duration.get() / 60, workout.duration.get() % 60))
        ResultMetricRow("Calories", "${workout.energy.get()} kcal")
        ResultMetricRow("Average stroke rate", if (avgRate == 0) "—" else "$avgRate SPM")
        ResultMetricRow("Average power", if (avgPower == 0) "—" else "$avgPower W")
        ResultMetricRow("Average split", if (splits.isEmpty()) "—" else formatSplit(splits.average().toInt()))
        ResultMetricRow("Best split", if (splits.isEmpty()) "—" else formatSplit(splits.minOrNull()!!))
        ResultMetricRow("Total strokes", "%,d".format(workout.strokes.get()))
        ResultMetricRow("Stroke rate range", if (rates.isEmpty()) "—" else "${rates.minOrNull()}–${rates.maxOrNull()} SPM")
        ResultChart("Split time", splits, workout.duration.get(), "s /500 m", "avg ${if (splits.isEmpty()) "—" else formatSplit(splits.average().toInt())}  •  best ${if (splits.isEmpty()) "—" else formatSplit(splits.minOrNull()!!)}")
        ResultChart("Power", snapshots.map { it.power.get() }, workout.duration.get(), "W", "avg ${if (avgPower == 0) "—" else "$avgPower W"}  •  max ${if (maxPower == 0) "—" else "$maxPower W"}")
        ResultChart("Stroke rate", snapshots.map { it.strokeRate.get() }, workout.duration.get(), "SPM", "avg ${if (avgRate == 0) "—" else "$avgRate SPM"}  •  min ${rates.minOrNull() ?: "—"}  •  max ${rates.maxOrNull() ?: "—"}  •  ${workout.strokes.get()} strokes")
    }
}

@Composable
fun RaceResultSummary(workout: Workout) {
    val outcome = workout.raceOutcome.get()
    if (outcome == RaceOutcome.NONE) return
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (outcome == RaceOutcome.WON) Color(0xFFE4F7EC) else Color(0xFFFFEEF0))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (outcome == RaceOutcome.WON) "You won the race" else if (outcome == RaceOutcome.TIED) "Race tied" else "Best time ahead", fontWeight = FontWeight.Bold)
            val timed = workoutDefinitionType(workout) == SessionType.DURATION
            val margin = workout.raceMargin.get()
            val formatted = if (timed) "${kotlin.math.abs(margin)} m" else String.format("%.1f s", kotlin.math.abs(margin) / 1000f)
            Text("Margin: $formatted", fontSize = 13.sp, color = Color(0xFF53647C))
        }
    }
}

private fun formatSplit(seconds: Int): String = "%d:%02d /500 m".format(seconds / 60, seconds % 60)

@Composable private fun ResultMetricRow(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Color(0xFF53647C)); Text(value, fontWeight = FontWeight.Bold) } }

fun workoutDefinitionType(workout: Workout): SessionType {
    if (workout.sessionType.get() != SessionType.RACE) return workout.sessionType.get()
    return runCatching { WorkoutDefinition.typeOf(WorkoutDefinition.thaw(workout.programDefinition.get())) }.getOrDefault(SessionType.DISTANCE)
}

fun workoutPrimaryValue(workout: Workout): String = when (workoutDefinitionType(workout)) {
    SessionType.DURATION -> "%,d m".format(workout.distance.get())
    SessionType.INTERVAL -> {
        val segments = runCatching { WorkoutDefinition.thaw(workout.programDefinition.get()).segments.get().size }.getOrDefault(0)
        "$segments segments"
    }
    else -> "%d:%02d".format(workout.duration.get() / 60, workout.duration.get() % 60)
}

@Composable private fun ResultChart(title: String, values: List<Int>, durationSeconds: Int, unit: String, statistics: String) {
    val visible = values.filter { it > 0 }.takeLast(24)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, fontWeight = FontWeight.Bold)
        Text(if (visible.isEmpty()) "No samples" else statistics, fontSize = 11.sp, color = Color(0xFF53647C))
        Row(Modifier.fillMaxWidth().height(190.dp).background(Color(0xFFF4F7FB), RoundedCornerShape(12.dp)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val max = (visible.maxOrNull() ?: 1).coerceAtLeast(1)
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                Text("$max", fontSize = 10.sp); Text("${max / 2}", fontSize = 10.sp); Text("0 $unit", fontSize = 10.sp)
            }
            Row(Modifier.weight(1f).fillMaxHeight(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (visible.isEmpty()) Text("No recorded samples", color = Color(0xFF53647C), modifier = Modifier.align(Alignment.CenterVertically))
                visible.forEach { value -> Box(Modifier.weight(1f).height((10 + (value * 145 / max)).dp).background(Color(0xFF0B63F6), RoundedCornerShape(3.dp))) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 46.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("0:00", fontSize = 10.sp); Text(formatAxisTime(durationSeconds / 2), fontSize = 10.sp); Text(formatAxisTime(durationSeconds), fontSize = 10.sp) }
    }
}

private fun formatAxisTime(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)
