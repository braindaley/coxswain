package svenmeier.coxswain

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import propoid.db.Reference
import svenmeier.coxswain.compose.*
import svenmeier.coxswain.gym.Difficulty
import svenmeier.coxswain.gym.Program
import svenmeier.coxswain.gym.Segment
import svenmeier.coxswain.gym.WorkoutDefinition
import svenmeier.coxswain.view.MaterialLimitPickerDialog
import svenmeier.coxswain.view.MaterialTargetPickerDialog
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

class ProgramActivity : FragmentActivity() {

    private lateinit var gym: Gym
    private var originalProgram: Program? = null
    private lateinit var draftProgram: Program
    private val refreshTrigger = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        gym = Gym.instance(this)

        val reference = Reference.from<Program>(intent)
        originalProgram = if (reference != null) gym.getProgram(reference) else null

        val isReadOnly = intent.getBooleanExtra(EXTRA_READ_ONLY, false)

        if (isReadOnly && originalProgram != null) {
            val program = originalProgram!!
            val compatibility = WorkoutDefinition.compatibilityKey(program)
            val history = ArrayList(gym.getAllWorkouts().list()).filter { workout ->
                WorkoutDefinition.compatibilityKey(workout.programDefinition.get()) == compatibility ||
                    workout.programName.get() == program.name.get()
            }.sortedByDescending { it.start.get() }
            val raceCandidates = gym.getRaceCandidates(program)
            setContent {
                CoxswainTheme {
                    ProgramDetailsScreen(
                        program = program,
                        history = history,
                        raceCandidates = raceCandidates,
                        onBack = { finish() },
                        onStart = { raceAgainstBest ->
                            if (raceAgainstBest) gym.race(program, raceCandidates.first()) else gym.select(program)
                            WorkoutActivity.start(this)
                            finish()
                        },
                        onEdit = { startActivity(createIntent(this, program)); finish() },
                        onDuplicate = {
                            gym.duplicateProgram(program, getString(R.string.ui_program_copy, program.name.get()))
                            Toast.makeText(this, R.string.ui_program_saved, Toast.LENGTH_SHORT).show()
                            finish()
                        },
                        onDelete = { gym.delete(program); finish() }
                    )
                }
            }
            return
        }

