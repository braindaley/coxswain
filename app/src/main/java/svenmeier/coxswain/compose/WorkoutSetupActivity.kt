package svenmeier.coxswain.compose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import svenmeier.coxswain.Gym
import svenmeier.coxswain.R
import svenmeier.coxswain.WorkoutActivity
import svenmeier.coxswain.gym.Difficulty
import svenmeier.coxswain.gym.Program
import svenmeier.coxswain.gym.Segment
import svenmeier.coxswain.gym.SessionType
import java.util.Locale

class WorkoutSetupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gym = Gym.instance(this)

        setContent {
            CoxswainTheme {
                WorkoutSetupScreen(
                    initialType = intent.getStringExtra(EXTRA_TYPE) ?: "Duration",
                    onBack = { finish() },
                    onStart = { program ->
                        gym.start(program, when {
                            program.segments.get().size > 1 -> SessionType.INTERVAL
                            program.segments.get().first().duration.get() > 0 -> SessionType.DURATION
                            else -> SessionType.DISTANCE
                        })
                        WorkoutActivity.start(this)
                        finish()
                    },
                    onSaveAsProgram = { program ->
                        gym.mergeProgram(program)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_TYPE = "workoutType"

        @JvmStatic
        fun start(context: Context, type: String = "Duration") {
            context.startActivity(createIntent(context, type))
        }

        @JvmStatic
        fun createIntent(context: Context, type: String): Intent {
            return Intent(context, WorkoutSetupActivity::class.java).putExtra(EXTRA_TYPE, type)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSetupScreen(
    initialType: String = "Duration",
    onBack: () -> Unit,
    onStart: (Program) -> Unit,
    onSaveAsProgram: (Program) -> Unit
) {
    var selectedType by remember(initialType) { mutableStateOf(initialType) }
    val typeOptions = listOf(
        "Duration" to stringResource(R.string.ui_duration),
        "Distance" to stringResource(R.string.ui_distance),
        "Intervals" to stringResource(R.string.ui_intervals)
    )
    val selectedTypeLabel = typeOptions.first { it.first == selectedType }.second
    val quickWorkoutName = stringResource(R.string.ui_quick_workout)
    val defaultProgramName = stringResource(R.string.ui_default_program_name)
    var targetValue by remember(initialType) { mutableIntStateOf(if (initialType == "Distance") 5000 else 60) }
    var selectedGoal by remember { mutableStateOf("None") }
    var goalValue by remember { mutableIntStateOf(26) }
    var pendingSave by remember { mutableStateOf<Program?>(null) }
    var programName by remember { mutableStateOf(defaultProgramName) }
    val intervalSegments = remember { mutableStateListOf(DraftSegment(SegmentType.DURATION, 5), DraftSegment(SegmentType.REST, 1), DraftSegment(SegmentType.DURATION, 5)) }
    val definitionValid = if (selectedType == "Intervals") intervalSegments.isNotEmpty() && intervalSegments.all { it.value > 0 } else targetValue > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ui_workout_title, selectedTypeLabel), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ui_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, Color(0xFFE0E7F0)),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        enabled = definitionValid,
                        onClick = {
                            pendingSave = buildProgram(selectedType, targetValue, selectedGoal, goalValue, intervalSegments).apply { name.set(quickWorkoutName) }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, Color(0xFF0B63F6)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0B63F6))
                    ) {
                        Icon(painterResource(R.drawable.ic_nav_programs_24dp), contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.ui_save_as_program), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        enabled = definitionValid,
                        onClick = {
                            val p = buildProgram(selectedType, targetValue, selectedGoal, goalValue, intervalSegments).apply { name.set(quickWorkoutName) }
                            onStart(p)
                        },
                        modifier = Modifier.weight(1.3f).height(56.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B63F6))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.ui_start_workout), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionLabel(stringResource(R.string.ui_program_type))
            SingleSelectToggleGroup(
                options = typeOptions.map { it.second },
                selectedOption = selectedTypeLabel,
                onOptionSelected = { selectedLabel ->
                    selectedType = typeOptions.first { it.second == selectedLabel }.first
                    if (selectedType == "Duration") targetValue = 60 else if (selectedType == "Distance") targetValue = 5000
                }
            )

            if (selectedType != "Intervals") {
                SectionLabel(stringResource(R.string.ui_target))
                TargetInputCard(
                    value = targetValue,
                    unit = if (selectedType == "Duration") "min" else "m",
                    onValueChange = { targetValue = it },
                    presets = if (selectedType == "Duration") listOf(10, 20, 30, 45, 60, 90) else listOf(500, 1000, 2000, 5000, 6000, 10000)
                )
            } else {
                IntervalBuilderCard(intervalSegments,
                    onAdd = { intervalSegments.add(DraftSegment(SegmentType.DURATION, 5)) },
                    onDelete = { if (intervalSegments.size > 1) intervalSegments.removeAt(it) })
            }

            SectionLabel(stringResource(R.string.ui_set_goal_optional))
            GoalSelector(
                selectedGoal = selectedGoal,
                onGoalSelected = { selectedGoal = it },
                goalValue = goalValue,
                onGoalValueChange = { goalValue = it }
            )

            SectionLabel(stringResource(R.string.ui_workout_summary))
            SummaryCard(selectedType, targetValue, selectedGoal, goalValue, intervalSegments)
            
            Spacer(Modifier.height(40.dp))
        }
    }
    pendingSave?.let { program ->
        AlertDialog(onDismissRequest = { pendingSave = null }, title = { Text(stringResource(R.string.ui_save_as_program)) }, text = { OutlinedTextField(value = programName, onValueChange = { programName = it }, label = { Text(stringResource(R.string.ui_program_name)) }, singleLine = true) }, confirmButton = { TextButton(enabled = programName.isNotBlank(), onClick = { program.name.set(programName.trim()); onSaveAsProgram(program); pendingSave = null }) { Text(stringResource(R.string.ui_save)) } }, dismissButton = { TextButton(onClick = { pendingSave = null }) { Text(stringResource(R.string.ui_cancel)) } })
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontSize = 12.sp
        ),
        color = Color(0xFF53647C),
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
fun SingleSelectToggleGroup(
    options: List<String>,
    selectedOption: String,
    fontSize: TextUnit = 14.sp,
    onOptionSelected: (String) -> Unit
) {
    Surface(
        color = Color(0xFFE8EEF6),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Surface(
                    color = if (isSelected) Color.White else Color.Transparent,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOptionSelected(option) }
                ) {
                    Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = option,
                            fontSize = fontSize,
                            maxLines = 1,
                            softWrap = false,
                            color = if (isSelected) Color(0xFF0B63F6) else Color(0xFF53647C),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TargetInputCard(
    value: Int,
    unit: String,
    onValueChange: (Int) -> Unit,
    presets: List<Int>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 12.dp)) {
                TextField(
                    value = value.toString(),
                    onValueChange = { 
                        val v = it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0
                        onValueChange(v) 
                    },
                    textStyle = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.W500,
                        textAlign = TextAlign.Center,
                        fontSize = 52.sp,
                        color = Color(0xFF10213F)
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color(0xFF0B63F6),
                        unfocusedIndicatorColor = Color(0xFF0B63F6)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(140.dp)
                )
                Text(
                    text = unit,
                    fontSize = 17.sp,
                    color = Color(0xFF53647C),
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                maxItemsInEachRow = 3
            ) {
                presets.forEach { preset ->
                    val isSelected = value == preset
                    Surface(
                        onClick = { onValueChange(preset) },
                        color = if (isSelected) Color(0xFFDCEBFF) else Color(0xFFEFF4FA),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        modifier = Modifier.padding(vertical = 4.dp).height(44.dp).width(90.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (preset >= 1000) "${preset / 1000}K" else preset.toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF0B63F6) else Color(0xFF10213F)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoalSelector(
    selectedGoal: String,
    onGoalSelected: (String) -> Unit,
    goalValue: Int,
    onGoalValueChange: (Int) -> Unit
) {
    data class GoalItem(val key: String, val label: String, val icon: ImageVector)
    val goals = listOf(
        GoalItem("None", stringResource(R.string.ui_none), Icons.Default.Close),
        GoalItem("Stroke rate", stringResource(R.string.ui_stroke_rate), Icons.Default.Refresh),
        GoalItem("Speed", stringResource(R.string.ui_speed), Icons.Default.PlayArrow),
        GoalItem("Power", stringResource(R.string.ui_power), Icons.Default.Star)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                goals.forEach { item ->
                    val isSelected = selectedGoal == item.key
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .clickable { onGoalSelected(item.key) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFEEF6FF) else Color.White
                        ),
                        border = if (isSelected) BorderStroke(2.dp, Color(0xFF0B63F6)) else BorderStroke(1.dp, Color(0xFFD5DEE9)),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF0B63F6) else Color(0xFF10213F),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF0B63F6) else Color(0xFF10213F), 
                                textAlign = TextAlign.Center,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }

            if (selectedGoal != "None") {
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF7FAFD), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.ui_target_metric, goals.first { it.key == selectedGoal }.label), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (selectedGoal == "Speed") String.format(Locale.getDefault(), "%d:%02d /500m", goalValue/60, goalValue%60) else "$goalValue ${if (selectedGoal == "Power") "W" else "SPM"}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0B63F6)
                        )
                    }
                    Slider(
                        value = goalValue.toFloat(),
                        onValueChange = { onGoalValueChange(it.toInt()) },
                        valueRange = if (selectedGoal == "Stroke rate") 14f..40f else if (selectedGoal == "Power") 50f..400f else 90f..240f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFF0B63F6)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(type: String, target: Int, goal: String, goalValue: Int, intervals: List<DraftSegment> = emptyList()) {
    val typeLabel = when (type) {
        "Duration" -> stringResource(R.string.ui_duration)
        "Distance" -> stringResource(R.string.ui_distance)
        else -> stringResource(R.string.ui_intervals)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryRow(stringResource(R.string.ui_type), typeLabel)
            SummaryRow(stringResource(R.string.ui_target), if (type == "Intervals") pluralStringResource(R.plurals.ui_segment_count, intervals.size, intervals.size) else if (type == "Duration") pluralStringResource(R.plurals.ui_minutes_long, target, target) else pluralStringResource(R.plurals.ui_meters_long, target, target))
            SummaryRow(stringResource(R.string.ui_goal), if (goal == "None") stringResource(R.string.ui_no_performance_goal) else if (goal == "Speed") String.format(Locale.getDefault(), "%d:%02d /500m", goalValue/60, goalValue%60) else "$goalValue ${if (goal == "Power") "W" else "SPM"}")
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10213F))
        Text(value, fontSize = 14.sp, color = Color(0xFF53647C))
    }
}

