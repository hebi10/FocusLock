package com.haebi.focuslock

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.haebi.focuslock.data.UsageLoadState
import com.haebi.focuslock.data.UsageRepository
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Real Android usage-stat evidence only. Never inject fabricated app history
 * into the app, Room, system UsageStatsManager or Google Play screenshots.
 */
@RunWith(AndroidJUnit4::class)
class FocusLockUsageStateAuditTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun output(name: String, record: JSONObject) {
        val folder = File(requireNotNull(context.getExternalFilesDir(null)), "qa-r00")
        check(folder.isDirectory || folder.mkdirs())
        File(folder, "$name.json").writeText(record.toString(2) + "\n", Charsets.UTF_8)
    }

    @Test
    fun permissionDeniedReallyReturnsPermissionRequired() {
        val state = UsageRepository(context.applicationContext).load()
        val record = JSONObject()
            .put("test", "ui-r00-permission-denied")
            .put("usageSource", "Android UsageStatsManager")
            .put("dataInjected", false)
            .put("observedState", when (state) {
                UsageLoadState.PermissionRequired -> "PermissionRequired"
                UsageLoadState.Loading -> "Loading"
                is UsageLoadState.Unavailable -> "Unavailable"
                is UsageLoadState.Ready -> "Ready"
            })
        output("permission-denied", record)
        assertTrue("A denied system appop must require permission", state == UsageLoadState.PermissionRequired)
    }

    @Test
    fun permissionGrantedCapturesRealUsageCoverage() {
        val state = UsageRepository(context.applicationContext).load()
        val report = JSONObject()
            .put("test", "ui-r00-permission-granted")
            .put("usageSource", "Android UsageStatsManager")
            .put("dataInjected", false)
        if (state is UsageLoadState.Ready) {
            val snapshot = state.snapshot
            val rowsToday = snapshot.today.apps
            report.put("observedState", "Ready")
                .put("todayAppCount", rowsToday.size)
                .put("todayTotalMinutes", snapshot.today.totalMinutes)
                .put("last7AppCount", snapshot.last7.apps.size)
                .put("last7TotalMinutes", snapshot.last7.totalMinutes)
                .put("last30AppCount", snapshot.last30.apps.size)
                .put("last30TotalMinutes", snapshot.last30.totalMinutes)
                .put("nonzeroDays7", snapshot.daily7.count { it > 0 })
                .put("nonzeroDays30", snapshot.daily30.count { it > 0 })
                .put("hourlyEventsAvailable", snapshot.hourlyToday != null)
                .put("systemSettingsPresent", rowsToday.any { it.packageName == "com.android.settings" })
                .put("enoughDiverseHistoryForStoreReview",
                    rowsToday.size >= 3 && snapshot.daily7.count { it > 0 } >= 2)
                .put("notes", "Actual emulator OS usage history. No synthetic time totals; limited data is expected on fresh emulators.")
        } else {
            report.put("observedState", when (state) {
                UsageLoadState.PermissionRequired -> "PermissionRequired"
                UsageLoadState.Loading -> "Loading"
                is UsageLoadState.Unavailable -> "Unavailable"
                else -> "Unknown"
            })
        }
        output("permission-granted", report)
        assertTrue("System granted appop must allow usage stats read", state is UsageLoadState.Ready)
    }
}
