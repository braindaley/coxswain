package svenmeier.coxswain.compose.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import svenmeier.coxswain.Gym
import svenmeier.coxswain.R
import svenmeier.coxswain.gym.Measurement
import svenmeier.coxswain.gym.Difficulty
import svenmeier.coxswain.gym.Segment
import svenmeier.coxswain.view.ValueBinding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveRowScreen(
    gym: Gym,
    refreshTick: Int = 0,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit
) {
    // Reading this state makes live measurements invalidate the metric grid.
    @Suppress("UNUSED_VARIABLE") val measurementVersion = refreshTick
    val isPaused = gym.isPaused
    var editMode by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) }
    val context = LocalContext.current
    
    val activeMetrics = remember {
        val fallback = listOf(
            ValueBinding.DURATION,
            ValueBinding.DISTANCE,
            ValueBinding.SPLIT,
            ValueBinding.STROKE_RATE,
            ValueBinding.POWER,
            ValueBinding.PULSE)
        val saved = context.getSharedPreferences("live_row", 0).getString("metrics", null)
            ?.split(",")?.mapNotNull { runCatching { ValueBinding.valueOf(it) }.getOrNull() }
        mutableStateListOf(*(if (saved?.size == 6) saved else fallback).toTypedArray())
    }
    if (editMode && editingIndex >= 0) {
        AlertDialog(onDismissRequest = { editingIndex = -1 }, title = { Text(stringResource(R.string.ui_choose_metric)) },
            text = { Column { listOf(ValueBinding.DURATION, ValueBinding.DISTANCE, ValueBinding.SPLIT, ValueBinding.STROKE_RATE, ValueBinding.POWER, ValueBinding.PULSE, ValueBinding.SPEED, ValueBinding.ENERGY).forEach { metric ->
                Row(Modifier.fillMaxWidth().clickable { activeMetrics[editingIndex] = metric; editingIndex = -1; context.getSharedPreferences("live_row", 0).edit().putString("metrics", activeMetrics.joinToString(",") { it.name }).apply() }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = activeMetrics[editingIndex] == metric, onClick = null); Text(metric.name.replace('_', ' '), color = Color.White)
                }
            } } }, confirmButton = { TextButton(onClick = { editingIndex = -1 }) { Text(stringResource(R.string.ui_done)) } })
    }

    Scaffold(
        containerColor = Color(0xFF042C3D), 
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (gym.program != null) gym.program.name.get().uppercase() else stringResource(R.string.ui_free_row).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFC8E3E9)
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF22C55E).copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = if (gym.connected) stringResource(R.string.ui_connected_status, gym.connectedRowerName ?: stringResource(R.string.ui_connected)) else stringResource(R.string.ui_disconnected_status),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = if (gym.connected) Color(0xFF22C55E) else Color(0xFFFF7185),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { editMode = !editMode }) {
                        Text(if (editMode) stringResource(R.string.ui_done) else stringResource(R.string.ui_edit_display), color = Color(0xFFDCEBFF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { if (isPaused) onResume() else onPause() },
                    modifier = Modifier.weight(1f).height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B63F6)),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(if (isPaused) stringResource(R.string.ui_resume) else stringResource(R.string.ui_pause), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = onEnd,
                    modifier = Modifier.weight(1f).height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCEBFF)),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(stringResource(R.string.ui_end_session), color = Color(0xFF10213F), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            
            val active = gym.progress?.segment
            if (gym.program?.segments?.get()?.size ?: 0 > 1) IntervalStrip(gym)
            if (gym.pace != null) RaceComparison(gym)
            if (active?.difficulty?.get() == Difficulty.REST) RestCountdown(gym, Modifier.weight(1f)) else LazyVerticalGrid(
                columns = GridCells.Fixed(2), modifier = Modifier.weight(1f).background(Color(0xFF31505D)),
                horizontalArrangement = Arrangement.spacedBy(1.dp), verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                itemsIndexed(activeMetrics) { index, binding ->
                    MetricCell(binding, gym.getMeasurement(), goalDisplay(binding, active, gym.getMeasurement()), editMode) { editingIndex = index }
                }
            }
            
            if (gym.program != null) {
                TargetProgressBar(gym)
            }
        }
    }
}

