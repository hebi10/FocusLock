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
            // FL-DESIGN-01 keeps the qualifier visible and the full source notice expandable.
            assertTrue("The collapsed usage summary must identify Android observation",
                device.wait(Until.hasObject(By.text("오늘 진행 중 · Android 관측 합계")), 5000L))
            val measurement = requireNotNull(device.wait(
                Until.findObject(By.res("reference-home-note")), 6000L
            )) { "Home measurement details must remain reachable" }
            measurement.click()
            assertTrue("Expanding must disclose the original Android usage source",
                device.wait(Until.hasObject(By.textContains("Android에서 제공한 기기 사용 통계입니다.")), 6000L))
            assertTrue("Expanding must disclose the foreground aggregation limitation",
                device.wait(Until.hasObject(By.textContains("Android 앱별 포그라운드 시간 합계입니다.")), 5000L))
            assertTrue("Expanding must disclose the incomplete-day comparison limitation",
                device.wait(Until.hasObject(By.textContains("어제 전체 사용 시간과 직접 비교하지 않습니다.")), 5000L))
            val collapse = requireNotNull(device.wait(
                Until.findObject(By.res("reference-home-note")), 5000L
            )) { "Home measurement details must remain collapsible" }
            collapse.click()
            assertTrue("Collapse must hide the detailed notice",
                device.wait(Until.gone(By.textContains("Android에서 제공한 기기 사용 통계입니다.")), 5000L))
            assertTrue("The Android observation summary must remain visible after collapse",
                device.wait(Until.hasObject(By.text("오늘 진행 중 · Android 관측 합계")), 5000L))
            reachable("많이 사용한 앱")
            reachable("필요할 때 집중하기")
            reachable("집중 시작")
            val navAnalysis = requireNotNull(
                device.wait(Until.findObject(By.desc("사용 분석")), 5000L)
                    ?: device.wait(Until.findObject(By.text("사용 분석")), 3000L)
            ) { "Analysis bottom navigation must remain accessible" }
            navAnalysis.click()
            assertTrue("Analysis tab must remain reachable from the dashboard",
                device.wait(Until.hasObject(By.text("사용 분석")), 9000L))
        }
    }
}
