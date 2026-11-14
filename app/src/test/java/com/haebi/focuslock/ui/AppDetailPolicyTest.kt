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

class AppDetailPolicyTest {
    private val today = LocalDate.of(2026, 11, 14)
    private fun usage(id: String, minutes: Int) = AppUsage(
        id, id, "기타", minutes, -1, "미확인", "A", 0xFF666666,
        id, minutes.toLong() * 60_000L
    )
    private fun window(from: Long, to: Long, apps: List<AppUsage>) = UsageWindow(
        apps, apps.sumOf { it.minutes }, today.minusDays(from), today.minusDays(to),
        apps.sumOf { it.foregroundMillis }
    )
    private fun snapshot(recordedDays: Int, current: Int = 120, previous: Int = 60): UsageSnapshot {
        val cur = listOf(usage("video", current), usage("browser", 30))
        val old = listOf(usage("video", previous), usage("browser", 20))
        val observed = (0 until recordedDays).map { today.minusDays(it.toLong()) }.toSet()
        return UsageSnapshot(
            today = window(0, 0, cur),
            last7 = window(6, 0, cur),
            last30 = window(29, 0, cur),
            yesterday = window(1, 1, old),
            previous7 = window(13, 7, old),
            previous30 = window(59, 30, old),
            daily7 = List(7) { 0 },
            daily30 = List(30) { 0 },
            appDaily7 = emptyMap(),
            hourlyToday = null,
            collectedAtMillis = 0L,
            coverage = UsageCoverage(observed)
        )
    }

    @Test fun weekdayMaskPreservesExistingGoalConfiguration() {
        assertEquals("선택된 요일 없음", AppDetailPolicy.activeDays(0))
        assertEquals("매일", AppDetailPolicy.activeDays(127))
        assertEquals("월·수·금", AppDetailPolicy.activeDays(0b0010101))
        assertEquals("선택 요일에 80%·100% 경고 설정",
            AppDetailPolicy.warningSummary(127, true, true))
        assertEquals("선택 요일에 100% 경고 설정",
            AppDetailPolicy.warningSummary(0b0010101, false, true))
        assertEquals("경고 알림 없음 (요일 미선택)",
            AppDetailPolicy.warningSummary(0, true, true))
        assertEquals("경고 알림 사용 안 함",
            AppDetailPolicy.warningSummary(127, false, false))
    }

    @Test fun rawDurationShareNeverClaimsUnknownIsZero() {
        val snap = snapshot(14)
        assertEquals(80, AppDetailPolicy.appShare(snap.last7, "video"))
        assertNull(AppDetailPolicy.appShare(snap.last7, "missing"))
        assertNull(AppDetailPolicy.appShare(snap.last7.copy(totalForegroundMillis = 0L), "video"))
    }

    @Test fun appComparisonRequiresBothWindowsAndObservedDays() {
        val insufficient = snapshot(7)
        assertNull(AppDetailPolicy.periodComparison(insufficient, "video", "7일"))
        val valid = snapshot(14)
        assertTrue(AppDetailPolicy.periodComparison(valid, "video", "7일")!!.contains("증가"))
        assertNull(AppDetailPolicy.periodComparison(valid, "missing", "7일"))
        assertNull(AppDetailPolicy.periodComparison(valid, "video", "오늘"))
        assertNull(AppDetailPolicy.periodComparison(valid, "video", "30일"))
    }

    @Test fun noPastGoalAchievementCanBeInferredFromCurrentUsage() {
        val valid = snapshot(14, current = 45, previous = 60)
        assertTrue(AppDetailPolicy.periodComparison(valid, "video", "7일")!!.contains("감소"))
        assertNull(AppDetailPolicy.periodComparison(snapshot(14, current = 0), "video", "7일"))
    }
}
