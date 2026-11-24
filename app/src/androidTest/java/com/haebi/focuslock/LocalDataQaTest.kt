package com.haebi.focuslock

import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.haebi.focuslock.data.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class LocalDataQaTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    private class Clock : FocusClock {
        var elapsed = 50_000L
        var wall = 1_800_000_000_000L
        override fun monotonicMillis() = elapsed
        override fun wallMillis() = wall
        override fun bootCount() = 1
        fun advance(ms: Long) { elapsed += ms; wall += ms }
    }

    @Test fun exportAndSelectiveDeletionPreserveActiveAndUnrelatedData() = runBlocking {
        val name = "local_data_test_" + java.util.UUID.randomUUID().toString()
        val prefs = context.getSharedPreferences(name, 0)
        val db = Room.inMemoryDatabaseBuilder(context, FocusDatabase::class.java).build()
        try {
            val clock = Clock()
            val focus = FocusSessionStore(db, clock)
            val data = LocalDataStore(prefs, focus)
            val today = LocalDate.now()
            val zone = ZoneId.systemDefault().id
            val edit = prefs.edit().putInt("goal.com.example.app", 60)
                .putString("goal_label.com.example.app", "Example app")
                .putBoolean("warn80.com.example.app", true)
                .putString("theme", "다크")
                .putString("placed_garden_item_id", "sprout")
                .putBoolean("goal_alert.1.com.example.app.80", true)
                .putString("unrelated_test_marker", "not-exported")
            GoalHistoryStore.record(prefs, edit, "com.example.app", GoalConfig(60), today.minusDays(3), zone)
            GoalHistoryStore.record(prefs, edit, "com.example.removed", GoalConfig(0), today.minusDays(2), zone)
            assertTrue(edit.commit())

            val completed = focus.start(5)
            clock.advance(300_000)
            assertNotNull(focus.finish(completed.id)?.reward)
            val stopped = focus.start(5)
            clock.advance(11_000)
            assertNull(focus.finish(stopped.id)?.reward)
            val active = focus.start(25)
            clock.advance(2000)

            val exported = JSONObject(data.exportJson())
            assertEquals("FocusLock-local-records", exported.getString("format"))
            assertEquals(1, exported.getJSONArray("goals").length())
            assertEquals(2, exported.getJSONArray("goalHistory").length())
            assertEquals(2, exported.getJSONArray("focusHistory").length())
            assertFalse(exported.getBoolean("includesAndroidUsageHistory"))
            assertFalse(exported.getBoolean("includesActiveFocusSession"))
            assertFalse(exported.toString().contains(active.id))
            assertFalse(exported.toString().contains("not-exported"))
            assertEquals("sprout", exported.getString("gardenItem"))
            assertNull("Cannot delete while active", data.clearFocusHistory())
            assertEquals(2, focus.historySnapshot().sessions.size)
            assertEquals(30, focus.metrics().experience)

            assertTrue(data.clearGoalHistory())
            val baseline = GoalHistoryStore.read(prefs)
            assertEquals(1, baseline.size)
            assertEquals(today, baseline.single().date)
            assertEquals("com.example.app", baseline.single().scope)
            assertEquals(60, prefs.getInt("goal.com.example.app", 0))
            assertTrue(prefs.getBoolean("warn80.com.example.app", false))
            assertTrue(prefs.getBoolean("goal_alert.1.com.example.app.80", false))
            assertEquals("다크", prefs.getString("theme", ""))
            assertEquals(30, focus.metrics().experience)

            focus.togglePause(active.id)
            assertNull("Paused session also blocks deletion", data.clearFocusHistory())
            focus.finish(active.id)
            assertEquals(3, data.clearFocusHistory())
            assertEquals(0, focus.historySnapshot().sessions.size)
            assertEquals(0, focus.historySnapshot().rewards.size)
            assertEquals(0, focus.metrics().experience)
            assertEquals(0, focus.metrics().points)
            assertEquals(0, focus.metrics().report.last30.completedCount)
            assertEquals(60, prefs.getInt("goal.com.example.app", 0))
            assertEquals(1, GoalHistoryStore.read(prefs).size)
        } finally {
            db.close()
            context.deleteSharedPreferences(name)
        }
    }

    private fun findText(text: String): UiObject2 {
        device.wait(Until.findObject(By.text(text)), 2500L)?.let { return it }
        repeat(12) {
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4,
                device.displayWidth / 2, device.displayHeight * 2 / 5, 40)
            device.wait(Until.findObject(By.text(text)), 600L)?.let { return it }
        }
        error("Expected visible text: $text")
    }

    @Test fun dataScreenHasExportAndCancellableDeletion() {
        val prefs = context.getSharedPreferences("focuslock_ui_prototype", 0)
        val completed = prefs.getBoolean("onboarding_completed", false)
        prefs.edit().putBoolean("onboarding_completed", true).commit()
        try {
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ActivityScenario.launch<MainActivity>(intent).use {
                assertTrue(device.wait(Until.hasObject(By.text("오늘의 FocusLock")), 20_000L))
                val settings = device.wait(Until.findObject(By.text("설정")), 6_000L)
                    ?: device.findObject(By.desc("설정"))
                requireNotNull(settings).click()
                assertTrue(device.wait(Until.hasObject(By.text("사용 정보·알림 권한 관리")), 10_000L))
                findText("데이터 관리").click()
                assertTrue(device.wait(Until.hasObject(By.text("기록 내보내기")), 8_000L))
                assertTrue(findText("JSON 내보내기").isEnabled)
                findText("목표 이력 삭제").click()
                assertTrue(device.wait(Until.hasObject(By.text("목표 이력을 삭제할까요?")), 8_000L))
                findText("취소").click()
                assertTrue(device.wait(Until.gone(By.text("목표 이력을 삭제할까요?")), 5_000L))
                findText("집중 기록 및 보상 삭제").click()
                assertTrue(device.wait(Until.hasObject(By.text("집중 기록 및 보상을 삭제할까요?")), 8_000L))
                findText("취소").click()
                assertTrue(device.wait(Until.gone(By.text("집중 기록 및 보상을 삭제할까요?")), 5_000L))
            }
        } finally { prefs.edit().putBoolean("onboarding_completed", completed).apply() }
    }
}
