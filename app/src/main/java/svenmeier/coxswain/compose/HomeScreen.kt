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
            text = "Welcome back",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Ready to row?",
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
                        text = "Free Row", 
                        color = Color.White, 
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Start rowing without a target", 
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

        SectionLabel("QUICK START")
        Spacer(Modifier.height(12.dp))

        // 2x2 Grid for Quick Actions (matches manifest "Duration, Distance, My Programs, and Library")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(
                title = "Quick Start",
                subtitle = "Choose duration, distance, or intervals",
                iconRes = R.drawable.ic_nav_workouts_24dp,
                modifier = Modifier.fillMaxWidth(),
                onClick = onQuickDuration
            )
        }
        gym?.let { HomeProgress(it) }
        Spacer(Modifier.height(24.dp))
        gym?.getWorkouts()?.list()?.firstOrNull()?.let { LastWorkoutCard(it, { onWorkoutDetails(it) }, { onRowAgain(it) }) }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun HomeProgress(gym: Gym) {
    var period by remember { mutableStateOf("This week") }
    val now = System.currentTimeMillis()
    val days = when (period) { "This month" -> 31; "This year" -> 366; else -> 7 }
    val workouts = gym.getWorkouts(now - days * 86400000L, now).list()
    val meters = workouts.sumOf { it.distance.get() }
    val seconds = workouts.sumOf { it.duration.get() }
    val bucketCount = when (period) { "This month" -> 5; "This year" -> 12; else -> 7 }
    val bucketMeters = MutableList(bucketCount) { 0 }
    val nowCalendar = java.util.Calendar.getInstance()
    workouts.forEach { workout ->
        val ageDays = ((now - workout.start.get()) / 86400000L).toInt().coerceAtLeast(0)
        val workoutCalendar = java.util.Calendar.getInstance().also { it.timeInMillis = workout.start.get() }
        val monthAge = (nowCalendar.get(java.util.Calendar.YEAR) - workoutCalendar.get(java.util.Calendar.YEAR)) * 12 + nowCalendar.get(java.util.Calendar.MONTH) - workoutCalendar.get(java.util.Calendar.MONTH)
        val bucket = when (period) { "This year" -> monthAge.coerceIn(0, 11); "This month" -> (ageDays / 7).coerceIn(0, 4); else -> ageDays.coerceIn(0, 6) }
        bucketMeters[bucket] += workout.distance.get()
    }
    val maxBucket = bucketMeters.maxOrNull()?.coerceAtLeast(1) ?: 1
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("YOUR ROWING", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color(0xFF53647C))
            SingleSelectToggleGroup(listOf("This week", "This month", "This year"), period) { period = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Meters rowed", "%,d m".format(meters)); Stat("Time rowed", "%d:%02d".format(seconds/60, seconds%60))
            }
            Text(if (workouts.isEmpty()) "No rowing recorded for this period yet." else "${workouts.size} workout${if (workouts.size == 1) "" else "s"} completed. Keep the streak going.", fontSize = 13.sp, color = Color(0xFF53647C))
            Text("Activity", fontWeight = FontWeight.Bold, color = Color(0xFF10213F))
            Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                bucketMeters.reversed().forEach { value -> Box(Modifier.weight(1f).height((18 + (value * 102 / maxBucket)).dp).background(if (value > 0) Color(0xFF0B63F6) else Color(0xFFDCE5EF), RoundedCornerShape(4.dp))) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("0 m", fontSize = 11.sp); Text(if (period == "This year") "Months" else if (period == "This month") "Weeks" else "Days", fontSize = 11.sp); Text("%,d m".format(maxBucket), fontSize = 11.sp) }
        }
    }
}

@Composable private fun Stat(label: String, value: String) { Column { Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10213F)); Text(label, fontSize = 12.sp, color = Color(0xFF53647C)) } }

@Composable private fun LastWorkoutCard(workout: Workout, onDetails: () -> Unit, onRowAgain: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onDetails), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(20.dp)) { Text("LAST WORKOUT", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color(0xFF53647C)); Text(workout.programName("Free Row"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text("%,d m  •  %d:%02d".format(workout.distance.get(), workout.duration.get()/60, workout.duration.get()%60), color = Color(0xFF53647C)); Spacer(Modifier.height(12.dp)); Button(onClick = onRowAgain, modifier = Modifier.fillMaxWidth()) { Text("Row again") } } }
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
