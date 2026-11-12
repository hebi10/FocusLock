package com.haebi.focuslock.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportAxisLabelTest {
    @Test
    fun monthlyAxisLabelsKeepReadableDayOffsets() {
        val labels = thirtyDayAxisLabels(30)

        assertEquals(30, labels.size)
        assertEquals("29", labels[0])
        assertEquals("24", labels[5])
        assertEquals("19", labels[10])
        assertEquals("14", labels[15])
        assertEquals("9", labels[20])
        assertEquals("4", labels[25])
        assertEquals("0", labels[29])
        assertTrue(labels.filterIndexed { index, _ -> index !in setOf(0, 5, 10, 15, 20, 25, 29) }
            .all { it.isEmpty() })
    }

    @Test
    fun axisAdaptsToShorterAvailableHistoryWithoutOverflow() {
        assertEquals(listOf("6", "", "", "", "", "1", "0"), thirtyDayAxisLabels(7))
        assertEquals(emptyList<String>(), thirtyDayAxisLabels(0))
    }
}
