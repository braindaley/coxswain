package svenmeier.coxswain.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import svenmeier.coxswain.Gym
import svenmeier.coxswain.gym.Workout
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorkoutsScreen(
    gym: Gym,
    onWorkoutClick: (Workout) -> Unit
) {
    val workouts = remember { mutableStateListOf<Workout>() }
    
    LaunchedEffect(Unit) {
        workouts.clear()
        workouts.addAll(gym.getWorkouts().list())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FB))
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        
        Text(
            text = "WORKOUT HISTORY",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold, 
                letterSpacing = 1.sp
            ),
            color = Color(0xFF53647C),
            modifier = Modifier.padding(start = 4.dp)
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(workouts) { workout ->
                WorkoutHistoryCard(workout, onClick = { onWorkoutClick(workout) })
            }
        }
    }
}

@Composable
fun WorkoutHistoryCard(workout: Workout, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(workout.start.get()))

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10213F)
                )
                IconButton(
                    onClick = { /* Menu */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color(0xFF53647C))
                }
            }

            Text(
                text = workout.programName("Free Row"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B63F6),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${workout.distance.get()}m • ${workout.duration.get() / 60} min • ${workout.energy.get()} kcal",
                fontSize = 14.sp,
                color = Color(0xFF53647C)
            )
        }
    }
}
