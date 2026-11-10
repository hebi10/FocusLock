package com.haebi.focuslock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageFormatTest {
    @Test fun formatActualMinutes() {
        assertEquals("0분", UsageFormat.minutesLabel(0))
        assertEquals("58분", UsageFormat.minutesLabel(58))
        assertEquals("2시간 15분", UsageFormat.minutesLabel(135))
    }

    @Test fun missingPreviousPeriodDoesNotInventComparison() {
        assertNull(UsageFormat.comparison(15, 0))
        assertEquals("이전 기간보다 15분 감소", UsageFormat.comparison(45, 60))
        assertEquals("이전 기간과 동일", UsageFormat.comparison(60, 60))
    }

    @Test fun percentagesHandleEmptyAndFullRecords() {
        assertEquals(0, UsageFormat.sharePercent(20, 0))
        assertEquals(50, UsageFormat.sharePercent(30, 60))
    }
}
