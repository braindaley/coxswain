package svenmeier.coxswain.compose

import android.os.Build
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import svenmeier.coxswain.R

// Design-specific colors from Coxswain Product Spec v2.0
val BrandBlue = Color(0xFF0B63F6)
val BrandBackground = Color(0xFFF4F7FB)
val TextPrimary = Color(0xFF10213F)
val TextSecondary = Color(0xFF53647C)
val CardShadow = Color(0x1210213F)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFF),
    onPrimaryContainer = Color(0xFF10213F),
    secondary = BrandBlue,
    secondaryContainer = Color(0xFFE8EEF6),
    onSecondaryContainer = Color(0xFF53647C),
    background = BrandBackground,
    surface = Color.White,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = Color(0xFFF4F7FB),
    surfaceContainer = Color(0xFFEEF3F8),
    surfaceContainerHigh = Color(0xFFE8EEF5),
    surfaceVariant = Color(0xFFE8EEF6),
    outline = Color(0xFFD5DEE9),
    outlineVariant = Color(0xFFE0E7F0),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF72A7FF),
    onPrimary = Color(0xFF071A34),
    primaryContainer = Color(0xFF173B68),
    onPrimaryContainer = Color(0xFFD9E8FF),
    secondary = Color(0xFF9FC5FF),
    onSecondary = Color(0xFF08213F),
    secondaryContainer = Color(0xFF24364A),
    onSecondaryContainer = Color(0xFFD9E8F5),
    background = Color(0xFF08131F),
    onBackground = Color(0xFFF3F6FA),
    surface = Color(0xFF121F2C),
    surfaceVariant = Color(0xFF1A2A39),
    surfaceContainerLow = Color(0xFF0E1A26),
    surfaceContainer = Color(0xFF162534),
    surfaceContainerHigh = Color(0xFF1D2D3D),
    onSurface = Color(0xFFF3F6FA),
    onSurfaceVariant = Color(0xFFAAB8C7),
    outline = Color(0xFF405267),
    outlineVariant = Color(0xFF2A3A4B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6)
)

val CoxswainTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun CoxswainTheme(
    darkTheme: Boolean? = null,
    dynamicColor: Boolean = false, // Keep branded colors stable for now
    content: @Composable () -> Unit
) {
    val useDarkTheme = darkTheme ?: rememberDarkThemePreference()
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CoxswainTypography,
        content = content
    )
}

/**
 * Keeps Compose screens in sync with the existing Settings > Use a dark theme
 * preference. The system theme is used until a preference has been stored.
 */
@Composable
private fun rememberDarkThemePreference(): Boolean {
    val context = LocalContext.current
    val preferences = remember(context) { PreferenceManager.getDefaultSharedPreferences(context) }
    val key = remember(context) { context.getString(R.string.preference_theme_dark) }
    val systemDark = isSystemInDarkTheme()
    var dark by remember(preferences, key, systemDark) {
        mutableStateOf(if (preferences.contains(key)) preferences.getBoolean(key, false) else systemDark)
    }
    DisposableEffect(preferences, key) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, changedKey ->
            if (changedKey == key) dark = prefs.getBoolean(key, false)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return dark
}
