package svenmeier.coxswain.compose.workout

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.text.rememberTextMeasurer
import svenmeier.coxswain.HeartRateZones
import svenmeier.coxswain.heartZoneName
import svenmeier.coxswain.trainingZoneTileColors
import svenmeier.coxswain.Gym
import svenmeier.coxswain.R
import svenmeier.coxswain.gym.Difficulty
import svenmeier.coxswain.gym.IntervalRailReplay
import svenmeier.coxswain.gym.Measurement
import svenmeier.coxswain.gym.Program
import svenmeier.coxswain.gym.Segment
import svenmeier.coxswain.gym.Workout
import svenmeier.coxswain.gym.WorkoutDefinition
import svenmeier.coxswain.pete.peteLiveInstruction
import svenmeier.coxswain.view.ValueBinding
import java.util.Locale
import kotlin.math.roundToInt

private const val LIVE_ROW_DISPLAY_PREFERENCES = "live_row_display"
private const val LIVE_ROW_METRICS_KEY = "metric_bindings"
private val defaultLiveRowMetrics = listOf(
    ValueBinding.DURATION,
    ValueBinding.DISTANCE,
    ValueBinding.SPLIT,
    ValueBinding.STROKE_RATE,
    ValueBinding.POWER,
    ValueBinding.PULSE
)

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
    val context = LocalContext.current

    val activeMetrics = remember(context) {
        mutableStateListOf<ValueBinding>().apply {
            addAll(loadLiveRowMetrics(context))
        }
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
        gym.isPlanSession -> "PETE'S PLAN"
        gym.pace != null -> "RACE YOUR BEST"
        gym.program != null -> gym.program.name.get()?.uppercase() ?: "WORKOUT"
        else -> "FREE ROW"
    }
    val smoother = remember(gym.sessionGeneration, activeSegment) { LiveGoalSmoother() }
    val feedbackMeasurement = smoother.update(gym.getMeasurement())
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
                    Text(
                        text = sessionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC8E3E9),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        val (statusText, statusColor) = when {
                            gym.connected -> "Connected" to Color(0xFF6DE0A8)
                            gym.connecting -> "Connecting" to Color(0xFFFFD166)
                            else -> "Disconnected" to Color(0xFFFF8A80)
                        }
                        Box(
                            Modifier.size(9.dp).background(statusColor, CircleShape)
                                .semantics { contentDescription = statusText }
                        )

                        if (rest == null) {
                            OutlinedButton(
                                onClick = { isEditingDisplay = !isEditingDisplay; selectedSlotIndex = -1 },
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFF60758A)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF123F51), contentColor = Color(0xFFDCEBFF)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    imageVector = if (isEditingDisplay) Icons.Default.Check else Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (isEditingDisplay) "Done" else "Edit",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E69F4), contentColor = Color.White),
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
        Row(Modifier.padding(innerPadding).fillMaxSize().background(Color(0xFF042C3D)).padding(horizontal = 10.dp)) {
            if (gym.program != null || gym.pace != null) {
                SideProgressRail(gym, refreshTick, Modifier.fillMaxHeight().width(if (gym.pace != null || gym.hasPlanPaceTarget()) 30.dp else 24.dp))
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (visibleRest != null) {
                    RestCountdownCard(visibleRest, Modifier.weight(1f).fillMaxWidth())
                } else {
                    MetricGrid(
                        metrics = activeMetrics, gym = gym, refreshTick = refreshTick,
                        feedbackMeasurement = feedbackMeasurement,
                        isEditing = isEditingDisplay,
                        selectedSlotIndex = selectedSlotIndex,
                        onMetricSelected = { selectedSlotIndex = it },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
                LiveRowStatus(gym, visibleRest, refreshTick, feedbackMeasurement, activeMetrics)
            }
        }
    }

    if (isEditingDisplay && selectedSlotIndex >= 0) {
        MetricPickerDialog(
            positionIndex = selectedSlotIndex,
            currentBinding = activeMetrics[selectedSlotIndex],
            onDismiss = { selectedSlotIndex = -1 },
            onSelect = { newBinding ->
                val changed = activeMetrics.toMutableList().apply { this[selectedSlotIndex] = newBinding }
                activeMetrics.clear()
                activeMetrics.addAll(changed)
                saveLiveRowMetrics(context, activeMetrics)
                selectedSlotIndex = -1
            }
        )
    }
}

