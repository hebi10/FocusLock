package com.haebi.focuslock

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import java.io.File
import org.junit.Assert.assertTrue

/** Actual Android screen evidence. Do not accept an offscreen accessibility node as a captured screen. */
internal object FinalQaEvidence {
    val enabled: Boolean
        get() = InstrumentationRegistry.getArguments().getString("flfix08_capture") == "true"

    fun visible(device: UiDevice, selector: BySelector): Boolean =
        device.findObjects(selector).any { node ->
            runCatching {
                val bounds = node.visibleBounds
                bounds.width() > 0 && bounds.height() > 0 &&
                    bounds.left >= 0 && bounds.right <= device.displayWidth &&
                    bounds.top >= 0 && bounds.bottom <= device.displayHeight
            }.getOrDefault(false)
        }

    fun capture(name: String, required: List<BySelector>, forbidden: List<BySelector> = emptyList()) {
        if (!enabled) return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val directory = File(requireNotNull(instrumentation.targetContext.getExternalFilesDir(null)),
            "qa-flfix08-captures")
        assertTrue(directory.isDirectory || directory.mkdirs())
        val ready = {
            required.all { visible(device, it) } && forbidden.none { visible(device, it) }
        }
        val deadline = SystemClock.elapsedRealtime() + 12_000L
        var stableSince = 0L
        while (SystemClock.elapsedRealtime() < deadline) {
            device.waitForIdle(500L)
            val now = SystemClock.elapsedRealtime()
            if (ready()) {
                if (stableSince == 0L) stableSince = now
                if (now - stableSince >= 1_500L) break
            } else stableSince = 0L
            SystemClock.sleep(250L)
        }
        assertTrue("Capture $name did not reach a stable visible destination",
            stableSince > 0L && SystemClock.elapsedRealtime() - stableSince >= 1_500L && ready())
        assertTrue("Actual FL-FIX-08 screenshot failed: $name",
            device.takeScreenshot(File(directory, "$name.png")))
        assertTrue("Capture $name changed during screenshot", ready())
        // Accessibility hierarchy is supplemental evidence, not a substitute for inspecting PNG pixels.
        device.dumpWindowHierarchy(File(directory, "$name.xml"))
    }
}
