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
import java.io.File

@RunWith(AndroidJUnit4::class)
class FocusLockAnalysisQaTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    private fun launch(): ActivityScenario<MainActivity> {
        context.getSharedPreferences("focuslock_ui_prototype", 0)
            .edit().putBoolean("onboarding_completed", true).commit()
        return ActivityScenario.launch(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    private fun capture(name: String) {
        device.waitForIdle(1500L)
        val directory = File(context.getExternalFilesDir(null), "qa-r04-captures").apply { mkdirs() }
        assertTrue("UI-R04 real Android capture missing: $name",
            device.takeScreenshot(File(directory, "$name.png")))
    }

    private fun findAfterScroll(resourceId: String): androidx.test.uiautomator.UiObject2 {
        repeat(7) {
            device.findObject(By.res(resourceId))?.let { return it }
            device.swipe(
                device.displayWidth * 3 / 4, device.displayHeight * 75 / 100,
                device.displayWidth * 3 / 4, device.displayHeight * 32 / 100, 30
            )
        }
        return requireNotNull(device.findObject(By.res(resourceId))) {
            "Unable to find visible analysis UI resource $resourceId"
        }
    }

    private fun findTextAfterScroll(label: String): androidx.test.uiautomator.UiObject2 {
        repeat(7) {
            device.findObject(By.text(label))?.let { return it }
            device.swipe(
                device.displayWidth * 3 / 4, device.displayHeight * 75 / 100,
                device.displayWidth * 3 / 4, device.displayHeight * 32 / 100, 30
            )
        }
        return requireNotNull(device.findObject(By.text(label))) {
            "Unable to find visible analysis text: $label"
        }
    }

    private fun selectPeriod(period: String) {
        val tag = "period-option-$period"
        repeat(7) {
            device.findObject(By.res(tag))?.let { option ->
                option.click()
                device.waitForIdle(1200L)
                return
            }
            device.swipe(
                device.displayWidth / 2, device.displayHeight * 27 / 100,
                device.displayWidth / 2, device.displayHeight * 75 / 100, 25
            )
        }
        requireNotNull(device.findObject(By.res(tag))) { "Period tab unavailable: $tag" }.click()
        device.waitForIdle(1200L)
    }

    @Test
    fun usageAnalysisSearchSortAndPeriodAreOperableWithRealAndroidData() {
        launch().use {
            val analysis = device.wait(Until.findObject(By.text("사용 분석")), 15000L)
            requireNotNull(analysis).click()
            assertTrue("Android usage summary missing",
                device.wait(Until.hasObject(By.text("총 사용 시간")), 15000L))
            capture("r04-analysis-today")
            val search = findAfterScroll("analysis-search")
            search.click()
            search.setText("focuslock-no-such-installed-app")
            device.pressBack()
            val emptyResult = findTextAfterScroll("검색어나 카테고리에 맞는 앱이 없습니다.")
            assertTrue("Search must show a truthful empty result", emptyResult.isEnabled)
            capture("r04-analysis-search-empty")

            findTextAfterScroll("홈").click()
            val analysisTab = device.wait(Until.findObject(By.text("사용 분석")), 5000L)
            requireNotNull(analysisTab).click()
            findTextAfterScroll("정렬: 사용 시간").click()
            val byName = device.wait(Until.findObject(By.text("이름")), 5000L)
            requireNotNull(byName).click()
            assertTrue("Name sort cannot be selected",
                device.wait(Until.hasObject(By.text("정렬: 이름")), 5000L))
            capture("r04-analysis-sort-name")

        }
    }

    @Test
    fun periodTabsShowWeeklyAndMonthlyCharts() {
        launch().use {
            val analysis = device.wait(Until.findObject(By.text("사용 분석")), 15000L)
            requireNotNull(analysis) { "Analysis tab unavailable" }.click()
            assertTrue(device.wait(Until.hasObject(By.text("총 사용 시간")), 15000L))
            selectPeriod("7일")
            assertTrue(device.wait(Until.hasObject(By.text("일별 사용 시간")), 5000L))
            capture("r04-analysis-week")
            selectPeriod("30일")
            assertTrue(device.wait(Until.hasObject(By.text("일별 사용 시간")), 5000L))
            capture("r04-analysis-month")
        }
    }
}
