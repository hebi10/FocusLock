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
class RenewNavigationQaTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun reportsAreTopLevelAndGardenRetainsFocusTab() {
        context.getSharedPreferences("focuslock_ui_prototype", 0)
            .edit().putBoolean("onboarding_completed", true).commit()
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ActivityScenario.launch<MainActivity>(intent).use {
            assertTrue(device.wait(Until.hasObject(By.text("오늘의 FocusLock")), 15000L))
            val report = requireNotNull(device.wait(Until.findObject(By.res("nav-REPORT")), 5000L))
            report.click()
            assertTrue("Report must open as a top-level destination",
                device.wait(Until.hasObject(By.text("사용 리포트")), 7000L))
            assertTrue("Report tab must remain in bottom navigation",
                device.hasObject(By.res("nav-REPORT")))
            assertTrue("Garden must not remain a standalone bottom tab",
                !device.hasObject(By.res("nav-GARDEN")))

            val focus = requireNotNull(device.wait(Until.findObject(By.res("nav-FOCUS")), 5000L))
            focus.click()
            assertTrue(device.wait(Until.hasObject(By.text("집중 모드")), 8000L))
            var garden = device.wait(Until.findObject(By.text("정원 보기")), 2000L)
            repeat(7) {
                if (garden == null) {
                    device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4,
                        device.displayWidth / 2, device.displayHeight / 3, 25)
                    garden = device.wait(Until.findObject(By.text("정원 보기")), 800L)
                }
            }
            requireNotNull(garden) { "Nested garden action must be available inside Focus" }.click()
            assertTrue(device.wait(Until.hasObject(By.text("나의 정원")), 6000L))
            assertTrue("Nested garden must preserve Focus as the selected main tab",
                device.hasObject(By.res("nav-FOCUS")))
            device.pressBack()
            assertTrue("Back from nested garden must return to Focus setup",
                device.wait(Until.hasObject(By.text("집중 모드")), 7000L))
        }
    }
}
