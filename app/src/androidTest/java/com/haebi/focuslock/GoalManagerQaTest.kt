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
                assertTrue("Home must be loaded", device.wait(Until.hasObject(By.text("오늘의 FocusLock")), 20_000L))
                val settings = device.wait(Until.findObject(By.text("설정")), 6_000L)
                    ?: device.findObject(By.desc("설정"))
                    ?: device.findObject(By.res("nav-SETTINGS"))
                assertNotNull("Settings must be reachable", settings)
                settings!!.click()
                assertTrue("Settings screen must open", device.wait(
                    Until.hasObject(By.text("사용 정보·알림 권한 관리")), 10_000L))
                val goals = device.wait(Until.findObject(By.text("목표 관리")), 8_000L)
                assertNotNull("Goal manager entry", goals)
                goals!!.click()
                assertTrue(device.wait(Until.hasObject(By.text("전체 앱 사용 목표")), 8_000L))
                repeat(6) {
                    if (!device.hasObject(By.res("whole-goal-save")) && !device.hasObject(By.text("전체 목표 저장"))) {
                        device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4,
                            device.displayWidth / 2, device.displayHeight / 2, 35)
                        device.waitForIdle(500L)
                    }
                }
                assertTrue("Whole goal save action must be reachable",
                    device.hasObject(By.res("whole-goal-save")) || device.hasObject(By.text("전체 목표 저장")))
                if (FinalQaEvidence.enabled) {
                    FinalQaEvidence.capture("goal-manager",
                        listOf(By.text("전체 앱 사용 목표"), By.text("전체 목표 저장")),
                        listOf(By.text("사용 정보·알림 권한 관리")))
                    val disclaimer = By.textStartsWith("오늘은 제외합니다.")
                    val dateRow = By.text(java.util.regex.Pattern.compile("[0-9]+월 [0-9]+일 · .+"))
                    // The goal editor and history are one scrolling screen. The save action
                    // may legitimately remain visible; require actual history content instead.
                    fun historyVisible() = FinalQaEvidence.visibleCount(device, disclaimer, 24) >= 1 &&
                        FinalQaEvidence.visibleCount(device, dateRow, 24) >= 2
                    repeat(18) {
                        if (!historyVisible()) {
                            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 5,
                                device.displayWidth / 2, device.displayHeight * 2 / 5, 40)
                            device.waitForIdle(700L)
                        }
                    }
                    repeat(12) {
                        if (!historyVisible()) {
                            device.swipe(device.displayWidth / 2, device.displayHeight * 2 / 5,
                                device.displayWidth / 2, device.displayHeight * 3 / 5, 40)
                            device.waitForIdle(700L)
                        }
                    }
                    FinalQaEvidence.capture("goal-history", listOf(disclaimer, dateRow),
                        listOf(By.text("사용 정보·알림 권한 관리"), By.text("기록 내보내기")),
                        additionalReady = { historyVisible() })
                }
            }
        } finally {
            prefs.edit().putBoolean("onboarding_completed", wasCompleted).apply()
        }
    }
}