enum class SegmentType { DURATION, DISTANCE, REST }
data class DraftSegment(val type: SegmentType, val value: Int)

@Composable
fun IntervalBuilderCard(segments: MutableList<DraftSegment>, onAdd: () -> Unit, onDelete: (Int) -> Unit) {
    val typeLabels = mapOf(
        SegmentType.DURATION to stringResource(R.string.ui_duration),
        SegmentType.DISTANCE to stringResource(R.string.ui_distance),
        SegmentType.REST to stringResource(R.string.ui_rest)
    )
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            segments.forEachIndexed { index, segment ->
                Column(Modifier.fillMaxWidth().background(Color(0xFFF7FAFD), RoundedCornerShape(12.dp)).padding(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SegmentType.values().forEach { type -> FilterChip(selected = segment.type == type, onClick = { segments[index] = segment.copy(type = type, value = if (type == SegmentType.DISTANCE) 500 else 1) }, label = { Text(typeLabels.getValue(type)) }) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = segment.value.toString(), onValueChange = { segments[index] = segment.copy(value = it.filter(Char::isDigit).toIntOrNull() ?: 0) }, label = { Text(stringResource(if (segment.type == SegmentType.DISTANCE) R.string.ui_meters else R.string.ui_minutes)) }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        IconButton(onClick = { onDelete(index) }, enabled = segments.size > 1) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.ui_delete_segment)) }
                    }
                }
            }
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.ui_add_segment)) }
        }
    }
}

