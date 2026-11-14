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

class ReportChangePolicyTest {
    private val day = LocalDate.of(2026, 11, 14)

    private fun app(id: String, minutes: Int): AppUsage = AppUsage(
        id, id, "기타", minutes, -1, "확인 불가", "A", 0xFF666666,
        id, minutes.toLong() * 60_000L
    )
    private fun window(from: Long, to: Long, apps: List<AppUsage>) = UsageWindow(
        apps, apps.sumOf { it.minutes }, day.minusDays(from), day.minusDays(to),
        apps.sumOf { it.foregroundMillis }
    )
    private fun snapshot(
        observed: Int,
        current: List<AppUsage> = listOf(app("video", 180), app("browser", 60)),
        prior: List<AppUsage> = listOf(app("video", 90), app("browser", 100))
    ): UsageSnapshot = UsageSnapshot(
        today = window(0, 0, current),
        last7 = window(6, 0, current),
        last30 = window(29, 0, current),
        yesterday = window(1, 1, prior),
        previous7 = window(13, 7, prior),
        previous30 = window(59, 30, prior),
        daily7 = List(7) { 0 },
        daily30 = List(30) { 0 },
        appDaily7 = emptyMap(),
        hourlyToday = null,
        collectedAtMillis = 0L,
        coverage = UsageCoverage((0 until observed).map { day.minusDays(it.toLong()) }.toSet())
    )

    @Test fun incompletePeriodHidesAllComparisonMetrics() {
        val partial = snapshot(7)
        assertNull(ReportChangePolicy.totalChange(partial, "7일"))
        assertTrue(ReportChangePolicy.appChanges(partial, "7일", emptyMap()).isEmpty())
        assertNull(ReportChangePolicy.totalChange(partial, "30일"))
        assertTrue(ReportChangePolicy.appChanges(partial, "30일", emptyMap()).isEmpty())
    }

    @Test fun changesUseSameAppsAndBothObservedWindows() {
        val recorded = snapshot(14)
        assertTrue(ReportChangePolicy.totalChange(recorded, "7일")!!.contains("증가"))
        val trends = ReportChangePolicy.appChanges(recorded, "7일", emptyMap())
        assertEquals(2, trends.size)
        assertEquals("video", trends.first().app.id)
        assertTrue(ReportChangePolicy.appChangeLabel(trends.first()).contains("증가"))
        assertTrue(ReportChangePolicy.appChangeLabel(trends.last()).contains("감소"))
        assertEquals(1, ReportChangePolicy.appChanges(recorded, "7일", mapOf("video" to true)).size)
    }

    @Test fun newlyObservedAppDoesNotInventPreviousZeroMinutes() {
        val recorded = snapshot(14, listOf(app("new", 100)), listOf(app("old", 80)))
        assertTrue(ReportChangePolicy.appChanges(recorded, "7일", emptyMap()).isEmpty())
        assertNull(ReportChangePolicy.totalChange(recorded, "오늘"))
    }

    @Test fun thirtyDayRequiresSixtyObservedCalendarDays() {
        assertNull(ReportChangePolicy.totalChange(snapshot(59), "30일"))
        assertTrue(ReportChangePolicy.totalChange(snapshot(60), "30일")!!.contains("증가"))
    }

    @Test fun tinyOrMissingPreviousPeriodsNeverCreateInflatedRates() {
        assertNull(ReportChangePolicy.totalChange(
            snapshot(14, listOf(app("video", 20)), listOf(app("video", 1))), "7일"))
        assertEquals("이전 동일 기간과 사용 시간이 거의 같습니다.",
            ReportChangePolicy.totalChange(
                snapshot(14, listOf(app("video", 120)), listOf(app("video", 118))), "7일"))
    }
}
