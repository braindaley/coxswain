package svenmeier.coxswain

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
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
    val context = LocalContext.current
    val preferences = remember(context) { androidx.preference.PreferenceManager.getDefaultSharedPreferences(context) }
    var currentHeart by remember(context) { mutableStateOf(HeartRateZones.decode(HeartRateZones.freeze(context))) }
    var currentOutput by remember(context) { mutableStateOf(PerformanceZones.decode(PerformanceZones.freeze(context))) }
    DisposableEffect(preferences, context) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == HeartRateZones.KEY) currentHeart = HeartRateZones.decode(HeartRateZones.freeze(context))
            if (key == PerformanceZones.KEY) currentOutput = PerformanceZones.decode(PerformanceZones.freeze(context))
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val profiles = WorkoutZoneProfiles.resolve(workout, currentHeart, currentOutput)
    val zones = profiles.heart
    val outputs = profiles.output
    val zoneTimes = remember(summary, zones) { zones?.let { HeartRateZoneTimes(it, summary.samples) } }
    val charts = remember(summary) { WorkoutChartData(workout, summary) }
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
        ResultChart(stringResource(R.string.ui_split_time), ResultMeasure.SPLIT, charts, clockFormat,
            stringResource(R.string.ui_chart_average_best, averageSplit, if (splits.isEmpty()) "—" else formatSplit(splits.minOrNull()!!)), outputZones = outputs?.pace, currentOutput = profiles.currentOutput)
        ResultChart(stringResource(R.string.ui_power), ResultMeasure.POWER, charts, clockFormat,
            stringResource(R.string.ui_chart_average_max, "$avgPower W", "$maxPower W"), outputZones = outputs?.power, currentOutput = profiles.currentOutput)
        ResultChart(stringResource(R.string.ui_stroke_rate), ResultMeasure.RATE, charts, clockFormat,
            stringResource(R.string.ui_chart_stroke_statistics, "$avgRate SPM", rates.minOrNull()?.toString() ?: "—", rates.maxOrNull()?.toString() ?: "—", summary.workStrokes))
        if (summary.pulse.average != null) {
            ResultChart(stringResource(R.string.ui_heart_rate), ResultMeasure.PULSE,
            charts, clockFormat, stringResource(R.string.ui_chart_heart_statistics,
                summary.pulse.average!!, summary.pulse.minimum!!, summary.pulse.maximum!!), zones,
                currentHeart = profiles.currentHeart, zoneTimes = zoneTimes)
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
    val levels = heartZoneColors()
    return mapOf(Difficulty.NONE to levels[0],
        Difficulty.REST to MaterialTheme.colorScheme.onSurfaceVariant,
        Difficulty.EASY to levels[0], Difficulty.MEDIUM to levels[1],
        Difficulty.HARD to levels[2], Difficulty.PEAK to levels[3])
}

