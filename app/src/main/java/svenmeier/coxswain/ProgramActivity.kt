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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
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
import svenmeier.coxswain.view.MaterialLimitPickerDialog
import svenmeier.coxswain.view.MaterialTargetPickerDialog
import java.util.Locale

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
    @Suppress("UNUSED_VARIABLE") val trigger = refreshTrigger

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
                    Text(stringResource(R.string.ui_discard), color = Color(0xFFBA1A1A))
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
                    color = Color.White,
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B63F6))
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
                .background(Color(0xFFF4F7FB))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Program Name Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            color = Color(0xFF10213F)
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            OutlinedButton(
                                onClick = {
                                    val newSeg = Segment(Difficulty.EASY).apply { setDuration(300) }
                                    draftProgram.addSegment(newSeg)
                                    segments.add(newSeg)
                                    isDirty = true
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, Color(0xFF0B63F6))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF0B63F6))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.ui_add_segment), color = Color(0xFF0B63F6), fontWeight = FontWeight.Bold)
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
    onTargetClick: () -> Unit,
    onGoalClick: () -> Unit,
    onDelete: () -> Unit,
    onCycleDifficulty: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFD)),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
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
                    color = Color(0xFF10213F)
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF53647C)
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
                color = if (hasLimit) Color(0xFFDCEBFF) else Color(0xFFE8EEF6),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = limitText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasLimit) Color(0xFF0B63F6) else Color(0xFF53647C),
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
                    color = getIntensityColor(segment.difficulty.get()),
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
                        tint = Color(0xFFBA1A1A).copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

fun speedToPaceSeconds(speedCmPerSec: Int): Int =
    if (speedCmPerSec <= 0) 120 else (50_000f / speedCmPerSec).toInt()
