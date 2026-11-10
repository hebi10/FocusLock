package com.haebi.focuslock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoDataTest {
    @Test fun dailySumsMatchHourlyChart() {
        assertEquals(135, DemoData.todayTotalMinutes)
        assertEquals(DemoData.todayTotalMinutes, DemoData.hourlyUsageMinutes.sum())
    }

    @Test fun weeklyChartSumsToReportedTotal() {
        assertEquals(860, DemoData.weeklyTotalMinutes)
        assertEquals(7, DemoData.dailyUsageMinutes.size)
    }

    @Test fun everyPeriodMatchesAppBreakdown() {
        val totals = mapOf(
            "오늘" to DemoData.todayTotalMinutes,
            "7일" to DemoData.weeklyTotalMinutes,
            "30일" to DemoData.monthlyTotalMinutes,
        )
        totals.forEach { (period, expected) ->
            assertEquals(expected, DemoData.usageByPeriod(period).sumOf { it.minutes })
        }
    }

    @Test fun timeAndPercentageFormatting() {
        assertEquals("2시간 15분", DemoData.minutesLabel(135))
        assertEquals("1시간", DemoData.minutesLabel(60))
        assertEquals("58분", DemoData.minutesLabel(58))
        assertEquals(43, DemoData.sharePercent(58, 135))
        assertEquals(0, DemoData.sharePercent(10, 0))
    }

    @Test fun categoryAndAppIdsAreConsistent() {
        assertEquals(DemoData.apps.size, DemoData.apps.map { it.id }.distinct().size)
        assertEquals(DemoData.todayTotalMinutes, DemoData.apps.groupBy { it.category }.values.sumOf { apps -> apps.sumOf { it.minutes } })
        assertTrue(DemoData.apps.none { it.minutes < 0 })
    }
}
