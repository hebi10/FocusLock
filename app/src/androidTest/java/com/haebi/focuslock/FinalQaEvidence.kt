package com.haebi.focuslock

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import org.junit.Assert.assertTrue

/** Additional actual-screen evidence for the final emulator run, separate from UI-R00. */
internal object FinalQaEvidence {
    val enabled: Boolean
        get() = InstrumentationRegistry.getArguments().getString("flfix08_capture") == "true"

    fun capture(name: String) {
        if (!enabled) return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val directory = File(requireNotNull(instrumentation.targetContext.getExternalFilesDir(null)),
            "qa-flfix08-captures")
        assertTrue(directory.isDirectory || directory.mkdirs())
        device.waitForIdle(1500L)
        assertTrue("Actual FL-FIX-08 screenshot failed: $name",
            device.takeScreenshot(File(directory, "$name.png")))
    }
}