        draftProgram = if (originalProgram != null) {
            cloneProgram(originalProgram!!)
        } else {
            Program(getString(R.string.program_name_new)).apply {
                segments.get().clear()
                addSegment(Segment(Difficulty.EASY).setDuration(60 * 60))
            }
        }

        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                if (f is MaterialTargetPickerDialog || f is MaterialLimitPickerDialog) {
                    refreshTrigger.intValue++
                }
            }
        }, false)

        setContent {
            CoxswainTheme {
                ProgramEditorScreen(
                    draftProgram = draftProgram,
                    readOnly = isReadOnly,
                    isNewProgram = (originalProgram == null),
                    refreshTrigger = refreshTrigger.intValue,
                    onSave = {
                        val finalName = draftProgram.name.get()?.trim().orEmpty().ifEmpty { getString(R.string.program_name_new) }
                        draftProgram.name.set(finalName)

                        if (originalProgram == null) {
                            gym.mergeProgram(draftProgram)
                        } else {
                            originalProgram!!.name.set(finalName)
                            originalProgram!!.segments.get().clear()
                            for (segment in draftProgram.segments.get()) {
                                originalProgram!!.segments.get().add(segment.duplicate())
                            }
                            gym.mergeProgram(originalProgram!!)
                        }
                        Toast.makeText(this, R.string.ui_program_saved, Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onBack = { finish() },
                    onShowTargetDialog = { segment ->
                        MaterialTargetPickerDialog.create(segment).show(supportFragmentManager, "target")
                    },
                    onShowLimitDialog = { segment ->
                        MaterialLimitPickerDialog.create(segment).show(supportFragmentManager, "limit")
                    }
                )
            }
        }
    }

    private fun cloneProgram(original: Program): Program {
        val copy = Program(original.name.get() ?: getString(R.string.program_name_new))
        copy.segments.get().clear()
        for (s in original.segments.get()) {
            copy.segments.get().add(s.duplicate())
        }
        return copy
    }

    companion object {
        private const val EXTRA_READ_ONLY = "readOnly"

        @JvmStatic
        fun createIntent(context: Context, program: Program?): Intent {
            val intent = Intent(context, ProgramActivity::class.java)
            if (program != null) {
                intent.data = Reference(program).toUri()
            }
            return intent
        }

        @JvmStatic
        fun createReadOnlyIntent(context: Context, program: Program): Intent =
            createIntent(context, program).putExtra(EXTRA_READ_ONLY, true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgramDetailsScreen(
    program: Program,
    history: List<svenmeier.coxswain.gym.Workout>,
    raceCandidates: List<svenmeier.coxswain.gym.Workout>,
    onBack: () -> Unit,
    onStart: (raceAgainstBest: Boolean) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var raceAgainstBest by remember(program) { mutableStateOf(raceCandidates.isNotEmpty()) }
    val hasHistory = history.isNotEmpty()
    val type = WorkoutDefinition.typeOf(program)
    val segments = program.segments.get()
    val target = when (type) {
        svenmeier.coxswain.gym.SessionType.DURATION -> stringResource(R.string.ui_minutes_value, segments.firstOrNull()?.duration?.get()?.div(60) ?: 0)
        svenmeier.coxswain.gym.SessionType.DISTANCE -> stringResource(R.string.ui_meters_value, segments.firstOrNull()?.distance?.get() ?: 0)
        else -> pluralStringResource(R.plurals.ui_segment_count, segments.size, segments.size)
    }
    val goal = when (val value = WorkoutDefinition.goalOf(program)) {
        svenmeier.coxswain.gym.PerformanceGoal.NONE -> stringResource(R.string.ui_no_performance_goal)
        svenmeier.coxswain.gym.PerformanceGoal.STROKE_RATE -> "${WorkoutDefinition.goalTargetOf(program)} SPM"
        svenmeier.coxswain.gym.PerformanceGoal.POWER -> "${WorkoutDefinition.goalTargetOf(program)} W"
        svenmeier.coxswain.gym.PerformanceGoal.PULSE -> "${WorkoutDefinition.goalTargetOf(program)} BPM"
        svenmeier.coxswain.gym.PerformanceGoal.SPEED -> {
            val pace = speedToPaceSeconds(WorkoutDefinition.goalTargetOf(program))
            "%d:%02d /500 m".format(Locale.getDefault(), pace / 60, pace % 60)
        }
    }
    val timed = type == svenmeier.coxswain.gym.SessionType.DURATION
    val raceToggleDescription = stringResource(R.string.ui_race_your_best)
    val best = if (timed) history.maxByOrNull { it.distance.get() } else history.minByOrNull { it.duration.get() }
    val averageResult = if (history.isEmpty()) null else if (timed) history.map { it.distance.get() }.average().toInt() else history.map { it.duration.get() }.average().toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ui_program_details)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ui_back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.ui_menu))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (!hasHistory) DropdownMenuItem(
                                text = { Text(stringResource(R.string.ui_edit_program)) },
                                onClick = { menuOpen = false; onEdit() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_duplicate)) },
                                onClick = { menuOpen = false; onDuplicate() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete)) },
                                onClick = { menuOpen = false; confirmDelete = true }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Button(
                        onClick = { onStart(raceAgainstBest) },
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp).height(54.dp),
                        shape = RoundedCornerShape(27.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(if (raceAgainstBest) R.string.ui_start_race else R.string.ui_start_workout),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { inset ->
        Column(
            Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(program.name.get(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            SectionLabel(stringResource(R.string.ui_workout_summary))
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailValueRow(stringResource(R.string.ui_type), stringResource(when (type) {
                        svenmeier.coxswain.gym.SessionType.DURATION -> R.string.ui_duration
                        svenmeier.coxswain.gym.SessionType.DISTANCE -> R.string.ui_distance
                        else -> R.string.ui_intervals
                    }))
                    DetailValueRow(stringResource(R.string.ui_target), target)
                    DetailValueRow(stringResource(R.string.ui_goal), goal)
                }
            }

            if (type == svenmeier.coxswain.gym.SessionType.INTERVAL) {
                SectionLabel(stringResource(R.string.ui_intervals).uppercase(Locale.getDefault()))
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        segments.forEachIndexed { index, segment ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    segment.name.get()?.takeIf { it.isNotBlank() }
                                        ?: stringResource(if (segment.difficulty.get() == Difficulty.REST) R.string.ui_rest else R.string.ui_row),
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    when {
                                        segment.duration.get() > 0 -> "%d:%02d".format(segment.duration.get() / 60, segment.duration.get() % 60)
                                        segment.distance.get() > 0 -> "%,d m".format(Locale.getDefault(), segment.distance.get())
                                        segment.strokes.get() > 0 -> "${segment.strokes.get()} strokes"
                                        else -> stringResource(R.string.ui_target_set)
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (index < segments.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }

            SectionLabel(stringResource(R.string.ui_program_history))
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(pluralStringResource(R.plurals.ui_workouts_completed, history.size, history.size), fontWeight = FontWeight.Bold)
                    if (best != null) {
                        DetailValueRow(stringResource(R.string.ui_best), if (timed) "%,d m".format(Locale.getDefault(), best.distance.get()) else formatProgramTime(best.duration.get()))
                        averageResult?.let { average ->
                            DetailValueRow(stringResource(R.string.ui_average), if (timed) "%,d m".format(Locale.getDefault(), average) else formatProgramTime(average))
                        }
                        val latest = history.first()
                        Text(
                            stringResource(R.string.ui_program_last_completed, SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(latest.start.get()))),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(stringResource(R.string.ui_program_no_history), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.ui_race_your_best),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = raceAgainstBest,
                            onCheckedChange = { raceAgainstBest = it },
                            enabled = raceCandidates.isNotEmpty(),
                            modifier = Modifier.semantics { contentDescription = raceToggleDescription }
                        )
                    }
                    Text(
                        if (raceCandidates.isEmpty()) stringResource(R.string.ui_race_empty)
                        else stringResource(R.string.ui_program_race_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (raceCandidates.isNotEmpty()) {
                        val reference = raceCandidates.first()
                        Text(
                            stringResource(R.string.ui_program_race_record, if (timed) "%,d m".format(Locale.getDefault(), reference.distance.get()) else formatProgramTime(reference.duration.get())),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.ui_delete_program_question)) },
        text = { Text(stringResource(R.string.ui_delete_program_explanation, program.name.get())) },
        confirmButton = { TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.ui_cancel)) } }
    )
}

@Composable
private fun DetailValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
    }
}

