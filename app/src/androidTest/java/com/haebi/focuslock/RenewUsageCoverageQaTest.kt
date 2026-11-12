package com.haebi.focuslock

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.haebi.focuslock.data.UsageLoadState
import com.haebi.focuslock.data.UsageMetrics
import com.haebi.focuslock.data.UsageRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Uses the OS usage store only. No fabricated usage history or injected launch counts. */
@RunWith(AndroidJUnit4::class)
class RenewUsageCoverageQaTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun actualAndroidUsageSummaryKeepsRawTotalsAndCoverageHonest() {
        val state = UsageRepository(context.applicationContext).load()
        assertTrue("Granted Android appop must give real usage data", state is UsageLoadState.Ready)
        val snapshot = (state as UsageLoadState.Ready).snapshot
        val coverage = snapshot.coverage
        assertNotNull("Actual Android usage observations must carry coverage metadata", coverage)
        listOf(snapshot.today, snapshot.last7, snapshot.last30).forEach { period ->
            assertEquals(
                "One rounding operation must follow the sum of original foreground milliseconds",
                UsageMetrics.roundedMinutes(period.totalForegroundMillis),
                period.totalMinutes
            )
            assertEquals(
                "Visible apps must cover actual observed duration, including sub-minute events",
                period.totalForegroundMillis,
                UsageMetrics.totalMillis(period.apps.map { it.foregroundMillis })
            )
            assertTrue("All app intervals must be actual positive OS usage",
                period.apps.all { it.foregroundMillis > 0L })
            assertTrue("App listing cannot invent missing launch events",
                period.apps.all { it.launches >= 0 || it.launches == -1 })
            assertTrue("Reported observed days must fit within the selected window",
                coverage!!.observedDays(period.startDate, period.endDate)
                    <= coverage.periodDays(period.startDate, period.endDate))
        }
    }
}
