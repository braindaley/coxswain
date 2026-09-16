package svenmeier.coxswain.compose.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import svenmeier.coxswain.Gym
import svenmeier.coxswain.gym.Measurement
import svenmeier.coxswain.view.ValueBinding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveRowScreen(
    gym: Gym,
    refreshTick: Int = 0,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit,
    onEditMetric: (Int) -> Unit
) {
    // Reading this state makes live measurements invalidate the metric grid.
    @Suppress("UNUSED_VARIABLE") val measurementVersion = refreshTick
    var isPaused by remember { mutableStateOf(false) } 
    var editMode by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) }
    val context = LocalContext.current
    
    val activeMetrics = remember {
        val fallback = listOf(
            ValueBinding.DURATION,
            ValueBinding.DISTANCE,
            ValueBinding.SPLIT,
            ValueBinding.STROKE_RATE,
            ValueBinding.POWER,
            ValueBinding.PULSE)
        val saved = context.getSharedPreferences("live_row", 0).getString("metrics", null)
            ?.split(",")?.mapNotNull { runCatching { ValueBinding.valueOf(it) }.getOrNull() }
        mutableStateListOf(*(if (saved?.size == 6) saved else fallback).toTypedArray())
    }
    if (editMode && editingIndex >= 0) {
        AlertDialog(onDismissRequest = { editingIndex = -1 }, title = { Text("Choose metric") },
            text = { Column { listOf(ValueBinding.DURATION, ValueBinding.DISTANCE, ValueBinding.SPLIT, ValueBinding.STROKE_RATE, ValueBinding.POWER, ValueBinding.PULSE, ValueBinding.SPEED, ValueBinding.ENERGY).forEach { metric ->
                Row(Modifier.fillMaxWidth().clickable { activeMetrics[editingIndex] = metric; editingIndex = -1; context.getSharedPreferences("live_row", 0).edit().putString("metrics", activeMetrics.joinToString(",") { it.name }).apply() }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = activeMetrics[editingIndex] == metric, onClick = null); Text(metric.name.replace('_', ' '), color = Color.White)
                }
            } } }, confirmButton = { TextButton(onClick = { editingIndex = -1 }) { Text("Done") } })
    }

    Scaffold(
        containerColor = Color(0xFF042C3D), 
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (gym.program != null) gym.program.name.get().uppercase() else "FREE ROW",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFC8E3E9)
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF22C55E).copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "● Connected",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = Color(0xFF22C55E),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { editMode = !editMode }) {
                        Text(if (editMode) "Done" else "Edit display", color = Color(0xFFDCEBFF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { if (isPaused) onResume() else onPause(); isPaused = !isPaused },
                    modifier = Modifier.weight(1f).height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B63F6)),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(if (isPaused) "Resume" else "Pause", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = onEnd,
                    modifier = Modifier.weight(1f).height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCEBFF)),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text("End session", color = Color(0xFF10213F), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).background(Color(0xFF31505D)),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                itemsIndexed(activeMetrics) { index, binding ->
                    MetricCell(
                        binding = binding,
                        measurement = gym.getMeasurement(), // Use getter
                        onClick = { if (editMode) { editingIndex = index; onEditMetric(index) } }
                    )
                }
            }
            
            if (gym.program != null) {
                TargetProgressBar(gym)
            }
        }
    }
}

@Composable
fun MetricCell(
    binding: ValueBinding,
    measurement: Measurement,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val valueStr = binding.format(context, getValueForBinding(binding, measurement), false)
    val label = context.getString(binding.label).uppercase()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF042C3D))
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = valueStr,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 56.sp,
                    fontWeight = FontWeight.W500,
                    textAlign = TextAlign.Center
                ),
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFCAD4E1),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TargetProgressBar(gym: Gym) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF123F51))
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("WORKOUT PROGRESS", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCAD4E1))
            Text("${((gym.progress?.completion() ?: 0f) * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color(0xFF83D7FF))
        }
        Text(
            text = gym.progress?.describe() ?: "Ready",
            style = MaterialTheme.typography.titleLarge, 
            color = Color.White,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        LinearProgressIndicator(
            progress = { gym.progress?.completion() ?: 0f },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Color(0xFF0B8FFF),
            trackColor = Color(0xFF53647C),
            strokeCap = StrokeCap.Round
        )
    }
}

private fun getValueForBinding(binding: ValueBinding, m: Measurement): Int {
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
