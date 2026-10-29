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
    primary = Color(0xFF8DB8FF),
    onPrimary = Color(0xFF0A1C35),
    primaryContainer = Color(0xFF203753),
    onPrimaryContainer = Color(0xFFE0EBFF),
    secondary = Color(0xFFA9C7EE),
    onSecondary = Color(0xFF10243D),
    secondaryContainer = Color(0xFF253448),
    onSecondaryContainer = Color(0xFFDCE7F5),
    tertiary = Color(0xFF84C9CD),
    onTertiary = Color(0xFF092F33),
    tertiaryContainer = Color(0xFF1B4145),
    onTertiaryContainer = Color(0xFFBCECEF),
    // Neutral slate separates the page, cards and raised controls without a blue wash.
    background = Color(0xFF0F141A),
    onBackground = Color(0xFFE5EBF2),
    surface = Color(0xFF19212A),
    onSurface = Color(0xFFE5EBF2),
    onSurfaceVariant = Color(0xFFAEBBCB),
    surfaceVariant = Color(0xFF26323F),
    surfaceDim = Color(0xFF0F141A),
    surfaceBright = Color(0xFF303E4D),
    surfaceContainerLowest = Color(0xFF0B1015),
    surfaceContainerLow = Color(0xFF141B23),
    surfaceContainer = Color(0xFF1E2732),
    surfaceContainerHigh = Color(0xFF26323F),
    surfaceContainerHighest = Color(0xFF303E4D),
    // Interactive field borders stay distinct; decorative separators are quieter.
    outline = Color(0xFF798A9F),
    outlineVariant = Color(0xFF374454),
    inverseSurface = Color(0xFFE5EBF2),
    inverseOnSurface = Color(0xFF19212A),
    inversePrimary = BrandBlue,
    surfaceTint = Color(0xFF8DB8FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF572824),
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
