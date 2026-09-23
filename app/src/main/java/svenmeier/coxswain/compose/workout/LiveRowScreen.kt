package svenmeier.coxswain.compose.workout

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
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
import svenmeier.coxswain.gym.Difficulty
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
    onEditMetric: (Int) -> Unit = {}
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

    var isEditingDisplay by remember { mutableStateOf(false) }
    var selectedSlotIndex by remember { mutableIntStateOf(-1) }

    val sessionTitle = remember(gym.program, gym.pace) {
        when {
            gym.pace != null -> "RACE YOUR BEST"
            gym.program != null -> gym.program.name.get()?.uppercase() ?: "WORKOUT"
            else -> "FREE ROW"
        }
    }

    Scaffold(
        containerColor = Color(0xFF042C3D),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sessionTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC8E3E9)
                        )
                        Spacer(Modifier.width(10.dp))

                        val (statusText, statusColor) = when {
                            gym.connected -> "● Connected" to Color(0xFF22C55E)
                            gym.connecting -> "● Connecting..." to Color(0xFFF2BA00)
                            else -> "● Disconnected" to Color(0xFFBA1A1A)
                        }

                        Surface(
                            color = statusColor.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = statusText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { isEditingDisplay = !isEditingDisplay },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF53647C)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFDCEBFF)
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isEditingDisplay) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isEditingDisplay) "Done" else "Edit display",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isPaused = gym.isPaused
                Button(
                    onClick = { if (isPaused) onResume() else onPause() },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B63F6)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isPaused) stringResource(R.string.ui_resume) else stringResource(R.string.ui_pause),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onEnd,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDCEBFF),
                        contentColor = Color(0xFF10213F)
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ui_end_session),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF31505D)),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                itemsIndexed(activeMetrics) { index, binding ->
                    MetricCell(
                        binding = binding,
                        gym = gym,
                        isEditing = isEditingDisplay,
                        isSelected = (selectedSlotIndex == index),
                        onClick = {
                            if (isEditingDisplay) {
                                selectedSlotIndex = index
                            } else {
                                onEditMetric(index)
                            }
                        }
                    )
                }
            }

            if (gym.pace != null) {
                RaceProgressBar(gym)
            } else if (gym.program != null && gym.progress != null) {
                TargetProgressBar(gym)
            }
        }
    }

    if (isEditingDisplay && selectedSlotIndex >= 0) {
        MetricPickerDialog(
            positionIndex = selectedSlotIndex,
            currentBinding = activeMetrics[selectedSlotIndex],
            onDismiss = { selectedSlotIndex = -1 },
            onSelect = { newBinding ->
                activeMetrics[selectedSlotIndex] = newBinding
                selectedSlotIndex = -1
            }
        )
    }
}

@Composable
fun MetricCell(
    binding: ValueBinding,
    gym: Gym,
    isEditing: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val rawValue = getValueForBinding(binding, gym)
    val valueStr = binding.format(context, rawValue, false)
    val (titleLabel, unitLabel) = getMetricTitleAndUnit(binding, context)

    val modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF042C3D))
        .then(
            if (isEditing && isSelected) {
                Modifier.border(2.dp, Color(0xFF0B63F6))
            } else Modifier
        )
        .clickable { onClick() }
        .padding(16.dp)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = valueStr,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                ),
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (unitLabel.isNotEmpty()) "$titleLabel · $unitLabel" else titleLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = Color(0xFFCAD4E1)
            )
        }
    }
}

@Composable
fun TargetProgressBar(gym: Gym) {
    val progress = gym.progress ?: return
    val program = gym.program
    val m = gym.getMeasurement()
    val segment = progress.segment
    val startM = progress.startMeasurement

    val completion = progress.completion()
    val percentInt = (completion * 100).toInt().coerceIn(0, 100)

    val primaryText = when {
        segment.duration.get() > 0 -> {
            val remSec = maxOf(0, segment.duration.get() - (m.duration - startM.duration))
            String.format(Locale.getDefault(), "%d:%02d remaining", remSec / 60, remSec % 60)
        }
        segment.distance.get() > 0 -> {
            val remDist = maxOf(0, segment.distance.get() - (m.distance - startM.distance))
            String.format(Locale.getDefault(), "%,d m remaining", remDist)
        }
        segment.strokes.get() > 0 -> {
            val remStrokes = maxOf(0, segment.strokes.get() - (m.strokes - startM.strokes))
            String.format(Locale.getDefault(), "%,d strokes remaining", remStrokes)
        }
        else -> progress.describe()
    }

    val (leftMeta, rightMeta) = when {
        segment.distance.get() > 0 -> {
            val done = m.distance - startM.distance
            String.format(Locale.getDefault(), "%,d m complete", done) to String.format(Locale.getDefault(), "%,d m target", segment.distance.get())
        }
        segment.duration.get() > 0 -> {
            val done = m.duration - startM.duration
            String.format(Locale.getDefault(), "%d:%02d elapsed", done / 60, done % 60) to String.format(Locale.getDefault(), "%d:%02d target", segment.duration.get() / 60, segment.duration.get() % 60)
        }
        else -> "" to ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF123F51))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if ((program?.getSegmentsCount() ?: 0) > 1) "TIMED INTERVALS" else "WORKOUT PROGRESS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCAD4E1),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "$percentInt%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF83D7FF)
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = primaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { completion.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = Color(0xFF0B8FFF),
                trackColor = Color(0xFF53647C),
                strokeCap = StrokeCap.Round
            )

            if (leftMeta.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(leftMeta, fontSize = 11.sp, color = Color(0xFFCAD4E1))
                    Text(rightMeta, fontSize = 11.sp, color = Color(0xFFCAD4E1))
                }
            }
        }
    }
}