@Composable
private fun heartZoneColors(): List<Color> {
    val dark = MaterialTheme.colorScheme.surface.luminance() < .4f
    return listOf(MaterialTheme.colorScheme.primary,
        if (dark) Color(0xFF54CDD0) else Color(0xFF007D83),
        if (dark) Color(0xFFFFCB74) else Color(0xFFB07800),
        if (dark) Color(0xFF73DFA3) else Color(0xFF217B45))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ChartLevelLegend(times: HeartRateZoneTimes? = null) {
    val colors = heartZoneColors()
    if (times != null) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..3).forEach { zone ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(heartZoneName(zone)), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(6.dp).background(colors[zone], androidx.compose.foundation.shape.CircleShape))
                        Text(formatAxisTime(times.seconds[zone].roundToInt()),
                            style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    } else {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..3).forEach { zone ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).background(colors[zone], androidx.compose.foundation.shape.CircleShape))
                    Text(stringResource(heartZoneName(zone)), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ResultChart(title: String, measure: ResultMeasure, charts: WorkoutChartData,
                        clockFormat: SimpleDateFormat, statistics: String,
                        zones: HeartRateZones? = null, outputZones: OutputZones? = null,
                        currentOutput: Boolean = false, currentHeart: Boolean = false,
                        zoneTimes: HeartRateZoneTimes? = null) {
    val levelGuides = remember(zones, outputZones) { resultLevelGuides(zones, outputZones) }
    val scale = remember(charts, measure, levelGuides) { charts.scale(measure, levelGuides) }
    val displayValues = remember(charts, measure) { charts.displayValues(measure) }
    val zoneColors = heartZoneColors()
    val primaryColor = MaterialTheme.colorScheme.primary
    val referenceColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val includesRest = measure == ResultMeasure.PULSE
    val hasSamples = charts.points.any { (includesRest || !charts.isRest(it)) && (charts.plottedValue(it, measure) ?: 0f) > 0f }
    val unit = when (measure) { ResultMeasure.SPLIT -> "/500 m"; ResultMeasure.POWER -> "W"; ResultMeasure.RATE -> "SPM"; ResultMeasure.PULSE -> "BPM" }
    val chartDescription = stringResource(R.string.ui_chart_accessibility, title, statistics)
    var showInfo by remember { mutableStateOf(false) }
    fun label(value: Float) = if (measure == ResultMeasure.SPLIT) formatAxisTime(value.roundToInt()) else value.roundToInt().toString()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(unit, style = MaterialTheme.typography.bodySmall, color = referenceColor)
        }
        IconButton(onClick = { showInfo = true }) {
            Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.chart_info, title), tint = referenceColor)
        }
    }
    val chartBackground = if (MaterialTheme.colorScheme.surface.luminance() < .4f)
        MaterialTheme.colorScheme.surfaceContainerHigh else Color(0xFFEEF3F9)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = chartBackground)) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth().height(190.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (!hasSamples) Text(stringResource(R.string.ui_no_recorded_samples),
                        color = referenceColor, modifier = Modifier.align(Alignment.Center))
                    else Canvas(Modifier.fillMaxSize().semantics { contentDescription = chartDescription }) {
                        fun x(clock: Long) = size.width * charts.clockFraction(clock)
                        fun y(value: Float) = 6.dp.toPx() + (size.height - 12.dp.toPx()) * scale.fraction(value, measure == ResultMeasure.SPLIT)
                        clipRect {
                            charts.phases.forEach { phase ->
                                val left = x(charts.clockAt(phase.start)); val right = x(charts.clockAt(phase.end))
                                if (phase.difficulty == Difficulty.REST) drawRect(referenceColor.copy(alpha = .06f),
                                    Offset(left, 0f), androidx.compose.ui.geometry.Size((right - left).coerceAtLeast(0f),size.height))
                                if (phase.start > 0f) drawLine(gridColor.copy(alpha = .6f), Offset(left,size.height - 7.dp.toPx()),
                                    Offset(left,size.height),1.dp.toPx())
                            }
                            charts.pauseRanges.forEach { (start,end) ->
                                drawRect(gridColor.copy(alpha = .25f), Offset(x(start),0f),
                                    androidx.compose.ui.geometry.Size(x(end)-x(start),size.height))
                            }
                            levelGuides.forEachIndexed { zone, guide ->
                                val spacing = 5.dp.toPx()
                                val count = (size.width / spacing).toInt().coerceAtLeast(1)
                                for (dot in 0..count) drawCircle(zoneColors[zone], .75.dp.toPx(),
                                    Offset(size.width * dot / count, y(guide)))
                            }
                            charts.points.forEachIndexed { index,point ->
                                val value = displayValues[index]
                                val before = charts.points.getOrNull(index-1)
                                if (value != null && (includesRest || !charts.isRest(point))) {
                                    val color = if (measure == ResultMeasure.RATE) referenceColor
                                        else if (includesRest) zones?.zone(value)?.let { zoneColors[it] } ?: primaryColor
                                        else outputZones?.zone(value)?.let { zoneColors[it] } ?: primaryColor
                                    val previous = displayValues.getOrNull(index-1)
                                    if (before != null && previous != null && (includesRest || !charts.isRest(before)) &&
                                        !charts.breaksBefore(index,measure) && (!includesRest || point.elapsed-before.elapsed <= 5f)) {
                                        fun connected(left: Int, right: Int): Boolean {
                                            val a = charts.points.getOrNull(left) ?: return false
                                            val b = charts.points.getOrNull(right) ?: return false
                                            return displayValues.getOrNull(left) != null && displayValues.getOrNull(right) != null &&
                                                (includesRest || (!charts.isRest(a) && !charts.isRest(b))) &&
                                                a.interval == b.interval && !charts.breaksBefore(right,measure) &&
                                                b.clock > a.clock && (!includesRest || b.elapsed-a.elapsed <= 5f)
                                        }
                                        val leftValue = if (connected(index-2,index-1)) displayValues[index-2] else null
                                        val rightValue = if (connected(index,index+1)) displayValues[index+1] else null
                                        val curve = ResultCurve(previous,value,leftValue,rightValue,
                                            beforeSpan = if (leftValue != null) (before.clock-charts.points[index-2].clock).toFloat() else 1f,
                                            span = (point.clock-before.clock).coerceAtLeast(1L).toFloat(),
                                            afterSpan = if (rightValue != null) (charts.points[index+1].clock-point.clock).toFloat() else 1f)
                                        val x0 = x(before.clock); val x1 = x(point.clock)
                                        val steps = kotlin.math.ceil((x1-x0)/2.dp.toPx()).toInt().coerceIn(4,32)
                                        for (step in 0 until steps) {
                                            val f0 = step.toFloat()/steps; val f1 = (step+1).toFloat()/steps
                                            val v0 = curve.value(f0); val v1 = curve.value(f1)
                                            val pieces = if (includesRest && zones != null) zones.pieces(v0,v1)
                                                else if (outputZones != null && v0>0f && v1>0f) outputZones.pieces(v0,v1) else null
                                            if (pieces != null) pieces.forEach { (a,b,zone) ->
                                                drawLine(zoneColors[zone],
                                                    Offset(x0+(x1-x0)*(f0+(f1-f0)*a),y(v0+(v1-v0)*a)),
                                                    Offset(x0+(x1-x0)*(f0+(f1-f0)*b),y(v0+(v1-v0)*b)),
                                                    1.5.dp.toPx(),cap = StrokeCap.Round)
                                            } else drawLine(color,Offset(x0+(x1-x0)*f0,y(v0)),Offset(x0+(x1-x0)*f1,y(v1)),
                                                1.5.dp.toPx(),cap = StrokeCap.Round)
                                        }
                                    } else drawCircle(color,1.dp.toPx(),Offset(x(point.clock),y(value)))
                                }
                            }
                        }
                    }
                }
                val axisValues = if (levelGuides.isNotEmpty()) levelGuides else
                    listOf(scale.low,(scale.low+scale.high)/2,scale.high)
                androidx.compose.ui.layout.Layout(
                    content = { axisValues.forEach { value -> Text(label(value),fontSize = 12.sp,color = referenceColor) } },
                    modifier = Modifier.width(52.dp).fillMaxHeight()
                ) { measurables, constraints ->
                    val labels = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
                    layout(constraints.maxWidth,constraints.maxHeight) {
                        labels.forEachIndexed { index, text ->
                            val center = 6.dp.toPx() + (constraints.maxHeight-12.dp.toPx()) *
                                scale.fraction(axisValues[index],measure == ResultMeasure.SPLIT)
                            text.placeRelative(0,(center-text.height/2).roundToInt().coerceIn(0,(constraints.maxHeight-text.height).coerceAtLeast(0)))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(end = 60.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(clockFormat.format(Date(charts.start)),fontSize = 12.sp,color = referenceColor)
                Text(clockFormat.format(Date(charts.end)),fontSize = 12.sp,color = referenceColor)
            }
            if (zones != null || outputZones != null) ChartLevelLegend(if (includesRest) zoneTimes else null)
        }
    }
    if (showInfo) AlertDialog(onDismissRequest = { showInfo = false },title = { Text(title) },
        text = { Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(statistics)
            Text(stringResource(R.string.chart_info_help))
            Text(stringResource(R.string.chart_filtered_help))
            Text(stringResource(R.string.ui_chart_started_finished,
                clockFormat.format(Date(charts.start)), clockFormat.format(Date(charts.end))))
            if (charts.estimatedClock) Text(stringResource(R.string.ui_chart_estimated_clock))
            if (currentOutput) Text(stringResource(R.string.ui_chart_current_output_zones))
            if (currentHeart) Text(stringResource(R.string.ui_chart_current_heart_zones))
            if (zones != null) {
                (0..3).forEach { zone ->
                    Text("${stringResource(heartZoneName(zone))}: ${zones.bounds(zone)}")
                    if (zoneTimes != null) Text("${formatAxisTime(zoneTimes.seconds[zone].roundToInt())} · ${zoneTimes.percent(zone).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (zoneTimes != null) Text(stringResource(R.string.hr_zone_coverage,formatAxisTime(zoneTimes.total.roundToInt())))
            } else if (outputZones != null) {
                val values = listOf(outputZones.moderate,outputZones.vigorous,outputZones.peak)
                (1..3).forEach { zone -> Text("${stringResource(heartZoneName(zone))}: ${if (measure == ResultMeasure.SPLIT) formatZoneSplit(values[zone-1]) else values[zone-1].toString()} $unit") }
            } else if (measure != ResultMeasure.RATE) Text(stringResource(if (includesRest) R.string.hr_zone_legacy else R.string.output_zones_legacy))
        } },confirmButton = { TextButton(onClick = { showInfo = false }) { Text(stringResource(R.string.ui_done)) } })
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
