package svenmeier.coxswain

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import propoid.db.Reference
import svenmeier.coxswain.compose.CoxswainTheme
import svenmeier.coxswain.compose.getIntensityColor
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
        gym = Gym.instance(this)

        val reference = Reference.from<Program>(intent)
        originalProgram = if (reference != null) gym.getProgram(reference) else null

        val isReadOnly = intent.getBooleanExtra(EXTRA_READ_ONLY, false)

        draftProgram = if (originalProgram != null) {
            cloneProgram(originalProgram!!)
        } else {
            Program(getString(R.string.program_name_new)).apply {
                segments.get().clear()
                addSegment(Segment(Difficulty.EASY).setDistance(500))
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
    var programName by remember { mutableStateOf(draftProgram.name.get() ?: "") }
    var isDirty by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val segments = remember { mutableStateListOf<Segment>().apply { addAll(draftProgram.segments.get()) } }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (readOnly) {
                        Text(
                            text = programName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        TextField(
                            value = programName,
                            onValueChange = {
                                programName = it
                                draftProgram.name.set(it)
                                isDirty = true
                            },
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
                            onClick = onSave,
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
        },
        floatingActionButton = {
            if (!readOnly) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val newSegment = Segment(Difficulty.EASY)
                        newSegment.distance.set(500)
                        draftProgram.segments.get().add(newSegment)
                        segments.add(newSegment)
                        isDirty = true
                    },
                    containerColor = Color(0xFF0B63F6),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(28.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.ui_add_segment), fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF4F7FB)),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(segments, key = { index, _ -> index }) { index, segment ->
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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

            // Primary Content Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = !readOnly) { onTargetClick() }
                    .padding(vertical = 14.dp, horizontal = 8.dp)
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10213F)
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF53647C),
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            // 4. Goal Chip
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
                color = if (hasLimit) Color(0xFFDCEBFF) else Color(0xFFF4F7FB),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = limitText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasLimit) Color(0xFF0B63F6) else Color(0xFF53647C),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            // 5. Difficulty Cycle (Indicator & Toggle)
            Surface(
                onClick = onCycleDifficulty,
                enabled = !readOnly,
                color = getIntensityColor(segment.difficulty.get()).copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = segment.difficulty.get().name.take(1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = getIntensityColor(segment.difficulty.get()),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            // 6. Delete Action
            if (!readOnly) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp).padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = Color(0xFFBA1A1A).copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
