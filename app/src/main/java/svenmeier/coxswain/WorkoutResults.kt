package svenmeier.coxswain

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import svenmeier.coxswain.gym.Difficulty
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt
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
    val summary = remember(workout, snapshots) { WorkoutStatistics(workout, snapshots) }
    val charts = remember(summary) { WorkoutChartData(workout, summary) }
    var includeStartup by remember(workout) { mutableStateOf(false) }
    var selectedFraction by remember(workout) { mutableStateOf<Float?>(null) }
    val context = LocalContext.current
    val clockFormat = remember(context, charts.span) {
        val seconds = charts.span < 300_000L
        SimpleDateFormat(if (android.text.format.DateFormat.is24HourFormat(context)) {
            if (seconds) "HH:mm:ss" else "HH:mm"
        } else if (seconds) "h:mm:ss a" else "h:mm a", Locale.getDefault())
    }
    val preciseFormat = remember(clockFormat) {
        SimpleDateFormat(clockFormat.toPattern().let { if (it.contains("ss")) it else it.replace("mm", "mm:ss") },
            Locale.getDefault()).apply { timeZone = clockFormat.timeZone }
    }
    fun clock(time: Long) = clockFormat.format(Date(time))
    val avgPower = summary.averagePower ?: 0
    val avgRate = summary.averageRate ?: 0
    val maxPower = summary.maximumPower ?: 0
    val rates = listOfNotNull(summary.minimumRate, summary.maximumRate)
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
        Text(stringResource(R.string.ui_chart_started_finished, preciseFormat.format(Date(charts.start)), preciseFormat.format(Date(charts.end))),
            style = MaterialTheme.typography.bodyMedium)
        if (charts.estimatedClock) Text(stringResource(R.string.ui_chart_estimated_clock),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (charts.hasStartup) {
            Row(Modifier.fillMaxWidth().clickable { includeStartup = !includeStartup }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeStartup, onCheckedChange = { includeStartup = it })
                Text(stringResource(R.string.ui_chart_include_startup), style = MaterialTheme.typography.bodyMedium)
            }
            Text(stringResource(R.string.ui_chart_startup_note), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (charts.phases.size > 1) ProgramEffortLegend(charts)
        if (charts.points.isNotEmpty()) {
            Text(stringResource(R.string.ui_chart_inspect), style = MaterialTheme.typography.titleMedium)
            val point = selectedFraction?.let(charts::nearest)
            if (point != null) {
                Text(stringResource(R.string.ui_chart_selected_time, preciseFormat.format(Date(point.clock)), formatAxisTime(point.elapsed.roundToInt())),
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(ResultMeasure.entries.joinToString("   ·   ") { measure ->
                    val value = charts.value(point, measure)
                    when (measure) {
                        ResultMeasure.SPLIT -> if (value == null) "— /500 m" else formatSplit(value.roundToInt())
                        ResultMeasure.POWER -> if (value == null) "— W" else "${value.roundToInt()} W"
                        ResultMeasure.RATE -> if (value == null) "— SPM" else "${value.roundToInt()} SPM"
                    }
                }, style = MaterialTheme.typography.bodyMedium)
                charts.phase(point)?.takeIf { charts.phases.size > 1 }?.let { Text(phaseName(it), style = MaterialTheme.typography.bodyMedium) }
            } else Text(stringResource(R.string.ui_chart_inspect_hint), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value = selectedFraction ?: 0f, onValueChange = { selectedFraction = it },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = context.getString(R.string.ui_chart_inspect) })
        }
        ResultChart(stringResource(R.string.ui_split_time), ResultMeasure.SPLIT, charts, includeStartup,
            selectedFraction, { selectedFraction = it }, clockFormat, summary.averageSplit?.toFloat(),
            stringResource(R.string.ui_chart_average_best, averageSplit, if (splits.isEmpty()) "—" else formatSplit(splits.minOrNull()!!)))
        ResultChart(stringResource(R.string.ui_power), ResultMeasure.POWER, charts, includeStartup,
            selectedFraction, { selectedFraction = it }, clockFormat, summary.averagePower?.toFloat(),
            stringResource(R.string.ui_chart_average_max, "$avgPower W", "$maxPower W"))
        ResultChart(stringResource(R.string.ui_stroke_rate), ResultMeasure.RATE, charts, includeStartup,
            selectedFraction, { selectedFraction = it }, clockFormat, summary.averageRate?.toFloat(),
            stringResource(R.string.ui_chart_stroke_statistics, "$avgRate SPM", rates.minOrNull()?.toString() ?: "—", rates.maxOrNull()?.toString() ?: "—", summary.workStrokes))
        if (charts.phases.size > 1) IntervalResults(charts, preciseFormat, onSelect = { phase ->
            selectedFraction = charts.clockFraction(charts.clockAt((phase.start + phase.end) / 2f))
        })
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


@Composable
private fun phaseName(phase: ResultPhase): String = phase.segment?.name?.get()?.takeIf { it.isNotBlank() }
    ?: if (phase.difficulty == Difficulty.REST) stringResource(R.string.ui_rest)
    else stringResource(R.string.ui_chart_interval_name, phase.index + 1)

@Composable
private fun effortName(difficulty: Difficulty): String = stringResource(when (difficulty) {
    Difficulty.REST -> R.string.ui_rest
    Difficulty.EASY -> R.string.ui_chart_light
    Difficulty.MEDIUM -> R.string.ui_chart_moderate
    Difficulty.HARD -> R.string.ui_chart_vigorous
    Difficulty.PEAK -> R.string.ui_chart_peak
    else -> R.string.ui_chart_row
})

@Composable
private fun effortColors(): Map<Difficulty, Color> {
    val dark = MaterialTheme.colorScheme.surface.luminance() < .4f
    return mapOf(Difficulty.NONE to MaterialTheme.colorScheme.primary,
        Difficulty.REST to MaterialTheme.colorScheme.onSurfaceVariant,
        Difficulty.EASY to if (dark) Color(0xFF73DFA3) else Color(0xFF217B45),
        Difficulty.MEDIUM to MaterialTheme.colorScheme.primary,
        Difficulty.HARD to if (dark) Color(0xFFFFCB74) else Color(0xFF986600),
        Difficulty.PEAK to MaterialTheme.colorScheme.error)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgramEffortLegend(charts: WorkoutChartData) {
    val colors = effortColors()
    Text(stringResource(R.string.ui_chart_program_effort), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        charts.phases.map { it.difficulty }.distinct().forEach { difficulty ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).background(colors.getValue(difficulty), RoundedCornerShape(2.dp)))
                Text(effortName(difficulty), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    Text(stringResource(R.string.ui_chart_effort_note), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ResultChart(title: String, measure: ResultMeasure, charts: WorkoutChartData,
                        includeStartup: Boolean, selectedFraction: Float?, onSelect: (Float) -> Unit,
                        clockFormat: SimpleDateFormat, average: Float?, statistics: String) {
    val scale = remember(charts, measure, includeStartup) { charts.scale(measure, includeStartup) }
    val colors = effortColors()
    val chartColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val referenceColor = MaterialTheme.colorScheme.onSurfaceVariant
    val startupColor = MaterialTheme.colorScheme.secondaryContainer
    val hasSamples = charts.points.any { !charts.isRest(it) && (charts.value(it, measure) ?: 0f) > 0f }
    val unit = when (measure) { ResultMeasure.SPLIT -> "/500 m"; ResultMeasure.POWER -> "W"; ResultMeasure.RATE -> "SPM" }
    val chartDescription = stringResource(R.string.ui_chart_accessibility, title, statistics)
    fun label(value: Float) = if (measure == ResultMeasure.SPLIT) formatAxisTime(value.roundToInt()) else value.roundToInt().toString()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(if (hasSamples) statistics else stringResource(R.string.ui_no_samples),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        selectedFraction?.let(charts::nearest)?.let { point ->
            val value = charts.value(point, measure)
            val precise = SimpleDateFormat(clockFormat.toPattern().let { if (it.contains("ss")) it else it.replace("mm", "mm:ss") },
                Locale.getDefault()).apply { timeZone = clockFormat.timeZone }
            Text("${precise.format(Date(point.clock))} · ${value?.let(::label) ?: "—"} $unit",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Text(stringResource(R.string.ui_chart_reference_legend), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().height(256.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.width(66.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End) {
                Text(label(if (measure == ResultMeasure.SPLIT) scale.low else scale.high), fontSize = 12.sp)
                Text(label((scale.low + scale.high) / 2f), fontSize = 12.sp)
                Text(label(if (measure == ResultMeasure.SPLIT) scale.high else scale.low), fontSize = 12.sp)
            }
            Box(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(8.dp))) {
                if (!hasSamples) Text(stringResource(R.string.ui_no_recorded_samples),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Center))
                else Canvas(Modifier.fillMaxSize().semantics { contentDescription = chartDescription }
                    .pointerInput(charts) { detectTapGestures { onSelect((it.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)) } }) {
                    fun x(clock: Long) = size.width * charts.clockFraction(clock)
                    fun y(value: Float) = 4.dp.toPx() + (size.height - 8.dp.toPx()) * scale.fraction(value, measure == ResultMeasure.SPLIT)
                    clipRect {
                        charts.phases.forEach { phase ->
                            val left = x(charts.clockAt(phase.start)); val right = x(charts.clockAt(phase.end))
                            val color = colors.getValue(phase.difficulty)
                            drawRect(color.copy(alpha = if (phase.difficulty == Difficulty.REST) .12f else .07f),
                                Offset(left, 0f), androidx.compose.ui.geometry.Size((right - left).coerceAtLeast(0f), size.height))
                            drawRect(color, Offset(left, 0f), androidx.compose.ui.geometry.Size((right - left).coerceAtLeast(0f), 5.dp.toPx()))
                            if (phase.start > 0f) drawLine(gridColor, Offset(left, 0f), Offset(left, size.height), 1.dp.toPx())
                            measure.target(phase.segment)?.let { target ->
                                drawLine(referenceColor, Offset(left, y(target)), Offset(right, y(target)),
                                    1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 5.dp.toPx())))
                            }
                        }
                        if (charts.hasStartup) {
                            drawRect(startupColor.copy(alpha = .5f), Offset.Zero,
                                androidx.compose.ui.geometry.Size(x(charts.clockAt(charts.startupEnd)), size.height))
                        }
                        charts.pauseRanges.forEach { (start, end) ->
                            drawRect(gridColor.copy(alpha = .5f), Offset(x(start), 0f),
                                androidx.compose.ui.geometry.Size(x(end) - x(start), size.height))
                        }
                        listOf(0f, .5f, 1f).forEach { fraction ->
                            drawLine(gridColor, Offset(0f, y(scale.low + (scale.high - scale.low) * fraction)),
                                Offset(size.width, y(scale.low + (scale.high - scale.low) * fraction)), 1.dp.toPx())
                        }
                        average?.takeIf { it in scale.low..scale.high }?.let {
                            drawLine(referenceColor, Offset(0f, y(it)), Offset(size.width, y(it)), 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx())))
                        }
                        charts.points.forEachIndexed { index, point ->
                            val value = charts.value(point, measure)
                            val before = charts.points.getOrNull(index - 1)
                            if (value != null && !charts.isRest(point)) {
                                val color = colors.getValue(charts.phase(point)?.difficulty ?: Difficulty.NONE)
                                val startup = charts.hasStartup && !includeStartup && point.elapsed < charts.startupEnd
                                val previous = before?.let { charts.value(it, measure) }
                                val offScaleStartup = startup && (value !in scale.low..scale.high ||
                                    (before != null && before.elapsed < charts.startupEnd && previous != null && previous !in scale.low..scale.high))
                                if (before != null && previous != null && !charts.isRest(before) && !charts.breaksBefore(index) && !offScaleStartup) {
                                    drawLine(color.copy(alpha = if (startup) .35f else 1f), Offset(x(before.clock), y(previous)),
                                        Offset(x(point.clock), y(value)), 2.dp.toPx())
                                } else drawCircle(color, 2.dp.toPx(), Offset(x(point.clock), y(value)))
                                if (startup && value !in scale.low..scale.high) {
                                    // Explicit edge marks indicate off-scale startup readings, never a fabricated flat trace.
                                    drawCircle(referenceColor, 2.dp.toPx(), Offset(x(point.clock), y(value)))
                                }
                            }
                        }
                        selectedFraction?.let { fraction ->
                            val point = charts.nearest(fraction)
                            val cx = point?.let { x(it.clock) } ?: size.width * fraction
                            drawLine(referenceColor, Offset(cx, 0f), Offset(cx, size.height), 1.dp.toPx())
                            point?.let { charts.value(it, measure) }?.let { drawCircle(chartColor, 4.dp.toPx(), Offset(cx, y(it))) }
                        }
                    }
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 74.dp)) {
            val showMiddle = maxWidth >= 300.dp
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(clockFormat.format(Date(charts.start)), fontSize = 12.sp)
                if (showMiddle) Text(clockFormat.format(Date(charts.start + charts.span / 2)), fontSize = 12.sp)
                Text(clockFormat.format(Date(charts.end)), fontSize = 12.sp)
            }
        }
        Text(unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun IntervalResults(charts: WorkoutChartData, clockFormat: SimpleDateFormat, onSelect: (ResultPhase) -> Unit) {
    val colors = effortColors()
    Text(stringResource(R.string.ui_chart_intervals), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    if (charts.phases.any { it.estimated }) Text(stringResource(R.string.ui_chart_estimated_intervals),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    charts.phases.forEach { phase ->
        val result = remember(charts, phase) { charts.intervalResult(phase) }
        Card(onClick = { onSelect(phase) }, modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).background(colors.getValue(phase.difficulty), RoundedCornerShape(2.dp)))
                    Text(phaseName(phase), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text("${clockFormat.format(Date(charts.clockAt(phase.start)))} – ${clockFormat.format(Date(charts.clockAt(phase.end)))} · ${effortName(phase.difficulty)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ResultMetricRow(stringResource(R.string.ui_time_rowed), formatAxisTime(result.seconds))
                ResultMetricRow(stringResource(R.string.ui_distance), "%,d m".format(result.meters))
                if (phase.difficulty != Difficulty.REST) {
                    ResultMetricRow(stringResource(R.string.ui_average_split), result.split?.let(::formatSplit) ?: "—")
                    ResultMetricRow(stringResource(R.string.ui_average_power), result.power?.let { "$it W" } ?: "—")
                    ResultMetricRow(stringResource(R.string.ui_average_stroke_rate), result.rate?.let { "$it SPM" } ?: "—")
                    val goal = ResultMeasure.entries.firstNotNullOfOrNull { measure ->
                        measure.target(phase.segment)?.let { target -> measure to target }
                    }
                    if (goal != null) {
                        val (measure, target) = goal
                        val actual = when (measure) { ResultMeasure.SPLIT -> result.split; ResultMeasure.POWER -> result.power; ResultMeasure.RATE -> result.rate }
                        val unit = when (measure) { ResultMeasure.SPLIT -> "s /500 m"; ResultMeasure.POWER -> "W"; ResultMeasure.RATE -> "SPM" }
                        val targetText = if (measure == ResultMeasure.SPLIT) formatSplit(target.roundToInt()) else "${target.roundToInt()} $unit"
                        ResultMetricRow(stringResource(R.string.ui_chart_target), targetText)
                        if (actual != null) ResultMetricRow(stringResource(R.string.ui_chart_target_difference),
                            "%+d %s".format(actual - target.roundToInt(), unit))
                    }
                }
            }
        }
    }
}

private fun formatAxisTime(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)
