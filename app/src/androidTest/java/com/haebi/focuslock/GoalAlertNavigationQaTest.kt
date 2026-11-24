package com.haebi.focuslock

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.haebi.focuslock.data.GoalAlertNavigation
import com.haebi.focuslock.data.GoalHistoryPolicy
import com.haebi.focuslock.data.UsageRepository
import com.haebi.focuslock.ui.GoalNotificationAvailability
import com.haebi.focuslock.ui.goalNotificationAvailability
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoalAlertNavigationQaTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test fun distinctNotificationTargetsAndRealTodayReading() {
        val total = GoalAlertNavigation.intent(context, GoalHistoryPolicy.TOTAL)
        val settings = GoalAlertNavigation.intent(context, "com.android.settings")
        assertEquals(GoalHistoryPolicy.TOTAL, GoalAlertNavigation.target(total))
        assertEquals("com.android.settings", GoalAlertNavigation.target(settings))
        assertNull(GoalAlertNavigation.target(Intent()))
        assertNull(GoalAlertNavigation.target(Intent(settings).setData(Uri.parse("focuslock://goal/a/b"))))
        val totalPending = GoalAlertNavigation.pendingIntent(context, GoalHistoryPolicy.TOTAL)
        val appPending = GoalAlertNavigation.pendingIntent(context, "com.android.settings")
        try { assertNotEquals(totalPending, appPending) }
        finally { totalPending.cancel(); appPending.cancel() }
        val reading = UsageRepository(context).loadTodayForAlerts()
        assertNotNull("Usage access must be granted by QA runner", reading)
        assertEquals(reading!!.apps.sumOf { it.foregroundMillis }, reading.totalMillis)
        assertTrue(reading.apps.none { it.id == context.packageName })
    }

    @Test fun totalNotificationOpensGoalsWithoutChangingExistingSettings() {
        val prefs = context.getSharedPreferences("focuslock_ui_prototype", 0)
        val completed = prefs.getBoolean("onboarding_completed", false)
        val old = prefs.getInt("whole_usage_goal_minutes", 0)
        prefs.edit().putBoolean("onboarding_completed", true).commit()
        try {
            val intent = GoalAlertNavigation.intent(context, GoalHistoryPolicy.TOTAL)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ActivityScenario.launch<MainActivity>(intent).use {
                assertTrue(device.wait(Until.hasObject(By.text("전체 앱 사용 목표")), 20_000L))
                assertEquals(old, prefs.getInt("whole_usage_goal_minutes", 0))
            }
        } finally { prefs.edit().putBoolean("onboarding_completed", completed).apply() }
    }

    @Test fun deniedUsageNeverReturnsAnAlertReading() {
        assertNull(UsageRepository(context).loadTodayForAlerts())
    }
}
