package com.haebi.focuslock.ui

import com.haebi.focuslock.data.AppUsage
import com.haebi.focuslock.data.UsageCoverage
import com.haebi.focuslock.data.UsageSnapshot
import com.haebi.focuslock.data.UsageWindow
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageInsightPolicyTest {
    private val day = LocalDate.of(2026, 11, 14)

    private fun app(id: String, minutes: Int, launches: Int = -1) = AppUsage(
        id, id, "기타", minutes, launches, "확인 불가", "앱",
        0xFF666666, id, minutes.toLong() * 60_000L
    )

    private fun window(from: Long, to: Long, apps: List<AppUsage>): UsageWindow = UsageWindow(
        apps, apps.sumOf { it.minutes }, day.minusDays(from), day.minusDays(to),
        apps.sumOf { it.foregroundMillis }
    )

    private fun sample(
        current: List<AppUsage>,
        previous: List<AppUsage> = emptyList(),
        observed: Set<LocalDate>? = null,
        hourly: List<Int>? = null
    ): UsageSnapshot = UsageSnapshot(
        today = window(0, 0, current),
        last7 = window(6, 0, current),
        last30 = window(29, 0, current),
        yesterday = window(1, 1, previous),
        previous7 = window(13, 7, previous),
        previous30 = window(59, 30, previous),
        daily7 = List(7) { 0 },
        daily30 = List(30) { 0 },
        appDaily7 = emptyMap(),
        hourlyToday = hourly,
        collectedAtMillis = 0L,
        coverage = observed?.let(::UsageCoverage)
    )

    private fun observed(days: Int) =
        (0 until days).map { day.minusDays(it.toLong()) }.toSet()

    @Test
    fun noDataMeansNoInsightsOrGoalSuggestion() {
        val noData = sample(emptyList())
        assertTrue(UsageInsightPolicy.forPeriod(noData, "오늘", emptyMap()).isEmpty())
        assertTrue(UsageInsightPolicy.forPeriod(noData, "7일", emptyMap()).isEmpty())
        assertNull(UsageInsightPolicy.totalChange(noData, "30일"))
        assertNull(UsageInsightPolicy.goalSuggestion(noData, emptyMap(), emptyMap()))
    }

    @Test
    fun todayTopUsesRawDurationAndPeakRequiresRealEvents() {
        val data = sample(
            current = listOf(app("video", 45, 3), app("browser", 15)),
            hourly = List(24) { if (it == 21) 12 else 0 }
        )
        assertEquals("I01", UsageInsightPolicy.todayTop(data, emptyMap())?.id)
        assertTrue(UsageInsightPolicy.todayTop(data, emptyMap())?.message?.contains("75%") == true)
        assertEquals("I02", UsageInsightPolicy.todayPeak(data)?.id)
        assertTrue(UsageInsightPolicy.todayPeak(data)?.message?.contains("21시~22시") == true)
        assertNull(UsageInsightPolicy.todayPeak(data.copy(hourlyToday = null)))
        assertNull(UsageInsightPolicy.todayPeak(data.copy(hourlyToday = List(24) { 0 })))
        assertNull(UsageInsightPolicy.todayTop(data, mapOf("video" to true, "browser" to true)))
    }

    @Test
    fun sevenDayComparisonNeedsObservedDaysAndTwoAppRecords() {
        val now = listOf(app("video", 120, 10), app("browser", 30))
        val before = listOf(app("video", 60), app("browser", 20))
        val incomplete = sample(now, before, observed(7))
        assertTrue(UsageInsightPolicy.weeklyChanges(incomplete, emptyMap()).isEmpty())
        assertNull(UsageInsightPolicy.totalChange(incomplete, "7일"))
        assertNull(UsageInsightPolicy.goalSuggestion(incomplete, emptyMap(), emptyMap()))

        val valid = sample(now, before, observed(14))
        assertEquals(2, UsageInsightPolicy.weeklyChanges(valid, emptyMap()).size)
        assertEquals("I04", UsageInsightPolicy.totalChange(valid, "7일")?.id)
        assertEquals("I05", UsageInsightPolicy.frequentApps(valid, emptyMap())?.id)
        val suggestion = UsageInsightPolicy.goalSuggestion(valid, emptyMap(), emptyMap())
        assertEquals("I06", suggestion?.insight?.id)
        assertEquals("video", suggestion?.app?.id)
        assertNull(UsageInsightPolicy.goalSuggestion(
            valid, mapOf("video" to 30), mapOf("browser" to true)
        ))
        assertTrue(UsageInsightPolicy.weeklyChanges(valid, mapOf("video" to true))
            .all { !it.message.contains("video") })
    }

    @Test
    fun unknownLaunchCountsAndInsufficientMonthlyHistoryRemainUnknown() {
        val data = sample(
            current = listOf(app("video", 180)),
            previous = listOf(app("video", 60)),
            observed = observed(14)
        )
        assertNull(UsageInsightPolicy.frequentApps(data, emptyMap()))
        assertNull(UsageInsightPolicy.totalChange(data, "30일"))
        assertTrue(UsageInsightPolicy.forPeriod(data, "7일", emptyMap()).isNotEmpty())
    }
}
