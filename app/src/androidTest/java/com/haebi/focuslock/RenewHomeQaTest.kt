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

@RunWith(AndroidJUnit4::class)
class RenewHomeQaTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private fun reachable(text: String): UiObject2 {
        device.wait(Until.findObject(By.text(text)), 3500L)?.let { return it }
        repeat(10) {
            device.swipe(
                device.displayWidth * 2 / 3, device.displayHeight * 3 / 4,
                device.displayWidth * 2 / 3, device.displayHeight / 4, 24
            )
            device.wait(Until.findObject(By.text(text)), 700L)?.let { return it }
        }
        throw AssertionError("Expected actual home UI item is unreachable: " + text)
    }

    @Test
    fun homePresentsUsageBeforeOptionalFocusAndAllowsAnalysis() {
        context.getSharedPreferences("focuslock_ui_prototype", 0)
            .edit().putBoolean("onboarding_completed", true).commit()
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ActivityScenario.launch<MainActivity>(intent).use {
            assertTrue(device.wait(Until.hasObject(By.text("오늘의 FocusLock")), 20000L))
            assertTrue("Actual OS usage total should be the primary home metric",
                device.wait(Until.hasObject(By.text("오늘 사용 시간")), 10000L))
            reachable("Android에서 제공한 기기 사용 통계입니다.")
            reachable("많이 사용한 앱")
            reachable("필요할 때 집중하기")
            reachable("집중 시작")
            val navAnalysis = requireNotNull(device.wait(
                Until.findObject(By.res("nav-ANALYSIS")), 5000L))
            navAnalysis.click()
            assertTrue("Analysis tab must remain reachable from the dashboard",
                device.wait(Until.hasObject(By.text("사용 분석")), 9000L))
        }
    }
}
