package com.haebi.focuslock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class GoalDayPolicyTest {
    @Test
    fun oldDailySettingKeepsItsOriginalMeaning() {
        assertEquals(GoalDayPolicy.EVERY_DAY, GoalDayPolicy.fromLegacyEveryday(true))
        assertEquals(GoalDayPolicy.NONE, GoalDayPolicy.fromLegacyEveryday(false))
    }

    @Test
    fun selectedWeekdaysAreIndependentAndUseIsoMondayFirst() {
        var days = GoalDayPolicy.NONE
        days = GoalDayPolicy.toggle(days, DayOfWeek.MONDAY)
        days = GoalDayPolicy.toggle(days, DayOfWeek.WEDNESDAY)
        assertTrue(GoalDayPolicy.selected(days, LocalDate.of(2026, 11, 9)))
        assertFalse(GoalDayPolicy.selected(days, LocalDate.of(2026, 11, 10)))
        assertTrue(GoalDayPolicy.selected(days, LocalDate.of(2026, 11, 11)))
        assertFalse(GoalDayPolicy.selected(days, LocalDate.of(2026, 11, 15)))
        days = GoalDayPolicy.toggle(days, DayOfWeek.MONDAY)
        assertFalse(GoalDayPolicy.selected(days, DayOfWeek.MONDAY))
    }

    @Test
    fun allAndNoneBehaveSafelyWithUnknownBits() {
        assertEquals(GoalDayPolicy.EVERY_DAY, GoalDayPolicy.normalize(-1))
        assertEquals(GoalDayPolicy.NONE, GoalDayPolicy.normalize(128))
        DayOfWeek.entries.forEach { day ->
            assertTrue(GoalDayPolicy.selected(GoalDayPolicy.EVERY_DAY, day))
            assertFalse(GoalDayPolicy.selected(GoalDayPolicy.NONE, day))
        }
    }
}
