package svenmeier.coxswain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import svenmeier.coxswain.compose.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import svenmeier.coxswain.gym.Program
import svenmeier.coxswain.gym.WorkoutDefinition
import svenmeier.coxswain.google.HealthConnectExport
import svenmeier.coxswain.google.HealthConnectManageActivity
import svenmeier.coxswain.pete.PetePlanStore
import svenmeier.coxswain.pete.PeteGoal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val gym = Gym.instance(this)

        if (savedInstanceState == null) handleIntent(intent, gym)

        setContent {
            CoxswainTheme {
                MainContainer(gym, this)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent, Gym.instance(this))
    }

    private fun handleIntent(intent: Intent?, gym: Gym) {
        if (intent == null) return
        if (Intent.ACTION_VIEW == intent.action || Intent.ACTION_SEND == intent.action) {
            svenmeier.coxswain.io.ImportIntention(this).onIntent(intent)
            return
        }

        if (UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
            @Suppress("DEPRECATION")
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            }
            if (device != null) {
                GymService.start(this, device)

                if (gym.program == null) {
                    // Switch to home or show connection
                    window.addFlags(
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    )
                } else {
                    // Program already selected, go to workout
                    WorkoutActivity.start(this)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(gym: Gym, activity: MainActivity) {
    val peteStore = remember(gym) { PetePlanStore(activity, gym) }
    var currentTab by remember { mutableIntStateOf(0) }
    var showPetePlan by remember { mutableStateOf(false) }
    var initialPeteSession by remember { mutableStateOf<Int?>(null) }
    var connectRequest by remember { mutableIntStateOf(0) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val backupSaved = stringResource(R.string.ui_backup_saved)
    val backupFailed = stringResource(R.string.ui_backup_failed)
    val backupRestored = stringResource(R.string.ui_backup_restored)
    val restoreFailed = stringResource(R.string.ui_restore_failed)
    val backupReadFailed = stringResource(R.string.ui_backup_read_failed)
    val programCopy = stringResource(R.string.ui_program_copy)
    val backupScope = rememberCoroutineScope()
    var backupBusy by remember { mutableStateOf(false) }
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) backupScope.launch {
            backupBusy = true
            runCatching { withContext(Dispatchers.IO) {
                val output = activity.contentResolver.openOutputStream(uri) ?: error("Cannot open backup destination")
                output.bufferedWriter().use { it.write(gym.createBackup()) }
            } }.onSuccess { Toast.makeText(activity, backupSaved, Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(activity, backupFailed.format(it.message), Toast.LENGTH_LONG).show() }
            backupBusy = false
        }
    }
    val restoreBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) backupScope.launch {
            backupBusy = true
            runCatching { withContext(Dispatchers.IO) {
                check(!gym.hasActiveSession()) { "End the active row before restoring a backup" }
                activity.contentResolver.openInputStream(uri)?.bufferedReader()?.use { gym.restoreBackup(it.readText()) } ?: error(backupReadFailed)
            } }.onSuccess { refreshKey++; Toast.makeText(activity, backupRestored, Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(activity, restoreFailed.format(it.message), Toast.LENGTH_LONG).show() }
            backupBusy = false
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(gym) {
        val listener = Gym.Listener { scope ->
            if (scope == null || scope is Program || scope is svenmeier.coxswain.gym.Workout) refreshKey++
        }
        gym.addListener(listener)
        onDispose { gym.removeListener(listener) }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refreshKey++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    val tabs = listOf(
        TabItem(stringResource(R.string.ui_home), Icons.Default.Home),
        TabItem(stringResource(R.string.ui_programs), Icons.AutoMirrored.Filled.List),
        TabItem(stringResource(R.string.ui_history), Icons.Default.History),
        TabItem(stringResource(R.string.ui_more), Icons.Default.MoreHoriz)
    )

    if (backupBusy) AlertDialog(onDismissRequest = {}, confirmButton = {}, title = { Text("Working on backup") }, text = { CircularProgressIndicator() })
    Scaffold(
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    windowInsets = NavigationBarDefaults.windowInsets
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = currentTab == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                currentTab = index
                                showPetePlan = false
                                initialPeteSession = null
                                if (index != 3) connectRequest = 0
                            },
                            icon = {
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                                    shape = MaterialTheme.shapes.extraLarge,
                                    modifier = Modifier.size(width = 56.dp, height = 28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (showPetePlan) {
                PetePlanScreen(
                    gym = gym,
                    store = peteStore,
                    refreshKey = refreshKey,
                    initialSessionIndex = initialPeteSession,
                    onInitialSessionConsumed = { initialPeteSession = null },
                    onBack = { showPetePlan = false },
                    onStartWorkout = { session, goal, state ->
                        val program = session.program(goal)
                        gym.startPlanSession(
                            program, state.enrollmentId, state.activeWeek, state.activeAttempt,
                            session.index, goal.kind, goal.value,
                            goal.sourceWorkout?.start?.get() ?: 0L,
                            (goal as? PeteGoal.RowAgainst)?.source
                        )
                        WorkoutActivity.start(activity)
                    }
                )
            } else when (currentTab) {
                0 -> HomeScreen(
                    gym = gym,
                    refreshKey = refreshKey,
                    peteStore = peteStore,
                    onOpenPetePlan = { showPetePlan = true },
                    onPeteSession = { index -> initialPeteSession = index; showPetePlan = true },
                    onFreeRow = {
                        gym.startFreeRow()
                        WorkoutActivity.start(activity)
                    },
                    onQuickStart = { WorkoutSetupActivity.start(activity, it) },
                    onConnectRower = { connectRequest++; currentTab = 3 },
                    onSettings = { activity.startActivity(SettingsActivity.createIntent(activity)) },
                    onWorkoutDetails = { WorkoutDetailsActivity.start(activity, it) },
                    onRowAgain = { workout ->
                        val definition = WorkoutDefinition.thaw(workout.programDefinition.get())
                        if (definition == null) {
                            gym.startFreeRow()
                        } else {
                            gym.startPreferredProgram(definition)
                        }
                        WorkoutActivity.start(activity)
                    }
                )
                1 -> ProgramsScreen(
                    gym = gym,
                    refreshKey = refreshKey,
                    onCreateProgram = {
                        activity.startActivity(ProgramActivity.createIntent(activity, null))
                    },
                    onEditProgram = { program ->
                        activity.startActivity(ProgramActivity.createReadOnlyIntent(activity, program))
                    },
                    onStartProgram = { program ->
                        gym.startPreferredProgram(program)
                        WorkoutActivity.start(activity)
                    },
                    onDuplicateProgram = { program ->
                        gym.duplicateProgram(program, programCopy.format(program.name.get()))
                    },
                    onDeleteProgram = { program ->
                        gym.delete(program)
                    },
                    onSaveLibraryProgram = { program ->
                        gym.duplicateProgram(program, program.name.get())
                    }
                )
                2 -> WorkoutsScreen(
                    gym = gym,
                    refreshKey = refreshKey,
                    onWorkoutClick = { WorkoutDetailsActivity.start(activity, it) }
                )
                3 -> MoreScreen(
                    gym = gym,
                    connectRequestKey = connectRequest,
                    onConnect = { GymService.start(activity, GymService.CONNECTOR_BLUETOOTH) },
                    onDisconnect = { GymService.start(activity, GymService.CONNECTOR_NONE) },
                    onSettings = { activity.startActivity(SettingsActivity.createIntent(activity)) },
                    onHealthSettings = { HealthConnectManageActivity.start(activity) },
                    onEnableAutomaticHealthExport = { HealthConnectExport.enableAutomatic(activity) },
                    onSyncHealthHistory = { HealthConnectExport.syncHistory(activity) },
                    onBackup = { createBackup.launch("coxswain-backup.json") },
                    onRestore = { restoreBackup.launch(arrayOf("application/json", "application/octet-stream")) }
                )
            }
        }
    }
}

data class TabItem(val title: String, val icon: ImageVector)
