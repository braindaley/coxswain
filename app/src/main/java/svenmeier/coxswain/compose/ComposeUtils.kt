package svenmeier.coxswain.compose

import androidx.compose.ui.graphics.Color
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
