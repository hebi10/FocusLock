package com.haebi.focuslock

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusLockOnboardingQaTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    private fun launchFresh(): ActivityScenario<MainActivity> {
        context.getSharedPreferences("focuslock_ui_prototype", 0)
            .edit().putBoolean("onboarding_completed", false).commit()
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return ActivityScenario.launch(intent)
    }

    private fun visible(text: String): Boolean =
        device.wait(Until.findObject(By.text(text)), 8000L) != null

    private fun visibleAfterScroll(text: String): Boolean {
        if (visible(text)) return true
        repeat(9) {
            device.swipe(
                device.displayWidth * 2 / 3,
                device.displayHeight * 3 / 4,
                device.displayWidth * 2 / 3,
                device.displayHeight / 4,
                24
            )
            if (device.wait(Until.hasObject(By.text(text)), 1200L)) return true
        }
        return false
    }

    private fun tap(text: String) {
        val item = device.wait(Until.findObject(By.text(text)), 8000L)
        requireNotNull(item) { "Onboarding action not visible: $text" }.click()
        device.waitForIdle(1200L)
    }

    @Test
    fun deniedPermissionAndNoGoalStillEnterUsableHome() {
        launchFresh().use {
            assertTrue(visible("내 사용 습관을 한눈에"))
            tap("시작하기")
            assertTrue(visible("사용 정보 접근 미허용"))
            tap("나중에")
            assertTrue(visible("관리할 앱 선택"))
            assertTrue(visible("FocusLock 시작"))
            tap("나중에 설정")
            assertTrue(context.getSharedPreferences("focuslock_ui_prototype", 0)
                .getBoolean("onboarding_completed", false))
            assertTrue(
                "Optional focus start must remain reachable on the redesigned home",
                visibleAfterScroll("집중 시작")
            )
        }
    }

    @Test
    fun permissionSettingsReturnRefreshesStepAndGoalIsSkippable() {
        launchFresh().use {
            tap("시작하기")
            assertTrue(visible("사용 정보 접근 미허용"))
            tap("권한 설정 열기")
            assertTrue("Android usage-access settings did not open",
                device.wait(Until.hasObject(By.pkg("com.android.settings")), 10000L))
            device.executeShellCommand("appops set com.haebi.focuslock GET_USAGE_STATS allow")
            device.pressBack()
            assertTrue("Permission should update when returning from Android Settings",
                visible("사용 정보 접근 허용됨"))
            tap("목표 설정으로")
            assertTrue(visible("관리할 앱 선택"))
            tap("FocusLock 시작")
            assertTrue(
                "Optional focus start must remain reachable on the redesigned home",
                visibleAfterScroll("집중 시작")
            )
        }
    }
}