@Composable
fun RaceProgressBar(gym: Gym) {
    val pace = gym.pace ?: return
    val current = gym.getMeasurement()
    val race = raceDisplay(current, pace, gym.program)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF123F51))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RACE YOUR BEST",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCAD4E1),
                    letterSpacing = 0.5.sp
                )
                val deltaStr = if (race.leadMeters >= 0) "+${race.leadMeters} m ahead" else "${race.leadMeters} m behind"
                Text(
                    text = deltaStr,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (race.leadMeters >= 0) Color(0xFF83D7FF) else Color(0xFFE53935)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.ui_you),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.width(50.dp)
                )
                LinearProgressIndicator(
                    progress = { race.currentProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp),
                    color = Color(0xFF0B8FFF),
                    trackColor = Color(0xFF53647C),
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = " %,d m".format(Locale.getDefault(), current.distance),
                    fontSize = 12.sp,
                    color = Color(0xFFCAD4E1),
                    modifier = Modifier.width(70.dp),
                    textAlign = TextAlign.End
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.ui_best),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFBFEAFF),
                    modifier = Modifier.width(50.dp)
                )
                LinearProgressIndicator(
                    progress = { race.bestProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp),
                    color = Color(0xFF83D7FF),
                    trackColor = Color(0xFF53647C),
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "History",
                    fontSize = 12.sp,
                    color = Color(0xFFCAD4E1),
                    modifier = Modifier.width(70.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
fun MetricPickerDialog(
    positionIndex: Int,
    currentBinding: ValueBinding,
    onDismiss: () -> Unit,
    onSelect: (ValueBinding) -> Unit
) {
    val options = listOf(
        ValueBinding.DISTANCE to "Distance (m)",
        ValueBinding.DURATION to "Duration (time)",
        ValueBinding.SPLIT to "Split (/500m)",
        ValueBinding.STROKE_RATE to "Stroke Rate (SPM)",
        ValueBinding.POWER to "Power (W)",
        ValueBinding.PULSE to "Heart Rate (BPM)",
        ValueBinding.ENERGY to "Calories (kcal)",
        ValueBinding.STROKES to "Stroke Count",
        ValueBinding.SPEED to "Speed (km/h)",
        ValueBinding.AVERAGE_SPLIT to "Average Split (/500m)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Display Slot ${positionIndex + 1}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (binding, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(binding) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = if (binding == currentBinding) FontWeight.Bold else FontWeight.Normal,
                            color = if (binding == currentBinding) Color(0xFF0B63F6) else Color(0xFF10213F)
                        )
                        if (binding == currentBinding) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0B63F6))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ui_cancel))
            }
        }
    )
}

private fun getMetricTitleAndUnit(binding: ValueBinding, context: Context): Pair<String, String> {
    return when (binding) {
        ValueBinding.DISTANCE -> "DISTANCE" to "M"
        ValueBinding.DURATION -> "DURATION" to "ELAPSED"
        ValueBinding.SPLIT -> "SPLIT" to "/500 M"
        ValueBinding.STROKE_RATE -> "STROKE RATE" to "SPM"
        ValueBinding.POWER -> "POWER" to "W"
        ValueBinding.PULSE -> "HEART RATE" to "BPM"
        ValueBinding.ENERGY -> "CALORIES" to "KCAL"
        ValueBinding.STROKES -> "STROKES" to "COUNT"
        ValueBinding.SPEED -> "SPEED" to "KM/H"
        ValueBinding.AVERAGE_SPLIT -> "AVG SPLIT" to "/500 M"
        else -> context.getString(binding.label).uppercase() to ""
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
