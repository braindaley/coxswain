package svenmeier.coxswain.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import svenmeier.coxswain.Gym
import svenmeier.coxswain.gym.Workout
import svenmeier.coxswain.pete.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.max

private val PlanBlue: Color
    @Composable get() = MaterialTheme.colorScheme.primary
private val PlanMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun PetePlanScreen(
    gym: Gym,
    store: PetePlanStore,
    refreshKey: Int,
    initialSessionIndex: Int?,
    onInitialSessionConsumed: () -> Unit,
    onBack: () -> Unit,
    onStartWorkout: (PeteSession, PeteGoal, PetePlanState) -> Unit
) {
    val context = LocalContext.current
    val resolver = remember(store, gym) { PeteGoalResolver.load(context, store, gym) }
    var revision by remember { mutableIntStateOf(0) }
    @Suppress("UNUSED_VARIABLE") val refresh = refreshKey + revision
    val planState = store.state()
    var page by remember { mutableStateOf(if (planState.started) "overview" else "intro") }
    var viewWeek by remember { mutableIntStateOf(planState.activeWeek) }
    var brief by remember { mutableStateOf<PeteSession?>(null) }
    var chartMetric by remember { mutableStateOf("meters") }
    var selectedChartWeek by remember { mutableIntStateOf(planState.activeWeek) }
    var confirmStop by remember { mutableStateOf(false) }
    var estimateSplit by remember(refresh) { mutableIntStateOf(store.estimateSplit()) }

    LaunchedEffect(initialSessionIndex) {
        if (initialSessionIndex != null && planState.started) {
            page = "week"
            viewWeek = planState.activeWeek
            brief = store.catalog.session(planState.activeWeek, initialSessionIndex)
            onInitialSessionConsumed()
        }
    }
    fun goBack() {
        when {
            brief != null -> brief = null
            page == "week" -> page = "overview"
            else -> onBack()
        }
    }
    BackHandler { goBack() }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF0B63F6)).height(76.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = ::goBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text(
                when (page) { "week" -> "Week $viewWeek"; else -> "Pete's Plan" },
                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
            )
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (page) {
                "intro" -> {
                    Text("24 weeks of steady progress", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Pete's Plan builds your indoor rowing gradually. Each Sunday–Saturday week has three required rows and two optional rows. Pick the full row that fits your day.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val firstEnd = LocalDate.now().with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SATURDAY))
                    Text("Start today · Week 1 ends ${planDate(firstEnd)}", color = PlanBlue, fontWeight = FontWeight.Bold)
                    EstimatePaceCard(estimateSplit) { estimateSplit = it; store.setEstimateSplit(it) }
                    PlanCard {
                        Text("Week 1 preview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        store.catalog.weeks[0].forEach { session ->
                            SessionRow(session, false, session.estimatedMinutes(estimateSplit), onClick = {})
                        }
                    }
                    Button(
                        onClick = { store.enroll(); revision++; page = "overview"; viewWeek = 1 },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(27.dp)
                    ) { Text("Start week 1", fontWeight = FontWeight.Bold) }
                }
                "overview" -> {
                    val completed = store.completedWeeks(planState)
                    Text("$completed of 24 weeks complete", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Three required rows complete a week. Optional rows add training without affecting advancement.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!planState.finished) Text(
                        "Week ${planState.activeWeek} · ${planDateRange(planState)}",
                        color = PlanBlue, fontWeight = FontWeight.Bold
                    )
                    if (planState.pendingRollover) RolloverCard(planState, store.requiredComplete(planState), onDecision = {
                        store.resolveRollover(it); revision++; viewWeek = store.state().activeWeek
                    })
                    EstimatePaceCard(estimateSplit) { estimateSplit = it; store.setEstimateSplit(it) }
                    if (planState.finished) PlanCard { Text("24-week plan finished", fontWeight = FontWeight.Bold) }
                    if (planState.started) PlanProgressChart(store, planState, chartMetric, selectedChartWeek,
                        onMetric = { chartMetric = it }, onWeek = { selectedChartWeek = it })
                    if (planState.started) {
                        TextButton(
                            onClick = { confirmStop = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Stop Pete's Plan", color = MaterialTheme.colorScheme.error) }
                    }
                    Text("All weeks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    (1..24).chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { week ->
                                val latestAttempt = store.latestAttempt(planState, week)
                                val required = store.requiredComplete(planState, week, latestAttempt)
                                Card(
                                    modifier = Modifier.weight(1f).clickable { viewWeek = week; page = "week" },
                                    colors = CardDefaults.cardColors(containerColor = if (week == planState.activeWeek) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(Modifier.padding(13.dp)) {
                                        Text("Week $week", fontWeight = FontWeight.Bold)
                                        Text("$required/3 required", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                        LinearProgressIndicator(
                                            progress = { required / 3f },
                                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                            color = PlanBlue
                                        )
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                else -> {
                    val active = viewWeek == planState.activeWeek && !planState.finished
                    val displayAttempt = store.latestAttempt(planState, viewWeek)
                    val required = store.requiredComplete(planState, viewWeek, displayAttempt)
                    Text("Five rows. Your order.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    if (active) Text(planDateRange(planState), color = PlanBlue, fontWeight = FontWeight.Bold)
                    Text("$required of 3 required complete", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (active && planState.pendingRollover) RolloverCard(planState, required, onDecision = {
                        store.resolveRollover(it); revision++; viewWeek = store.state().activeWeek
                    })
                    PlanCard {
                        Text("REQUIRED · SEPARATE DAYS", color = PlanMuted, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        store.catalog.weeks[viewWeek - 1].take(3).forEach { session ->
                            val done = store.completedWorkout(planState, viewWeek, displayAttempt, session.index) != null
                            SessionRow(session, done, session.estimatedMinutes(estimateSplit)) { brief = session }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("OPTIONAL", color = PlanMuted, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        store.catalog.weeks[viewWeek - 1].drop(3).forEach { session ->
                            val done = store.completedWorkout(planState, viewWeek, displayAttempt, session.index) != null
                            SessionRow(session, done, session.estimatedMinutes(estimateSplit)) { brief = session }
                        }
                    }
                    if (active && required == 3) Text("Required rows complete. The next week starts Sunday.", color = PlanBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("Stop Pete's Plan?") },
            text = { Text("This resets your current plan participation and progress. Completed rows stay in History. You can start a new enrollment later.") },
            confirmButton = {
                TextButton(onClick = {
                    store.stop()
                    confirmStop = false
                    revision++
                    page = "intro"
                    viewWeek = 1
                }) { Text("Stop plan", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("Keep plan") } }
        )
    }

    brief?.let { session ->
        val current = store.state()
        val done = store.completedWorkout(current, session.week, current.activeAttempt, session.index) != null
        val canStart = session.week == current.activeWeek && !current.pendingRollover && !current.finished && !done
        val goal = resolver.resolve(session, current)
        val requiredToday = if (session.required) (0..2).any { index ->
            store.completedWorkout(current, current.activeWeek, current.activeAttempt, index)?.let { workout ->
                java.time.Instant.ofEpochMilli(workout.start.get()).atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()
            } == true
        } else false
        AlertDialog(
            onDismissRequest = { brief = null },
            title = { Text(session.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("WEEK ${session.week} · ${if (session.required) "REQUIRED" else "OPTIONAL"}", color = PlanBlue, fontWeight = FontWeight.Bold)
                    GoalBrief(goal, store)
                    Text(session.note)
                    if (requiredToday && !done) Text("Required rows are scheduled on separate days. You can choose this one tomorrow.", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(
                    onClick = { brief = null; onStartWorkout(session, goal, current) },
                    enabled = canStart && !requiredToday
                ) { Text(if (goal is PeteGoal.Speed && goal.firstPieces == 0 || goal is PeteGoal.RowAgainst) "Row against target" else "Start row") }
            },
            dismissButton = { TextButton(onClick = { brief = null }) { Text("Done") } }
        )
    }
}

@Composable
fun PetePlanHomeCard(store: PetePlanStore, onOpen: () -> Unit, onSession: (Int) -> Unit) {
    val state = store.state()
    PlanCard {
        if (!state.started) {
            Text("24-WEEK PLAN", color = PlanBlue, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(9.dp))
            Text("Pete's Plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Three required and two optional rows each week, with coaching based on your completed rows.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text("Explore Pete's Plan") }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pete's Plan · Week ${state.activeWeek}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${store.requiredComplete(state)}/3", color = PlanBlue, fontWeight = FontWeight.Bold)
            }
            Text(planDateRange(state), color = PlanMuted, fontSize = 13.sp)
            if (state.pendingRollover) {
                Text("Week ended · choose whether to move on or repeat", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text("Review week") }
            } else if (state.finished) {
                Text("Plan finished", color = PlanBlue, fontWeight = FontWeight.Bold)
            } else {
                val split = store.estimateSplit()
                Text("REQUIRED", color = PlanMuted, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                store.catalog.weeks[state.activeWeek - 1].take(3).forEach { session ->
                    val done = store.completedWorkout(state, state.activeWeek, state.activeAttempt, session.index) != null
                    SessionRow(session, done, session.estimatedMinutes(split)) { onSession(session.index) }
                }
                Spacer(Modifier.height(8.dp))
                Text("OPTIONAL", color = PlanMuted, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                store.catalog.weeks[state.activeWeek - 1].drop(3).forEach { session ->
                    val done = store.completedWorkout(state, state.activeWeek, state.activeAttempt, session.index) != null
                    SessionRow(session, done, session.estimatedMinutes(split)) { onSession(session.index) }
                }
            }
            TextButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.finished) "View progress and manage plan" else "View all 24 weeks")
            }
        }
    }
}

@Composable
private fun GoalBrief(goal: PeteGoal, store: PetePlanStore) {
    val title: String
    val value: String
    val detail: String
    when (goal) {
        is PeteGoal.Speed -> {
            title = if (goal.firstPieces > 0) "SPEED TARGET · FIRST ${goal.firstPieces} PIECES" else "SPEED GOAL · ROW AGAINST PACE"
            value = "${clock(goal.splitSeconds)} /500 m"
            detail = "Based on ${sourceLabel(goal.source, store)}. The target holds a steady pace."
        }
        is PeteGoal.StrokeRateCap -> {
            title = "STROKE-RATE CEILING"; value = "≤ ${goal.spm} SPM"
            detail = goal.description.ifBlank { "Stay at or below this rate while focusing on technique." }
        }
        is PeteGoal.StrokeRate -> {
            title = "STROKE-RATE TARGET"; value = "${goal.spm} SPM"
            detail = "Hold a steady rate while focusing on easy efficiency."
        }
        is PeteGoal.RowAgainst -> {
            title = "ROW AGAINST"; value = goal.source.programName("Previous row")
            detail = "Race the actual recorded progress from ${sourceLabel(goal.source, store)}."
        }
        is PeteGoal.Reference -> {
            title = "PACING REFERENCE"; value = "${clock(goal.splitSeconds)} /500 m"
            detail = "${goal.description} Based on ${sourceLabel(goal.source, store)}."
        }
        is PeteGoal.Unavailable -> { title = "TARGET UNAVAILABLE"; value = "No completed reference yet"; detail = goal.description }
        PeteGoal.None -> return
    }
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RolloverCard(state: PetePlanState, required: Int, onDecision: (Boolean) -> Unit) {
    PlanCard {
        Text("Week ${state.activeWeek} ended", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("$required of 3 required rows completed. Optional rows do not affect this choice.")
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onDecision(false) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.activeWeek == 24) "Finish plan" else "Move to Week ${state.activeWeek + 1}")
        }
        OutlinedButton(onClick = { onDecision(true) }, modifier = Modifier.fillMaxWidth()) { Text("Repeat Week ${state.activeWeek}") }
        Text("Repeating restarts all three required rows. Earlier workouts stay in History.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PlanProgressChart(
    store: PetePlanStore,
    state: PetePlanState,
    metric: String,
    selectedWeek: Int,
    onMetric: (String) -> Unit,
    onWeek: (Int) -> Unit
) {
    val totals = store.allWeeklyTotals(state)
    val values = totals.map { if (metric == "meters") it.first else it.second / 60 }
    val top = max(1, values.maxOrNull() ?: 1)
    PlanCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Progress by week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row {
                TextButton(onClick = { onMetric("meters") }) { Text("Meters", color = if (metric == "meters") PlanBlue else PlanMuted) }
                TextButton(onClick = { onMetric("time") }) { Text("Time", color = if (metric == "time") PlanBlue else PlanMuted) }
            }
        }
        Row(Modifier.fillMaxWidth().height(162.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Column(Modifier.width(34.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                Text(if (metric == "meters") "${top / 1000}k" else "${top}m", fontSize = 10.sp)
                Text(if (metric == "meters") "${top / 2000}k" else "${top / 2}m", fontSize = 10.sp)
                Text("0", fontSize = 10.sp)
            }
            values.forEachIndexed { index, value ->
                Column(Modifier.weight(1f).fillMaxHeight().clickable { onWeek(index + 1) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.fillMaxWidth().height((130f * value / top).coerceAtLeast(2f).dp)
                        .background(if (selectedWeek == index + 1) PlanBlue else Color(0xFF7FB9FA), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
                    Text("${index + 1}", fontSize = 8.sp, maxLines = 1)
                }
            }
        }
        val selected = totals[(selectedWeek - 1).coerceIn(0, 23)]
        Text("Week $selectedWeek · ${NumberFormat.getIntegerInstance().format(selected.first)} m · ${selected.second / 60} min", color = PlanMuted, fontSize = 12.sp)
    }
}

@Composable
private fun PlanCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}

@Composable
private fun SessionRow(session: PeteSession, done: Boolean, minutes: Int, onClick: () -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (done) Icon(Icons.Default.Check, contentDescription = "Completed", tint = PlanBlue, modifier = Modifier.size(23.dp))
        else Text("${session.index + 1}", color = PlanMuted, modifier = Modifier.width(23.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(session.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (done) "Completed" else if (session.optional) "Optional" else "Required", color = PlanMuted, fontSize = 11.sp)
        }
        Text("~$minutes min", color = PlanBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PlanMuted, modifier = Modifier.size(18.dp))
    }
}

private fun clock(seconds: Int): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

private fun sourceLabel(workout: Workout, store: PetePlanStore): String {
    val day = workout.start.get()?.let {
        java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
    }?.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())) ?: "earlier row"
    val split = store.averageSplit(workout)?.let { " · ${clock(it)} /500 m" } ?: ""
    return "${workout.programName("Earlier row")} ($day$split)"
}

@Composable
private fun EstimatePaceCard(seconds: Int, onChange: (Int) -> Unit) {
    PlanCard {
        Text("Estimated pace · ${clock(seconds)} /500 m", fontWeight = FontWeight.Bold)
        Text("Adjust this to make the row time estimates useful for your schedule.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Slider(
            value = seconds.toFloat(),
            onValueChange = { onChange((it / 5).toInt().coerceIn(18, 48) * 5) },
            valueRange = 90f..240f,
            steps = 29
        )
    }
}
private fun planDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
private fun planDateRange(state: PetePlanState): String = state.weekStart?.let { "${planDate(it)}–${planDate(it.plusDays(6))} · Ends Saturday" } ?: "Sunday–Saturday"
