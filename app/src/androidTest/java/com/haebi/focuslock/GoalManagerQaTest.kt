package com.haebi.focuslock

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.haebi.focuslock.data.GoalConfig
import com.haebi.focuslock.data.GoalHistoryPolicy
import com.haebi.focuslock.data.GoalHistoryStore
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoalManagerQaTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test fun legacyGoalsAndChangeHistoryRemainLocal() {
        val name = "goal_history_test_" + java.util.UUID.randomUUID().toString()
        val prefs = context.getSharedPreferences(name, 0)
        val today = LocalDate.of(2026, 11, 24)
        try {
            prefs.edit().putInt("goal.com.example.app", 45)
                .putBoolean("warn80.com.example.app", true).commit()
            assertTrue(GoalHistoryStore.seedCurrentGoals(prefs))
            assertEquals(45, prefs.getInt("goal.com.example.app", 0))
            assertEquals(1, GoalHistoryStore.read(prefs).size)
            assertFalse(GoalHistoryStore.seedCurrentGoals(prefs))
            val editor = prefs.edit().putInt(GoalHistoryStore.TOTAL_KEY, 180)
            assertTrue(GoalHistoryStore.record(prefs, editor, GoalHistoryPolicy.TOTAL,
                GoalConfig(180), today, "Asia/Seoul"))
            assertTrue(editor.commit())
            assertEquals(180, prefs.getInt(GoalHistoryStore.TOTAL_KEY, 0))
            assertEquals(180, GoalHistoryStore.read(prefs).single { it.scope == GoalHistoryPolicy.TOTAL }.config.minutes)
            val clear = prefs.edit().putInt(GoalHistoryStore.TOTAL_KEY, 0)
            assertTrue(GoalHistoryStore.record(prefs, clear, GoalHistoryPolicy.TOTAL,
                GoalConfig(0), today.plusDays(1), "Asia/Seoul"))
            assertTrue(clear.commit())
            assertEquals(0, GoalHistoryStore.read(prefs).filter { it.scope == GoalHistoryPolicy.TOTAL }.last().config.minutes)
            assertEquals(45, prefs.getInt("goal.com.example.app", 0))
        } finally {
            context.deleteSharedPreferences(name)
        }
    }

    @Test fun settingsCanOpenGoalManagerWithoutChangingUserGoals() {
        val prefs = context.getSharedPreferences("focuslock_ui_prototype", 0)
        val wasCompleted = prefs.getBoolean("onboarding_completed", false)
        prefs.edit().putBoolean("onboarding_completed", true).commit()
        try {
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ActivityScenario.launch<MainActivity>(intent).use {
                val settings = device.wait(Until.findObject(By.res("nav-SETTINGS")), 10_000L)
                    ?: device.wait(Until.findObject(By.desc("설정")), 5_000L)
                assertNotNull("Settings must be reachable", settings)
                settings!!.click()
                val goals = device.wait(Until.findObject(By.text("목표 관리")), 8_000L)
                assertNotNull("Goal manager entry", goals)
                goals!!.click()
                assertTrue(device.wait(Until.hasObject(By.text("전체 앱 사용 목표")), 8_000L))
                repeat(5) {
                    if (!device.hasObject(By.res("whole-goal-save"))) {
                        device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4,
                            device.displayWidth / 2, device.displayHeight / 2, 35)
                        device.waitForIdle(500L)
                    }
                }
                assertTrue(device.wait(Until.hasObject(By.res("whole-goal-save")), 8_000L))
            }
        } finally {
            prefs.edit().putBoolean("onboarding_completed", wasCompleted).apply()
        }
    }
}
