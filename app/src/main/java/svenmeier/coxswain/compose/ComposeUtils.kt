package svenmeier.coxswain.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import svenmeier.coxswain.gym.Difficulty

fun getIntensityColor(difficulty: Difficulty): Color {
    return when (difficulty) {
        Difficulty.REST -> Color(0xFF6C8CE8) // Light Blue
        Difficulty.EASY -> Color(0xFF34A853) // Green
        Difficulty.MEDIUM -> Color(0xFFF2BA00) // Yellow
        Difficulty.HARD -> Color(0xFFF57C00) // Orange
        Difficulty.PEAK -> Color(0xFFE53935) // Red
        Difficulty.NONE -> Color.Transparent
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontSize = 12.sp
        ),
        color = Color(0xFF53647C),
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
fun SingleSelectToggleGroup(
    options: List<String>,
    selectedOption: String,
    fontSize: TextUnit = 14.sp,
    onOptionSelected: (String) -> Unit
) {
    Surface(
        color = Color(0xFFE8EEF6),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Surface(
                    color = if (isSelected) Color.White else Color.Transparent,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOptionSelected(option) }
                ) {
                    Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = option,
                            fontSize = fontSize,
                            maxLines = 1,
                            softWrap = false,
                            color = if (isSelected) Color(0xFF0B63F6) else Color(0xFF53647C),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
