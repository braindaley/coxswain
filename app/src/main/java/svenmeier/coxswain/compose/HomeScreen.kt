package svenmeier.coxswain.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import svenmeier.coxswain.Gym
import svenmeier.coxswain.R
import svenmeier.coxswain.gym.Workout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil

private val HomeHeroColor = Color(0xFF052F43)
private val HeroControl = Color(0xFF16475B)
private val HeroBlue = Color(0xFF0B63F6)

@Composable
fun HomeScreen(
    gym: Gym? = null,
    refreshKey: Int = 0,
    onFreeRow: () -> Unit,
    onQuickStart: (String) -> Unit,
    onConnectRower: () -> Unit,
    onSettings: () -> Unit,
    onWorkoutDetails: (Workout) -> Unit = {},
    onRowAgain: (Workout) -> Unit = {}
) {
    var connectionUpdate by remember { mutableIntStateOf(0) }
    DisposableEffect(gym) {
        if (gym == null) onDispose {}
        else {
            val listener = Gym.Listener { connectionUpdate++ }
            gym.addListener(listener)
            onDispose { gym.removeListener(listener) }
        }
    }
    @Suppress("UNUSED_VARIABLE") val refresh = refreshKey + connectionUpdate
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HomeHeader(gym?.connected == true, onFreeRow, { onQuickStart("Duration") }, onConnectRower, onSettings)
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            gym?.getWorkouts()?.list()?.maxByOrNull { it.start.get() }?.let { last ->
                LastWorkoutCard(last, { onWorkoutDetails(last) }, { onRowAgain(last) })
            }
            gym?.let { HomeProgress(it) { onQuickStart("Duration") } }
        }
    }
}

