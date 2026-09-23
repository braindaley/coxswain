package svenmeier.coxswain.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import svenmeier.coxswain.R
import svenmeier.coxswain.Gym
import svenmeier.coxswain.gym.Workout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(
    gym: Gym? = null,
    refreshKey: Int = 0,
    onFreeRow: () -> Unit,
    onQuickStart: (String) -> Unit,
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
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        
        Text(
            text = stringResource(R.string.ui_welcome_back),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Text(
            text = stringResource(R.string.ui_ready_to_row),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp)
        )

        Spacer(Modifier.height(32.dp))

        // Hero Action: Free Row (matches 01_home.png)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clickable { onFreeRow() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                Column(Modifier.align(Alignment.BottomStart)) {
                    Text(
                        text = stringResource(R.string.ui_free_row),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.ui_free_row_subtitle),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp).align(Alignment.CenterEnd)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // QUICK START SECTION (EXACT MATCH TO USER SCREENSHOT)
        SectionLabel(stringResource(R.string.ui_quick_start_heading))
        Spacer(Modifier.height(8.dp))

        QuickActionListCard(
            title = stringResource(R.string.ui_quick_start),
            subtitle = stringResource(R.string.ui_quick_start_subtitle),
            iconRes = R.drawable.ic_nav_workouts_24dp,
            onClick = { onQuickStart("Duration") }
        )

        Spacer(Modifier.height(24.dp))

        // GRID ACTIONS FROM DESIGN SPEC V2
        SectionLabel("WORKOUT MODES")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(
                title = stringResource(R.string.ui_my_programs),
                subtitle = "Your plans",
                iconRes = R.drawable.ic_nav_programs_24dp,
                modifier = Modifier.weight(1f),
                onClick = onMyPrograms
            )
            QuickActionCard(
                title = "Library",
                subtitle = "Curated",
                iconRes = R.drawable.ic_nav_performance_24dp,
                modifier = Modifier.weight(1f),
                onClick = onLibrary
            )
        }

        Spacer(Modifier.height(32.dp))

        // YOUR ROWING SECTION
        gym?.let { 
            SectionLabel(stringResource(R.string.ui_your_rowing))
            Spacer(Modifier.height(8.dp))
            HomeProgress(it, onQuickStart = { onQuickStart("Duration") }) 
        }

        Spacer(Modifier.height(24.dp))
        
        // LAST WORKOUT
        gym?.getWorkouts()?.list()?.firstOrNull()?.let { last ->
            SectionLabel(stringResource(R.string.ui_last_workout))
            Spacer(Modifier.height(8.dp))
            LastWorkoutCard(last, onDetails = { onWorkoutDetails(last) }, onRowAgain = { onRowAgain(last) })
        }
        
        Spacer(Modifier.height(48.dp))
    }
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
        shape = RoundedCornerShape(24.dp),
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
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle, 
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickActionListCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .semantics { contentDescription = "$title: $subtitle" },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, 
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle, 
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
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
    val workouts = ArrayList(gym.getWorkouts(range.first, range.second).list())
    val previous = ArrayList(gym.getWorkouts(range.first - (range.second - range.first), range.first).list())
    val meters = workouts.sumOf { it.distance.get() }
    val seconds = workouts.sumOf { it.duration.get() }
    val bucketCount = when (period) { "This month" -> 5; "This year" -> 12; else -> 7 }
    val bucketMeters = MutableList(bucketCount) { 0 }
    workouts.forEach { workout ->
        val bucket = homeBucketIndex(period, range.first, workout.start.get())
        if (bucket in 0 until bucketCount) {
            bucketMeters[bucket] += workout.distance.get()
        }
    }
    val labels = bucketLabels(period, range.first, bucketCount)
    val maxBucket = bucketMeters.maxOrNull()?.coerceAtLeast(1) ?: 1
    val streak = rowingStreak(workouts, now)
    val priorMeters = previous.sumOf { it.distance.get() }
    val comparison = if (priorMeters == 0) null else ((meters - priorMeters) * 100 / priorMeters)
    val streakText = pluralStringResource(R.plurals.ui_streak, streak, streak)
    val comparisonText = comparison?.let { stringResource(R.string.ui_prior_period, if (it >= 0) "+$it" else it.toString()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(), 
        shape = RoundedCornerShape(24.dp), 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SingleSelectToggleGroup(periodOptions.map { it.second }, periodOptions.first { it.first == period }.second, fontSize = 11.sp) { selected -> period = periodOptions.first { it.second == selected }.first }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(stringResource(R.string.ui_meters_rowed), "%,d m".format(Locale.getDefault(), meters)); Stat(stringResource(R.string.ui_time_rowed), "%d:%02d".format(
                Locale.getDefault(), seconds/60, seconds%60))
            }
            if (workouts.isEmpty()) {
                Text(stringResource(R.string.ui_first_row_chart), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onQuickStart, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) { Text(stringResource(R.string.ui_set_up_quick_row), color = MaterialTheme.colorScheme.primary) }
            } else {
                Text(buildString { append(streakText); if (comparisonText != null) { append("  •  "); append(comparisonText) } }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(stringResource(R.string.ui_activity), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Row(Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                    Text("%,d m".format(Locale.getDefault(), maxBucket), fontSize = 10.sp); Text("%,d m".format(
                    Locale.getDefault(), maxBucket / 2), fontSize = 10.sp); Text("0 m", fontSize = 10.sp)
                }
                Row(Modifier.weight(1f).fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
                    bucketMeters.forEach { value -> Box(Modifier.weight(1f).height((8 + (value * 130 / maxBucket)).dp).background(if (value > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 52.dp), horizontalArrangement = Arrangement.SpaceBetween) { labels.forEach { label: String -> Text(label, fontSize = 9.sp) } }
        }
    }
}

@Composable private fun Stat(label: String, value: String) { Column { Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun LastWorkoutCard(workout: Workout, onDetails: () -> Unit, onRowAgain: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onDetails), 
        shape = RoundedCornerShape(24.dp), 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) { 
        Column(Modifier.padding(20.dp)) { 
            Text(workout.programName(stringResource(R.string.ui_free_row)), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.ui_distance_time_summary, workout.distance.get(), workout.duration.get()/60, workout.duration.get()%60), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRowAgain, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text(stringResource(R.string.ui_row_again), fontWeight = FontWeight.Bold) 
            } 
        } 
    }
}

internal fun calendarRange(period: String, now: Long): Pair<Long, Long> {
    val start = Calendar.getInstance().apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 0); set(
        Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    when (period) {
        "This month" -> start.set(Calendar.DAY_OF_MONTH, 1)
        "This year" -> { start.set(Calendar.MONTH, Calendar.JANUARY); start.set(Calendar.DAY_OF_MONTH, 1) }
        else -> { start.firstDayOfWeek = Calendar.MONDAY; start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) }
    }
    val end = Calendar.getInstance().apply { timeInMillis = start.timeInMillis }
    when (period) { "This month" -> end.add(Calendar.MONTH, 1); "This year" -> end.add(Calendar.YEAR, 1); else -> end.add(
        Calendar.DAY_OF_MONTH, 7) }
    return start.timeInMillis to end.timeInMillis
}

private fun bucketLabels(period: String, start: Long, count: Int): List<String> {
    val calendar = Calendar.getInstance().apply { timeInMillis = start }
    val format = SimpleDateFormat(if (period == "This year") "MMM" else if (period == "This month") "d" else "EEE", Locale.getDefault())
    return List(count) {
        val label = format.format(calendar.time)
        calendar.add(if (period == "This year") Calendar.MONTH else Calendar.DAY_OF_MONTH, if (period == "This month") 7 else 1)
        label
    }
}

internal fun startOfDay(time: Long): Long = Calendar.getInstance().apply { timeInMillis = time; set(
    Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis

internal fun homeBucketIndex(period: String, rangeStart: Long, workoutStart: Long): Int {
    val dayFromStart = ((startOfDay(workoutStart) - rangeStart) / 86400000L).toInt()
    return when (period) { 
        "This year" -> {
            val calendar = Calendar.getInstance().apply { timeInMillis = workoutStart }
            calendar.get(Calendar.MONTH)
        }
        "This month" -> (dayFromStart / 7).coerceIn(0, 4)
        else -> dayFromStart.coerceIn(0, 6) 
    }
}

internal fun rowingStreak(workouts: List<Workout>, now: Long): Int {
    val days = workouts.asSequence().map { startOfDay(it.start.get()) }.toSet()
    var cursor = startOfDay(now)
    if (cursor !in days) cursor -= 86400000L
    var streak = 0
    while (cursor in days) { streak++; cursor -= 86400000L }
    return streak
}
