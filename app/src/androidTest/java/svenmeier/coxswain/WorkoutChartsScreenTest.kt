package svenmeier.coxswain

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import svenmeier.coxswain.compose.CoxswainTheme
import svenmeier.coxswain.gym.*
import java.text.SimpleDateFormat
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class WorkoutChartsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun clockChartsAreReadOnlyAndIntervalBreakdownIsReachable() = checkCharts(false)
    @Test fun darkChartsSupportLargerTextWithoutExtraControls() = checkCharts(true)

    @Test fun recordedHeartRateAddsStatisticsAndGraph() = checkCharts(false, true)
    @Test fun darkHeartRateGraphIncludesRecovery() = checkCharts(true, true)

    private fun checkCharts(dark: Boolean, withHeartRate: Boolean = false) {
        val start = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse("2026-10-08 06:00")!!.time
        val program = Program.minutes("Pyramid", 1, Difficulty.HARD).apply {
            getSegment(0).setDuration(60).setPower(100); getSegment(0).name.set("Build")
            addSegment(Segment(Difficulty.REST).setDuration(30).apply { name.set("Recover") })
            addSegment(Segment(Difficulty.HARD).setDuration(60).setPower(200).apply { name.set("Peak") })
        }
        val workout = Workout().apply {
            if (withHeartRate) heartRateZones.set(HeartRateZones.reserve(60, 175).encode())
            this.start.set(start); duration.set(150); completed.set(start + 150_000)
            distance.set(500); strokes.set(60); programDefinition.set(WorkoutDefinition.freeze(program))
        }
        val snapshots = (1..150).map { second -> Snapshot().apply {
            duration.set(second); recordedAt.set(start + second * 1000L)
            intervalIndex.set(if (second <= 60) 0 else if (second <= 90) 1 else 2)
            intervalStart.set(if (second <= 60) 0 else if (second <= 90) 60 else 90)
            difficulty.set(if (second in 61..90) Difficulty.REST else Difficulty.HARD)
            speed.set(if (second <= 4) 175 else if (second <= 60) 400 else if (second <= 90) 0 else 500)
            power.set(if (second <= 8 || second in 61..90) 0 else if (second <= 60) 100 else 200)
            pulse.set(if (!withHeartRate || second in 100..105) 0 else if (second <= 60) 140 else if (second <= 90) 120 else 160)
            strokeRate.set(if (second in 61..90) 0 else 24); strokes.set(second / 2)
            distance.set(if (second <= 60) second * 4 else if (second <= 90) 240 else 240 + (second - 90) * 5)
        } }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (dark) 1.3f else 1f)) {
            CoxswainTheme(darkTheme = dark) {
                Surface {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                        WorkoutResults(workout, snapshots)
                    }
                }
            }
            }
        }
        compose.onNodeWithText("Include first strokes in chart scale").assertDoesNotExist()
        compose.onNodeWithText("Inspect your row").assertDoesNotExist()
        compose.onNodeWithText("Programmed effort").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Split time by clock time", substring = true)
            .performScrollTo().performTouchInput { click(center) }
        compose.onNodeWithText("elapsed", substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Split time by clock time", substring = true).performScrollTo()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        FileOutputStream(File(context.cacheDir, if (dark) "charts-dark.png" else "charts-light.png")).use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        if (withHeartRate) {
            compose.onNodeWithText("Time in heart-rate zones").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("106–128 BPM").assertExists()
            compose.onNodeWithText("Maximum heart rate").performScrollTo().assertIsDisplayed()
            compose.onAllNodesWithText("160 BPM").onFirst().assertExists()
            compose.onNodeWithContentDescription("Heart rate by clock time", substring = true)
                .performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("BPM", substring = false).assertExists()
            FileOutputStream(File(context.cacheDir, if (dark) "heart-chart-dark.png" else "heart-chart-light.png")).use {
                compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } else {
            compose.onNodeWithText("Average heart rate").assertDoesNotExist()
            compose.onNodeWithContentDescription("Heart rate by clock time", substring = true).assertDoesNotExist()
        }
        compose.onNodeWithText("Interval breakdown").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Recover").onLast().performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Peak").onLast().performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Inspect your row").assertDoesNotExist()
    }
}
