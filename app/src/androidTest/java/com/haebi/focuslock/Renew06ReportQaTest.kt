package com.haebi.focuslock

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.haebi.focuslock.data.UsageCoverage
import com.haebi.focuslock.data.UsageLoadState
import com.haebi.focuslock.data.UsageRepository
import com.haebi.focuslock.ui.ReportChangePolicy
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Renew06ReportQaTest {
    @Test
    fun realAndroidReportNeverInventsHistoryWhenObservationsAreMissing() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val state = UsageRepository(context).load()
        assertTrue("Granted OS usage permission required for real report", state is UsageLoadState.Ready)
        val snapshot = (state as UsageLoadState.Ready).snapshot
        val missing = snapshot.copy(coverage = UsageCoverage(emptySet()))
        listOf("7일", "30일").forEach { period ->
            assertNull(ReportChangePolicy.totalChange(missing, period))
            assertTrue(ReportChangePolicy.appChanges(missing, period, emptyMap()).isEmpty())
        }
    }
}
