package com.haebi.focuslock

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.InstrumentationRegistry
import com.haebi.focuslock.data.UsageCoverage
import com.haebi.focuslock.data.UsageLoadState
import com.haebi.focuslock.data.UsageRepository
import com.haebi.focuslock.ui.UsageInsightPolicy
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Renew04ObservedInsightsQaTest {
    @Test
    fun realAndroidRecordsNeverProduceInventedInsights() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val state = UsageRepository(appContext).load()
        assertTrue("Use only Android usage records after permission grant", state is UsageLoadState.Ready)
        val snapshot = (state as UsageLoadState.Ready).snapshot
        val allowed = setOf("I01", "I02", "I03", "I04", "I05", "I06")
        val today = UsageInsightPolicy.forPeriod(snapshot, "오늘", emptyMap())
        val weekly = UsageInsightPolicy.forPeriod(snapshot, "7일", emptyMap())
        assertTrue("Only planned evidence-based insight IDs are allowed",
            (today + weekly).all { it.id in allowed && it.source.contains("Android") })

        if (snapshot.today.totalForegroundMillis < 60_000L) {
            assertNull("No top app from insufficient foreground usage",
                UsageInsightPolicy.todayTop(snapshot, emptyMap()))
        }
        val noObservations = snapshot.copy(coverage = UsageCoverage(emptySet()))
        assertTrue("Missing observations must never create weekly changes",
            UsageInsightPolicy.weeklyChanges(noObservations, emptyMap()).isEmpty())
        assertNull("Missing observations must never create a comparison rate",
            UsageInsightPolicy.totalChange(noObservations, "7일"))
        assertNull("Missing observations must never create a goal recommendation",
            UsageInsightPolicy.goalSuggestion(noObservations, emptyMap(), emptyMap()))
    }
}
