package svenmeier.coxswain.compose

import org.junit.Assert.assertEquals
import org.junit.Test
import svenmeier.coxswain.gym.Workout
import java.util.Calendar

class HomeAnalyticsTest {
    private fun instant(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear(); set(year, month, day, 12, 0, 0)
    }.timeInMillis

    @Test fun rangesUseCalendarBoundaries() {
        val now = instant(2026, Calendar.SEPTEMBER, 19)
        val week = calendarRange("This week", now)
        val month = calendarRange("This month", now)
        val year = calendarRange("This year", now)
        assertEquals(Calendar.SUNDAY, Calendar.getInstance().apply { timeInMillis = week.first }.get(Calendar.DAY_OF_WEEK))
        assertEquals(1, Calendar.getInstance().apply { timeInMillis = month.first }.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.JANUARY, Calendar.getInstance().apply { timeInMillis = year.first }.get(Calendar.MONTH))
        assertEquals(7L, (week.second - week.first) / 86_400_000L)
    }

    @Test fun yearBucketsDoNotLeakAcrossYearBoundary() {
        val range = calendarRange("This year", instant(2026, Calendar.JUNE, 1))
        assertEquals(0, homeBucketIndex("This year", range.first, instant(2026, Calendar.JANUARY, 1)))
        assertEquals(11, homeBucketIndex("This year", range.first, instant(2026, Calendar.DECEMBER, 31)))
    }

    @Test fun monthUsesOneBucketPerDayIncludingLastDay() {
        val range = calendarRange("This month", instant(2026, Calendar.JANUARY, 15))
        assertEquals(0, homeBucketIndex("This month", range.first, instant(2026, Calendar.JANUARY, 1)))
        assertEquals(14, homeBucketIndex("This month", range.first, instant(2026, Calendar.JANUARY, 15)))
        assertEquals(30, homeBucketIndex("This month", range.first, instant(2026, Calendar.JANUARY, 31)))
    }

    @Test fun streakMayStartTodayOrYesterday() {
        val now = instant(2026, Calendar.SEPTEMBER, 19)
        val workouts = listOf(0, 1, 2).map { daysAgo -> Workout().apply { start.set(startOfDay(now) - daysAgo * 86_400_000L) } }
        assertEquals(3, rowingStreak(workouts, now))
        val yesterdayOnly = listOf(Workout().apply { start.set(startOfDay(now) - 86_400_000L) })
        assertEquals(1, rowingStreak(yesterdayOnly, now))
    }
}