private fun formatProgramTime(seconds: Int): String = "%d:%02d".format(Locale.getDefault(), seconds / 60, seconds % 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramEditorScreen(
    draftProgram: Program,
    readOnly: Boolean = false,
    isNewProgram: Boolean = false,
    refreshTrigger: Int = 0,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onShowTargetDialog: (Segment) -> Unit,
    onShowLimitDialog: (Segment) -> Unit
) {
    val typeOptions = listOf(
        "Duration" to stringResource(R.string.ui_duration),
        "Distance" to stringResource(R.string.ui_distance),
        "Intervals" to stringResource(R.string.ui_intervals)
    )

    val initialSegments = draftProgram.segments.get()
    val initialType = remember {
        when {
            initialSegments.size > 1 -> "Intervals"
            (initialSegments.firstOrNull()?.duration?.get() ?: 0) > 0 -> "Duration"
            (initialSegments.firstOrNull()?.distance?.get() ?: 0) > 0 -> "Distance"
            else -> "Duration"
        }
    }

    var programName by remember { mutableStateOf(draftProgram.name.get() ?: "") }
    var selectedType by remember { mutableStateOf(initialType) }
    var isDirty by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val segments = remember { mutableStateListOf<Segment>().apply { addAll(initialSegments) } }

    var targetValue by remember {
        mutableIntStateOf(
            when (initialType) {
                "Distance" -> initialSegments.firstOrNull()?.distance?.get() ?: 5000
                "Duration" -> maxOf(1, (initialSegments.firstOrNull()?.duration?.get() ?: 3600) / 60)
                else -> 60
            }
        )
    }

    val firstSeg = initialSegments.firstOrNull()
    val initialGoalKey = remember {
        when {
            (firstSeg?.strokeRate?.get() ?: 0) > 0 -> "Stroke rate"
            (firstSeg?.speed?.get() ?: 0) > 0 -> "Speed"
            (firstSeg?.power?.get() ?: 0) > 0 -> "Power"
            else -> "None"
        }
    }

    val initialGoalVal = remember {
        when {
            (firstSeg?.strokeRate?.get() ?: 0) > 0 -> firstSeg!!.strokeRate.get()
            (firstSeg?.speed?.get() ?: 0) > 0 -> speedToPaceSeconds(firstSeg!!.speed.get())
            (firstSeg?.power?.get() ?: 0) > 0 -> firstSeg!!.power.get()
            else -> 26
        }
    }

    var selectedGoal by remember { mutableStateOf(initialGoalKey) }
    var goalValue by remember { mutableIntStateOf(initialGoalVal) }
    val segmentNames = remember { mutableStateMapOf<Segment, String>().apply {
        initialSegments.forEach { segment -> segment.name.get()?.let { put(segment, it) } }
    } }

    fun syncProgram() {
        draftProgram.name.set(programName.trim())
        val segList = draftProgram.segments.get()
        if (selectedType == "Duration" || selectedType == "Distance") {
            segList.clear()
            val s = Segment(Difficulty.EASY)
            if (selectedType == "Duration") {
                s.setDuration(targetValue * 60)
            } else {
                s.setDistance(targetValue)
            }
            when (selectedGoal) {
                "Stroke rate" -> s.setStrokeRate(goalValue)
                "Speed" -> s.setSpeed(paceToCentimetersPerSecond(goalValue))
                "Power" -> s.setPower(goalValue)
                else -> {
                    s.setStrokeRate(0)
                    s.setSpeed(0)
                    s.setPower(0)
                    s.setPulse(0)
                }
            }
            segList.add(s)
            segments.clear()
            segments.addAll(segList)
        } else {
            // Intervals
            if (segList.isEmpty()) {
                val s1 = Segment(Difficulty.EASY).apply { setDuration(300) }
                val s2 = Segment(Difficulty.REST).apply { setDuration(60) }
                val s3 = Segment(Difficulty.EASY).apply { setDuration(300) }
                segList.add(s1)
                segList.add(s2)
                segList.add(s3)
            }
            segments.clear()
            segments.addAll(segList)
        }
    }

    fun handleBack() {
        if (!readOnly && isDirty) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    BackHandler {
        handleBack()
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.ui_discard_changes_title)) },
            text = { Text(stringResource(R.string.ui_discard_changes_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) {
                    Text(stringResource(R.string.ui_discard), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.ui_keep_editing))
                }
            }
        )
    }

    val selectedTypeLabel = typeOptions.first { it.first == selectedType }.second

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (readOnly) R.string.ui_view_program else if (isNewProgram) R.string.ui_create_program else R.string.ui_edit_program
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.ui_back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            if (!readOnly) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                syncProgram()
                                onSave()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.ui_save_program),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Program Name Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionLabel(stringResource(R.string.ui_program_name).uppercase())
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = programName,
                        onValueChange = {
                            programName = it
                            draftProgram.name.set(it)
                            isDirty = true
                        },
                        enabled = !readOnly,
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 2. Program Type
            SectionLabel(stringResource(R.string.ui_program_type))
            SingleSelectToggleGroup(
                options = typeOptions.map { it.second },
                selectedOption = selectedTypeLabel,
                onOptionSelected = { selectedLabel ->
                    if (!readOnly) {
                        val newType = typeOptions.first { it.second == selectedLabel }.first
                        if (newType != selectedType) {
                            selectedType = newType
                            if (newType == "Duration") targetValue = 60 else if (newType == "Distance") targetValue = 5000
                            isDirty = true
                            syncProgram()
                        }
                    }
                }
            )

            // 3. Target Input or Interval Segments
            if (selectedType != "Intervals") {
                SectionLabel(stringResource(R.string.ui_target))
                TargetInputCard(
                    value = targetValue,
                    unit = if (selectedType == "Duration") "min" else "m",
                    onValueChange = {
                        if (!readOnly) {
                            targetValue = it
                            isDirty = true
                            syncProgram()
                        }
                    },
                    presets = if (selectedType == "Duration") listOf(10, 20, 30, 45, 60, 90) else listOf(500, 1000, 2000, 5000, 6000, 10000)
                )
            } else {
                SectionLabel(stringResource(R.string.ui_intervals).uppercase())
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        segments.forEachIndexed { index, segment ->
                            SegmentCard(
                                segment = segment,
                                readOnly = readOnly,
                                allowName = selectedType == "Intervals",
                                refreshKey = refreshTrigger,
                                nameValue = segmentNames[segment] ?: segment.name.get().orEmpty(),
                                onNameChange = { name ->
                                    segment.name.set(name)
                                    segmentNames[segment] = name
                                    isDirty = true
                                },
                                onTargetClick = {
                                    if (!readOnly) {
                                        isDirty = true
                                        onShowTargetDialog(segment)
                                    }
                                },
                                onGoalClick = {
                                    if (!readOnly) {
                                        isDirty = true
                                        onShowLimitDialog(segment)
                                    }
                                },
                                onDelete = {
                                    if (!readOnly) {
                                        draftProgram.removeSegment(segment)
                                        segments.clear()
                                        segments.addAll(draftProgram.segments.get())
                                        isDirty = true
                                    }
                                },
                                onCycleDifficulty = {
                                    if (!readOnly) {
                                        segment.difficulty.set(segment.difficulty.get().increase())
                                        segments[index] = segment
                                        isDirty = true
                                    }
                                }
                            )
                        }

                        if (!readOnly) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val newSeg = Segment(Difficulty.EASY).apply { setDuration(300) }
                                        draftProgram.addSegment(newSeg)
                                        segments.add(newSeg)
                                        isDirty = true
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.ui_add_segment), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val rest = Segment(Difficulty.REST).apply { setDuration(60) }
                                        draftProgram.addSegment(rest)
                                        segments.add(rest)
                                        isDirty = true
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.ui_add_rest), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Set Goal (Optional)
            SectionLabel(stringResource(R.string.ui_set_goal_optional))
            GoalSelector(
                selectedGoal = selectedGoal,
                onGoalSelected = { goal ->
                    if (!readOnly) {
                        selectedGoal = goal
                        isDirty = true
                        syncProgram()
                    }
                },
                goalValue = goalValue,
                onGoalValueChange = { valNew ->
                    if (!readOnly) {
                        goalValue = valNew
                        isDirty = true
                        syncProgram()
                    }
                }
            )

            // 5. Workout Summary
            SectionLabel(stringResource(R.string.ui_workout_summary))
            SummaryCard(
                type = selectedType,
                target = if (selectedType == "Intervals") segments.size else targetValue,
                goal = selectedGoal,
                goalValue = goalValue
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun SegmentCard(
    segment: Segment,
    readOnly: Boolean = false,
    allowName: Boolean = false,
    refreshKey: Int = 0,
    nameValue: String = segment.name.get().orEmpty(),
    onNameChange: (String) -> Unit = {},
    onTargetClick: () -> Unit,
    onGoalClick: () -> Unit,
    onDelete: () -> Unit,
    onCycleDifficulty: () -> Unit
) {
    @Suppress("UNUSED_VARIABLE") val targetRefreshKey = refreshKey
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            if (allowName && (!readOnly || !segment.name.get().isNullOrBlank())) {
                OutlinedTextField(
                    value = nameValue,
                    onValueChange = onNameChange,
                    enabled = !readOnly,
                    placeholder = { if (!readOnly) Text(stringResource(R.string.ui_interval_name_hint), fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 6.dp).height(52.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
            // 1. Intensity Accent Strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(getIntensityColor(segment.difficulty.get()))
            )

            // 2. Primary Target
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = !readOnly) { onTargetClick() }
                    .padding(vertical = 12.dp, horizontal = 10.dp)
            ) {
                val (value, subtitle) = when {
                    segment.duration.get() > 0 -> {
                        val s = segment.duration.get()
                        String.format(Locale.getDefault(), "%02d:%02d", s / 60, s % 60) to stringResource(R.string.ui_duration)
                    }
                    segment.distance.get() > 0 -> "%,d m".format(Locale.getDefault(), segment.distance.get()) to stringResource(R.string.ui_distance)
                    segment.strokes.get() > 0 -> "%,d".format(Locale.getDefault(), segment.strokes.get()) to stringResource(R.string.ui_strokes)
                    segment.energy.get() > 0 -> "%,d kcal".format(Locale.getDefault(), segment.energy.get()) to stringResource(R.string.ui_energy)
                    else -> stringResource(R.string.ui_set_target) to stringResource(R.string.ui_action)
                }

                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 3. Goal Chip
            val hasLimit = segment.strokeRate.get() > 0 || segment.pulse.get() > 0 || segment.speed.get() > 0 || segment.power.get() > 0
            val limitText = when {
                segment.strokeRate.get() > 0 -> "${segment.strokeRate.get()} SPM"
                segment.pulse.get() > 0 -> "${segment.pulse.get()} BPM"
                segment.speed.get() > 0 -> String.format(Locale.US, "%.2f m/s", segment.speed.get() / 100f)
                segment.power.get() > 0 -> "${segment.power.get()} W"
                else -> stringResource(if (readOnly) R.string.ui_no_goal else R.string.ui_set_goal)
            }

            Surface(
                onClick = onGoalClick,
                enabled = !readOnly,
                color = if (hasLimit) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = limitText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasLimit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            // 4. Difficulty Indicator & Toggle
            Surface(
                onClick = onCycleDifficulty,
                enabled = !readOnly,
                color = getIntensityColor(segment.difficulty.get()).copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = segment.difficulty.get().name.take(1),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            // 5. Delete Action
            if (!readOnly) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp).padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
}

fun speedToPaceSeconds(speedCmPerSec: Int): Int =
    if (speedCmPerSec <= 0) 120 else (50_000f / speedCmPerSec).toInt()
