package com.haebi.focuslock.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartSelectionPolicyTest {
    @Test fun dailySelectionStaysWithinPeriod() {
        assertEquals(0, ChartSelectionPolicy.step(30, 0, -1))
        assertEquals(29, ChartSelectionPolicy.step(30, 29, 1))
        assertEquals(28, ChartSelectionPolicy.step(30, 29, -1))
        assertEquals(29, ChartSelectionPolicy.step(30, 28, 1))
        assertEquals(6, ChartSelectionPolicy.index(7, 29))
        assertEquals(-1, ChartSelectionPolicy.step(0, 0, 1))
    }
}
