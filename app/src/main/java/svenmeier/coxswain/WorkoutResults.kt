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
    val zones = remember(workout) { HeartRateZones.decode(workout.heartRateZones.get()) }
    val outputs = remember(workout) { PerformanceZones.decode(workout.performanceZones.get()) }
    val zoneTimes = remember(summary, zones) { zones?.let { HeartRateZoneTimes(it, summary.samples) } }
    val charts = remember(summary) { WorkoutChartData(workout, summary) }
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
        if (summary.pulse.average != null) {
            ResultMetricRow(stringResource(R.string.ui_average_heart_rate), "${summary.pulse.average} BPM")
            ResultMetricRow(stringResource(R.string.ui_minimum_heart_rate), "${summary.pulse.minimum} BPM")
            ResultMetricRow(stringResource(R.string.ui_maximum_heart_rate), "${summary.pulse.maximum} BPM")
        }
        Text(stringResource(R.string.ui_chart_started_finished, preciseFormat.format(Date(charts.start)), preciseFormat.format(Date(charts.end))),
            style = MaterialTheme.typography.bodyMedium)
        if (charts.estimatedClock) Text(stringResource(R.string.ui_chart_estimated_clock),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (charts.phases.size > 1) ProgramEffortLegend(charts)
        ResultChart(stringResource(R.string.ui_split_time), ResultMeasure.SPLIT, charts, clockFormat, summary.averageSplit?.toFloat(),
            stringResource(R.string.ui_chart_average_best, averageSplit, if (splits.isEmpty()) "—" else formatSplit(splits.minOrNull()!!)), outputZones = outputs?.pace)
        ResultChart(stringResource(R.string.ui_power), ResultMeasure.POWER, charts, clockFormat, summary.averagePower?.toFloat(),
            stringResource(R.string.ui_chart_average_max, "$avgPower W", "$maxPower W"), outputZones = outputs?.power)
        ResultChart(stringResource(R.string.ui_stroke_rate), ResultMeasure.RATE, charts, clockFormat, summary.averageRate?.toFloat(),
            stringResource(R.string.ui_chart_stroke_statistics, "$avgRate SPM", rates.minOrNull()?.toString() ?: "—", rates.maxOrNull()?.toString() ?: "—", summary.workStrokes))
        if (summary.pulse.average != null) {
            if (zones != null && zoneTimes != null) HeartZoneSummary(zones, zoneTimes)
            else Text(stringResource(R.string.hr_zone_legacy), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ResultChart(stringResource(R.string.ui_heart_rate), ResultMeasure.PULSE,
            charts, clockFormat, summary.pulse.average?.toFloat(), stringResource(R.string.ui_chart_heart_statistics,
                summary.pulse.average!!, summary.pulse.minimum!!, summary.pulse.maximum!!), zones)
        }
        if (charts.phases.size > 1) IntervalResults(charts, preciseFormat)
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

@Composable
private fun ResultMetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

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
        Difficulty.EASY to MaterialTheme.colorScheme.primary,
        Difficulty.MEDIUM to if (dark) Color(0xFF73DFA3) else Color(0xFF217B45),
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
private fun heartZoneColors(): List<Color> {
    val dark = MaterialTheme.colorScheme.surface.luminance() < .4f
    return listOf(MaterialTheme.colorScheme.primary,
        if (dark) Color(0xFF73DFA3) else Color(0xFF217B45),
        if (dark) Color(0xFFFFCB74) else Color(0xFF986600), MaterialTheme.colorScheme.error)
}

@Composable
private fun HeartZoneSummary(zones: HeartRateZones, times: HeartRateZoneTimes) {
    val colors = heartZoneColors()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.hr_zone_time), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.hr_zone_coverage, formatAxisTime(times.total.roundToInt())),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        (0..3).forEach { zone ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ResultMetricRow(stringResource(heartZoneName(zone)),
                    "${formatAxisTime(times.seconds[zone].roundToInt())} · ${times.percent(zone).roundToInt()}%")
                Text(zones.bounds(zone), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = { times.percent(zone) / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp), color = colors[zone],
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun OutputZoneLegend(zones: OutputZones) {
    val colors = heartZoneColors()
    Text(stringResource(R.string.output_zones_legend), style = MaterialTheme.typography.bodySmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        (0..3).forEach { zone ->
            val threshold = when (zone) { 0, 1 -> zones.moderate; 2 -> zones.vigorous; else -> zones.peak }
            val value = if (zones.fasterIsLower) "${formatZoneSplit(threshold)} /500 m" else "$threshold W"
            val relation = if (zones.fasterIsLower) { if (zone == 0) ">" else "≤" } else { if (zone == 0) "<" else "≥" }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).background(colors[zone], RoundedCornerShape(2.dp)))
                Text("${stringResource(heartZoneName(zone))} $relation $value", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ResultChart(title: String, measure: ResultMeasure, charts: WorkoutChartData,
                        clockFormat: SimpleDateFormat, average: Float?, statistics: String, zones: HeartRateZones? = null, outputZones: OutputZones? = null) {
    val scale = remember(charts, measure) { charts.scale(measure) }
    val colors = effortColors()
    val zoneColors = heartZoneColors()
    val pulseColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val referenceColor = MaterialTheme.colorScheme.onSurfaceVariant
    val includesRest = measure == ResultMeasure.PULSE
    val hasSamples = charts.points.any { (includesRest || !charts.isRest(it)) && (charts.plottedValue(it, measure) ?: 0f) > 0f }
    val unit = when (measure) { ResultMeasure.SPLIT -> "/500 m"; ResultMeasure.POWER -> "W"; ResultMeasure.RATE -> "SPM"; ResultMeasure.PULSE -> "BPM" }
    val chartDescription = stringResource(R.string.ui_chart_accessibility, title, statistics)
    fun label(value: Float) = if (measure == ResultMeasure.SPLIT) formatAxisTime(value.roundToInt()) else value.roundToInt().toString()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(if (hasSamples) statistics else stringResource(R.string.ui_no_samples),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.ui_chart_reference_legend), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (outputZones != null) OutputZoneLegend(outputZones)
        else if (measure == ResultMeasure.POWER || measure == ResultMeasure.SPLIT) Text(
            stringResource(R.string.output_zones_legacy), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (includesRest && zones != null) Text(stringResource(R.string.hr_zone_line_help),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                else Canvas(Modifier.fillMaxSize().semantics { contentDescription = chartDescription }) {
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
                            val value = charts.plottedValue(point, measure)
                            val before = charts.points.getOrNull(index - 1)
                            if (value != null && (includesRest || !charts.isRest(point))) {
                                val color = if (measure == ResultMeasure.RATE) referenceColor
                                    else if (includesRest) zones?.zone(value)?.let { zoneColors[it] } ?: pulseColor
                                    else outputZones?.zone(value)?.let { zoneColors[it] } ?: pulseColor
                                val previous = before?.let { charts.plottedValue(it, measure) }
                                if (before != null && previous != null && (includesRest || !charts.isRest(before)) && !charts.breaksBefore(index, measure) &&
                                    (!includesRest || point.elapsed - before.elapsed <= 5f)) {
                                    if (includesRest && zones != null) zones.pieces(previous, value).forEach { (a, b, zone) ->
                                        val x0 = x(before.clock); val x1 = x(point.clock)
                                        drawLine(zoneColors[zone], Offset(x0 + (x1 - x0) * a, y(previous + (value - previous) * a)),
                                            Offset(x0 + (x1 - x0) * b, y(previous + (value - previous) * b)), 2.dp.toPx())
                                    } else if (outputZones != null && previous > 0f && value > 0f) outputZones.pieces(previous, value).forEach { (a, b, zone) ->
                                        val x0 = x(before.clock); val x1 = x(point.clock)
                                        drawLine(zoneColors[zone], Offset(x0 + (x1 - x0) * a, y(previous + (value - previous) * a)),
                                            Offset(x0 + (x1 - x0) * b, y(previous + (value - previous) * b)), 2.dp.toPx())
                                    } else drawLine(color, Offset(x(before.clock), y(previous)),
                                        Offset(x(point.clock), y(value)), 2.dp.toPx())
                                } else drawCircle(color, 2.dp.toPx(), Offset(x(point.clock), y(value)))
                            }
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
private fun IntervalResults(charts: WorkoutChartData, clockFormat: SimpleDateFormat) {
    val colors = effortColors()
    Text(stringResource(R.string.ui_chart_intervals), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    if (charts.phases.any { it.estimated }) Text(stringResource(R.string.ui_chart_estimated_intervals),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    charts.phases.forEach { phase ->
        val result = remember(charts, phase) { charts.intervalResult(phase) }
        Card(modifier = Modifier.fillMaxWidth(),
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
                if (result.pulse != null) ResultMetricRow(stringResource(R.string.ui_average_heart_rate), "${result.pulse} BPM")
                if (phase.difficulty != Difficulty.REST) {
                    ResultMetricRow(stringResource(R.string.ui_average_split), result.split?.let(::formatSplit) ?: "—")
                    ResultMetricRow(stringResource(R.string.ui_average_power), result.power?.let { "$it W" } ?: "—")
                    ResultMetricRow(stringResource(R.string.ui_average_stroke_rate), result.rate?.let { "$it SPM" } ?: "—")
                    val goal = ResultMeasure.entries.firstNotNullOfOrNull { measure ->
                        measure.target(phase.segment)?.let { target -> measure to target }
                    }
                    if (goal != null) {
                        val (measure, target) = goal
                        val actual = when (measure) { ResultMeasure.SPLIT -> result.split; ResultMeasure.POWER -> result.power; ResultMeasure.RATE -> result.rate; ResultMeasure.PULSE -> result.pulse }
                        val unit = when (measure) { ResultMeasure.SPLIT -> "s /500 m"; ResultMeasure.POWER -> "W"; ResultMeasure.RATE -> "SPM"; ResultMeasure.PULSE -> "BPM" }
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
