package svenmeier.coxswain.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import svenmeier.coxswain.Gym
import svenmeier.coxswain.R
import svenmeier.coxswain.gym.RaceOutcome
import svenmeier.coxswain.gym.SessionType
import svenmeier.coxswain.gym.Workout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutsScreen(
    gym: Gym,
    refreshKey: Int = 0,
    onWorkoutClick: (Workout) -> Unit
) {
    val workouts = remember { mutableStateListOf<Workout>() }
    var selectedFilter by remember { mutableIntStateOf(0) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(refreshKey) {
        workouts.clear()
        workouts.addAll(gym.getAllWorkouts().list())
    }

    val filters = listOf(
        stringResource(R.string.ui_all),
        stringResource(R.string.ui_programs),
        stringResource(R.string.ui_free_rows),
        stringResource(R.string.ui_races)
    )
    val freeRowName = stringResource(R.string.ui_free_row)
    val filtered = workouts.asSequence()
        .filter { workout ->
            when (selectedFilter) {
                1 -> workout.sessionType.get() in setOf(SessionType.DURATION, SessionType.DISTANCE, SessionType.INTERVAL)
                2 -> workout.sessionType.get() == SessionType.FREE
                3 -> workout.sessionType.get() == SessionType.RACE || workout.raceOutcome.get() != RaceOutcome.NONE
                else -> true
            }
        }
        .filter { it.programName(freeRowName).contains(query, ignoreCase = true) }
        .sortedByDescending { it.start.get() }
        .toList()
    val grouped = filtered.groupBy { monthLabel(it.start.get()) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (searchOpen) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f).testTag("history-search"),
                    singleLine = true,
                    label = { Text(stringResource(R.string.ui_search_history)) }
                )
            } else {
                Text(
                    text = stringResource(R.string.ui_history),
                    modifier = Modifier.weight(1f).testTag("history-title"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = {
                searchOpen = !searchOpen
                if (!searchOpen) query = ""
            }) {
                Icon(
                    imageVector = if (searchOpen) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = stringResource(if (searchOpen) R.string.ui_close_search else R.string.ui_search_history)
                )
            }
        }
        SingleSelectToggleGroup(
            options = filters,
            selectedOption = filters[selectedFilter],
            fontSize = 12.sp,
            onOptionSelected = { selectedFilter = filters.indexOf(it).coerceAtLeast(0) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 100.dp)
        ) {
            grouped.forEach { (month, monthWorkouts) ->
                item(key = "month-$month") {
                    Text(
                        month.uppercase(Locale.getDefault()),
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(monthWorkouts, key = { "workout-${it.start.get()}-${it.programName("")}" }) { workout ->
                    WorkoutHistoryCard(workout, onClick = { onWorkoutClick(workout) })
                }
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.ui_no_workouts_found),
                        modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutHistoryCard(workout: Workout, onClick: () -> Unit) {
    val dateStr = remember(workout.start.get()) {
        SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()).format(Date(workout.start.get()))
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(
                    workout.programName(stringResource(R.string.ui_free_row)),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (workout.raceOutcome.get() == RaceOutcome.WON) {
                    Spacer(Modifier.width(5.dp))
                    Icon(Icons.Default.EmojiEvents, contentDescription = stringResource(R.string.ui_race_won), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("%,d m".format(Locale.getDefault(), workout.distance.get()), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("%d:%02d".format(Locale.getDefault(), workout.duration.get() / 60, workout.duration.get() % 60), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun monthLabel(time: Long): String = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(time))
