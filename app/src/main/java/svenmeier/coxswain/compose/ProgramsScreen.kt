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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import svenmeier.coxswain.Gym
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
    ,onDuplicateProgram: (Program) -> Unit = {}, onDeleteProgram: (Program) -> Unit = {}, onSaveLibraryProgram: (Program) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var previewProgram by remember { mutableStateOf<Program?>(null) }
    var pendingDelete by remember { mutableStateOf<Program?>(null) }
    val programs = remember { mutableStateListOf<Program>() }
    previewProgram?.let { preview ->
        AlertDialog(
            onDismissRequest = { previewProgram = null },
            title = { Text(preview.name.get()) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { preview.segments.get().forEachIndexed { index, segment -> Text("${index + 1}. ${if (segment.difficulty.get() == Difficulty.REST) "Rest · " else "Row · "}${segment.describeTarget()}") } } },
            confirmButton = { TextButton(onClick = { previewProgram = null }) { Text("Done") } }
        )
    }
    pendingDelete?.let { target ->
        AlertDialog(onDismissRequest = { pendingDelete = null }, title = { Text("Delete program?") }, text = { Text("${target.name.get()} will be removed. Completed workout history is preserved.") }, confirmButton = { TextButton(onClick = { onDeleteProgram(target); programs.remove(target); pendingDelete = null }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } })
    }
    
    LaunchedEffect(selectedTab, refreshKey) {
        programs.clear()
        if (selectedTab == 0) {
            programs.addAll(gym.getPrograms().list())
        } else {
            programs.addAll(curatedPrograms())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FB))
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        
        SingleSelectToggleGroup(
            options = listOf("My Programs", "Workout Library"),
            selectedOption = if (selectedTab == 0) "My Programs" else "Workout Library",
            onOptionSelected = { selectedTab = if (it == "My Programs") 0 else 1 }
        )

        Spacer(Modifier.height(20.dp))

        if (selectedTab == 0) {
            Button(
                onClick = onCreateProgram,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B63F6))
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create program", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (selectedTab == 0) "MY PROGRAMS" else "WORKOUT LIBRARY",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold, 
                    letterSpacing = 1.sp
                ),
                color = Color(0xFF53647C)
            )
            Text(
                text = "${programs.size} programs",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF53647C)
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
    onStart: () -> Unit
    ,isLibrary: Boolean = false, onDuplicate: () -> Unit = {}, onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        color = Color(0xFF10213F)
                    )
                    
                    val segments = program.segments.get()
                    val summary = if (segments.isEmpty()) "Empty program" 
                                 else if (segments.size == 1) segments[0].describeTarget()
                                 else "${segments.size} segments"
                                 
                    Text(
                        text = summary,
                        fontSize = 14.sp,
                        color = Color(0xFF53647C),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color(0xFF53647C))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("View program") }, onClick = { menuOpen = false; onView() })
                        DropdownMenuItem(text = { Text(if (isLibrary) "Save to My Programs" else "Duplicate") }, onClick = { menuOpen = false; onDuplicate() })
                        if (!isLibrary) DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
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
                    Text("View program", color = Color(0xFF0B63F6), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onStart,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B63F6)),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun curatedPrograms(): List<Program> = listOf(
    Program.minutes("Steady 20", 20, Difficulty.EASY),
    Program.meters("Foundation 2K", 2000, Difficulty.MEDIUM),
    Program.minutes("Power 30", 30, Difficulty.HARD)
)

fun Segment.describeTarget(): String {
    return if (duration.get() > 0) "${duration.get() / 60} min"
           else if (distance.get() > 0) "${distance.get()} m"
           else if (strokes.get() > 0) "${strokes.get()} strokes"
           else "Target set"
}
