package com.haebi.focuslock

import android.content.Intent
import android.os.SystemClock
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
            tap("리포트")
            assertTrue("The top-level report tab must show the report screen",
                visible("사용 리포트", 8000L) != null)
            var thirtyDayMetric: UiObject2? = null
            for (attempt in 1..3) {
                val option30 = device.wait(
                    Until.findObject(By.res("period-option-30일")), 6000L
                ) ?: find("30일")
                option30.click()
                device.waitForIdle(1200L)
                thirtyDayMetric = visible("최근 30일 총 사용 시간", 3500L)
                if (thirtyDayMetric != null) break
                device.swipe(
                    device.displayWidth / 2, device.displayHeight / 3,
                    device.displayWidth / 2, device.displayHeight * 4 / 5, 28
                )
            }
            val verifiedMetric = requireNotNull(thirtyDayMetric) {
                "Selecting the 30-day period must update the visible report"
            }
            assertTrue("30-day report metric must have a visible frame",
                verifiedMetric.visibleBounds.width() > 0 && verifiedMetric.visibleBounds.height() > 0)
            device.waitForIdle(1800L)
            capture("report-30")
            // Verify full tick text on the same real viewport, including compact 160% font.
            find("사용 시간 추이")
            val ticks = listOf("29", "24", "19", "14", "9", "4", "0")
            fun axisVisible(): Boolean {
                val first = device.findObject(By.res("report-axis-tick-29"))
                val last = device.findObject(By.res("report-axis-tick-0"))
                return first != null && last != null &&
                    first.visibleBounds.height() > 0 && last.visibleBounds.height() > 0
            }
            // Small drags avoid jumping past the axis on compact screens; recover in reverse
            // if the prior section search left the viewport below the chart.
            repeat(10) {
                if (!axisVisible()) {
                    device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 5,
                        device.displayWidth / 2, device.displayHeight * 9 / 20, 40)
                    device.waitForIdle(600L)
                }
            }
            repeat(22) {
                if (!axisVisible()) {
                    device.swipe(device.displayWidth / 2, device.displayHeight * 9 / 20,
                        device.displayWidth / 2, device.displayHeight * 3 / 5, 40)
                    device.waitForIdle(600L)
                }
            }
            assertTrue("30-day axis not reachable ($variant)", axisVisible())
            val bounds = ticks.map { tick ->
                val label = requireNotNull(device.wait(Until.findObject(By.res("report-axis-tick-$tick")), 5000L)) {
                    "Missing 30-day axis tick $tick ($variant)"
                }
                assertTrue("Truncated axis tick $tick", label.text == tick)
                label.visibleBounds.also {
                    assertTrue("Axis tick is not visible: $tick", it.width() > 0 && it.height() > 0)
                    assertTrue("Axis tick outside display: $tick", it.left >= 0 && it.right <= device.displayWidth)
                }
            }
            assertTrue("Axis labels overlap", bounds.zipWithNext().all { (left, right) -> left.right <= right.left })
            capture("report-30-axis")
            // On the full-width and compact phone cases, verify that an actual day can be
            // selected and returned to the latest day without changing its stored statistics.
            if (variant == "api36-phone" || variant == "api36-compact-font160" || variant == "api29-phone" || variant == "api35-phone") {
                fun chartControlsVisible(): Boolean {
                    val prev = device.findObject(By.res("report-chart-prev"))
                    val selected = device.findObject(By.res("report-chart-selected"))
                    val next = device.findObject(By.res("report-chart-next"))
                    return prev != null && selected != null && next != null &&
                        prev.visibleBounds.height() > 0 && selected.visibleBounds.height() > 0 &&
                        next.visibleBounds.height() > 0
                }
                repeat(8) {
                    if (!chartControlsVisible()) {
                        device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 5,
                            device.displayWidth / 2, device.displayHeight * 9 / 20, 40)
                        device.waitForIdle(600L)
                    }
                }
                repeat(12) {
                    if (!chartControlsVisible()) {
                        device.swipe(device.displayWidth / 2, device.displayHeight * 9 / 20,
                            device.displayWidth / 2, device.displayHeight * 3 / 5, 40)
                        device.waitForIdle(600L)
                    }
                }
                assertTrue("Day selection controls must be reachable ($variant)", chartControlsVisible())
                val original = requireNotNull(device.findObject(By.res("report-chart-selected"))).text
                assertTrue("Selected day must have a readable value", !original.isNullOrBlank())
                requireNotNull(device.findObject(By.res("report-chart-prev"))).click()
                device.waitForIdle(1000L)
                val previous = requireNotNull(device.findObject(By.res("report-chart-selected"))).text
                assertTrue("Previous day must change the visible date", previous != original)
                requireNotNull(device.findObject(By.res("report-chart-next"))).click()
                device.waitForIdle(1000L)
                assertTrue("Next day must restore the latest day",
                    requireNotNull(device.findObject(By.res("report-chart-selected"))).text == original)
                capture("report-day-selection")
            }
            tap("홈")
            tap("설정")
            assertTrue(visible("사용 정보·알림 권한 관리", 7000L) != null)
            capture("settings")
            tap("사용 정보·알림 권한 관리")
            val directory = File(requireNotNull(context.getExternalFilesDir(null)), "qa-r08")
            assertTrue(directory.isDirectory || directory.mkdirs())
            fun destinationReady(): Boolean =
                FinalQaEvidence.visible(device, By.text("권한 관리")) &&
                    FinalQaEvidence.visible(device, By.text("개인정보 보호")) &&
                    !FinalQaEvidence.visible(device, By.text("사용 정보·알림 권한 관리"))
            val deadline = SystemClock.elapsedRealtime() + 15_000L
            var stableSince = 0L
            while (SystemClock.elapsedRealtime() < deadline) {
                device.waitForIdle(500L)
                val now = SystemClock.elapsedRealtime()
                if (destinationReady()) {
                    if (stableSince == 0L) stableSince = now
                    if (now - stableSince >= 1500L) break
                } else stableSince = 0L
                SystemClock.sleep(250L)
            }
            val stable = stableSince > 0L && SystemClock.elapsedRealtime() - stableSince >= 1500L && destinationReady()
            if (!stable) {
                device.takeScreenshot(File(directory, "$variant-permissions-failed.png"))
                device.dumpWindowHierarchy(File(directory, "$variant-permissions-failed.xml"))
            }
            assertTrue("Permission destination must be stable before capture ($variant)", stable)
            capture("permissions")
            assertTrue("Permission destination changed during capture ($variant)", destinationReady())
            device.dumpWindowHierarchy(File(directory, "$variant-permissions.xml"))
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
