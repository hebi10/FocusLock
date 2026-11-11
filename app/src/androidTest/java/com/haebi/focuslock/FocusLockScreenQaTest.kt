package com.haebi.focuslock

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.StaleObjectException
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import com.haebi.focuslock.data.GoalDayPolicy
import java.time.DayOfWeek
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Captures the actual emulator UI. No mock permission state or screenshot placeholders. */
@RunWith(AndroidJUnit4::class)
class FocusLockScreenQaTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val context = instrumentation.targetContext

    private fun launch(onboardingCompleted: Boolean): ActivityScenario<MainActivity> {
        context.getSharedPreferences("focuslock_ui_prototype", 0)
            .edit().putBoolean("onboarding_completed", onboardingCompleted).commit()
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return ActivityScenario.launch(intent)
    }

    private fun dismissEmulatorSystemUiAnr() {
        repeat(3) {
            val dialog = device.findObject(By.text("System UI isn't responding")) ?: return
            System.err.println("Android emulator System UI ANR detected; selecting Wait for environment recovery")
            val waitButton = device.wait(Until.findObject(By.text("Wait")), 1500L)
            requireNotNull(waitButton) { "System UI ANR dialog has no Wait option" }.click()
            Thread.sleep(2500L)
        }
        check(device.findObject(By.text("System UI isn't responding")) == null) {
            "Android emulator System UI remains unresponsive"
        }
    }

    private fun capture(name: String) {
        dismissEmulatorSystemUiAnr()
        device.waitForIdle(1500L)
        dismissEmulatorSystemUiAnr()
        Thread.sleep(700L)
        val dir = File(context.getExternalFilesDir(null), "qa-captures").apply { mkdirs() }
        assertTrue("Screenshot failed: $name", device.takeScreenshot(File(dir, "$name.png")))
    }

    private fun captureScrolledContent(name: String) {
        val x = device.displayWidth * 2 / 3
        val top = device.displayHeight * 30 / 100
        val bottom = device.displayHeight * 78 / 100
        repeat(4) {
            device.swipe(x, bottom, x, top, 28)
        }
        capture(name)
        repeat(5) {
            device.swipe(x, top, x, bottom, 28)
        }
        device.waitForIdle(1200L)
    }

    private fun tapText(label: String) {
        dismissEmulatorSystemUiAnr()
        // Content descriptions (for icon-only back buttons) must be checked before scrolling.
        var item = device.findObject(By.desc(label))
            ?: device.wait(Until.findObject(By.text(label)), 2500L)
        for (tries in 0 until 5) {
            if (item != null) break
            device.swipe(
                device.displayWidth / 2, device.displayHeight * 3 / 4,
                device.displayWidth / 2, device.displayHeight / 3, 25
            )
            item = device.findObject(By.desc(label))
                ?: device.wait(Until.findObject(By.text(label)), 1700L)
        }
        requireNotNull(item) { "Could not find visible label or description: $label" }
        repeat(3) { attempt ->
            try {
                val current = device.findObject(By.desc(label))
                    ?: device.wait(Until.findObject(By.text(label)), 1500L)
                requireNotNull(current) { "Could not refind visible label or description: $label" }.click()
                device.waitForIdle(1200L)
                return
            } catch (error: StaleObjectException) {
                if (attempt == 2) throw error
                device.waitForIdle(1200L)
            }
        }
    }

    private fun tapTag(label: String) {
        val item = device.wait(Until.findObject(By.res(label)), 3500L)
        requireNotNull(item) { "Could not find UI testTag: $label" }.click()
        device.waitForIdle(1200L)
    }

    @Test
    fun onboardingAndPermissionDeniedScreens() {
        launch(onboardingCompleted = false).use {
            capture("01-onboarding-intro")
            captureScrolledContent("24-onboarding-intro-scroll")
            tapText("시작하기")
            capture("02-onboarding-permission")
            tapText("나중에")
            capture("03-onboarding-goal")
            captureScrolledContent("25-onboarding-goal-scroll")
            tapText("FocusLock 시작")
            capture("04-home-denied")
            assertTrue("Focus CTA must remain available without usage access",
                device.wait(Until.findObject(By.text("집중 시작")), 3500L) != null)
            tapText("사용 분석")
            capture("05-analysis-denied")
            tapText("설정")
            capture("06-settings")
            captureScrolledContent("26-settings-scroll")
            tapText("사용 정보·알림 권한 관리")
            capture("07-permission-management-denied")
            captureScrolledContent("27-permissions-denied-scroll")
            tapText("뒤로")
            tapText("집중")
            capture("08-focus-setup")
            tapText("집중 시작")
            capture("09-focus-running")
            tapText("집중 종료")
            capture("10-focus-stop-confirm")
            tapText("종료")
            capture("11-focus-result")
            assertTrue(
                "Early stopped focus must not claim any earned reward",
                device.wait(Until.hasObject(By.text("완료 조건 미충족 · 지급된 보상 없음")), 4000L)
            )
            tapText("정원 보기")
            capture("12-garden")
            captureScrolledContent("28-garden-scroll")
            tapText("작은 새싹")
            capture("13-garden-item")
        }
    }

    @Test
    fun grantedUsageAndDetailScreens() {
        launch(onboardingCompleted = true).use {
            device.wait(Until.findObject(By.text("오늘의 FocusLock")), 15000L)
            Thread.sleep(1200L)
            capture("14-home-granted")
            assertTrue("Usage total must appear in the home hero",
                device.wait(Until.findObject(By.text("오늘 사용 시간")), 3500L) != null)
            captureScrolledContent("29-home-granted-scroll")
            tapText("사용 분석")
            capture("15-analysis-today")
            captureScrolledContent("30-analysis-today-scroll")
            tapText("7일")
            capture("16-analysis-week")
            captureScrolledContent("31-analysis-week-scroll")
            tapText("30일")
            capture("17-analysis-month")
            captureScrolledContent("32-analysis-month-scroll")
            tapText("오늘")
            tapText("Settings")
            capture("18-app-detail")
            captureScrolledContent("33-app-detail-scroll")
            tapText("사용 목표")
            capture("19-goal-sheet")
            assertTrue(
                "Goal sheet save action must be visible without scrolling",
                device.wait(Until.hasObject(By.text("저장")), 5000L)
            )
            val pref = context.getSharedPreferences("focuslock_ui_prototype", 0)
            val previousDays = if (pref.contains("goalDays.com.android.settings"))
                pref.getInt("goalDays.com.android.settings", GoalDayPolicy.EVERY_DAY)
            else GoalDayPolicy.fromLegacyEveryday(
                pref.getBoolean("everyday.com.android.settings", true)
            )
            tapText("월")
            tapText("저장")
            assertEquals(
                "Changing Monday must persist exact selected notification weekdays",
                GoalDayPolicy.toggle(previousDays, DayOfWeek.MONDAY),
                pref.getInt("goalDays.com.android.settings", -1)
            )
            assertTrue("Goal sheet did not close after saving", device.wait(Until.gone(By.text("사용 목표 설정")), 10000L))
            tapText("뒤로")
            tapText("리포트 보기")
            capture("20-report")
            captureScrolledContent("34-report-scroll")
            tapText("뒤로")
            tapText("설정")
            tapText("다크")
            capture("21-dark-settings")
            captureScrolledContent("35-dark-settings-scroll")
            tapText("홈")
            capture("22-dark-home")
            captureScrolledContent("36-dark-home-scroll")
            tapText("설정")
            tapText("라이트")
            tapText("사용 정보·알림 권한 관리")
            capture("23-permission-management-granted")
            captureScrolledContent("37-permissions-granted-scroll")
        }
    }
}
