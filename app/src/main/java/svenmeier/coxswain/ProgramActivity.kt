package svenmeier.coxswain

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
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
    private var program: Program? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gym = Gym.instance(this)

        val reference = Reference.from<Program>(intent)
        program = gym.getProgram(reference)

        if (program == null) {
            finish()
            return
        }

        setContent {
            CoxswainTheme {
                ProgramEditorScreen(
                    program = program!!,
                    onBack = { finish() },
                    onMerge = { gym.mergeProgram(program) },
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

    override fun onPause() {
        super.onPause()
        program?.let { gym.mergeProgram(it) }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context, program: Program): Intent {
            val intent = Intent(context, ProgramActivity::class.java)
            intent.data = Reference(program).toUri()
            return intent
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramEditorScreen(
    program: Program,
    onBack: () -> Unit,
    onMerge: () -> Unit,
    onShowTargetDialog: (Segment) -> Unit,
    onShowLimitDialog: (Segment) -> Unit
) {
    var programName by remember { mutableStateOf(program.name.get() ?: "") }
    val segments = remember { mutableStateListOf<Segment>().apply { addAll(program.segments.get()) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = programName,
                        onValueChange = { 
                            programName = it
                            program.name.set(it)
                            onMerge()
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
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val newSegment = Segment(Difficulty.EASY)
                    newSegment.distance.set(500)
                    program.segments.get().add(newSegment)
                    onMerge()
                    segments.add(newSegment)
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Segment") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            itemsIndexed(segments, key = { _, item -> item.hashCode() }) { index, segment ->
                SegmentCard(
                    segment = segment,
                    onTargetClick = { onShowTargetDialog(segment) },
                    onGoalClick = { onShowLimitDialog(segment) },
                    onDelete = {
                        program.segments.get().remove(segment)
                        onMerge()
                        segments.remove(segment)
                    },
                    onCycleDifficulty = {
                        segment.difficulty.set(segment.difficulty.get().increase())
                        onMerge()
                        segments[index] = segment 
                    }
                )
            }
        }
    }
}

@Composable
fun SegmentCard(
    segment: Segment,
    onTargetClick: () -> Unit,
    onGoalClick: () -> Unit,
    onDelete: () -> Unit,
    onCycleDifficulty: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
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
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(getIntensityColor(segment.difficulty.get()))
            )

            // 2. Drag Handle (Affordance only for now)
            Icon(
                painter = painterResource(id = R.drawable.ic_reorder_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 12.dp, end = 8.dp).size(20.dp)
            )

            // 3. Primary Target Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTargetClick() }
                    .padding(vertical = 16.dp, horizontal = 8.dp)
            ) {
                val (value, subtitle) = when {
                    segment.duration.get() > 0 -> {
                        val s = segment.duration.get()
                        String.format("%02d:%02d", s / 60, s % 60) to "Duration"
                    }
                    segment.distance.get() > 0 -> "%,d m".format(segment.distance.get()) to "Distance"
                    segment.strokes.get() > 0 -> "%,d".format(segment.strokes.get()) to "Strokes"
                    segment.energy.get() > 0 -> "%,d kcal".format(segment.energy.get()) to "Energy"
                    else -> "Set target" to "Action"
                }

                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 4. Pace Goal Chip
            val limitText = when {
                segment.strokeRate.get() > 0 -> "${segment.strokeRate.get()} spm"
                segment.pulse.get() > 0 -> "${segment.pulse.get()} bpm"
                segment.speed.get() > 0 -> String.format(Locale.US, "%.2f m/s", segment.speed.get() / 100f)
                segment.power.get() > 0 -> "${segment.power.get()} W"
                else -> null
            }

            if (limitText != null) {
                SuggestionChip(
                    onClick = onGoalClick,
                    label = { Text(limitText, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            } else {
                TextButton(
                    onClick = onGoalClick,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("+ Set goal", style = MaterialTheme.typography.labelMedium)
                }
            }

            // 5. Difficulty Cycle
            Surface(
                onClick = onCycleDifficulty,
                color = getIntensityColor(segment.difficulty.get()).copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = segment.difficulty.get().name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = getIntensityColor(segment.difficulty.get()),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // 6. Delete Action
            IconButton(
                onClick = onDelete,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
