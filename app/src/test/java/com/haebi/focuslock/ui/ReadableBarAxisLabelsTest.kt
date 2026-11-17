package com.haebi.focuslock.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadableBarAxisLabelsTest {
    @Test
    fun monthlyChartShowsOnlyReadableTickLabels() {
        assertEquals(
            listOf("29", "24", "19", "14", "9", "4", "0"),
            readableBarAxisLabels(thirtyDayAxisLabels(30))
        )
    }

    @Test
    fun weeklyChartKeepsEveryDayLabel() {
        val week = listOf("6일 전", "5일 전", "4일 전", "3일 전", "2일 전", "어제", "오늘")
        assertEquals(week, readableBarAxisLabels(week))
        assertEquals(emptyList<String>(), readableBarAxisLabels(emptyList()))
    }
}
