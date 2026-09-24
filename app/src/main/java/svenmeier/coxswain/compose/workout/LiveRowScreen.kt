package svenmeier.coxswain.compose.workout

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.text.rememberTextMeasurer
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
    onEnd: () -> Unit
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

    val progress = gym.progress
    val activeSegment = progress?.segment
    val segments = gym.program?.getSegments().orEmpty()
    val isRest = segments.size > 1 && activeSegment?.difficulty?.get() == Difficulty.REST
    val rest = if (isRest && activeSegment != null && progress != null) {
        restDisplay(segments, activeSegment, progress.completion())
    } else null
    val sessionTitle = when {
        rest != null -> "REST INTERVAL"
        gym.pace != null -> "RACE YOUR BEST"
        gym.program != null -> gym.program.name.get()?.uppercase() ?: "WORKOUT"
        else -> "FREE ROW"
    }
    val goalBinding = activeSegment?.let(::goalBinding)
    val goal = if (goalBinding != null) goalDisplay(goalBinding, activeSegment, gym.getMeasurement()) else null
    var countdownTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(rest != null, gym.isPaused) {
        if (rest != null && !gym.isPaused) {
            while (true) {
                delay(250)
                countdownTick++
            }
        }
    }
    @Suppress("UNUSED_VARIABLE") val keepCountdownAlive = countdownTick
    val countdownBaseTime = remember(rest?.remaining, gym.isPaused) { System.currentTimeMillis() }
    val visibleRest = rest?.let { value ->
        if (value.remaining.contains(':')) {
            val elapsed = if (gym.isPaused) 0 else ((System.currentTimeMillis() - countdownBaseTime) / 1000L).toInt()
            val baseSeconds = (value.remaining.substringBefore(':').toIntOrNull() ?: 0) * 60 +
                (value.remaining.substringAfter(':').toIntOrNull() ?: 0)
            value.copy(remaining = formatClock((baseSeconds - elapsed).coerceAtLeast(0)))
        } else value
    }
    LaunchedEffect(goalBinding) {
        if (goalBinding != null && goalBinding !in activeMetrics) {
            activeMetrics[3.coerceAtMost(activeMetrics.lastIndex)] = goalBinding
        }
    }
    LaunchedEffect(rest != null) {
        if (rest != null) {
            isEditingDisplay = false
            selectedSlotIndex = -1
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
                    if (rest == null) {
                        OutlinedButton(
                            onClick = { isEditingDisplay = !isEditingDisplay; selectedSlotIndex = -1 },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF53647C)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDCEBFF)),
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
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
        Column(Modifier.padding(innerPadding).fillMaxSize().background(Color(0xFF042C3D))) {
            if (visibleRest != null) {
                RestCountdownCard(visibleRest, Modifier.weight(1f).fillMaxWidth().padding(12.dp))
            } else {
                MetricGrid(
                    metrics = activeMetrics,
                    gym = gym,
                    goalBinding = goalBinding,
                    goal = goal,
                    isEditing = isEditingDisplay,
                    selectedSlotIndex = selectedSlotIndex,
                    onMetricSelected = { selectedSlotIndex = it },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }

            if (gym.pace != null) {
                RaceProgressBar(gym)
            } else if (gym.program != null && progress != null) {
                TargetProgressBar(gym, visibleRest)
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
private fun MetricGrid(
    metrics: List<ValueBinding>,
    gym: Gym,
    goalBinding: ValueBinding?,
    goal: GoalDisplay?,
    isEditing: Boolean,
    selectedSlotIndex: Int,
    onMetricSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BoxWithConstraints(modifier.background(Color(0xFF31505D))) {
        val rows = (metrics.size + 1) / 2
        val cellWidth = (maxWidth - 1.dp) / 2
        val cellHeight = (maxHeight - (rows - 1).dp) / rows
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val values = metrics.map { binding ->
            val rawValue = getValueForBinding(binding, gym)
            val value = when {
                binding == goalBinding && goal != null -> goal.variance.substringBefore(' ')
                binding == ValueBinding.DISTANCE -> java.text.NumberFormat.getIntegerInstance(Locale.getDefault()).format(rawValue)
                else -> binding.format(context, rawValue, false)
            }
            value
        }
        val availableWidthPx = with(density) { (cellWidth - 24.dp).roundToPx() }.coerceAtLeast(1)
        val availableHeightPx = with(density) { (cellHeight - 52.dp).roundToPx() }.coerceAtLeast(1)
        val baseMeasures = values.map { value ->
            textMeasurer.measure(AnnotatedString(value), style = TextStyle(fontSize = 100.sp, fontWeight = FontWeight.Medium)).size
        }
        val widest = baseMeasures.maxOfOrNull { it.width }?.coerceAtLeast(1) ?: 1
        val tallest = baseMeasures.maxOfOrNull { it.height }?.coerceAtLeast(1) ?: 1
        val sharedFontSize = (100f * minOf(availableWidthPx.toFloat() / widest, availableHeightPx.toFloat() / tallest))
            .coerceIn(12f, 112f).sp

        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            metrics.chunked(2).forEachIndexed { rowIndex, rowMetrics ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    rowMetrics.forEachIndexed { columnIndex, binding ->
                        val index = rowIndex * 2 + columnIndex
                        val cellGoal = if (binding == goalBinding) goal else null
                        MetricCell(
                            binding = binding,
                            gym = gym,
                            isEditing = isEditing,
                            isSelected = selectedSlotIndex == index,
                            onClick = { onMetricSelected(index) },
                            displayValue = values[index],
                            displayLabel = if (cellGoal != null) {
                                "${getMetricTitleAndUnit(binding, context).first} · TO TARGET"
                            } else null,
                            goalState = cellGoal?.state,
                            targetDescription = cellGoal?.target,
                            valueFontSize = sharedFontSize,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    if (rowMetrics.size == 1) Spacer(Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
fun MetricCell(
    binding: ValueBinding,
    gym: Gym,
    isEditing: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    displayValue: String? = null,
    displayLabel: String? = null,
    goalState: Int? = null,
    targetDescription: String? = null,
    valueFontSize: TextUnit = 52.sp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rawValue = getValueForBinding(binding, gym)
    val valueStr = displayValue ?: binding.format(context, rawValue, false)
    val (titleLabel, unitLabel) = getMetricTitleAndUnit(binding, context)

    val cellColor = when (goalState) {
        -1 -> Color(0xFF7A2836)
        0, 1 -> Color(0xFF126B4D)
        else -> Color(0xFF042C3D)
    }
    val accessibleDescription = if (goalState != null && targetDescription != null) {
        "$titleLabel goal variance $valueStr, target $targetDescription"
    } else "$titleLabel $valueStr"
    val cellModifier = modifier
        .background(cellColor)
        .then(
            if (isEditing && isSelected) {
                Modifier.border(3.dp, Color(0xFF0B8FFF))
            } else Modifier
        )
        .clickable(enabled = isEditing) { onClick() }
        .semantics { contentDescription = accessibleDescription }
        .padding(horizontal = 8.dp, vertical = 10.dp)

    Box(
        modifier = cellModifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = valueStr,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = valueFontSize * 1.02f
                ),
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = displayLabel ?: if (unitLabel.isNotEmpty()) "$titleLabel · $unitLabel" else titleLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = Color(0xFFCAD4E1),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
        if (isEditing) {
            Text(
                text = "Tap to change",
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 5.dp),
                color = Color(0xFF83D7FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TargetProgressBar(gym: Gym, rest: RestDisplay? = null) {
    val progress = gym.progress ?: return
    val program = gym.program
    val m = gym.getMeasurement()
    val segment = progress.segment
    val startM = progress.startMeasurement
    val segments = program?.getSegments().orEmpty()
    val isIntervals = segments.size > 1
    val isRest = rest != null

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

    val currentIndex = segments.indexOfFirst { it === segment }.coerceAtLeast(0)
    val currentOrdinal = segments.take(currentIndex + 1).count { it.difficulty.get() != Difficulty.REST }.coerceAtLeast(1)
    val currentTitle = if (isRest) "Rest $currentOrdinal" else "Row $currentOrdinal"
    val intervalPrimary = "$currentTitle · $primaryText"
    val intervalPercent = when {
        segment.duration.get() > 0 -> "${formatClock(m.duration - startM.duration)} / ${formatClock(segment.duration.get())}"
        segment.distance.get() > 0 -> String.format(Locale.getDefault(), "%,d / %,d m", m.distance - startM.distance, segment.distance.get())
        else -> "$percentInt%"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF123F51))
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ui_workout_progress),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCAD4E1),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = if (isIntervals) intervalPercent else "$percentInt%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF83D7FF)
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (isIntervals) intervalPrimary else primaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )

            if (isIntervals) {
                IntervalSequence(segments, segment, completion)
            } else {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { completion.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
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
}

@Composable
private fun IntervalSequence(segments: List<Segment>, active: Segment, completion: Float) {
    val weights = segments.map { it.asDuration().coerceAtLeast(1).toFloat() }
    val total = weights.sum().coerceAtLeast(1f)
    val activeIndex = segments.indexOfFirst { it === active }.coerceAtLeast(0)
    val position = (weights.take(activeIndex).sum() + weights[activeIndex] * completion.coerceIn(0f, 1f)) / total

    BoxWithConstraints(Modifier.fillMaxWidth().height(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(10.dp).align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            segments.forEachIndexed { index, segment ->
                val isRest = segment.difficulty.get() == Difficulty.REST
                Box(
                    Modifier.weight(weights[index]).fillMaxHeight()
                        .background(
                            color = if (isRest) Color(0xFF83D7FF) else Color(0xFF0B63F6),
                            shape = RoundedCornerShape(5.dp)
                        )
                )
            }
        }
        Box(
            Modifier.align(Alignment.CenterStart).offset(x = maxWidth * position - 1.dp)
                .width(3.dp).height(18.dp)
                .background(Color.White, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun RestCountdownCard(rest: RestDisplay, modifier: Modifier = Modifier) {
    val nextTarget = rest.next.removePrefix("Next: ").removeSuffix(" row")
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B3B4E))
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = rest.remaining,
                fontSize = 92.sp,
                lineHeight = 96.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.ui_rest_remaining).uppercase(Locale.getDefault()),
                color = Color(0xFFCAD4E1), fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = if (rest.next.startsWith("Next:")) "Next · Row $nextTarget" else rest.next,
                color = Color(0xFF83D7FF), fontSize = 14.sp
            )
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
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RACE PROGRESS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCAD4E1),
                    letterSpacing = 0.5.sp
                )
                val deltaStr = if (race.leadMeters >= 0) "You are ${race.leadMeters} m ahead" else "You are ${-race.leadMeters} m behind"
                Text(
                    text = deltaStr,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (race.leadMeters >= 0) Color(0xFF83D7FF) else Color(0xFFE53935)
                )
            }

            Spacer(Modifier.height(8.dp))
            RaceLane(
                label = stringResource(R.string.ui_you),
                progress = race.currentProgress,
                value = "%,d m".format(Locale.getDefault(), current.distance),
                color = Color(0xFF0B8FFF)
            )
            Spacer(Modifier.height(5.dp))
            RaceLane(
                label = stringResource(R.string.ui_best),
                progress = race.bestProgress,
                value = "%,d m".format(Locale.getDefault(), race.bestMeters),
                color = Color(0xFF83D7FF)
            )
        }
    }
}

@Composable
private fun RaceLane(label: String, progress: Float, value: String, color: Color) {
    val markerProgress = progress.coerceIn(0.05f, 0.95f)
    Row(
        modifier = Modifier.fillMaxWidth().height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.width(42.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (label.equals("Best", ignoreCase = true)) Color(0xFFBFEAFF) else Color.White
        )
        BoxWithConstraints(Modifier.weight(1f).height(12.dp), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0xFF53647C), RoundedCornerShape(3.dp)))
            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).background(color, RoundedCornerShape(3.dp)))
            Box(
                Modifier.offset(x = (maxWidth * markerProgress) - 6.dp)
                    .size(12.dp).border(2.dp, Color.White, RoundedCornerShape(50)).background(color, RoundedCornerShape(50))
            )
        }
        Text(
            text = value,
            modifier = Modifier.widthIn(min = 58.dp),
            fontSize = 11.sp,
            color = Color(0xFFCAD4E1),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
        )
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
        ValueBinding.SPEED to "Speed (m/s)",
        ValueBinding.STROKE_RATIO to "Stroke ratio",
        ValueBinding.TIME to "Time of day",
        ValueBinding.AVERAGE_SPLIT to "Average Split (/500m)",
        ValueBinding.DELTA_DISTANCE to "Distance delta",
        ValueBinding.DELTA_DURATION to "Time delta"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change metric ${positionIndex + 1}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (binding, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = binding == currentBinding,
                                onClick = { onSelect(binding) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(selected = binding == currentBinding, onClick = null)
                        Text(label, fontSize = 15.sp, fontWeight = if (binding == currentBinding) FontWeight.Bold else FontWeight.Normal)
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
        ValueBinding.SPEED -> "SPEED" to "M/S"
        ValueBinding.AVERAGE_SPLIT -> "AVG SPLIT" to "/500 M"
        ValueBinding.STROKE_RATIO -> "STROKE RATIO" to "DRIVE : RECOVERY"
        ValueBinding.TIME -> "TIME" to "CLOCK"
        ValueBinding.DELTA_DISTANCE -> "DISTANCE DELTA" to "M"
        ValueBinding.DELTA_DURATION -> "TIME DELTA" to "S"
        else -> context.getString(binding.label).uppercase() to ""
    }
}

private fun goalBinding(segment: Segment): ValueBinding? = when {
    segment.strokeRate.get() > 0 -> ValueBinding.STROKE_RATE
    segment.speed.get() > 0 -> ValueBinding.SPEED
    segment.power.get() > 0 -> ValueBinding.POWER
    segment.pulse.get() > 0 -> ValueBinding.PULSE
    else -> null
}

private fun formatClock(seconds: Int): String {
    val value = seconds.coerceAtLeast(0)
    return "%d:%02d".format(Locale.getDefault(), value / 60, value % 60)
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
        binding == ValueBinding.PULSE && segment.pulse.get() > 0 -> signedGoal(measurement.pulse - segment.pulse.get(), "", "${segment.pulse.get()} BPM")
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

data class RaceDisplay(val currentProgress: Float, val bestProgress: Float, val leadMeters: Int, val bestMeters: Int)

internal fun raceDisplay(current: Measurement, pace: Workout, program: Program?): RaceDisplay {
    val expectedDistance = if (pace.duration.get() > 0) pace.distance.get() * current.duration.toFloat() / pace.duration.get() else 0f
    val distanceRace = program?.getSegmentsCount() == 1 && (program.getSegment(0).distance.get() > 0)
    val target = if (distanceRace) program.getSegment(0).distance.get().coerceAtLeast(1) else pace.distance.get().coerceAtLeast(1)
    return RaceDisplay(
        (current.distance.toFloat() / target).coerceIn(0f, 1f),
        (expectedDistance / target).coerceIn(0f, 1f),
        current.distance - expectedDistance.toInt(),
        expectedDistance.toInt()
    )
}

internal fun getValueForBinding(binding: ValueBinding, gym: Gym): Int {
    val m = gym.getMeasurement()
    val progress = gym.progress
    val segment = progress?.segment ?: gym.program?.let { program ->
        if (program.getSegmentsCount() > 0) program.getSegment(0) else null
    }
    if (segment != null) {
        val start = progress?.startMeasurement ?: Measurement()
        val (target, completed) = when (binding) {
            ValueBinding.DURATION -> segment.duration.get() to (m.duration - start.duration)
            ValueBinding.DISTANCE -> segment.distance.get() to (m.distance - start.distance)
            ValueBinding.STROKES -> segment.strokes.get() to (m.strokes - start.strokes)
            ValueBinding.ENERGY -> segment.energy.get() to (m.energy - start.energy)
            else -> 0 to 0
        }
        if (target > 0) return (target - completed).coerceAtLeast(0)
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
        ValueBinding.STROKE_RATIO -> m.strokeRatio
        ValueBinding.TIME -> {
            val calendar = java.util.Calendar.getInstance()
            calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE)
        }
        ValueBinding.SPLIT -> if (m.speed > 0) 50000 / m.speed else 0
        ValueBinding.AVERAGE_SPLIT -> if (m.distance > 0) m.duration * 500 / m.distance else 0
        ValueBinding.DELTA_DISTANCE -> {
            val pace = gym.pace
            if (pace != null && pace.duration.get() > 0) {
                m.distance - (pace.distance.get() * m.duration / pace.duration.get())
            } else 0
        }
        ValueBinding.DELTA_DURATION -> {
            val pace = gym.pace
            if (pace != null && pace.distance.get() > 0) {
                m.duration - (pace.duration.get() * m.distance / pace.distance.get())
            } else 0
        }
        else -> 0
    }
}
