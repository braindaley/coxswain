package svenmeier.coxswain.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import svenmeier.coxswain.Gym
import svenmeier.coxswain.R
import svenmeier.coxswain.gym.Program
import svenmeier.coxswain.gym.Segment
import svenmeier.coxswain.gym.Difficulty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramsScreen(
    gym: Gym,
    refreshKey: Int = 0,
    onCreateProgram: () -> Unit,
    onEditProgram: (Program) -> Unit,
    onStartProgram: (Program) -> Unit
    ,onDuplicateProgram: (Program) -> Unit = {}, onDeleteProgram: (Program) -> Unit = {}, onSaveLibraryProgram: (Program) -> Unit = {}, onRaceProgram: (Program) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var previewProgram by remember { mutableStateOf<Program?>(null) }
    var pendingDelete by remember { mutableStateOf<Program?>(null) }
    val programs = remember { mutableStateListOf<Program>() }
    val myProgramsLabel = stringResource(R.string.ui_my_programs)
    val workoutLibraryLabel = stringResource(R.string.ui_workout_library)
    val libraryProgramNames = listOf(
        stringResource(R.string.ui_library_steady_20),
        stringResource(R.string.ui_library_foundation_2k),
        stringResource(R.string.ui_library_power_30),
        stringResource(R.string.ui_library_intervals_4x2)
    )
    previewProgram?.let { preview ->
        AlertDialog(
            onDismissRequest = { previewProgram = null },
            title = { Text(preview.name.get()) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { preview.segments.get().forEachIndexed { index, segment -> Text(stringResource(R.string.ui_segment_preview, index + 1, stringResource(if (segment.difficulty.get() == Difficulty.REST) R.string.ui_rest else R.string.ui_row), segmentTargetText(segment))) } } },
            confirmButton = { TextButton(onClick = { previewProgram = null }) { Text(stringResource(R.string.ui_done)) } }
        )
    }
    pendingDelete?.let { target ->
        AlertDialog(onDismissRequest = { pendingDelete = null }, title = { Text(stringResource(R.string.ui_delete_program_question)) }, text = { Text(stringResource(R.string.ui_delete_program_explanation, target.name.get())) }, confirmButton = { TextButton(onClick = { onDeleteProgram(target); programs.remove(target); pendingDelete = null }) { Text(stringResource(R.string.action_delete)) } }, dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.ui_cancel)) } })
    }
    
    LaunchedEffect(selectedTab, refreshKey) {
        programs.clear()
        if (selectedTab == 0) {
            programs.addAll(gym.getPrograms().list())
        } else {
            programs.addAll(curatedPrograms(libraryProgramNames))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        
        SingleSelectToggleGroup(
            options = listOf(myProgramsLabel, workoutLibraryLabel),
            selectedOption = if (selectedTab == 0) myProgramsLabel else workoutLibraryLabel,
            onOptionSelected = { selectedTab = if (it == myProgramsLabel) 0 else 1 }
        )

        Spacer(Modifier.height(20.dp))

        if (selectedTab == 0) {
            Button(
                onClick = onCreateProgram,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ui_create_program), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = (if (selectedTab == 0) myProgramsLabel else workoutLibraryLabel).uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold, 
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = pluralStringResource(R.plurals.ui_program_count, programs.size, programs.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(programs) { program ->
                ProgramCard(
                    program = program,
                    onView = { if (selectedTab == 0) onEditProgram(program) else previewProgram = program },
                    onStart = { onStartProgram(program) },
                    onRace = { if (selectedTab == 0) onRaceProgram(program) },
                    isLibrary = selectedTab == 1,
                    onDuplicate = {
                        if (selectedTab == 0) {
                            onDuplicateProgram(program)
                            programs.clear(); programs.addAll(gym.getPrograms().list())
                        } else onSaveLibraryProgram(program)
                    },
                    onDelete = { pendingDelete = program }
                )
            }
        }
    }
}

@Composable
fun ProgramCard(
    program: Program,
    onView: () -> Unit,
    onStart: () -> Unit,
    isLibrary: Boolean = false,
    onDuplicate: () -> Unit = {},
    onDelete: () -> Unit = {},
    onRace: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onView() }.testTag("program-${program.name.get()}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = program.name.get(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    val segments = program.segments.get()
                    val summary = if (segments.isEmpty()) stringResource(R.string.ui_empty_program)
                                 else if (segments.size == 1) segmentTargetText(segments[0])
                                 else pluralStringResource(R.plurals.ui_segment_count, segments.size, segments.size)
                                 
                    Text(
                        text = summary,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.ui_menu), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.ui_view_program)) }, onClick = { menuOpen = false; onView() })
                        DropdownMenuItem(text = { Text(stringResource(if (isLibrary) R.string.ui_save_to_my_programs else R.string.action_duplicate)) }, onClick = { menuOpen = false; onDuplicate() })
                        if (!isLibrary) DropdownMenuItem(text = { Text(stringResource(R.string.action_delete)) }, onClick = { menuOpen = false; onDelete() })
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onView,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(stringResource(R.string.ui_view_program), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onStart,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ui_start), fontWeight = FontWeight.Bold)
                }
            }
            if (!isLibrary) TextButton(onClick = onRace, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_race_your_best), color = MaterialTheme.colorScheme.primary) }
        }
    }
}

fun curatedPrograms(names: List<String>): List<Program> = listOf(
    Program.minutes(names[0], 20, Difficulty.EASY),
    Program.meters(names[1], 2000, Difficulty.MEDIUM),
    Program.minutes(names[2], 30, Difficulty.HARD),
    Program(names[3]).also { program ->
        program.segments.get().clear()
        program.addSegment(Segment(Difficulty.HARD).setDuration(120))
        program.addSegment(Segment(Difficulty.REST).setDuration(60))
        program.addSegment(Segment(Difficulty.HARD).setDuration(120))
        program.addSegment(Segment(Difficulty.REST).setDuration(60))
        program.addSegment(Segment(Difficulty.HARD).setDuration(120))
        program.addSegment(Segment(Difficulty.REST).setDuration(60))
        program.addSegment(Segment(Difficulty.HARD).setDuration(120))
    }
)

@Composable
private fun segmentTargetText(segment: Segment): String = when {
    segment.duration.get() > 0 -> stringResource(R.string.ui_minutes_value, segment.duration.get() / 60)
    segment.distance.get() > 0 -> stringResource(R.string.ui_meters_value, segment.distance.get())
    segment.strokes.get() > 0 -> pluralStringResource(R.plurals.ui_strokes_value, segment.strokes.get(), segment.strokes.get())
    else -> stringResource(R.string.ui_target_set)
}

fun Segment.describeTarget(): String {
    return if (duration.get() > 0) "${duration.get() / 60} min"
           else if (distance.get() > 0) "${distance.get()} m"
           else if (strokes.get() > 0) "${strokes.get()} strokes"
           else "Target set"
}
