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
        ResultChart("Split time", snapshots.map { it.speed.get() }, " /500 m")
        ResultChart("Power", snapshots.map { it.power.get() }, " W")
        ResultChart("Stroke rate", snapshots.map { it.strokeRate.get() }, " SPM")
        if (maxPower > 0 || maxRate > 0) Text("Max power $maxPower W  •  Max stroke rate $maxRate SPM", fontSize = 12.sp, color = Color(0xFF53647C))
    }
}

@Composable
fun RaceResultSummary(workout: Workout) {
    val outcome = workout.raceOutcome.get()
    if (outcome == RaceOutcome.NONE) return
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (outcome == RaceOutcome.WON) Color(0xFFE4F7EC) else Color(0xFFFFEEF0))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (outcome == RaceOutcome.WON) "You won the race" else if (outcome == RaceOutcome.TIED) "Race tied" else "Best time ahead", fontWeight = FontWeight.Bold)
            Text("Margin: ${workout.raceMargin.get()}", fontSize = 13.sp, color = Color(0xFF53647C))
        }
    }
}

private fun formatSplit(seconds: Int): String = "%d:%02d /500 m".format(seconds / 60, seconds % 60)

@Composable private fun ResultMetricRow(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Color(0xFF53647C)); Text(value, fontWeight = FontWeight.Bold) } }

@Composable private fun ResultChart(title: String, values: List<Int>, unit: String) {
    val visible = values.filter { it > 0 }.takeLast(24)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, fontWeight = FontWeight.Bold); Text(if (visible.isEmpty()) "No samples" else "max ${visible.maxOrNull()}$unit", fontSize = 11.sp, color = Color(0xFF53647C)) }
        Row(Modifier.fillMaxWidth().height(150.dp).background(Color(0xFFF4F7FB), RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            val max = (visible.maxOrNull() ?: 1).coerceAtLeast(1)
            if (visible.isEmpty()) Text("No recorded samples", color = Color(0xFF53647C), modifier = Modifier.align(Alignment.CenterVertically))
            visible.forEach { value -> Box(Modifier.weight(1f).height((18 + (value * 110 / max)).dp).background(Color(0xFF0B63F6), RoundedCornerShape(3.dp))) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("0", fontSize = 10.sp); Text("time", fontSize = 10.sp); Text("${visible.maxOrNull() ?: 0}$unit", fontSize = 10.sp) }
    }
}
