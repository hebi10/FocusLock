package com.haebi.focuslock

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FocusLockResponsiveQaTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private val variant = InstrumentationRegistry.getArguments().getString("qa_variant") ?: "unknown"

    private fun launch(): ActivityScenario<MainActivity> {
        context.getSharedPreferences("focuslock_ui_prototype", 0)
            .edit().putBoolean("onboarding_completed", true).commit()
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return ActivityScenario.launch(intent)
    }

    private fun visible(text: String, timeout: Long = 3000L): UiObject2? =
        device.wait(Until.findObject(By.text(text)), timeout)
            ?: device.findObject(By.desc(text))

    private fun find(text: String): UiObject2 {
        visible(text, 6000L)?.let { return it }
        repeat(10) {
            device.swipe(
                device.displayWidth * 2 / 3, device.displayHeight * 3 / 4,
                device.displayWidth * 2 / 3, device.displayHeight / 4, 24
            )
            visible(text, 1500L)?.let { return it }
        }
        throw AssertionError("Required screen action not reachable: " + text + " (" + variant + ")")
    }

    private fun tap(text: String) {
        find(text).click()
        device.waitForIdle(1200L)
    }

    private fun capture(name: String) {
        device.waitForIdle(1000L)
        val directory = File(requireNotNull(context.getExternalFilesDir(null)), "qa-r08")
        assertTrue(directory.isDirectory || directory.mkdirs())
        assertTrue(
            "Unable to capture actual Android UI",
            device.takeScreenshot(File(directory, variant + "-" + name + ".png"))
        )
    }

    private fun assertVisibleAction(text: String) {
        val item = find(text)
        val box = item.visibleBounds
        assertTrue("Action has no visible area: " + text, box.width() > 0 && box.height() > 0)
        assertTrue("Action is outside horizontal display: " + text, box.left >= 0 && box.right <= device.displayWidth)
        assertTrue("Action is outside vertical display: " + text, box.top >= 0 && box.bottom <= device.displayHeight)
    }

    @Test
    fun homeReportSettingsAndPermissionsRemainAccessible() {
        launch().use {
            assertTrue(visible("오늘의 FocusLock", 20000L) != null)
            capture("home")
            tap("리포트 보기")
            assertTrue(visible("사용 리포트", 8000L) != null)
            val option30 = device.wait(Until.findObject(By.res("period-option-30일")), 6000L)
                ?: find("30일")
            option30.click()
            assertTrue(
                "Selecting 30 days must display the 30-day report, not the default 7-day report",
                device.wait(Until.hasObject(By.text("최근 30일 총 사용 시간")), 8000L)
            )
            device.waitForIdle(1800L)
            capture("report-30")
            tap("홈")
            tap("설정")
            assertTrue(visible("사용 정보·알림 권한 관리", 7000L) != null)
            capture("settings")
            tap("사용 정보·알림 권한 관리")
            assertTrue(visible("권한 관리", 6000L) != null)
            capture("permissions")
        }
    }

    @Test
    fun focusStartAndGardenNavigationRemainReachable() {
        launch().use {
            assertTrue(visible("오늘의 FocusLock", 20000L) != null)
            tap("집중")
            assertVisibleAction("집중 시작")
            capture("focus-setup")
            tap("정원 보기")
            assertTrue(visible("나의 정원", 6000L) != null)
            capture("garden")
        }
    }
}
