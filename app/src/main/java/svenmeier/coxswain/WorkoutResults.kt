package svenmeier.coxswain

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
    val summary = WorkoutStatistics(workout, snapshots)
    val avgPower = summary.averagePower ?: 0
    val avgRate = summary.averageRate ?: 0
    val maxPower = summary.maximumPower ?: 0
    val rates = listOfNotNull(summary.minimumRate, summary.maximumRate)
    val splitSeries = summary.samples.map { if (it.second.speed.get() > 0) 50000 / it.second.speed.get() else 0 }
    val splits = listOfNotNull(summary.bestSplit)
    val averageSplit = summary.averageSplit?.let { formatSplit(it) } ?: "—"
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ResultMetricRow(stringResource(R.string.ui_distance), "%,d m".format(summary.workMeters))
        ResultMetricRow(stringResource(R.string.ui_time_rowed), "%d:%02d".format(summary.workSeconds / 60, summary.workSeconds % 60))
        ResultMetricRow(stringResource(R.string.ui_calories), "${workout.energy.get()} kcal")
        ResultMetricRow(stringResource(R.string.ui_average_stroke_rate), if (summary.averageRate == null) "—" else "$avgRate SPM")
        ResultMetricRow(stringResource(R.string.ui_average_power), if (summary.averagePower == null) "—" else "$avgPower W")
        ResultMetricRow(stringResource(R.string.ui_average_split), averageSplit)
        ResultMetricRow(stringResource(R.string.ui_best_split), if (splits.isEmpty()) "—" else formatSplit(splits.minOrNull()!!))
        ResultMetricRow(stringResource(R.string.ui_total_strokes), "%,d".format(summary.workStrokes))
        ResultMetricRow(stringResource(R.string.ui_stroke_rate_range), if (rates.isEmpty()) "—" else "${rates.minOrNull()}–${rates.maxOrNull()} SPM")
        ResultChart(stringResource(R.string.ui_split_time), splitSeries, summary, "s /500 m", stringResource(R.string.ui_chart_average_best, averageSplit, if (splits.isEmpty()) "—" else formatSplit(splits.minOrNull()!!)))
        ResultChart(stringResource(R.string.ui_power), summary.samples.map { it.second.power.get() }, summary, "W", stringResource(R.string.ui_chart_average_max, "$avgPower W", "$maxPower W"))
        ResultChart(stringResource(R.string.ui_stroke_rate), summary.samples.map { it.second.strokeRate.get() }, summary, "SPM", stringResource(R.string.ui_chart_stroke_statistics, "$avgRate SPM", rates.minOrNull()?.toString() ?: "—", rates.maxOrNull()?.toString() ?: "—", summary.workStrokes))
    }
}

@Composable
fun RaceResultSummary(workout: Workout) {
    val outcome = workout.raceOutcome.get()
    if (outcome == RaceOutcome.NONE) return
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (outcome == RaceOutcome.WON) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(if (outcome == RaceOutcome.WON) R.string.ui_race_won else if (outcome == RaceOutcome.TIED) R.string.ui_race_tied else R.string.ui_race_best_ahead), fontWeight = FontWeight.Bold)
            val timed = runCatching { WorkoutDefinition.ranksByDistance(WorkoutDefinition.thaw(workout.programDefinition.get())) }.getOrDefault(false)
            val margin = workout.raceMargin.get()
            val formatted = if (timed) "${kotlin.math.abs(margin)} m" else String.format("%.1f s", kotlin.math.abs(margin) / 1000f)
            Text(stringResource(R.string.ui_margin, formatted), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatSplit(seconds: Int): String = "%d:%02d /500 m".format(seconds / 60, seconds % 60)

@Composable private fun ResultMetricRow(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.Bold) } }

fun workoutDefinitionType(workout: Workout): SessionType {
    if (workout.sessionType.get() != SessionType.RACE) return workout.sessionType.get()
    return runCatching { WorkoutDefinition.typeOf(WorkoutDefinition.thaw(workout.programDefinition.get())) }.getOrDefault(SessionType.DISTANCE)
}

@Composable
fun workoutPrimaryValue(workout: Workout): String = when (workoutDefinitionType(workout)) {
    SessionType.DURATION -> "%,d m".format(workout.distance.get())
    SessionType.INTERVAL -> {
        val segments = runCatching { WorkoutDefinition.thaw(workout.programDefinition.get()).segments.get().size }.getOrDefault(0)
        pluralStringResource(R.plurals.ui_segment_count, segments, segments)
    }
    else -> "%d:%02d".format(workout.duration.get() / 60, workout.duration.get() % 60)
}

@Composable private fun ResultChart(title: String, values: List<Int>, summary: WorkoutStatistics, unit: String, statistics: String) {
    val durationSeconds = summary.duration
    val visible = values
    val hasSamples = visible.any { it > 0 }
    val chartColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, fontWeight = FontWeight.Bold)
        Text(if (!hasSamples) stringResource(R.string.ui_no_samples) else statistics, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().height(190.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val max = (visible.maxOrNull() ?: 1).coerceAtLeast(1)
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                Text("$max", fontSize = 10.sp); Text("${max / 2}", fontSize = 10.sp); Text("0 $unit", fontSize = 10.sp)
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                if (!hasSamples) Text(stringResource(R.string.ui_no_recorded_samples), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Center))
                else Canvas(Modifier.fillMaxSize()) {
                    val plotHeight = size.height - 4.dp.toPx()
                    summary.restRanges.forEach { (start, end) ->
                        val left = size.width * start / durationSeconds.coerceAtLeast(1)
                        val right = size.width * end / durationSeconds.coerceAtLeast(1)
                        drawRect(gridColor.copy(alpha = .35f), Offset(left, 0f), androidx.compose.ui.geometry.Size(right - left, plotHeight))
                    }
                    summary.boundaries.forEach { time ->
                        val x = size.width * time / durationSeconds.coerceAtLeast(1)
                        drawLine(gridColor, Offset(x, 0f), Offset(x, plotHeight), 1.dp.toPx())
                    }
                    listOf(0f, .5f, 1f).forEach { fraction ->
                        drawLine(gridColor, Offset(0f, plotHeight * fraction), Offset(size.width, plotHeight * fraction), 1.dp.toPx())
                    }
                    val path = Path()
                    var connected = false
                    visible.forEachIndexed { index, value ->
                        val x = size.width * summary.samples[index].first / durationSeconds.coerceAtLeast(1)
                        val y = plotHeight * (1f - value.toFloat() / max)
                        if (unit == "s /500 m" && value <= 0) connected = false
                        else {
                            if (!connected) path.moveTo(x, y) else path.lineTo(x, y)
                            connected = true
                        }
                    }
                    drawPath(path, chartColor, style = Stroke(2.dp.toPx()))
                    if (visible.size == 1) drawCircle(chartColor, 3.dp.toPx(), Offset(size.width * summary.samples[0].first / durationSeconds.coerceAtLeast(1), plotHeight * (1f - visible[0].toFloat() / max)))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 46.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("0:00", fontSize = 10.sp); Text(formatAxisTime(durationSeconds / 2), fontSize = 10.sp); Text(formatAxisTime(durationSeconds), fontSize = 10.sp) }
    }
}

private fun formatAxisTime(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)
