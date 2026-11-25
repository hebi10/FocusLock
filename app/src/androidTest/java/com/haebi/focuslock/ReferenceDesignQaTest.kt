package com.haebi.focuslock

import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Capture real application pixels with the normal Android usage repository; no demo statistics. */
@RunWith(AndroidJUnit4::class)
class ReferenceDesignQaTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test fun fiveMainScreensCaptureActualUi() {
        val prefs = context.getSharedPreferences("focuslock_ui_prototype", 0)
        val completed = prefs.getBoolean("onboarding_completed", false)
        val previousTheme = prefs.getString("theme", "시스템")
        prefs.edit().putBoolean("onboarding_completed", true).putString("theme", "라이트").commit()
        try {
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ActivityScenario.launch<MainActivity>(intent).use {
                val directory = File(requireNotNull(context.getExternalFilesDir(null)), "qa-reference-design")
                assertTrue(directory.isDirectory || directory.mkdirs())
                fun capture(tab: String, label: String, name: String, required: String) {
                    val nav = device.wait(Until.findObject(By.res("nav-$tab")), 3_000L)
                        ?: device.wait(Until.findObject(By.desc(label)), 5_000L)
                        ?: device.wait(Until.findObject(By.text(label)), 7_000L)
                    requireNotNull(nav) { "Navigation tab not reachable: $tab" }.click()
                    assertTrue("Reference page not loaded: $name", device.wait(Until.hasObject(By.text(required)), 20_000L))
                    device.waitForIdle(1500L)
                    SystemClock.sleep(1500L)
                    assertTrue("Required page label not visible: $name", FinalQaEvidence.visible(device, By.text(required)))
                    assertTrue(device.takeScreenshot(File(directory, "$name.png")))
                    device.dumpWindowHierarchy(File(directory, "$name.xml"))
                }
                capture("HOME", "홈", "01-home", "오늘 사용 시간")
                capture("ANALYSIS", "사용 분석", "02-analysis", "총 사용 시간")
                capture("REPORT", "리포트", "03-report", "최근 7일 총 사용 시간")
                capture("FOCUS", "집중", "04-focus", "집중 시작")
                capture("SETTINGS", "설정", "05-settings", "사용 정보·알림 권한 관리")
            }
        } finally {
            prefs.edit().putBoolean("onboarding_completed", completed).putString("theme", previousTheme).commit()
        }
    }
}
