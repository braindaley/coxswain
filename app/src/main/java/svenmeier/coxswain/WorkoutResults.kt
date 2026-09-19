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

@Composable
fun WorkoutResults(workout: Workout, snapshots: List<Snapshot>) {
    val avgPower = snapshots.map { it.power.get() }.filter { it > 0 }.average().toInt()
    val avgRate = snapshots.map { it.strokeRate.get() }.filter { it > 0 }.average().toInt()
    val maxPower = snapshots.maxOfOrNull { it.power.get() } ?: 0
    val maxRate = snapshots.maxOfOrNull { it.strokeRate.get() } ?: 0
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ResultMetricRow("Distance", "%,d m".format(workout.distance.get()))
        ResultMetricRow("Time", "%d:%02d".format(workout.duration.get() / 60, workout.duration.get() % 60))
        ResultMetricRow("Calories", "${workout.energy.get()} kcal")
        ResultMetricRow("Average stroke rate", if (avgRate == 0) "—" else "$avgRate SPM")
        ResultMetricRow("Average power", if (avgPower == 0) "—" else "$avgPower W")
        ResultChart("Split time", snapshots.map { it.speed.get() }, " /500 m")
        ResultChart("Power", snapshots.map { it.power.get() }, " W")
        ResultChart("Stroke rate", snapshots.map { it.strokeRate.get() }, " SPM")
        if (maxPower > 0 || maxRate > 0) Text("Max power $maxPower W  •  Max stroke rate $maxRate SPM", fontSize = 12.sp, color = Color(0xFF53647C))
    }
}

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
