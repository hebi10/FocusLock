package com.haebi.focuslock.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ReferenceObservedLineTest {
    @Test fun gapsNeverConnectSeparateObservations() {
        assertEquals(listOf(listOf(1, 2), listOf(4), listOf(6, 7)),
            observedLineSegments(listOf(null, 12, 0, null, 9, null, 0, 15)))
    }

    @Test fun absentRecordsAndObservedZeroRemainDistinct() {
        assertEquals(emptyList<List<Int>>(), observedLineSegments(listOf(null, null)))
        assertEquals(listOf(listOf(0)), observedLineSegments(listOf(0)))
        assertEquals(emptyList<List<Int>>(), observedLineSegments(emptyList()))
    }
}
