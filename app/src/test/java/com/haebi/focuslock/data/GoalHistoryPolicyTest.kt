package com.haebi.focuslock.data

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class GoalHistoryPolicyTest {
    private val today = LocalDate.of(2026, 11, 24)
    private val zone = "Asia/Seoul"
    private fun event(offset: Long, minutes: Int, scope: String = GoalHistoryPolicy.TOTAL, z: String = zone) =
        GoalHistoryEvent(scope, today.minusDays(offset), z, GoalConfig(minutes))

    @Test fun noRetroactiveHistoryAndChangeDayExcluded() {
        val observed = (1L..7L).associate { today.minusDays(it) to 5_400_000L }
        val data = GoalHistoryPolicy.summarize(listOf(event(5, 60), event(2, 120)),
            GoalHistoryPolicy.TOTAL, today, zone, observed, 7)
        assertEquals(listOf(GoalDayResult.NO_GOAL, GoalDayResult.NO_GOAL, GoalDayResult.CHANGED,
            GoalDayResult.OVER, GoalDayResult.OVER, GoalDayResult.CHANGED, GoalDayResult.WITHIN), data.days.map { it.result })
        assertEquals(1, data.within)
        assertEquals(2, data.over)
        assertEquals(33, data.rate)
        assertEquals(1, data.streak)
    }

    @Test fun missingDataAndTimezoneChangeNeverCountAsSuccess() {
        val events = listOf(event(7, 120), event(3, 120, z = "America/New_York"))
        val observed = mapOf(today.minusDays(1) to 60_000L, today.minusDays(4) to 0L)
        val data = GoalHistoryPolicy.summarize(events, GoalHistoryPolicy.TOTAL, today, zone, observed, 7)
        assertEquals(0, data.observed)
        assertNull(data.rate)
        assertEquals(0, data.streak)
    }

    @Test fun rawMillisLimitAndClearAreIndependentOfOtherAppGoals() {
        val events = listOf(event(6, 60), event(4, 0), event(6, 90, "com.example.app"))
        val data = GoalHistoryPolicy.summarize(events, "com.example.app", today, zone,
            mapOf(today.minusDays(1) to 5_400_001L, today.minusDays(2) to 5_400_000L), 7)
        assertEquals(GoalDayResult.OVER, data.days.last().result)
        assertEquals(1, data.within)
        assertEquals(0, data.streak)
        val total = GoalHistoryPolicy.summarize(events, GoalHistoryPolicy.TOTAL, today, zone,
            mapOf(today.minusDays(1) to 60_000L), 7)
        assertEquals(GoalDayResult.NO_GOAL, total.days.last().result)
    }
}