fun buildProgram(type: String, target: Int, goal: String, goalValue: Int, intervals: List<DraftSegment> = emptyList()): Program {
    val p = Program("Quick Workout")
    p.segments.get().clear()
    if (type == "Intervals") {
        intervals.forEach { draft ->
            val segment = Segment(if (draft.type == SegmentType.REST) Difficulty.REST else Difficulty.EASY)
            if (draft.type == SegmentType.DISTANCE) segment.setDistance(draft.value) else segment.setDuration(draft.value * 60)
            if (draft.type != SegmentType.REST) when (goal) { "Stroke rate" -> segment.setStrokeRate(goalValue); "Speed" -> segment.setSpeed(paceToCentimetersPerSecond(goalValue)); "Power" -> segment.setPower(goalValue) }
            p.addSegment(segment)
        }
    } else {
        val s = Segment(Difficulty.EASY)
        if (type == "Duration") s.setDuration(target * 60) else s.setDistance(target)
        when (goal) { "Stroke rate" -> s.setStrokeRate(goalValue); "Speed" -> s.setSpeed(paceToCentimetersPerSecond(goalValue)); "Power" -> s.setPower(goalValue) }
        p.addSegment(s)
    }
    return p
}

fun buildProgram(type: String, target: Int, goal: String, goalValue: Int): Program =
    buildProgram(type, target, goal, goalValue, emptyList())

fun paceToCentimetersPerSecond(secondsPer500Meters: Int): Int =
    if (secondsPer500Meters <= 0) 0 else (50_000f / secondsPer500Meters).toInt()
