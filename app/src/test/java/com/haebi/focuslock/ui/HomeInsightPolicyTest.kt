package com.haebi.focuslock.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeInsightPolicyTest {
    @Test
    fun onlyObservedDailyHoursProducePeakHour() {
        assertNull(HomeInsightPolicy.observedPeakHour(null))
        assertNull(HomeInsightPolicy.observedPeakHour(emptyList()))
        assertNull(HomeInsightPolicy.observedPeakHour(List(24) { 0 }))
        assertNull(HomeInsightPolicy.observedPeakHour(List(7) { 10 }))
        assertEquals(21, HomeInsightPolicy.observedPeakHour(
            List(24) { if (it == 21) 30 else 2 }
        ))
    }

    @Test
    fun incompleteOrZeroUsageNeverCreatesShare() {
        assertNull(HomeInsightPolicy.topAppShare(0, 0))
        assertNull(HomeInsightPolicy.topAppShare(0, 10))
        assertNull(HomeInsightPolicy.topAppShare(12, 10))
        assertNull(HomeInsightPolicy.topAppShare(5, 0))
        assertEquals(50, HomeInsightPolicy.topAppShare(30, 60))
    }
}
