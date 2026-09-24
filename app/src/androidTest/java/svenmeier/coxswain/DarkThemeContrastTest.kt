package svenmeier.coxswain

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import svenmeier.coxswain.compose.CoxswainTheme
import svenmeier.coxswain.compose.SectionLabel
import svenmeier.coxswain.compose.SingleSelectToggleGroup

@RunWith(AndroidJUnit4::class)
class DarkThemeContrastTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun darkThemeTextAndControlsMeetContrastBaselines() {
        var scheme: androidx.compose.material3.ColorScheme? = null
        compose.setContent {
            CoxswainTheme(darkTheme = true) {
                scheme = MaterialTheme.colorScheme
                Column {
                    SectionLabel("DARK SECTION")
                    SingleSelectToggleGroup(
                        options = listOf("Selected", "Other"),
                        selectedOption = "Selected",
                        onOptionSelected = {}
                    )
                }
            }
        }
        compose.onNodeWithText("DARK SECTION").assertIsDisplayed()
        compose.onNodeWithText("Selected").assertIsDisplayed()

        compose.runOnIdle {
            val colors = requireNotNull(scheme)
            assertContrast("main text on background", colors.onSurface, colors.background, 4.5)
            assertContrast("main text on card", colors.onSurface, colors.surface, 4.5)
            assertContrast("secondary text on background", colors.onSurfaceVariant, colors.background, 4.5)
            assertContrast("secondary text on card", colors.onSurfaceVariant, colors.surface, 4.5)
            assertContrast("selected toggle text", colors.primary, colors.surface, 4.5)
            assertContrast("button text", colors.onPrimary, colors.primary, 4.5)
            assertContrast("chip text", colors.primary, colors.primaryContainer, 4.5)
            assertContrast("secondary container text", colors.onSecondaryContainer, colors.secondaryContainer, 4.5)
            assertContrast("error text", colors.error, colors.background, 4.5)
            assertContrast("input outline", colors.outline, colors.surface, 3.0)
            assertContrast("card/divider outline", colors.outlineVariant, colors.surface, 3.0)
        }
    }

    private fun assertContrast(label: String, foreground: Color, background: Color, minimum: Double) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("$label contrast was ${"%.2f".format(ratio)}:1; expected at least $minimum:1", ratio >= minimum)
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val firstLuminance = luminance(first)
        val secondLuminance = luminance(second)
        val lighter = maxOf(firstLuminance, secondLuminance)
        val darker = minOf(firstLuminance, secondLuminance)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun luminance(color: Color): Double {
        val argb = color.toArgb()
        fun linear(channel: Int): Double {
            val value = channel / 255.0
            return if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
        }
        val red = linear(android.graphics.Color.red(argb))
        val green = linear(android.graphics.Color.green(argb))
        val blue = linear(android.graphics.Color.blue(argb))
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }
}
