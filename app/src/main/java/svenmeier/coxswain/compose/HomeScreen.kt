package svenmeier.coxswain.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import svenmeier.coxswain.R
import svenmeier.coxswain.Gym
import svenmeier.coxswain.gym.Workout

@Composable
fun HomeScreen(
    gym: Gym? = null,
    refreshKey: Int = 0,
    onFreeRow: () -> Unit,
    onQuickDuration: () -> Unit,
    onQuickDistance: () -> Unit,
    onMyPrograms: () -> Unit,
    onLibrary: () -> Unit,
    onWorkoutDetails: (Workout) -> Unit = {},
    onRowAgain: (Workout) -> Unit = {}
) {
    @Suppress("UNUSED_VARIABLE") val refresh = refreshKey
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        
        Text(
            text = stringResource(R.string.ui_welcome_back),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.ui_ready_to_row),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(32.dp))

        // Hero Action: Free Row (matches 01_home.png "Free Row remain dominant")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clickable { onFreeRow() },
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                Column(Modifier.align(Alignment.BottomStart)) {
                    Text(
                        text = stringResource(R.string.ui_free_row),
                        color = Color.White, 
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = stringResource(R.string.ui_free_row_subtitle),
                        color = Color.White.copy(alpha = 0.8f), 
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp).align(Alignment.CenterEnd)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        SectionLabel(stringResource(R.string.ui_quick_start_heading))
        Spacer(Modifier.height(12.dp))

        // 2x2 Grid for Quick Actions (matches manifest "Duration, Distance, My Programs, and Library")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(
                title = stringResource(R.string.ui_quick_start),
                subtitle = stringResource(R.string.ui_quick_start_subtitle),
                iconRes = R.drawable.ic_nav_workouts_24dp,
                modifier = Modifier.fillMaxWidth(),
                onClick = onQuickDuration
            )
        }
        gym?.let { HomeProgress(it, onQuickDuration) }
        Spacer(Modifier.height(24.dp))
        gym?.getWorkouts()?.list()?.firstOrNull()?.let { LastWorkoutCard(it, { onWorkoutDetails(it) }, { onRowAgain(it) }) }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun HomeProgress(gym: Gym, onQuickStart: () -> Unit) {
    var period by remember { mutableStateOf("This week") }
    val periodOptions = listOf(
        "This week" to stringResource(R.string.ui_this_week),
        "This month" to stringResource(R.string.ui_this_month),
        "This year" to stringResource(R.string.ui_this_year)
    )
    val now = System.currentTimeMillis()
    val range = calendarRange(period, now)
    // Repository lists are cursor-backed. Materialize each query before opening
    // the next one so recomposition never reads from a cursor another query closed.
    val workouts = ArrayList(gym.getWorkouts(range.first, range.second).list())
    val previous = ArrayList(gym.getWorkouts(range.first - (range.second - range.first), range.first).list())
    val meters = workouts.sumOf { it.distance.get() }
    val seconds = workouts.sumOf { it.duration.get() }
    val bucketCount = when (period) { "This month" -> 5; "This year" -> 12; else -> 7 }
    val bucketMeters = MutableList(bucketCount) { 0 }
    workouts.forEach { workout ->
        val bucket = homeBucketIndex(period, range.first, workout.start.get())
        bucketMeters[bucket] += workout.distance.get()
    }
    val labels = bucketLabels(period, range.first, bucketCount)
    val maxBucket = bucketMeters.maxOrNull()?.coerceAtLeast(1) ?: 1
    val streak = rowingStreak(gym.getWorkouts().list(), now)
    val priorMeters = previous.sumOf { it.distance.get() }
    val comparison = if (priorMeters == 0) null else ((meters - priorMeters) * 100 / priorMeters)
    val streakText = stringResource(R.string.ui_streak, streak)
    val comparisonText = comparison?.let { stringResource(R.string.ui_prior_period, if (it >= 0) "+$it" else "$it") }
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.ui_your_rowing), fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color(0xFF53647C))
            SingleSelectToggleGroup(periodOptions.map { it.second }, periodOptions.first { it.first == period }.second) { selected -> period = periodOptions.first { it.second == selected }.first }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(stringResource(R.string.ui_meters_rowed), "%,d m".format(java.util.Locale.getDefault(), meters)); Stat(stringResource(R.string.ui_time_rowed), "%d:%02d".format(java.util.Locale.getDefault(), seconds/60, seconds%60))
            }
            if (workouts.isEmpty()) {
                Text(stringResource(R.string.ui_first_row_chart), fontSize = 13.sp, color = Color(0xFF53647C))
                OutlinedButton(onClick = onQuickStart, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_set_up_quick_row)) }
            } else {
                Text(buildString { append(streakText); comparisonText?.let { append("  •  "); append(it) } }, fontSize = 13.sp, color = Color(0xFF53647C))
            }
            Text(stringResource(R.string.ui_activity), fontWeight = FontWeight.Bold, color = Color(0xFF10213F))
            Row(Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                    Text("%,d m".format(maxBucket), fontSize = 10.sp); Text("%,d m".format(maxBucket / 2), fontSize = 10.sp); Text("0 m", fontSize = 10.sp)
                }
                Row(Modifier.weight(1f).fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
                    bucketMeters.forEach { value -> Box(Modifier.weight(1f).height((10 + (value * 150 / maxBucket)).dp).background(if (value > 0) Color(0xFF0B63F6) else Color(0xFFDCE5EF), RoundedCornerShape(4.dp))) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 52.dp), horizontalArrangement = Arrangement.SpaceBetween) { labels.forEach { Text(it, fontSize = 9.sp) } }
        }
    }
}

