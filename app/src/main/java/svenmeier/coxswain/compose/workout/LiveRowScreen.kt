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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import svenmeier.coxswain.Gym
import svenmeier.coxswain.R
import svenmeier.coxswain.gym.Measurement
import svenmeier.coxswain.gym.Program
import svenmeier.coxswain.gym.Segment
import svenmeier.coxswain.gym.Workout
import svenmeier.coxswain.view.ValueBinding
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveRowScreen(
    gym: Gym,
    refreshTick: Int = 0, // Force recomposition on each gym tick
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit,
    onEditMetric: (Int) -> Unit
) {
    @Suppress("UNUSED_VARIABLE") val refresh = refreshTick
    
    val activeMetrics = remember {
        mutableStateListOf(
            ValueBinding.DURATION,
            ValueBinding.DISTANCE,
            ValueBinding.SPLIT,
            ValueBinding.STROKE_RATE,
            ValueBinding.POWER,
            ValueBinding.PULSE
        )
    }

    Scaffold(
        containerColor = Color(0xFF042C3D), 
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (gym.program != null) gym.program.name.get().uppercase() else "FREE ROW",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFC8E3E9)
                        )
                        Spacer(Modifier.width(8.dp))
                        
                        // Connection Indicator
                        val (statusText, statusColor) = when {
                            gym.connected -> "● Connected" to Color(0xFF22C55E)
                            gym.connecting -> "● Connecting..." to Color(0xFFF2BA00)
                            else -> "● Disconnected" to Color(0xFFBA1A1A)
                        }
                        
                        Surface(
                            color = statusColor.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = statusText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { /* Edit Display */ }) {
                        Text("Edit display", color = Color(0xFFDCEBFF))
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isPaused = gym.isPaused
                Button(
                    onClick = { if (isPaused) onResume() else onPause() },
                    modifier = Modifier.weight(1f).height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B63F6)),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(if (isPaused) "Resume" else "Pause", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = onEnd,
                    modifier = Modifier.weight(1f).height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCEBFF)),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text("End session", color = Color(0xFF10213F), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).background(Color(0xFF31505D)),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                itemsIndexed(activeMetrics) { index, binding ->
                    MetricCell(
                        binding = binding,
                        gym = gym,
                        onClick = { onEditMetric(index) }
                    )
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
    gym: Gym,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val valueStr = binding.format(context, getValueForBinding(binding, gym), false)
    val label = context.getString(binding.label).uppercase()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF042C3D))
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = valueStr,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 56.sp,
                    fontWeight = FontWeight.W500,
                    textAlign = TextAlign.Center
                ),
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFCAD4E1),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TargetProgressBar(gym: Gym) {
    val progress = gym.progress
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF123F51))
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("WORKOUT PROGRESS", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCAD4E1))
            val percent = if (progress != null) (progress.completion() * 100).toInt() else 0
            Text("$percent%", style = MaterialTheme.typography.labelSmall, color = Color(0xFF83D7FF))
        }
        Text(
            text = if (progress != null) progress.describe() else "Ready", 
            style = MaterialTheme.typography.titleLarge, 
            color = Color.White,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        LinearProgressIndicator(
            progress = { progress?.completion() ?: 0f },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Color(0xFF0B8FFF),
            trackColor = Color(0xFF53647C),
            strokeCap = StrokeCap.Round
        )
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
            GoalDisplay(String.format(Locale.getDefault(), "%+.1f m/s", difference / 100f), String.format(
                Locale.getDefault(), "%.1f m/s", segment.speed.get() / 100f), stateFor(difference, 5))
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

data class RaceDisplay(val currentProgress: Float, val bestProgress: Float, val leadMeters: Int)

internal fun raceDisplay(current: Measurement, pace: Workout, program: Program?): RaceDisplay {
    val expectedDistance = if (pace.duration.get() > 0) pace.distance.get() * current.duration.toFloat() / pace.duration.get() else 0f
    val distanceRace = program?.getSegmentsCount() == 1 && (program.getSegment(0).distance.get() > 0)
    val target = if (distanceRace) program.getSegment(0).distance.get().coerceAtLeast(1) else pace.distance.get().coerceAtLeast(1)
    return RaceDisplay((current.distance.toFloat() / target).coerceIn(0f, 1f), (expectedDistance / target).coerceIn(0f, 1f), current.distance - expectedDistance.toInt())
}

internal fun getValueForBinding(binding: ValueBinding, gym: Gym): Int {
    val m = gym.getMeasurement()
    val progress = gym.progress
    if (progress != null) {
        val startM = progress.startMeasurement
        val segment = progress.segment
        when (binding) {
            ValueBinding.DURATION -> {
                val target = segment.duration.get()
                if (target > 0) {
                    val achieved = m.duration - startM.duration
                    return maxOf(0, target - achieved)
                }
            }
            ValueBinding.DISTANCE -> {
                val target = segment.distance.get()
                if (target > 0) {
                    val achieved = m.distance - startM.distance
                    return maxOf(0, target - achieved)
                }
            }
            ValueBinding.STROKES -> {
                val target = segment.strokes.get()
                if (target > 0) {
                    val achieved = m.strokes - startM.strokes
                    return maxOf(0, target - achieved)
                }
            }
            ValueBinding.ENERGY -> {
                val target = segment.energy.get()
                if (target > 0) {
                    val achieved = m.energy - startM.energy
                    return maxOf(0, target - achieved)
                }
            }
            else -> {}
        }
    }

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