@Composable
private fun HomeHeader(
    connected: Boolean,
    onFreeRow: () -> Unit,
    onQuickStart: () -> Unit,
    onConnectRower: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(HomeHeroColor).padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 22.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.app_name).uppercase(Locale.getDefault()), color = Color.White, fontSize = 18.sp, letterSpacing = 4.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(38.dp)) {
                    IconButton(
                        onClick = onConnectRower,
                        modifier = Modifier.fillMaxSize().background(HeroControl, RoundedCornerShape(19.dp))
                    ) {
                        Icon(Icons.Default.Link, contentDescription = stringResource(R.string.ui_connect_rower), tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    if (connected) Box(Modifier.align(Alignment.TopEnd).size(9.dp).background(Color(0xFF25C778), RoundedCornerShape(50)))
                }
                IconButton(onClick = onSettings, modifier = Modifier.size(38.dp).background(HeroControl, RoundedCornerShape(19.dp))) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.action_settings), tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.ui_ready_to_row), color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp)
        Text(stringResource(R.string.ui_choose_how_to_start), color = Color(0xFFB9CCDA), fontSize = 13.sp)
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onFreeRow, modifier = Modifier.weight(1f).height(62.dp),
                shape = RoundedCornerShape(31.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HeroBlue, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(3.dp))
                Text(stringResource(R.string.ui_free_row), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onQuickStart, modifier = Modifier.weight(1f).height(62.dp),
                shape = RoundedCornerShape(31.dp), border = BorderStroke(1.dp, Color(0xFF80B7FF)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(stringResource(R.string.ui_quick_start), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun LastWorkoutCard(workout: Workout, onDetails: () -> Unit, onRowAgain: () -> Unit) {
    val date = remember(workout.start.get()) {
        SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()).format(workout.start.get())
    }
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 15.dp, bottom = 14.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onDetails),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.ui_last_workout), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                    Text(
                        workout.programName(stringResource(R.string.ui_free_row)),
                        color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(date, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "%d:%02d".format(Locale.getDefault(), workout.duration.get() / 60, workout.duration.get() % 60),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("%,d m".format(Locale.getDefault(), workout.distance.get()), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = stringResource(R.string.ui_view_workout_details),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            OutlinedButton(
                onClick = onRowAgain,
                modifier = Modifier.fillMaxWidth().padding(top = 13.dp).height(44.dp),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.ui_row_again), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
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
    // Matches are cursor-backed; materialize each before opening the next.
    val workouts = ArrayList(gym.getWorkouts(range.first, range.second).list())
    val previousRange = previousCalendarRange(period, range.first)
    val previous = ArrayList(gym.getWorkouts(previousRange.first, previousRange.second).list())
    val allWorkouts = ArrayList(gym.getWorkouts().list())
    val meters = workouts.sumOf { it.distance.get() }
    val seconds = workouts.sumOf { it.duration.get() }
    val bucketCount = when (period) { "This month" -> 5; "This year" -> 12; else -> 7 }
    val bucketMeters = MutableList(bucketCount) { 0 }
    workouts.forEach { workout ->
        val bucket = homeBucketIndex(period, range.first, workout.start.get())
        if (bucket in bucketMeters.indices) bucketMeters[bucket] += workout.distance.get()
    }
    val labels = bucketLabels(period, range.first, bucketCount)
    val streak = rowingStreak(allWorkouts, now)
    val priorMeters = previous.sumOf { it.distance.get() }
    val comparison = if (priorMeters > 0) ((meters - priorMeters) * 100 / priorMeters) else null
    val priorPeriod = when (period) {
        "This month" -> stringResource(R.string.ui_last_month)
        "This year" -> stringResource(R.string.ui_last_year)
        else -> stringResource(R.string.ui_last_week)
    }
    val compareLabel = comparison?.let {
        stringResource(R.string.ui_vs_prior_period, if (it >= 0) "+$it" else it.toString(), priorPeriod)
    } ?: ""
    val periodTitle = periodOptions.first { it.first == period }.second
    val streakLabel = if (streak > 0) pluralStringResource(R.plurals.ui_streak, streak, streak)
        else pluralStringResource(R.plurals.ui_rows, workouts.size, workouts.size)
    val chartMax = niceChartMaximum(bucketMeters.maxOrNull() ?: 0)
    val encouragement = if (workouts.isEmpty()) stringResource(R.string.ui_first_row_chart)
        else if (comparison != null && comparison > 0) stringResource(R.string.ui_home_rowing_ahead, compareLabel)
        else stringResource(R.string.ui_home_rowing_encouragement)

    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 19.dp, bottom = 22.dp)) {
            Text(stringResource(R.string.ui_your_rowing_title), color = MaterialTheme.colorScheme.onSurface, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(15.dp))
            SingleSelectToggleGroup(periodOptions.map { it.second }, periodTitle, fontSize = 12.sp) { selected ->
                period = periodOptions.first { it.second == selected }.first
            }
            Spacer(Modifier.height(21.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(periodTitle, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    streakLabel,
                    modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(13.dp))
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryMetric(formatHomeDistance(meters), stringResource(R.string.ui_meters_rowed).uppercase(Locale.getDefault()), Modifier.weight(1f))
                SummaryMetric(formatHomeTime(seconds), stringResource(R.string.ui_time_rowed).uppercase(Locale.getDefault()), Modifier.weight(1f))
            }
            Spacer(Modifier.height(21.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.ui_rowing_distance).uppercase(Locale.getDefault()), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Text(compareLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().height(190.dp)) {
                Column(
                    Modifier.width(45.dp).fillMaxHeight().padding(top = 4.dp, bottom = 22.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatAxisDistance(chartMax), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(formatAxisDistance(chartMax / 2), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text("0", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
                Column(Modifier.weight(1f)) {
                    DistanceChart(bucketMeters, chartMax, Modifier.fillMaxWidth().weight(1f))
                    Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf(labels.first(), labels[bucketCount / 2], labels.last()).forEach { label ->
                            Text(label.uppercase(Locale.getDefault()), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 17.dp, bottom = 15.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Text(encouragement, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
            if (workouts.isEmpty()) OutlinedButton(
                onClick = onQuickStart, modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) { Text(stringResource(R.string.ui_set_up_quick_row)) }
        }
    }
}

@Composable
private fun SummaryMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.heightIn(min = 82.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 15.dp)
    ) {
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 27.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DistanceChart(values: List<Int>, maximum: Int, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val areaColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisColor = MaterialTheme.colorScheme.outline
    val description = stringResource(R.string.ui_rowing_distance)
    Canvas(modifier.semantics { contentDescription = description }) {
        val plotBottom = size.height - 4.dp.toPx()
        val plotTop = 4.dp.toPx()
        val plotHeight = plotBottom - plotTop
        for (fraction in listOf(0f, .5f, 1f)) {
            val y = plotTop + plotHeight * fraction
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
        drawLine(axisColor, Offset(0f, plotTop), Offset(0f, plotBottom), 1.dp.toPx())
        drawLine(axisColor, Offset(0f, plotBottom), Offset(size.width, plotBottom), 1.dp.toPx())
        if (values.isEmpty() || values.all { it == 0 }) return@Canvas
        val points = values.mapIndexed { index, value ->
            Offset(
                x = if (values.size == 1) size.width / 2 else size.width * index / (values.size - 1),
                y = plotBottom - value.toFloat() / maximum * plotHeight
            )
        }
        val area = Path().apply {
            moveTo(points.first().x, plotBottom)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, plotBottom)
            close()
        }
        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(area, areaColor)
        drawPath(line, lineColor, style = Stroke(width = 3.dp.toPx()))
    }
}

private fun niceChartMaximum(maximum: Int): Int {
    if (maximum <= 0) return 1000
    val step = when {
        maximum <= 500 -> 100
        maximum <= 3000 -> 500
        maximum <= 10000 -> 1000
        else -> 5000
    }
    return (ceil(maximum.toDouble() / step).toInt() * step).coerceAtLeast(step)
}

private fun formatHomeDistance(meters: Int): String =
    if (meters < 1000) "%,d m".format(Locale.getDefault(), meters)
    else "%.1f km".format(Locale.getDefault(), meters / 1000.0)

private fun formatAxisDistance(meters: Int): String =
    if (meters < 1000) "$meters m" else if (meters % 1000 == 0) (meters / 1000).toString() + " km"
    else "%.1f km".format(Locale.getDefault(), meters / 1000.0)

private fun formatHomeTime(seconds: Int): String {
    val minutes = seconds / 60
    return if (minutes < 60) "$minutes m" else (minutes / 60).toString() + "h " + (minutes % 60) + "m"
}

internal fun calendarRange(period: String, now: Long): Pair<Long, Long> {
    val start = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    when (period) {
        "This month" -> start.set(Calendar.DAY_OF_MONTH, 1)
        "This year" -> { start.set(Calendar.MONTH, Calendar.JANUARY); start.set(Calendar.DAY_OF_MONTH, 1) }
        else -> { start.firstDayOfWeek = Calendar.MONDAY; start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) }
    }
    val end = Calendar.getInstance().apply { timeInMillis = start.timeInMillis }
    when (period) {
        "This month" -> end.add(Calendar.MONTH, 1)
        "This year" -> end.add(Calendar.YEAR, 1)
        else -> end.add(Calendar.DAY_OF_MONTH, 7)
    }
    return start.timeInMillis to end.timeInMillis
}

private fun previousCalendarRange(period: String, currentStart: Long): Pair<Long, Long> {
    val start = Calendar.getInstance().apply { timeInMillis = currentStart }
    when (period) {
        "This month" -> start.add(Calendar.MONTH, -1)
        "This year" -> start.add(Calendar.YEAR, -1)
        else -> start.add(Calendar.DAY_OF_MONTH, -7)
    }
    return start.timeInMillis to currentStart
}

private fun bucketLabels(period: String, start: Long, count: Int): List<String> {
    val calendar = Calendar.getInstance().apply { timeInMillis = start }
    val format = SimpleDateFormat(
        if (period == "This year") "MMM" else if (period == "This month") "d" else "EEE",
        Locale.getDefault()
    )
    return List(count) {
        val label = format.format(calendar.time)
        calendar.add(if (period == "This year") Calendar.MONTH else Calendar.DAY_OF_MONTH, if (period == "This month") 7 else 1)
        label
    }
}

internal fun startOfDay(time: Long): Long = Calendar.getInstance().apply {
    timeInMillis = time
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

internal fun homeBucketIndex(period: String, rangeStart: Long, workoutStart: Long): Int {
    val dayFromStart = ((startOfDay(workoutStart) - rangeStart) / 86400000L).toInt()
    return when (period) {
        "This year" -> Calendar.getInstance().apply { timeInMillis = workoutStart }.get(Calendar.MONTH)
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
