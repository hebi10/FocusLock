package com.haebi.focuslock.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageMetricsTest {
    @Test
    fun aggregateBeforeRoundingPreventsZeroMinuteTotals() {
        assertEquals(0, UsageMetrics.roundedMinutes(20_000L))
        assertEquals(1, UsageMetrics.totalMinutes(listOf(20_000L, 20_000L)))
        assertEquals(1, UsageMetrics.roundedMinutes(30_000L))
        assertEquals(1, UsageMetrics.totalMinutes(listOf(-5L, 35_000L)))
    }

    @Test
    fun longDurationsCannotOverflowIntoNegativeTotals() {
        assertEquals(Int.MAX_VALUE,
            UsageMetrics.totalMinutes(listOf(Long.MAX_VALUE, Long.MAX_VALUE)))
    }

    @Test
    fun rawSharesAreGroundedInRealDurationNotRoundedAppMinutes() {
        assertEquals(50, UsageMetrics.rawSharePercent(20_000L, 40_000L))
        assertEquals(100, UsageMetrics.rawSharePercent(40_000L, 40_000L))
        assertNull(UsageMetrics.rawSharePercent(0L, 40_000L))
        assertNull(UsageMetrics.rawSharePercent(40_000L, 0L))
        assertNull(UsageMetrics.rawSharePercent(50_000L, 40_000L))
    }

    @Test
    fun observationDaysDoNotPretendMissingHistoryIsZeroUsage() {
        val start = LocalDate.of(2026, 11, 6)
        val end = LocalDate.of(2026, 11, 12)
        val observed = UsageCoverage(setOf(start, end, start.minusDays(1)))
        assertEquals(2, observed.observedDays(start, end))
        assertEquals(7, observed.periodDays(start, end))
        assertEquals("선택 기간 7일 중 사용 기록이 확인된 날 2일",
            observed.description(start, end))
        assertEquals(0, observed.observedDays(end, start))
        assertEquals(0, observed.periodDays(end, start))
    }
}
