package com.haebi.focuslock.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RenewNavigationTest {
    @Test
    fun gardenFromOlderBuildRestoresWithinFocus() {
        assertEquals(MainTab.FOCUS, restoredMainTab("GARDEN"))
    }

    @Test
    fun newReportTabAndExistingDestinationsRestore() {
        assertEquals(MainTab.REPORT, restoredMainTab("REPORT"))
        assertEquals(MainTab.ANALYSIS, restoredMainTab("ANALYSIS"))
        assertEquals(MainTab.HOME, restoredMainTab("HOME"))
        assertEquals(MainTab.FOCUS, restoredMainTab("FOCUS"))
        assertEquals(MainTab.SETTINGS, restoredMainTab("SETTINGS"))
    }

    @Test
    fun invalidTabStateNeverCrashesTheApp() {
        assertEquals(MainTab.HOME, restoredMainTab(""))
        assertEquals(MainTab.HOME, restoredMainTab("unknown"))
    }
}
