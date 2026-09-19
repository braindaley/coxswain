package svenmeier.coxswain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import svenmeier.coxswain.compose.*
import svenmeier.coxswain.gym.Program
import svenmeier.coxswain.gym.WorkoutDefinition
import svenmeier.coxswain.bluetooth.BluetoothActivity
import svenmeier.coxswain.bluetooth.BlueWriter

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gym = Gym.instance(this)

        setContent {
            CoxswainTheme {
                MainContainer(gym, this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(gym: Gym, activity: MainActivity) {
    var currentTab by remember { mutableIntStateOf(0) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refreshKey++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    val tabs = listOf(
        TabItem("Home", Icons.Default.Home),
        TabItem("Programs", Icons.AutoMirrored.Filled.List),
        TabItem("History", Icons.Default.Refresh),
        TabItem("More", Icons.Default.MoreVert)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.height(80.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = currentTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = index },
                        icon = { 
                            Surface(
                                color = if (isSelected) Color(0xFFDCEBFF) else Color.Transparent,
                                shape = MaterialTheme.shapes.extraLarge,
                                modifier = Modifier.size(width = 64.dp, height = 32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = tab.icon, 
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (isSelected) Color(0xFF0B63F6) else Color(0xFF53647C)
                                    )
                                }
                            }
                        },
                        label = { 
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF0B63F6) else Color(0xFF53647C)
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentTab) {
                0 -> HomeScreen(
                    gym = gym,
                    refreshKey = refreshKey,
                    onFreeRow = {
                        gym.startFreeRow()
                        WorkoutActivity.start(activity)
                    },
                    onQuickDuration = { WorkoutSetupActivity.start(activity, "Duration") },
                    onQuickDistance = { WorkoutSetupActivity.start(activity, "Distance") },
                    onMyPrograms = { currentTab = 1 },
                    onLibrary = { currentTab = 1 },
                    onWorkoutDetails = { WorkoutDetailsActivity.start(activity, it) },
                    onRowAgain = { workout ->
                        val definition = WorkoutDefinition.thaw(workout.programDefinition.get())
                        if (definition == null) gym.startFreeRow() else gym.start(definition, WorkoutDefinition.typeOf(definition))
                        WorkoutActivity.start(activity)
                    }
                )
                1 -> ProgramsScreen(
                    gym = gym,
                    refreshKey = refreshKey,
                    onCreateProgram = {
                        val p = gym.newProgram()
                        activity.startActivity(ProgramActivity.createIntent(activity, p))
                    },
                    onEditProgram = { program ->
                        activity.startActivity(if (gym.hasWorkoutHistory(program)) ProgramActivity.createReadOnlyIntent(activity, program) else ProgramActivity.createIntent(activity, program))
                    },
                    onStartProgram = { program ->
                        gym.select(program)
                        WorkoutActivity.start(activity)
                    },
                    onDuplicateProgram = { program ->
                        gym.duplicateProgram(program, "${program.name.get()} copy")
                    },
                    onDeleteProgram = { program ->
                        gym.delete(program)
                    },
                    onSaveLibraryProgram = { program ->
                        gym.duplicateProgram(program, program.name.get())
                    },
                    onRaceProgram = { RaceYourBestActivity.start(activity, it) }
                )
                2 -> WorkoutsScreen(
                    gym = gym,
                    refreshKey = refreshKey,
                    onWorkoutClick = { WorkoutDetailsActivity.start(activity, it) }
                )
                3 -> MoreScreen(
                    onConnect = { BluetoothActivity.start(activity, "Rower", BlueWriter.SERVICE_FITNESS_MACHINE.toString()) },
                    onSettings = { activity.startActivity(SettingsActivity.createIntent(activity)) },
                    onDataExport = { activity.startActivity(SettingsActivity.createIntent(activity)) }
                )
            }
        }
    }
}

data class TabItem(val title: String, val icon: ImageVector)
