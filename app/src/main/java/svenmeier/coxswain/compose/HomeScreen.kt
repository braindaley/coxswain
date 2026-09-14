package svenmeier.coxswain.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import svenmeier.coxswain.R

@Composable
fun HomeScreen(
    onFreeRow: () -> Unit,
    onQuickDuration: () -> Unit,
    onQuickDistance: () -> Unit,
    onMyPrograms: () -> Unit,
    onLibrary: () -> Unit
) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    title = "Duration",
                    subtitle = "Time-based",
                    iconRes = R.drawable.ic_nav_workouts_24dp,
                    modifier = Modifier.weight(1f),
                    onClick = onQuickDuration
                )
                QuickActionCard(
                    title = "Distance",
                    subtitle = "Meter-based",
                    iconRes = R.drawable.ic_nav_performance_24dp,
                    modifier = Modifier.weight(1f),
                    onClick = onQuickDistance
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    title = "My Programs",
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
        }
        
        Spacer(Modifier.height(40.dp))
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
            .clickable { onClick() },
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
