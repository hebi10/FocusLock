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
                val note = requireNotNull(device.wait(Until.findObject(By.res("reference-home-note")), 6_000L))
                note.click()
                assertTrue("Full source and measurement notice must remain reachable",
                    device.wait(Until.hasObject(By.textContains("Android 앱별 포그라운드 시간 합계입니다.")), 5_000L))
                requireNotNull(device.findObject(By.res("reference-home-note"))).click()
                capture("ANALYSIS", "사용 분석", "02-analysis", "총 사용 시간")
                capture("REPORT", "리포트", "03-report", "최근 7일 총 사용 시간")
                capture("FOCUS", "집중", "04-focus", "집중 시작")
                capture("SETTINGS", "설정", "05-settings", "사용 정보·알림 권한 관리")
            }
        } finally {
            prefs.edit().putBoolean("onboarding_completed", completed).putString("theme", previousTheme).commit()
        }
    }

    @Test fun hourlySelectionShowsRealValueOrUnknown() {
        val prefs = context.getSharedPreferences("focuslock_ui_prototype", 0)
        val completed = prefs.getBoolean("onboarding_completed", false)
        val previousTheme = prefs.getString("theme", "시스템")
        prefs.edit().putBoolean("onboarding_completed", true).putString("theme", "라이트").commit()
        try {
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ActivityScenario.launch<MainActivity>(intent).use {
                val nav = device.wait(Until.findObject(By.res("nav-ANALYSIS")), 4_000L)
                    ?: device.wait(Until.findObject(By.desc("사용 분석")), 5_000L)
                    ?: device.wait(Until.findObject(By.text("사용 분석")), 8_000L)
                requireNotNull(nav) { "Analysis navigation unavailable" }.click()
                assertTrue(device.wait(Until.hasObject(By.text("총 사용 시간")), 20_000L))
                val directory = File(requireNotNull(context.getExternalFilesDir(null)), "qa-reference-design")
                assertTrue(directory.isDirectory || directory.mkdirs())
                fun evidence(name: String) {
                    device.takeScreenshot(File(directory, "$name.png"))
                    device.dumpWindowHierarchy(File(directory, "$name.xml"))
                }
                val overview = device.wait(Until.findObject(By.res("reference-hour-overview")), 8_000L)
                if (overview == null) {
                    assertTrue("Missing hourly overview must be explained by the actual data state",
                        device.hasObject(By.textStartsWith("시간대별 사용 이벤트를 확인할 수 없습니다.")) ||
                        device.hasObject(By.text("이 기간에 그래프로 표시할 사용 기록이 없습니다.")))
                    evidence("hourly-unavailable")
                    return@use
                }
                fun revealHour(index: Int): androidx.test.uiautomator.UiObject2 {
                    val tag = "analysis-chart-bar-$index"
                    repeat(8) {
                        val chip = device.wait(Until.findObject(By.res(tag)), 1_000L)
                        if (chip != null && chip.visibleBounds.width() >= 100 && chip.visibleBounds.height() >= 80) return chip
                        device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4,
                            device.displayWidth / 2, device.displayHeight / 2, 45)
                        device.waitForIdle(700L)
                    }
                    error("Hour $index does not have a reachable touch target")
                }
                fun verifyHour(index: Int) {
                    val tag = "analysis-chart-bar-$index"
                    val initialChip = revealHour(index)
                    val prefix = "${index}시, "
                    val description = initialChip.contentDescription?.takeIf { it.startsWith(prefix) }
                        ?: device.findObject(By.descStartsWith(prefix))?.contentDescription
                    val expectedValue = requireNotNull(description) {
                        "No real observation label for hour $index"
                    }.removePrefix(prefix)
                    assertTrue("Hour value must not be blank", expectedValue.isNotBlank())
                    val expected = "${index}시 · $expectedValue"
                    fun selected(): Boolean {
                        val label = device.findObject(By.res("analysis-chart-selected"))
                        val chip = device.findObject(By.res(tag))
                        return label?.text == expected && chip?.isSelected == true
                    }
                    // Tap an actual visible control, reacquiring bounds after any scroll or recompose.
                    repeat(3) {
                        if (!selected()) {
                            val bounds = revealHour(index).visibleBounds
                            device.click(bounds.centerX(), bounds.centerY())
                            val deadline = SystemClock.elapsedRealtime() + 3_000L
                            while (SystemClock.elapsedRealtime() < deadline && !selected()) {
                                device.waitForIdle(300L)
                                SystemClock.sleep(150L)
                            }
                        }
                    }
                    if (!selected()) {
                        evidence("hourly-selection-failed-$index")
                        System.err.println("Hour $index expected: $expected; actual: " +
                            device.findObject(By.res("analysis-chart-selected"))?.text +
                            "; selected: " + device.findObject(By.res(tag))?.isSelected)
                    }
                    assertTrue("Hour $index must be selected and expose its own real value or unknown state", selected())
                    evidence("hourly-selection-$index")
                }
                // Two different selectable hours exercise both the real touch path and selection change.
                // Their display values are read from actual Android usage, not a seeded usage fixture.
                verifyHour(0)
                verifyHour(1)
            }
        } finally {
            prefs.edit().putBoolean("onboarding_completed", completed).putString("theme", previousTheme).commit()
        }
    }
}
