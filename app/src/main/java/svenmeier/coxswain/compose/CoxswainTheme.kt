package svenmeier.coxswain.compose

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF2B5BE3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE1FF),
    onPrimaryContainer = Color(0xFF001551),
    secondary = Color(0xFFD49B00),
    secondaryContainer = Color(0xFFFFDF9E),
    onSecondaryContainer = Color(0xFF251A00),
    surface = Color(0xFFFBFBFF),
    onSurface = Color(0xFF191B22),
    surfaceContainerLow = Color(0xFFF5F5FA)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4C5FF),
    onPrimary = Color(0xFF002A78),
    primaryContainer = Color(0xFF0B41CB),
    onPrimaryContainer = Color(0xFFDCE1FF),
    secondary = Color(0xFFFFB951),
    secondaryContainer = Color(0xFF5A4100),
    onSecondaryContainer = Color(0xFFFFDF9E),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceContainerLow = Color(0xFF191B22)
)

@Composable
fun CoxswainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // Optional: define custom Typography here later
        content = content
    )
}