@Composable
fun MetricCell(
    binding: ValueBinding,
    measurement: Measurement,
    goal: GoalDisplay? = null,
    editable: Boolean = false,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val valueStr = binding.format(context, getValueForBinding(binding, measurement), false)
    val label = context.getString(binding.label).uppercase()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(when { goal == null -> Color(0xFF042C3D); goal.state > 0 -> Color(0xFF073E34); goal.state < 0 -> Color(0xFF4A2028); else -> Color(0xFF123F51) })
            .clickable(enabled = editable) { onClick() }
            .semantics { contentDescription = if (goal == null) "$label metric, $valueStr" else "$label goal variance ${goal.variance}, current $valueStr, target ${goal.target}" }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = goal?.variance ?: valueStr,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 56.sp,
                    fontWeight = FontWeight.W500,
                    textAlign = TextAlign.Center
                ),
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (goal == null) label else "$label  $valueStr",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFCAD4E1),
                fontWeight = FontWeight.Bold
            )
            if (goal != null) {
                Spacer(Modifier.height(3.dp))
                Text(stringResource(R.string.ui_target_value, goal.target), fontSize = 12.sp, color = Color(0xFFB5D3DE))
            }
        }
    }
}

@Composable
private fun IntervalStrip(gym: Gym) {
    val segments = gym.program?.segments?.get().orEmpty()
    val active = gym.progress?.segment
    val activeIndex = segments.indexOfFirst { it === active }.coerceAtLeast(0)
    val intervalDescription = stringResource(R.string.ui_interval_position, activeIndex + 1, segments.size)
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF123F51))
            .semantics { contentDescription = intervalDescription }
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        segments.forEach { segment ->
            Box(Modifier.weight(1f).height(34.dp).background(if (segment.difficulty.get() == Difficulty.REST) Color(0xFF6D8792) else Color(0xFF0B63F6), MaterialTheme.shapes.small), contentAlignment = Alignment.Center) { Text(if (segment.difficulty.get() == Difficulty.REST) stringResource(R.string.ui_rest) else stringResource(R.string.ui_row), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = if (segment === active) 1f else .65f)) }
        }
    }
}

@Composable
private fun RestCountdown(gym: Gym, modifier: Modifier = Modifier) {
    val progress = gym.progress
    val segment = progress?.segment
    val segments = gym.program?.segments?.get().orEmpty()
    val display = restDisplay(segments, segment, progress?.completion() ?: 0f)
    val nextText = segments.getOrNull(display.position)?.let { stringResource(R.string.ui_next_row, segmentTarget(it)) }
        ?: stringResource(R.string.ui_final_segment)
    val restDescription = stringResource(R.string.ui_rest_accessibility, display.position, display.total, display.remaining, nextText)
    Column(modifier.fillMaxWidth().background(Color(0xFF123F51)).semantics {
        contentDescription = restDescription
    }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.ui_segment_of, display.position, display.total), color = Color(0xFF83D7FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.ui_rest), color = Color(0xFFB5D3DE), fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text(display.remaining, color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Bold)
        Text(nextText, color = Color(0xFF83D7FF), fontSize = 18.sp)
    }
}

data class RestDisplay(val position: Int, val total: Int, val remaining: String, val next: String)

internal fun restDisplay(segments: List<Segment>, active: Segment?, completion: Float): RestDisplay {
    val index = segments.indexOfFirst { it === active }.coerceAtLeast(0)
    val remaining = if ((active?.duration?.get() ?: 0) > 0) {
        val seconds = (active!!.duration.get() * (1f - completion)).toInt().coerceAtLeast(0)
        "%d:%02d".format(seconds / 60, seconds % 60)
    } else "${((active?.getTarget() ?: 0) * (1f - completion)).toInt().coerceAtLeast(0)} m"
    val next = segments.getOrNull(index + 1)
    return RestDisplay(index + 1, segments.size, remaining, if (next == null) "Final segment" else "Next: ${segmentTarget(next)} row")
}

data class GoalDisplay(val variance: String, val target: String, val state: Int)

internal fun goalDisplay(binding: ValueBinding, segment: Segment?, measurement: Measurement): GoalDisplay? {
    if (segment == null || segment.getLimit() <= 0) return null
    return when {
        binding == ValueBinding.STROKE_RATE && segment.strokeRate.get() > 0 -> signedGoal(measurement.strokeRate - segment.strokeRate.get(), "", "${segment.strokeRate.get()}")
        binding == ValueBinding.POWER && segment.power.get() > 0 -> signedGoal(measurement.power - segment.power.get(), " W", "${segment.power.get()} W")
        binding == ValueBinding.SPEED && segment.speed.get() > 0 -> {
            val difference = measurement.speed - segment.speed.get()
            GoalDisplay(String.format(java.util.Locale.getDefault(), "%+.1f m/s", difference / 100f), String.format(java.util.Locale.getDefault(), "%.1f m/s", segment.speed.get() / 100f), stateFor(difference, 5))
        }
        binding == ValueBinding.SPLIT && segment.speed.get() > 0 && measurement.speed > 0 -> {
            val targetPace = 50000 / segment.speed.get()
            val actualPace = 50000 / measurement.speed
            signedGoal(targetPace - actualPace, " s", "%d:%02d /500 m".format(targetPace / 60, targetPace % 60))
        }
        else -> null
    }
}