internal fun loadLiveRowMetrics(context: Context): List<ValueBinding> {
    val stored = context.getSharedPreferences(LIVE_ROW_DISPLAY_PREFERENCES, Context.MODE_PRIVATE)
        .getString(LIVE_ROW_METRICS_KEY, null)
        ?.split(',')
        ?.mapNotNull { name -> runCatching { ValueBinding.valueOf(name) }.getOrNull() }
    return stored?.takeIf { it.size == defaultLiveRowMetrics.size } ?: defaultLiveRowMetrics
}

internal fun saveLiveRowMetrics(context: Context, metrics: List<ValueBinding>) {
    if (metrics.size != defaultLiveRowMetrics.size) return
    context.getSharedPreferences(LIVE_ROW_DISPLAY_PREFERENCES, Context.MODE_PRIVATE)
        .edit()
        .putString(LIVE_ROW_METRICS_KEY, metrics.joinToString(",") { it.name })
        .apply()
}

@Composable
private fun MetricGrid(
    metrics: List<ValueBinding>,
    gym: Gym,
    refreshTick: Int,
    feedbackMeasurement: Measurement,
    isEditing: Boolean,
    selectedSlotIndex: Int,
    onMetricSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    @Suppress("UNUSED_VARIABLE") val refresh = refreshTick
    val context = LocalContext.current
    val active = gym.progress?.segment ?: gym.program?.getSegments()?.firstOrNull()
    val targetBinding = active?.let { goalBinding(it, gym.planGoalKind) }
    val exactSplit = gym.planGoalValue.takeIf { gym.planGoalKind in listOf("SPEED", "SPEED_PHASE") && it > 0 }
    val goal = targetBinding?.let { goalDisplay(it, active, feedbackMeasurement, gym.hasPlanRateCap(), exactSplit) }
    val target = goal?.target?.let { if (targetBinding == ValueBinding.STROKE_RATE && !it.endsWith("SPM")) "$it SPM" else it }
    BoxWithConstraints(modifier.background(Color(0xFF31505D))) {
        val rows = (metrics.size + 1) / 2
        val cellWidth = (maxWidth - 1.dp) / 2
        val cellHeight = (maxHeight - (rows - 1).dp) / rows
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val values = metrics.map { binding ->
            val rawValue = if (binding == targetBinding) when (binding) {
                ValueBinding.STROKE_RATE -> feedbackMeasurement.strokeRate
                ValueBinding.POWER -> feedbackMeasurement.power
                else -> getValueForBinding(binding, gym)
            } else getValueForBinding(binding, gym)
            if (binding == ValueBinding.SPLIT)
                formatMetricValue(binding, if(feedbackMeasurement.speed > 0) (50000f/feedbackMeasurement.speed).roundToInt() else 0, context)
            else formatMetricValue(binding, rawValue, context)
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
                        MetricCell(
                            binding = binding,
                            gym = gym,
                            isEditing = isEditing,
                            isSelected = selectedSlotIndex == index,
                            onClick = { onMetricSelected(index) },
                            displayValue = values[index],
                            goalState = goal?.state.takeIf { binding == targetBinding && rawGoalValue(binding, feedbackMeasurement) > 0 },
                            targetDescription = target.takeIf { binding == targetBinding },
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
    val valueStr = if (binding == ValueBinding.PULSE) binding.format(context, rawValue, false)
        else displayValue ?: binding.format(context, rawValue, false)
    val (titleLabel, unitLabel) = getMetricTitleAndUnit(binding, context)

    val heartZones = if (binding == ValueBinding.PULSE) remember(gym.current) {
        val frozen = gym.current?.heartRateZones?.get()
        (HeartRateZones.decode(frozen) ?: HeartRateZones.decode(HeartRateZones.freeze(context)))?.asTraining()?.takeIf { it.maximum == null || it.maximumConfirmed || it.age != null }
    } else null
    val heartZone = heartZones?.zone(gym.getMeasurement().pulse.toFloat())
    val belowZone = heartZones?.belowTraining(gym.getMeasurement().pulse) == true
    val zoneLabel = if (heartZone != null) stringResource(if(belowZone) R.string.hr_below_zone1 else heartZoneName(heartZone,true)) else null
    val cellColor = if (binding == ValueBinding.PULSE) {
        if (heartZone != null && !belowZone) trainingZoneTileColors[heartZone] else Color(0xFF042C3D)
    } else when (goalState) {
        -1 -> Color(0xFF7A2836)
        0, 1 -> Color(0xFF126B4D)
        else -> Color(0xFF042C3D)
    }
    val accessibleDescription = (if (goalState != null && targetDescription != null) {
        "$titleLabel $valueStr, target $targetDescription"
    } else "$titleLabel $valueStr") + (zoneLabel?.let { ", $it" } ?: "")
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
                text = zoneLabel?.let { "$it · BPM" } ?: displayLabel ?: buildString {
                    append(if (unitLabel.isNotEmpty()) "$titleLabel · $unitLabel" else titleLabel)
                    targetDescription?.let { append("\nTarget $it") }
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (zoneLabel != null) 16.sp else 13.sp
                ),
                color = if (zoneLabel != null) Color.White else Color(0xFFCAD4E1),
                textAlign = TextAlign.Center,
                maxLines = if (targetDescription != null) 3 else 2
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
internal fun SideProgressRail(gym: Gym, refreshTick: Int, modifier: Modifier) {
    @Suppress("UNUSED_VARIABLE") val refresh = refreshTick
    val segments = gym.program?.getSegments().orEmpty()
    val activeIndex = segments.indexOfFirst { it === gym.progress?.segment }.takeIf { it >= 0 }
        ?: if (gym.getMeasurement().duration > 0 && gym.progress == null) segments.lastIndex.coerceAtLeast(0) else 0
    val weights = segments.map { it.asDuration().coerceAtLeast(1).toFloat() }
    val total = weights.sum().coerceAtLeast(1f)
    val completion = gym.progress?.completion()?.coerceIn(0f, 1f)
        ?: if (gym.getMeasurement().duration > 0 && segments.isNotEmpty()) 1f else 0f
    val position = if (segments.isNotEmpty())
        IntervalRailReplay.position(gym.program!!, activeIndex, completion) else completion
    val race = gym.pace?.let { raceDisplay(gym.getRaceMeasurement(), it, gym.program, gym.getPaceDistanceAt(gym.getMeasurement().duration), gym.getPaceScoringDistance()) }
        ?: planRaceDisplay(gym)
    // Distance comparison and interval position are different quantities. During recovery,
    // meters stay still while each marker must continue through its own recovery band.
    val currentPosition = if (segments.size > 1) position else race?.currentProgress ?: position
    val bestPosition = if (segments.size > 1 && race != null) {
        if (gym.pace != null) gym.getPaceRailProgress(gym.getMeasurement().duration)
        else IntervalRailReplay.paceProgress(gym.program!!, gym.planTargetSplitSeconds, gym.getMeasurement().duration.toFloat())
    } else race?.bestProgress ?: 0f
    Canvas(modifier.padding(vertical = 4.dp).semantics {
        contentDescription = if (race != null) "Live row ${(currentPosition * 100).toInt()} percent, ${if (gym.hasPlanPaceTarget()) "target pace" else "saved row"} ${(bestPosition * 100).toInt()} percent"
        else "Workout ${(position * 100).toInt()} percent complete"
    }) {
        val laneWidth = 9.dp.toPx()
        fun lane(x: Float, fraction: Float, color: Color, marker: Color) {
            val height = size.height * fraction.coerceIn(0f, 1f)
            if (segments.size > 1 && race != null) {
                var bottom = size.height
                val restColor = if (color == Color(0xFFFFCD72)) Color(0xFFFFE3AA) else Color(0xFF8BD6FA)
                segments.forEachIndexed { index, segment ->
                    val bandHeight = size.height * weights[index] / total
                    drawRect(if (segment.difficulty.get() == Difficulty.REST) restColor else color,
                        Offset(x, bottom - bandHeight), Size(laneWidth, bandHeight))
                    bottom -= bandHeight
                    if (index < segments.lastIndex) {
                        drawLine(Color(0xFF042C3D), Offset(x, bottom), Offset(x + laneWidth, bottom), 2.dp.toPx())
                    }
                }
                drawRect(Color(0xFF042C3D).copy(alpha = 0.18f), Offset(x, 0f), Size(laneWidth, size.height - height))
            } else {
                drawRoundRect(Color(0xFF244758), Offset(x, 0f), Size(laneWidth, size.height), androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
                if (height > 0f) drawRect(color, Offset(x, size.height - height), Size(laneWidth, height))
            }
            val y = (size.height - height).coerceIn(2.dp.toPx(), size.height - 2.dp.toPx())
            drawLine(marker, Offset(x - 2.dp.toPx(), y), Offset(x + laneWidth + 2.dp.toPx(), y), 4.dp.toPx(), StrokeCap.Round)
        }
        if (race != null) {
            lane(2.dp.toPx(), currentPosition, Color(0xFF0B8FFF), Color.White)
            lane(18.dp.toPx(), bestPosition, Color(0xFFFFCD72), Color(0xFFFFCD72))
        } else if (segments.size > 1) {
            val x = (size.width - laneWidth) / 2
            var bottom = size.height
            segments.forEachIndexed { index, segment ->
                val height = size.height * weights[index] / total
                drawRect(if (segment.difficulty.get() == Difficulty.REST) Color(0xFF8BD6FA) else Color(0xFF0B63F6),
                    Offset(x, bottom - height), Size(laneWidth, height))
                bottom -= height
                if (index < segments.lastIndex) {
                    drawLine(Color(0xFF042C3D), Offset(x, bottom), Offset(x + laneWidth, bottom), 2.dp.toPx())
                }
            }
            drawRect(Color(0xFF042C3D).copy(alpha = 0.18f), Offset(x, 0f), Size(laneWidth, size.height * (1f - position)))
            val y = (size.height * (1f - position)).coerceIn(2.dp.toPx(), size.height - 2.dp.toPx())
            drawLine(Color.White, Offset(x - 3.dp.toPx(), y), Offset(x + laneWidth + 3.dp.toPx(), y), 4.dp.toPx(), StrokeCap.Round)
        } else lane((size.width - laneWidth) / 2, position, Color(0xFF0B8FFF), Color.White)
    }
}

@Composable
internal fun LiveRowStatus(gym: Gym, rest: RestDisplay?, refreshTick: Int, displayMeasurement: Measurement? = null, metrics: List<ValueBinding> = emptyList()) {
    @Suppress("UNUSED_VARIABLE") val refresh = refreshTick
    val segments = gym.program?.getSegments().orEmpty()
    val active = gym.progress?.segment ?: segments.firstOrNull()
    val index = segments.indexOfFirst { it === active }.coerceAtLeast(0)
    val race = gym.pace?.let { raceDisplay(gym.getRaceMeasurement(), it, gym.program, gym.getPaceDistanceAt(gym.getMeasurement().duration), gym.getPaceScoringDistance()) }
        ?: planRaceDisplay(gym)
    val reference = if (gym.pace != null && !gym.hasPlanPaceTarget()) {
        remember(gym.pace, gym.program) {
            val best = gym.pace!!
            val definition = gym.program ?: WorkoutDefinition.thaw(best.programDefinition.get())
            val seconds = if (definition != null) gym.resultSeconds(best, definition) else best.duration.get().toFloat()
            Triple(definition, seconds, formatRaceReferenceValue(definition, gym.paceScoringDistance, seconds.roundToInt()))
        }
    } else null
    val referenceValue = reference?.third.orEmpty()
    val finishReadout = reference?.let { raceFinishReadout(it.first, gym.getMeasurement().duration, it.second) }
    val smoother = remember(gym.sessionGeneration, active) { LiveGoalSmoother() }
    val measurement = displayMeasurement ?: smoother.update(gym.getMeasurement())
    val binding = active?.let { goalBinding(it, gym.planGoalKind) }
    val exactSplit = gym.planGoalValue.takeIf { gym.planGoalKind in listOf("SPEED", "SPEED_PHASE") && it > 0 }
    val goal = if (rest == null && binding != null) goalDisplay(binding, active, measurement, gym.hasPlanRateCap(), exactSplit) else null
    val feedback = if (goal != null && binding != null && active != null)
        liveTargetFeedback(binding, active, measurement, goal, gym.hasPlanRateCap()) else null
    val coaching = if (gym.isPlanSession) peteLiveInstruction(
        gym.planGoalKind, gym.planGoalValue, active?.speed?.get() ?: 0, rest != null,
        WorkoutDefinition.ranksByDistance(gym.program)
    ) else null
    val instruction = coaching ?: goal?.let {
        "Target ${it.target}${if(binding == ValueBinding.STROKE_RATE && !it.target.endsWith("SPM")) " SPM" else ""}"
    }
    // A recorded race owns the large comparison. A prescribed pace is coaching,
    // not a replay; its direction is more useful than a virtual distance gap.
    val recordedRace = race != null && !gym.hasPlanPaceTarget()
    val targetFocus = rest == null && !recordedRace && feedback != null &&
        (gym.hasPlanPaceTarget() || binding !in metrics)
    val primary = when {
        targetFocus -> feedback?.substringAfter(" · ") ?: "On target"
        rest != null -> rest.next.substringBefore(" · ")
        race != null && finishReadout != null -> finishReadout
        race != null -> "${kotlin.math.abs(race.leadMeters)} m ${if (race.leadMeters >= 0) "ahead" else "behind"}"
        active == null -> "Find your rhythm"
        active.duration.get() > 0 && segments.size == 1 -> "${((gym.progress?.completion() ?: 0f) * 100).toInt().coerceIn(0, 100)}% complete"
        active.duration.get() > 0 -> "${formatClock(getValueForBinding(ValueBinding.DURATION, gym))} remaining"
        active.distance.get() > 0 && segments.size == 1 -> "${((gym.progress?.completion() ?: 0f) * 100).toInt().coerceIn(0, 100)}% complete"
        active.distance.get() > 0 -> "${formatMetricValue(ValueBinding.DISTANCE, getValueForBinding(ValueBinding.DISTANCE, gym), LocalContext.current)} m remaining"
        active.strokes.get() > 0 -> "${getValueForBinding(ValueBinding.STROKES, gym)} strokes remaining"
        else -> "${getValueForBinding(ValueBinding.ENERGY, gym)} kcal remaining"
    }
    val detail = when {
        rest != null -> rest.next.substringAfter(" · ", "Interval ${rest.position} of ${rest.total}")
        race != null && gym.hasPlanPaceTarget() -> "Target pace · ${formatClock(gym.planTargetSplitSeconds)} /500 m"
        race != null -> "${if (gym.isPlanSession) "Plan reference" else "Your best"} · ${referenceValue}"
        segments.size > 1 -> "Interval ${index + 1} of ${segments.size}"
        active != null && active.distance.get() > 0 -> {
            val remaining = getValueForBinding(ValueBinding.DISTANCE, gym)
            "${formatMetricValue(ValueBinding.DISTANCE, active.distance.get() - remaining, LocalContext.current)} of ${segmentTarget(active)}"
        }
        active != null && active.duration.get() > 0 ->
            "${formatClock(active.duration.get() - getValueForBinding(ValueBinding.DURATION, gym))} of ${segmentTarget(active)}"
        active != null -> "Target · ${segmentTarget(active)}"
        else -> "Free row · No target"
    }
    Column(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 16.dp, start = 4.dp, end = 4.dp)) {
        if (segments.size > 1 && rest == null) {
            val ordinal = segments.take(index + 1).count { it.difficulty.get() != Difficulty.REST }
            Text(active?.name?.get()?.takeIf { it.isNotBlank() } ?: "Row $ordinal",
                color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val measurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val measured = measurer.measure(AnnotatedString(primary),
                style = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold)).size.width.coerceAtLeast(1)
            val width = with(density) { maxWidth.toPx() }
            val font = (56f * (width / measured).coerceAtMost(1f)).sp
            Text(primary, color = if (targetFocus) {
                if (goal?.state == -1) Color(0xFFFF8A80) else Color(0xFF6DE0A8)
            } else if (race == null) Color.White else if (finishReadout == null && race.leadMeters >= 0) Color(0xFF6DE0A8) else Color(0xFFFF8A80),
                fontSize = font, lineHeight = font * 1.1f, fontWeight = FontWeight.Bold,
                maxLines = 1, softWrap = false)
        }
        if (recordedRace) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("━ Live row", color = Color(0xFF83D7FF), fontSize = 15.sp, maxLines = 1)
                Text("━ ${if (gym.isPlanSession) "Previous row" else "Saved best"}",
                    color = Color(0xFFFFCD72), fontSize = 15.sp, maxLines = 1)
            }
        } else {
            val supporting = when {
                targetFocus -> instruction
                goal != null && binding in metrics -> null // Already labelled on the chosen tile.
                else -> instruction ?: detail
            }
            supporting?.let {
                Text(it, modifier = Modifier.padding(top = 8.dp), color = Color(0xFFC5D7E2),
                    fontSize = 20.sp, lineHeight = 25.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RestCountdownCard(rest: RestDisplay, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(rest.title, color = Color(0xFFC5D7E2), fontSize = 23.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(18.dp))
        Text(rest.remaining, fontSize = 100.sp, lineHeight = 106.sp, fontWeight = FontWeight.Medium,
            color = Color.White, textAlign = TextAlign.Center, maxLines = 1)
        Spacer(Modifier.height(18.dp))
        Text("Catch your breath", color = Color(0xFFC5D7E2), fontSize = 23.sp)
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
        ValueBinding.SPLIT to "Live split (5-second average, /500m)",
        ValueBinding.STROKE_RATE to "Stroke Rate (SPM)",
        ValueBinding.POWER to "Power (W)",
        ValueBinding.PULSE to "Heart Rate (BPM)",
        ValueBinding.ENERGY to "Calories (kcal)",
        ValueBinding.STROKES to "Stroke Count",
        ValueBinding.SPEED to "Speed (m/s)",
        ValueBinding.STROKE_RATIO to "Rhythm (recovery / drive)",
        ValueBinding.TIME to "Time of day",
        ValueBinding.AVERAGE_SPLIT to "Work average split (all work intervals, /500m)",
        ValueBinding.DELTA_DISTANCE to "Distance delta",
        ValueBinding.DELTA_DURATION to "Time delta"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change metric ${positionIndex + 1}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Rhythm: recovery time ÷ drive time. Aim around 2.0× for a basic steady rowing rhythm.", fontSize = 13.sp)
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
        ValueBinding.SPLIT -> "LIVE SPLIT" to "/500 M"
        ValueBinding.STROKE_RATE -> "STROKE RATE" to "SPM"
        ValueBinding.POWER -> "POWER" to "W"
        ValueBinding.PULSE -> "HEART RATE" to "BPM"
        ValueBinding.ENERGY -> "CALORIES" to "KCAL"
        ValueBinding.STROKES -> "STROKES" to "COUNT"
        ValueBinding.SPEED -> "SPEED" to "M/S"
        ValueBinding.AVERAGE_SPLIT -> "WORK AVG SPLIT" to "/500 M"
        ValueBinding.STROKE_RATIO -> "RHYTHM" to "RECOVERY / DRIVE"
        ValueBinding.TIME -> "TIME" to "CLOCK"
        ValueBinding.DELTA_DISTANCE -> "DISTANCE DELTA" to "M"
        ValueBinding.DELTA_DURATION -> "TIME DELTA" to "S"
        else -> context.getString(binding.label).uppercase() to ""
    }
}

internal fun goalBinding(segment: Segment, planKind: String? = null): ValueBinding? = when {
    segment.difficulty.get() == Difficulty.REST -> null
    planKind in listOf("SPEED", "SPEED_PHASE") && segment.speed.get() > 0 -> ValueBinding.SPLIT
    planKind in listOf("RATE", "RATE_CAP") && segment.strokeRate.get() > 0 -> ValueBinding.STROKE_RATE
    segment.strokeRate.get() > 0 -> ValueBinding.STROKE_RATE
    segment.speed.get() > 0 -> ValueBinding.SPLIT
    segment.power.get() > 0 -> ValueBinding.POWER
    segment.pulse.get() > 0 -> ValueBinding.PULSE
    else -> null
}

/** Once a finish-time reference is done, equal final distances must not suggest a tie. */
internal fun raceFinishReadout(program: Program?, currentSeconds: Int, bestFinishSeconds: Float): String? {
    if (program == null || WorkoutDefinition.ranksByDistance(program) || !bestFinishSeconds.isFinite() ||
        bestFinishSeconds <= 0f || currentSeconds < bestFinishSeconds) return null
    val gap = (currentSeconds-bestFinishSeconds).roundToInt()
    return if (gap == 0) "Best finished" else "${formatClock(gap)} behind"
}

/** Match the metric used to rank completed races, including mixed interval programs. */
internal fun formatRaceReferenceValue(program: Program?, workMeters: Int, finishSeconds: Int): String =
    if (WorkoutDefinition.ranksByDistance(program)) "%,d m".format(Locale.getDefault(), workMeters)
    else formatClock(finishSeconds)

private fun formatClock(seconds: Int): String {
    val value = seconds.coerceAtLeast(0)
    return "%d:%02d".format(Locale.getDefault(), value / 60, value % 60)
}

internal fun formatMetricValue(binding: ValueBinding, value: Int, context: Context): String = when (binding) {
    ValueBinding.DISTANCE, ValueBinding.STROKES, ValueBinding.ENERGY ->
        java.text.NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)
    ValueBinding.STROKE_RATIO -> if (value > 0) String.format(Locale.getDefault(), "%.1f×", value / 10f) else "—"
    ValueBinding.DURATION, ValueBinding.SPLIT, ValueBinding.AVERAGE_SPLIT -> formatClock(value)
    ValueBinding.DELTA_DISTANCE -> {
        val sign = when { value > 0 -> "+"; value < 0 -> "-"; else -> "" }
        sign + java.text.NumberFormat.getIntegerInstance(Locale.getDefault()).format(kotlin.math.abs(value.toLong()))
    }
    ValueBinding.DELTA_DURATION -> {
        val sign = when { value > 0 -> "+"; value < 0 -> "-"; else -> "" }
        val absolute = kotlin.math.abs(value.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        "$sign${formatClock(absolute)}"
    }
    else -> binding.format(context, value, false)
}

data class RestDisplay(val position: Int, val total: Int, val remaining: String, val next: String, val title: String)

internal fun restDisplay(segments: List<Segment>, active: Segment?, completion: Float): RestDisplay {
    val index = segments.indexOfFirst { it === active }.coerceAtLeast(0)
    val remaining = if ((active?.duration?.get() ?: 0) > 0) {
        val seconds = (active!!.duration.get() * (1f - completion)).toInt().coerceAtLeast(0)
        "%d:%02d".format(seconds / 60, seconds % 60)
    } else "${((active?.getTarget() ?: 0) * (1f - completion)).toInt().coerceAtLeast(0)} m"
    val next = segments.getOrNull(index + 1)
    val ordinal = segments.take(index + 1).count { it.difficulty.get() != Difficulty.REST }.coerceAtLeast(1)
    val activeTitle = active?.name?.get()?.takeIf { it.isNotBlank() } ?: "Rest $ordinal"
    val nextOrdinal = segments.take(index + 2).count { it.difficulty.get() != Difficulty.REST }.coerceAtLeast(1)
    val nextTitle = next?.name?.get()?.takeIf { it.isNotBlank() } ?: "Row $nextOrdinal"
    val nextDescription = if (next == null) "Final segment" else if (next.name.get().isNullOrBlank()) {
        "Next: ${segmentTarget(next)} row"
    } else "Next: $nextTitle · ${segmentTarget(next)}"
    return RestDisplay(index + 1, segments.size, remaining, nextDescription, activeTitle)
}

data class GoalDisplay(val variance: String, val target: String, val state: Int, val feedback: String? = null)

internal fun goalDisplay(binding: ValueBinding, segment: Segment?, measurement: Measurement, rateCap: Boolean = false, exactSplit: Int? = null): GoalDisplay? {
    if (segment == null || segment.getLimit() <= 0) return null
    return when {
        binding == ValueBinding.STROKE_RATE && segment.strokeRate.get() > 0 && rateCap -> {
            val difference = measurement.strokeRate - segment.strokeRate.get()
            GoalDisplay(if (difference == 0) "${measurement.strokeRate}" else if (difference > 0) "+$difference" else "$difference",
                "≤${segment.strokeRate.get()} SPM", if (difference <= 0) 1 else -1)
        }
        binding == ValueBinding.STROKE_RATE && segment.strokeRate.get() > 0 -> signedGoal(measurement.strokeRate - segment.strokeRate.get(), "${segment.strokeRate.get()}", "", "${segment.strokeRate.get()}")
        binding == ValueBinding.POWER && segment.power.get() > 0 -> signedGoal(measurement.power - segment.power.get(), "${segment.power.get()}", "", "${segment.power.get()} W")
        binding == ValueBinding.PULSE && segment.pulse.get() > 0 -> signedGoal(measurement.pulse - segment.pulse.get(), "${segment.pulse.get()}", "", "${segment.pulse.get()} BPM")
        binding == ValueBinding.SPEED && segment.speed.get() > 0 -> {
            val difference = measurement.speed - segment.speed.get()
            val actual = String.format(Locale.getDefault(), "%.1f", measurement.speed / 100f)
            val value = if (difference == 0) actual else String.format(Locale.getDefault(), "%+.1f", difference / 100f)
            GoalDisplay(value, String.format(Locale.getDefault(), "%.1f m/s", segment.speed.get() / 100f), stateFor(difference))
        }
        binding == ValueBinding.SPLIT && segment.speed.get() > 0 && measurement.speed > 0 -> {
            val targetPace = exactSplit ?: (50000f / segment.speed.get()).roundToInt()
            val actualPace = (50000f / measurement.speed).roundToInt()
            val difference = targetPace - actualPace
            signedGoal(difference, "%d:%02d".format(targetPace / 60, targetPace % 60), " s", "%d:%02d /500 m".format(targetPace / 60, targetPace % 60)).copy(
                feedback = when { difference > 0 -> "$difference s faster"; difference < 0 -> "${-difference} s slower"; else -> "On target" }
            )
        }
        else -> null
    }
}

private fun signedGoal(value: Int, atTarget: String, unit: String, target: String) = GoalDisplay(
    if (value == 0) atTarget else if (value > 0) "+$value$unit" else "$value$unit",
    target,
    stateFor(value)
)
private fun stateFor(value: Int): Int = when { value > 0 -> 1; value < 0 -> -1; else -> 0 }

private fun segmentTarget(segment: Segment): String = when {
    segment.duration.get() > 0 -> "%d:%02d".format(segment.duration.get() / 60, segment.duration.get() % 60)
    segment.distance.get() > 0 -> "%,d m".format(Locale.getDefault(), segment.distance.get())
    segment.strokes.get() > 0 -> "${segment.strokes.get()} strokes"
    else -> "${segment.energy.get()} kcal"
}

data class RaceDisplay(val currentProgress: Float, val bestProgress: Float, val leadMeters: Int, val bestMeters: Int)

private fun planRaceDisplay(gym: Gym): RaceDisplay? {
    if (!gym.hasPlanPaceTarget()) return null
    val program = gym.program ?: return null
    val work = program.getSegments().filter { it.difficulty.get() != Difficulty.REST }
    val split = gym.planTargetSplitSeconds.coerceAtLeast(1)
    val targetDistance = if (work.all { it.distance.get() > 0 })
        work.sumOf { it.distance.get() }.toFloat()
    else work.sumOf { it.duration.get() } * 500f / split
    if (targetDistance <= 0f) return null
    val live = gym.planLiveDistance.toFloat()
    val target = gym.planTargetDistance
    return RaceDisplay(
        (live / targetDistance).coerceIn(0f, 1f),
        (target / targetDistance).coerceIn(0f, 1f),
        (live - target).toInt(), target.toInt()
    )
}

internal fun raceDisplay(current: Measurement, pace: Workout, program: Program?, recordedDistance: Float? = null, benchmarkDistance: Int? = null): RaceDisplay {
    val expectedDistance = recordedDistance ?: if (pace.duration.get() > 0) pace.distance.get() * current.duration.toFloat() / pace.duration.get() else 0f
    val distanceRace = program?.getSegmentsCount() == 1 && (program.getSegment(0).distance.get() > 0)
    val target = if (distanceRace) program.getSegment(0).distance.get().coerceAtLeast(1) else (benchmarkDistance ?: pace.distance.get()).coerceAtLeast(1)
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
        ValueBinding.AVERAGE_SPLIT -> {
            // Work totals exclude both completed rests and the rest in progress.
            val meters = gym.planLiveDistance
            if (meters > 0) (gym.planActiveSeconds * 500L / meters).toInt() else 0
        }
        ValueBinding.DELTA_DISTANCE -> {
            val pace = gym.pace
            if (pace != null && pace.duration.get() > 0) {
                gym.getPlanLiveDistance() - gym.getPaceDistanceAt(m.duration).toInt()
            } else 0
        }
        ValueBinding.DELTA_DURATION -> {
            val pace = gym.pace
            if (pace != null && pace.distance.get() > 0) {
                m.duration - gym.getPaceTimeAtDistance(gym.getPlanLiveDistance()).toInt()
            } else 0
        }
        else -> 0
    }
}