internal fun calendarRange(period: String, now: Long): Pair<Long, Long> {
    val start = java.util.Calendar.getInstance().apply { timeInMillis = now; set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
    when (period) {
        "This month" -> start.set(java.util.Calendar.DAY_OF_MONTH, 1)
        "This year" -> { start.set(java.util.Calendar.MONTH, java.util.Calendar.JANUARY); start.set(java.util.Calendar.DAY_OF_MONTH, 1) }
        else -> { start.firstDayOfWeek = java.util.Calendar.MONDAY; start.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY) }
    }
    val end = java.util.Calendar.getInstance().apply { timeInMillis = start.timeInMillis }
    when (period) { "This month" -> end.add(java.util.Calendar.MONTH, 1); "This year" -> end.add(java.util.Calendar.YEAR, 1); else -> end.add(java.util.Calendar.DAY_OF_MONTH, 7) }
    return start.timeInMillis to end.timeInMillis
}

private fun bucketLabels(period: String, start: Long, count: Int): List<String> {
    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = start }
    val format = java.text.SimpleDateFormat(if (period == "This year") "MMM" else if (period == "This month") "d" else "EEE", java.util.Locale.getDefault())
    return List(count) {
        val label = format.format(calendar.time)
        calendar.add(if (period == "This year") java.util.Calendar.MONTH else java.util.Calendar.DAY_OF_MONTH, if (period == "This month") 7 else 1)
        label
    }
}

internal fun startOfDay(time: Long): Long = java.util.Calendar.getInstance().apply { timeInMillis = time; set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }.timeInMillis

internal fun homeBucketIndex(period: String, rangeStart: Long, workoutStart: Long): Int {
    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = workoutStart }
    val dayFromStart = ((startOfDay(workoutStart) - rangeStart) / 86400000L).toInt()
    return when (period) { "This year" -> calendar.get(java.util.Calendar.MONTH); "This month" -> (dayFromStart / 7).coerceIn(0, 4); else -> dayFromStart.coerceIn(0, 6) }
}

internal fun rowingStreak(workouts: List<Workout>, now: Long): Int {
    val days = workouts.map { startOfDay(it.start.get()) }.toSet()
    var cursor = startOfDay(now)
    if (cursor !in days) cursor -= 86400000L
    var streak = 0
    while (cursor in days) { streak++; cursor -= 86400000L }
    return streak
}

@Composable private fun Stat(label: String, value: String) { Column { Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10213F)); Text(label, fontSize = 12.sp, color = Color(0xFF53647C)) } }

@Composable private fun LastWorkoutCard(workout: Workout, onDetails: () -> Unit, onRowAgain: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onDetails), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(20.dp)) { Text(stringResource(R.string.ui_last_workout), fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color(0xFF53647C)); Text(workout.programName(stringResource(R.string.ui_free_row)), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.ui_distance_time_summary, workout.distance.get(), workout.duration.get()/60, workout.duration.get()%60), color = Color(0xFF53647C)); Spacer(Modifier.height(12.dp)); Button(onClick = onRowAgain, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_row_again)) } } }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() }
            .semantics { contentDescription = "$title: $subtitle" },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle, 
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