private fun signedGoal(value: Int, unit: String, target: String) = GoalDisplay(if (value > 0) "+$value$unit" else "$value$unit", target, stateFor(value, 1))
private fun stateFor(value: Int, tolerance: Int): Int = when { value > tolerance -> 1; value < -tolerance -> -1; else -> 0 }

private fun segmentTarget(segment: Segment): String = when {
    segment.duration.get() > 0 -> "%d:%02d".format(segment.duration.get() / 60, segment.duration.get() % 60)
    segment.distance.get() > 0 -> "${segment.distance.get()} m"
    segment.strokes.get() > 0 -> "${segment.strokes.get()} strokes"
    else -> "${segment.energy.get()} kcal"
}

@Composable
private fun RaceComparison(gym: Gym) {
    val pace = gym.pace ?: return
    val state = raceDisplay(gym.getMeasurement(), pace, gym.program)
    val leadDistance = kotlin.math.abs(state.leadMeters)
    val raceDescription = pluralStringResource(if (state.leadMeters >= 0) R.plurals.ui_race_comparison_ahead else R.plurals.ui_race_comparison_behind, leadDistance, leadDistance)
    Column(Modifier.fillMaxWidth().background(Color(0xFF123F51)).semantics {
        contentDescription = raceDescription
    }.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RaceLine(stringResource(R.string.ui_you), state.currentProgress, Color(0xFF0B8FFF))
        RaceLine(stringResource(R.string.ui_best), state.bestProgress, Color(0xFF9CAFC0))
        Text(stringResource(if (state.leadMeters >= 0) R.string.ui_race_ahead else R.string.ui_race_behind, kotlin.math.abs(state.leadMeters)), Modifier.align(Alignment.End), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (state.leadMeters >= 0) Color(0xFF4ADE80) else Color(0xFFFF9AAA))
    }
}

data class RaceDisplay(val currentProgress: Float, val bestProgress: Float, val leadMeters: Int)

internal fun raceDisplay(current: Measurement, pace: svenmeier.coxswain.gym.Workout, program: svenmeier.coxswain.gym.Program?): RaceDisplay {
    val expectedDistance = if (pace.duration.get() > 0) pace.distance.get() * current.duration.toFloat() / pace.duration.get() else 0f
    val distanceRace = program?.getSegmentsCount() == 1 && (program.getSegment(0).distance.get() > 0)
    val target = if (distanceRace) program!!.getSegment(0).distance.get().coerceAtLeast(1) else pace.distance.get().coerceAtLeast(1)
    return RaceDisplay((current.distance.toFloat() / target).coerceIn(0f, 1f), (expectedDistance / target).coerceIn(0f, 1f), current.distance - expectedDistance.toInt())
}

@Composable
private fun RaceLine(label: String, progress: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.width(42.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCAD4E1))
        LinearProgressIndicator(progress = { progress }, Modifier.weight(1f).height(7.dp), color = color, trackColor = Color(0xFF53647C), strokeCap = StrokeCap.Round)
        Text("${(progress * 100).toInt()}%", fontSize = 11.sp, color = Color.White)
    }
}

@Composable
fun TargetProgressBar(gym: Gym) {
    val completionPercent = ((gym.progress?.completion() ?: 0f) * 100).toInt()
    val progressDescription = pluralStringResource(R.plurals.ui_workout_progress_accessibility, completionPercent, completionPercent, gym.progress?.describe() ?: stringResource(R.string.ui_ready))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF123F51))
            .semantics { contentDescription = progressDescription }
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ui_workout_progress), style = MaterialTheme.typography.labelSmall, color = Color(0xFFCAD4E1))
            Text("$completionPercent%", style = MaterialTheme.typography.labelSmall, color = Color(0xFF83D7FF))
        }
        Text(
            text = gym.progress?.describe() ?: stringResource(R.string.ui_ready),
            style = MaterialTheme.typography.titleLarge, 
            color = Color.White,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        LinearProgressIndicator(
            progress = { gym.progress?.completion() ?: 0f },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Color(0xFF0B8FFF),
            trackColor = Color(0xFF53647C),
            strokeCap = StrokeCap.Round
        )
    }
}

private fun getValueForBinding(binding: ValueBinding, m: Measurement): Int {
    return when (binding) {
        ValueBinding.DURATION -> m.duration
        ValueBinding.DISTANCE -> m.distance
        ValueBinding.STROKES -> m.strokes
        ValueBinding.ENERGY -> m.energy
        ValueBinding.SPEED -> m.speed
        ValueBinding.PULSE -> m.pulse
        ValueBinding.STROKE_RATE -> m.strokeRate
        ValueBinding.POWER -> m.power
        ValueBinding.SPLIT -> m.speed
        else -> 0
    }
}
